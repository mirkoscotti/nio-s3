package it.mirkoscotti.nio.s3.operations;

import java.lang.System.Logger.Level;
import java.nio.file.FileSystem;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * @author mirko.scotti
 * @version Dec 31, 2025
 */
class GlobPatternTest
{

	private static final Randomizer RANDOMIZER = new Randomizer();

	private static final List<String> TEST_CASES = new ArrayList<>();

	private static final String EMPTY = "";

	private static final String DOT = ".";

	private static final String SLASH = "/";

	private static final String STAR = "*";

	private static final String QUESTION_MARK = "?";

	private static final String DOUBLE_STAR = STAR.concat(STAR);

	private static final String EXTENSION_PATTERN = "/*.%s";

	@AfterAll
	static void afterAll()
	{
		var report = TEST_CASES.stream().collect(Collectors.joining("\n"));
		System.getLogger(GlobPatternTest.class.getName()).log(Level.INFO, report);
	}

	@Test
	void nullTest()
	{
		Assertions.assertThrows(NullPointerException.class, () -> GlobPattern.of(null));
	}

	@Test
	void emptyTest()
	{
		var globRecord = new GlobRecord(EMPTY);
		globRecord.baseTestCases();
	}

	@Test
	@DisplayName("*, **")
	void starsTest()
	{
		Stream.of(STAR, DOUBLE_STAR, RANDOMIZER.stars())
			  .map(GlobRecord::new)
			  .forEach(GlobRecord::baseTestCases);
	}

	@Test
	@DisplayName("directory/*, directory/**")
	void directoryTest()
	{
		var directory = RANDOMIZER.directory();
		Stream.of(directory.concat(STAR), directory.concat(DOUBLE_STAR),
				  directory.concat(RANDOMIZER.stars()))
			  .map(GlobRecord::new)
			  .forEach(item -> item.baseDirectoryTestCases(directory));
	}

	@Test
	@DisplayName("*.[extension], **.[extension]")
	void extensionTest()
	{
		var extension = RANDOMIZER.name();
		Stream.of(String.join(DOT, STAR, extension), String.join(DOT, DOUBLE_STAR, extension),
				  String.join(DOT, RANDOMIZER.stars(), extension))
			  .map(GlobRecord::new)
			  .forEach(item -> item.baseExtensionTestCases(extension));
	}

	@Test
	@DisplayName("directory/*.[extension], directory/**.[extension]")
	void directoryWithExtensionTest()
	{
		var directory = RANDOMIZER.directory();
		var extension = RANDOMIZER.name();
		Stream.of(String.join(DOT, directory.concat(STAR), extension),
				  String.join(DOT, directory.concat(DOUBLE_STAR), extension),
				  String.join(DOT, directory.concat(RANDOMIZER.stars()), extension))
			  .map(GlobRecord::new)
			  .forEach(item -> item.baseDirectoryAndExtensionTestCases(directory, extension));
	}

	@Test
	@DisplayName("*/*.[extension], **/*.[extension]")
	void mixedStarsWithExtensionTest()
	{
		var extension = RANDOMIZER.name();
		Stream.of(STAR.concat(EXTENSION_PATTERN).formatted(extension),
				  DOUBLE_STAR.concat(EXTENSION_PATTERN).formatted(extension),
				  RANDOMIZER.stars().concat(EXTENSION_PATTERN).formatted(extension))
			  .map(GlobRecord::new)
			  .forEach(item -> item.baseExtensionTestCases(extension));
	}

	@Test
	@DisplayName("directory/*/*.[extension], directory/**/*.[extension]")
	void mixedStarsUnderDirectoryWithExtensionTest()
	{
		var directory = RANDOMIZER.directory();
		var extension = RANDOMIZER.name();
		Stream.of(String.join(STAR, directory, EXTENSION_PATTERN).formatted(extension),
				  String.join(DOUBLE_STAR, directory, EXTENSION_PATTERN).formatted(extension),
				  String.join(RANDOMIZER.stars(), directory, EXTENSION_PATTERN)
						.formatted(extension))
			  .map(GlobRecord::new)
			  .forEach(item -> item.baseDirectoryAndExtensionTestCases(directory, extension));
	}

	@Test
	@DisplayName("[file].*, [file].**")
	void fileTest()
	{
		var fileName = RANDOMIZER.name();
		Stream.of(String.join(DOT, fileName, STAR), String.join(DOT, fileName, DOUBLE_STAR),
				  String.join(DOT, fileName, RANDOMIZER.stars()))
			  .map(GlobRecord::new)
			  .forEach(item -> item.baseFileTestCases(fileName));
	}

