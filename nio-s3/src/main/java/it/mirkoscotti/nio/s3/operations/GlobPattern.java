package it.mirkoscotti.nio.s3.operations;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * @author mirko.scotti
 * @version Dec 30, 2025
 */
public final class GlobPattern
{

	private final String pattern;

	private GlobPattern(String pattern)
	{
		this.pattern = pattern;
	}

	public static GlobPattern of(String glob)
	{
		return Optional.ofNullable(glob)
					   .map(GlobPattern::new)
					   .orElseThrow(() -> new NullPointerException("Missing glob pattern."));
	}

	public Pattern toRegexPattern()
	{
		return Pattern.compile(toRegex());
	}

	private String toRegex()
	{
		return "^%s$".formatted(parseGlob(0).regex());
	}

	private ParseState parseGlob(int start)
	{
		return Stream.iterate(ParseState.initial(start), this::parseGlob)
					 .takeWhile(Predicate.not(Objects::isNull))
					 .reduce((item1, item2) -> item2)
					 .orElseGet(() -> ParseState.initial(start));
	}

	private ParseState parseGlob(ParseState current)
	{
		return Optional.of(current)
					   .filter(Predicate.not(ParseState::shouldSkip))
					   .map(this::processPosition)
					   .orElseGet(() -> Optional.of(current))
					   .map(ParseState::advance)
					   .orElse(null);
	}

	private Optional<ParseState> processPosition(ParseState current)
	{
		return Optional.ofNullable(current)
					   .map(ParseState::position)
					   .filter(item -> item < pattern.length())
					   .map(pattern::charAt)
					   .map(item -> processPosition(current, item));
	}

	private ParseState processPosition(ParseState current, char character)
	{
		return switch (character)
		{
			case '\\' -> handleEscape(current);
			case '*' -> handleStar(current);
			case '?' -> current.append("[^/]");
			case '[' -> handleBracket(current);
			case '{' -> handleBrace(current);
			case '.' -> current.append("\\.");
			case '(' -> current.append("\\(");
			case ')' -> current.append("\\)");
			case '+' -> current.append("\\+");
			case '|' -> current.append("\\|");
			case '^' -> current.append("\\^");
			case '$' -> current.append("\\$");
			default -> current.append(character);
		};
	}

	private ParseState handleEscape(ParseState current)
	{
		return Optional.of(current.position() + 1)
					   .filter(item -> item < pattern.length())
					   .map(pattern::charAt)
					   .map(String::valueOf)
					   .map(Pattern::quote)
					   .map(current::append)
					   .map(item -> item.skip(1))
					   .orElseGet(() -> current.append("\\\\"));
	}

	private ParseState handleStar(ParseState current)
	{
		return Optional.of(current.position() + 1)
					   .filter(item -> item < pattern.length() && pattern.charAt(item) == '*')
					   .map(item -> current.append(".*").skip(1))
					   .orElseGet(() -> current.append("[^/]*"));
	}

	private ParseState handleBracket(ParseState current)
	{
		var position = current.position();
		return IntStream.range(position + 1, pattern.length())
						.filter(i -> !isEscaped(i) && pattern.charAt(i) == ']')
						.boxed()
						.findFirst()
						.map(item -> processClosingBracket(current, position, item))
						.orElseGet(() -> current.append("\\["));
	}

	private ParseState processClosingBracket(ParseState current,
											 int globPosition,
											 int bracketPosition)
	{
		var regex = Optional.of(pattern.substring(globPosition + 1, bracketPosition))
							.filter(Predicate.not(String::isEmpty))
							.map(this::convertBracketExpression)
							.orElse("\\[\\]");
		return current.append(regex).skip(bracketPosition - globPosition);
	}

	private String convertBracketExpression(String content)
	{
		var negated = content.charAt(0) == '!' || content.charAt(0) == '^';
		var inner = negated ? content.substring(1) : content;
		var escaped = IntStream.range(0, inner.length())
							   .mapToObj(i -> escapeBracket(inner, i))
							   .reduce("", String::concat);
		return "[%s%s]".formatted(negated ? "^" : "", escaped);
	}

	private String escapeBracket(String content, int i)
	{
		char c = content.charAt(i);
		return switch (c)
		{
			case '\\' -> i + 1 < content.length() ? "\\" + content.charAt(i + 1) : "\\\\";
			case ']' -> "\\]";
			case '^' -> i == 0 ? "\\^" : String.valueOf(c);
			default -> String.valueOf(c);
		};
	}

