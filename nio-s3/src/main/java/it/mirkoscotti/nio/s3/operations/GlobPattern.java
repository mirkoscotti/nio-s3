package it.mirkoscotti.nio.s3.operations;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
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
		var regex = toInternalRegex().map(this::appendSlash).map(this::appendPipe).orElse("");
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
					   .map(item -> item.skip(2))
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
						.filter(i -> pattern.charAt(i) == ']')
						.boxed()
						.findFirst()
						.map(item -> processClosingBracket(current, position, item))
						.orElseThrow(() -> new PatternSyntaxException("Malformed brackets.",
																	  pattern,
																	  position));
	}

	private ParseState handleBrace(ParseState current)
	{
		var position = current.position();
		var reference = new AtomicReference<>(new BraceSearchState(Optional.empty(), false));
		IntStream.range(position + 1, pattern.length())
				 .forEach(i -> reference.set(processBrace(reference.get(), i)));
		return reference.get()
						.result()
						.map(item -> processClosingBrace(current, position, item))
						.orElseThrow(() -> new PatternSyntaxException("Malformed braces.",
																	  pattern,
																	  position));
	}

	private ParseState processClosingBracket(ParseState current,
											 int globPosition,
											 int bracketPosition)
	{
		var regex = Optional.of(pattern.substring(globPosition + 1, bracketPosition))
							.filter(Predicate.not(String::isEmpty))
							.map(this::convertBracketExpression)
							.orElseThrow(() -> new PatternSyntaxException("Empty brackets.",
																		  pattern,
																		  bracketPosition));
		return current.append(regex).skip(bracketPosition - globPosition + 1);
	}

	private String convertBracketExpression(String content)
	{
		var negated = content.charAt(0) == '!';
		var escaped = (negated ? content.substring(1) : content).replace("\\", "\\\\");
		return "[[^/]&&[%s%s]]".formatted(negated ? "^" : "", escaped);
	}

	private BraceSearchState processBrace(BraceSearchState state, int position)
	{
		return Optional.of(state)
					   .filter(Predicate.not(item -> item.found()))
					   .map(item -> item.process(pattern, position))
					   .orElse(state);
	}

	private ParseState processClosingBrace(ParseState current, int globPosition, int bracePosition)
	{
		var content = pattern.substring(globPosition + 1, bracePosition);
		var regex = Optional.of(content)
							.filter(Predicate.not(String::isEmpty))
							.map(this::convertBraceExpression)
							.orElseThrow(() -> new PatternSyntaxException("Empty braces.",
																		  pattern,
																		  bracePosition));
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
		var reference = new AtomicReference<>(new BraceSplitState(List.of(""), false));
		IntStream.range(0, content.length())
				 .forEach(i -> reference.set(reference.get().process(content.charAt(i))));
		return reference.get().alternatives();
	}

	private String appendSlash(String regex)
	{
		var mustNotAppendOptionalSlash = regex.isEmpty();
		return mustNotAppendOptionalSlash ? regex : regex.concat("/?");
	}

	private String appendPipe(String regex)
	{
		var mustAppendOptionalPipe = pattern.isEmpty()
			|| pattern.chars().allMatch(item -> item == '*');
		return mustAppendOptionalPipe ? regex.concat("|") : regex;
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
				? new BraceSearchState(foundAt, pattern.charAt(position) == '\\')
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
			var character = pattern.charAt(position);
			return switch (character)
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
				? appendToLast(String.valueOf(character), character == '\\')
				: processNotEscaped(character);
		}

		private BraceSplitState processNotEscaped(char character)
		{
			return switch (character)
			{
				case '\\' -> appendToLast("\\", true);
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