	@Test
	@DisplayName("?, [file].?, ?.[extension], [file]?.[extension]")
	void questionMarkTest()
	{
		var fileName = RANDOMIZER.name();
		var extension = RANDOMIZER.name();
		Stream.of(QUESTION_MARK, String.join(DOT, fileName, QUESTION_MARK),
				  String.join(DOT, QUESTION_MARK, extension),
				  String.join(DOT, fileName.concat(QUESTION_MARK), extension))
			  .map(GlobRecord::new)
			  .forEach(item -> item.baseFileAndExtensionTestCases(fileName, extension));
	}

	@Test
	@DisplayName("[0-9], [A-Z], [a-z], [0-9A-Za-z]")
	void bracketsTest()
	{
		Stream.of("[0-9]", "[A-Z]", "[a-z]", "[0-9A-Za-z]")
			  .map(GlobRecord::new)
			  .forEach(GlobRecord::bracketsTestCases);
	}

	@Test
	@DisplayName("*.{extension1, extension2}")
	void bracesTest()
	{
		var extension1 = RANDOMIZER.name();
		var extension2 = RANDOMIZER.name();
		Stream.of("*.{%s,%s}".formatted(extension1, extension2))
			  .map(GlobRecord::new)
			  .forEach(item -> item.bracesTestCases(extension1, extension2));
	}

	@Test
	@DisplayName("*.\\, *.(, *.), *.$, *.+, *.^, *.|")
	void specialCharactersTest()
	{
		Stream.of("*.%s".formatted("\\\\"), "*.%s".formatted("("), "*.%s".formatted(")"),
				  "*.%s".formatted("$"), "*.%s".formatted("+"), "*.%s".formatted("^"))
			  .map(GlobRecord::new)
			  .forEach(GlobRecord::baseTestCases);
	}

	private static final class Randomizer
	{

		private static final Random RANDOM = ThreadLocalRandom.current();

		private static final String DOMAIN = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789-_.";

		int length(int bound)
		{
			return RANDOM.nextInt(bound);
		}

		String character()
		{
			var domain = DOMAIN.replace(".", "");
			var index = RANDOM.nextInt(domain.length());
			return String.valueOf(domain.charAt(index));
		}

		String name()
		{
			return name(RANDOM.nextInt(1, 21));
		}

		String name(int length)
		{
			return RANDOM.ints(length, 0, DOMAIN.length())
						 .mapToObj(DOMAIN::charAt)
						 .map(String::valueOf)
						 .collect(Collectors.collectingAndThen(Collectors.joining(),
															   this::normalizeName));
		}

		String file()
		{
			return String.join(DOT, name(), name(3));
		}

		String file(int depth)
		{
			return path(depth).concat(file());
		}

		String file(String extension)
		{
			return String.join(DOT, name(), extension);
		}

		String file(int depth, String extension)
		{
			return path(depth).concat(file(extension));
		}

		String directory()
		{
			return name().concat(SLASH);
		}

		String directory(int length)
		{
			return name(length).concat(SLASH);
		}

		String path()
		{
			return path(RANDOM.nextInt(0, 4));
		}

		String path(int depth)
		{
			return IntStream.range(0, Math.max(2, depth))
							.mapToObj(i -> directory())
							.collect(Collectors.joining());
		}

		String extension(String name)
		{
			return extension(name, 3);
		}

		String extension(String name, int length)
		{
			return String.join(DOT, name, name(length));
		}

		private String stars()
		{
			return IntStream.rangeClosed(1, 3 + RANDOMIZER.length(7))
							.mapToObj(i -> "*")
							.collect(Collectors.joining());
		}

		private String normalizeName(String name)
		{
			var string = name.replaceAll("\\.{2,}", DOT);
			var result = string.endsWith(DOT)
				? string.substring(0, string.length() - 1).concat(character())
				: string;
			return result.isEmpty() ? character() : result;
		}
	}

	private static record GlobRecord(String glob, String regex)
	{

		private static final FileSystem FILE_SYSTEM = Jimfs.newFileSystem(Configuration.unix());

		private static final String MESSAGE = "Glob: %s, Path: %s -> Expected: %s";

		GlobRecord(String glob)
		{
			this(glob, GlobPattern.of(glob).toRegex());
		}

		void baseTestCases()
		{
			assertMatch(EMPTY);
			assertMatch(SLASH);
			assertMatch(RANDOMIZER.character());
			assertMatch(RANDOMIZER.name());
			assertMatch(RANDOMIZER.file());
			assertMatch(RANDOMIZER.directory());
			assertMatch(RANDOMIZER.directory(1));
			assertMatch(RANDOMIZER.path());
			assertMatch("(".concat(RANDOMIZER.name()));
			assertMatch(")".concat(RANDOMIZER.name()));
			assertMatch("+".concat(RANDOMIZER.name()));
			assertMatch("$".concat(RANDOMIZER.name()));
			assertMatch("^".concat(RANDOMIZER.name()));
		}