	private ParseState handleBrace(ParseState current)
	{
		var position = current.position();
		return IntStream.range(position + 1, pattern.length())
						.boxed()
						.reduce(new BraceSearchState(Optional.empty(), 1, false),
								(item1, item2) -> item1.found()
									? item1
									: item1.process(pattern.charAt(item2)),
								(item1, item2) -> item2)
						.result()
						.map(depth -> position + depth)
						.map(item -> processClosingBrace(current, position, item))
						.orElseGet(() -> current.append("\\{"));
	}

	private ParseState processClosingBrace(ParseState current, int globPosition, int bracePosition)
	{
		String content = pattern.substring(globPosition + 1, bracePosition);
		String regex = convertBraceExpression(content);
		return current.append(regex).skip(bracePosition - globPosition);
	}

	private String convertBraceExpression(String content)
	{
		var split = splitBrace(content).stream()
									   .map(GlobPattern::new)
									   .map(GlobPattern::toRegex)
									   .reduce((item1, item2) -> String.join("|", item1, item2))
									   .orElse("");
		return "(?:%s)".formatted(split);
	}

	private List<String> splitBrace(String content)
	{
		return IntStream.range(0, content.length())
						.boxed()
						.reduce(new BraceSplitState(List.of(""), 0, false),
								(state, i) -> state.process(content.charAt(i)), (s1, s2) -> s2)
						.alternatives();
	}

	private boolean isEscaped(int position)
	{
		return position > 0 && pattern.charAt(position - 1) == '\\' && !isEscaped(position - 1);
	}

	private static record ParseState(String regex, int position, int charactersToSkip)
	{

		static ParseState initial(int position)
		{
			return new ParseState("", position, 0);
		}

		boolean shouldSkip()
		{
			return charactersToSkip > 0;
		}

		ParseState skip(int count)
		{
			return new ParseState(regex, position, count);
		}

		ParseState advance()
		{
			return new ParseState(regex, position + 1, Math.max(0, charactersToSkip - 1));
		}

		ParseState append(char character)
		{
			return append(String.valueOf(character));
		}

		ParseState append(String fragment)
		{
			return new ParseState(regex + fragment, position, Math.max(0, charactersToSkip - 1));
		}
	}

	private record BraceSearchState(Optional<Integer> foundAt, int depth, boolean escaped)
	{

		BraceSearchState process(char character)
		{
			return escaped
				? new BraceSearchState(foundAt, depth, false)
				: processNotEscaped(character);
		}

		boolean found()
		{
			return foundAt.isPresent();
		}

		Optional<Integer> result()
		{
			return foundAt;
		}

		private BraceSearchState processNotEscaped(char character)
		{
			return switch (character)
			{
				case '\\' -> new BraceSearchState(foundAt, depth, true);
				case '{' -> new BraceSearchState(foundAt, depth + 1, false);
				case '}' -> depth == 1
					? new BraceSearchState(Optional.of(depth), 0, false)
					: new BraceSearchState(foundAt, depth - 1, false);
				default -> this;
			};
		}
	}

	private record BraceSplitState(List<String> alternatives, int depth, boolean escaped)
	{

		BraceSplitState process(char character)
		{
			return escaped
				? appendToLast(String.valueOf(character), false)
				: processNotEscaped(character);
		}

		private BraceSplitState appendToLast(String s, boolean escaped)
		{
			var newList = alternatives.subList(0, alternatives.size() - 1);
			var last = alternatives.get(alternatives.size() - 1) + s;
			var result = new java.util.ArrayList<>(newList);
			result.add(last);
			return new BraceSplitState(List.copyOf(result), depth, escaped);
		}

		private BraceSplitState addAlternative()
		{
			var result = new java.util.ArrayList<>(alternatives);
			result.add("");
			return new BraceSplitState(List.copyOf(result), depth, false);
		}

		private BraceSplitState incrementDepth()
		{
			return new BraceSplitState(alternatives, depth + 1, false);
		}

		private BraceSplitState decrementDepth()
		{
			return new BraceSplitState(alternatives, depth - 1, false);
		}

		private BraceSplitState processNotEscaped(char character)
		{
			return switch (character)
			{
				case '\\' -> new BraceSplitState(alternatives, depth, true);
				case '{' -> appendToLast(String.valueOf(character), false).incrementDepth();
				case '}' -> appendToLast(String.valueOf(character), false).decrementDepth();
				case ',' -> depth == 0
					? addAlternative()
					: appendToLast(String.valueOf(character), false);
				default -> appendToLast(String.valueOf(character), false);
			};
		}
	}
}
