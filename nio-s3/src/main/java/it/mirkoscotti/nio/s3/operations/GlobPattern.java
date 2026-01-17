package it.mirkoscotti.nio.s3.operations;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * Same matching rules as in <a href="https://rapidtoolset.com/it/tool/glob-pattern-tester"/>
 *
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
		Objects.requireNonNull(glob, () -> "Missing glob pattern.");
		return new GlobPattern(glob);
	}

	public String toRegex()
	{
		var regex = toInternalRegex().map(item -> !item.isEmpty() ? item.concat("/?") : item)
									 .map(this::appendPipe)
									 .orElse("");
		return "^%s$".formatted(regex);
	}

	private Optional<String> toInternalRegex()
	{
		var current = Optional.of(new ParseState("", 0, 0, true));
		return Stream.iterate(current, this::parseGlob)
					 .takeWhile(Optional::isPresent)
					 .reduce((item1, item2) -> item2)
					 .flatMap(Function.identity())
					 .map(ParseState::regex);
	}

	private Optional<ParseState> parseGlob(Optional<ParseState> current)
	{
		return current.filter(Predicate.not(ParseState::shouldSkip))
					  .map(this::processPosition)
					  .orElse(current)
					  .map(ParseState::advance);
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
		return Optional.of(countStars(current))
					   .filter(item -> item > 1)
					   .map(current::skip)
					   .map(item -> item.appendStar(pattern.startsWith("*") ? ".*" : ".+"))
					   .orElseGet(() -> current.appendStar("[^/]+"));
	}

	private int countStars(ParseState current)
	{
		return (int) IntStream.range(current.position(), pattern.length())
							  .takeWhile(i -> pattern.charAt(i) == '*')
							  .count();
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
		return current.append(regex).skip(bracketPosition - globPosition + 1);
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
						.reduce(new BraceSearchState(Optional.empty(), false),
								(item1,
								 item2) -> item1.found() ? item1 : item1.process(pattern, item2),
								(item1, item2) -> item2)
						.result()
						.map(item -> processClosingBrace(current, position, item))
						.orElseGet(() -> current.append("\\{"));
	}

	private ParseState processClosingBrace(ParseState current, int globPosition, int bracePosition)
	{
		String content = pattern.substring(globPosition + 1, bracePosition);
		String regex = convertBraceExpression(content);
		return current.append(regex).skip(bracePosition - globPosition + 1);
	}

	private String convertBraceExpression(String content)
	{
		var split = splitBrace(content).stream()
									   .map(GlobPattern::new)
									   .map(GlobPattern::toInternalRegex)
									   .map(item -> item.orElse(""))
									   .reduce((item1, item2) -> String.join("|", item1, item2))
									   .orElse("");
		return "(?:%s)".formatted(split);
	}

	private List<String> splitBrace(String content)
	{
		return IntStream.range(0, content.length())
						.boxed()
						.reduce(new BraceSplitState(List.of(""), false),
								(state, i) -> state.process(content.charAt(i)), (s1, s2) -> s2)
						.alternatives();
	}

	private boolean isEscaped(int position)
	{
		return position > 0 && pattern.charAt(position - 1) == '\\' && !isEscaped(position - 1);
	}

	private String appendPipe(String regex)
	{
		return pattern.isEmpty() || pattern.chars().allMatch(item -> item == '*')
			? regex.concat("|")
			: regex;
	}

	private static record ParseState(String regex,
									 int position,
									 int charactersToSkip,
									 boolean mustMatchEmptyPath)
	{

		boolean shouldSkip()
		{
			return charactersToSkip > 0;
		}

		ParseState skip(int count)
		{
			return new ParseState(regex, position, count, mustMatchEmptyPath);
		}

		ParseState advance()
		{
			return new ParseState(regex,
								  position + 1,
								  Math.max(0, charactersToSkip - 1),
								  mustMatchEmptyPath);
		}

		ParseState append(char character)
		{
			return append(String.valueOf(character));
		}

		ParseState append(String fragment)
		{
			return new ParseState(regex.concat(fragment),
								  position,
								  Math.max(0, charactersToSkip),
								  false);
		}

		ParseState appendStar(String fragment)
		{
			return new ParseState(regex.concat(fragment),
								  position,
								  Math.max(0, charactersToSkip),
								  mustMatchEmptyPath);
		}
	}

	private record BraceSearchState(Optional<Integer> foundAt, boolean escaped)
	{

		BraceSearchState process(String pattern, int position)
		{
			return escaped
				? new BraceSearchState(foundAt, false)
				: processNotEscaped(pattern, position);
		}

		boolean found()
		{
			return foundAt.isPresent();
		}

		Optional<Integer> result()
		{
			return foundAt;
		}

		private BraceSearchState processNotEscaped(String pattern, int position)
		{
			return switch (pattern.charAt(position))
			{
				case '\\' -> new BraceSearchState(foundAt, true);
				case '{' -> throw new PatternSyntaxException("Nested braces are not allowed in glob patterns.",
															 pattern,
															 position);
				case '}' -> new BraceSearchState(Optional.of(position), false);
				default -> this;
			};
		}
	}

	private record BraceSplitState(List<String> alternatives, boolean escaped)
	{

		BraceSplitState process(char character)
		{
			return escaped
				? appendToLast(String.valueOf(character), false)
				: processNotEscaped(character);
		}

		private BraceSplitState processNotEscaped(char character)
		{
			return switch (character)
			{
				case '\\' -> new BraceSplitState(alternatives, true);
				case ',' -> addAlternative();
				default -> appendToLast(String.valueOf(character), false);
			};
		}

		private BraceSplitState appendToLast(String s, boolean escaped)
		{
			var newList = alternatives.subList(0, alternatives.size() - 1);
			var last = alternatives.get(alternatives.size() - 1) + s;
			var result = new java.util.ArrayList<>(newList);
			result.add(last);
			return new BraceSplitState(List.copyOf(result), escaped);
		}

		private BraceSplitState addAlternative()
		{
			var result = new java.util.ArrayList<>(alternatives);
			result.add("");
			return new BraceSplitState(List.copyOf(result), false);
		}
	}
}