		void baseDirectoryTestCases(String directory)
		{
			baseTestCases();
			directoryTestCases(directory);
		}

		void baseExtensionTestCases(String extension)
		{
			baseTestCases();
			extensionTestCases(extension);
		}

		void baseDirectoryAndExtensionTestCases(String directory, String extension)
		{
			baseTestCases();
			directoryTestCases(directory);
			extensionTestCases(extension);
			directoryAndExtensionTestCases(directory, extension);
		}

		void baseFileTestCases(String fileName)
		{
			baseTestCases();
			fileTestCases(fileName);
		}

		void baseFileAndExtensionTestCases(String fileName, String extension)
		{
			baseTestCases();
			extensionTestCases(extension);
			fileTestCases(fileName);
			fileAndExtensionTestCases(fileName, extension);
		}

		void bracketsTestCases()
		{
			baseTestCases();
		}

		void bracesTestCases(String extension1, String extension2)
		{
			baseTestCases();
			extensionTestCases(extension1);
			extensionTestCases(extension2);
		}

		private void directoryTestCases(String directory)
		{
			assertMatch(directory);
			assertMatch(directory.concat(RANDOMIZER.character()));
			assertMatch(directory.concat(RANDOMIZER.name()));
			assertMatch(directory.concat(RANDOMIZER.file()));
			assertMatch(directory.concat(RANDOMIZER.directory(1)));
			assertMatch(directory.concat(RANDOMIZER.directory()));
			assertMatch(directory.concat(RANDOMIZER.path()));
			assertMatch(directory.concat(RANDOMIZER.file(1)));
		}

		private void extensionTestCases(String extension)
		{
			assertMatch(RANDOMIZER.file(extension));
			assertMatch(RANDOMIZER.file(1, extension));
			assertMatch(RANDOMIZER.file(2, extension));
		}

		private void directoryAndExtensionTestCases(String directory, String extension)
		{
			assertMatch(directory.concat(RANDOMIZER.file(extension)));
			assertMatch(directory.concat(RANDOMIZER.file(1, extension)));
			assertMatch(directory.concat(RANDOMIZER.file(2, extension)));
		}

		private void fileTestCases(String fileName)
		{
			assertMatch(RANDOMIZER.extension(fileName));
			assertMatch(RANDOMIZER.extension(fileName, 1));
			assertMatch(RANDOMIZER.directory().concat(RANDOMIZER.extension(fileName)));
			assertMatch(RANDOMIZER.directory().concat(RANDOMIZER.extension(fileName, 1)));
			assertMatch(RANDOMIZER.path().concat(RANDOMIZER.extension(fileName)));
			assertMatch(RANDOMIZER.path().concat(RANDOMIZER.extension(fileName, 1)));
		}

		private void fileAndExtensionTestCases(String fileName, String extension)
		{
			assertMatch(RANDOMIZER.file(RANDOMIZER.character()));
			assertMatch(RANDOMIZER.directory().concat(RANDOMIZER.file(RANDOMIZER.character())));
			assertMatch(RANDOMIZER.extension(fileName, 1));
			assertMatch(RANDOMIZER.directory().concat(RANDOMIZER.extension(fileName, 1)));
			assertMatch(RANDOMIZER.file(extension));
			assertMatch(RANDOMIZER.directory().concat(RANDOMIZER.file(extension)));
		}

		private void assertMatch(String path)
		{
			var expected = expectedMatch(glob, path);
			var result = path.matches(regex);
			TEST_CASES.add(MESSAGE.formatted(normalizeEmpty(glob), normalizeEmpty(path),
											 expected ? "match" : "no match"));
			Assertions.assertEquals(expected, result,
									() -> errorMessage(glob, regex, path, expected));
		}

		private boolean expectedMatch(String glob, String path)
		{
			var expectedPath = FILE_SYSTEM.getPath(path);
			return FILE_SYSTEM.getPathMatcher("glob:".concat(glob)).matches(expectedPath);
		}

		private String normalizeEmpty(String value)
		{
			return Optional.of(value).filter(Predicate.not(EMPTY::equals)).orElse("<empty>");
		}

		private String errorMessage(String glob, String regex, String path, boolean mustMatch)
		{
			var response = mustMatch
				? "Expected to match but does not"
				: "Expected not to match but does";
			return """
				%s
				Original glob: %s
				Generated regex: %s
				Candidate path: %s
				""".formatted(response, glob, regex, path);
		}
	}
}
