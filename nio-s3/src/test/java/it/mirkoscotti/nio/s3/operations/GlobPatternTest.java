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

	private static final FileSystem FILE_SYSTEM = Jimfs.newFileSystem(Configuration.unix());

	private static final String DOMAIN = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789-_.";

	private static final Random RANDOM = ThreadLocalRandom.current();

	private static final String EMPTY_PATH = "";

	private static final String ROOT_PATH = "/";

	private static final String STAR = "*";

	private static final String DOUBLE_STAR = STAR.concat(STAR);

	private static final String DOT = ".";

	private static final String QUESTION_MARK = "?";

	private static final String WILDCARD_PATTERN = "*.%s";

	private static final String DIRECTORY_PATTERN = "%s/".concat(WILDCARD_PATTERN);

	private static final String FILE_PATTERN = "%s.%s";

	private static final String MESSAGE = "Glob: %s, Path: %s -> Expected: %s";

	private static final List<String> TEST_CASES = new ArrayList<>();

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
		baseTestCases(new GlobRecord(EMPTY_PATH));
	}

	@Test
	@DisplayName("*, **, ***[...]")
	void starsTest()
	{
		Stream.of(STAR, DOUBLE_STAR, randomStars())
			  .map(GlobRecord::new)
			  .forEach(this::baseTestCases);
	}

	@Test
	@DisplayName("directory/*, directory/**, directory/***[...]")
	void directoryTest()
	{
		var directory = randomDirectory();
		Stream.of(directory.concat(STAR), directory.concat(DOUBLE_STAR),
				  directory.concat(randomStars()))
			  .map(GlobRecord::new)
			  .forEach(item -> directoryTestCases(item, directory));
	}

	@Test
	@DisplayName("*.[extension], **.[extension], ***[...].[extension]")
	void extensionTest()
	{
		var extension = randomName();
		Stream.of(String.join(DOT, STAR, extension), String.join(DOT, DOUBLE_STAR, extension),
				  String.join(DOT, randomStars(), extension))
			  .map(GlobRecord::new)
			  .forEach(item -> extensionTestCases(item, extension));
	}

	@Test
	@DisplayName("directory/*.[extension], directory/**.[extension], directory/***[...].[extension]")
	void directoryWithExtensionTest()
	{
		var directory = randomDirectory();
		var extension = randomName();
		Stream.of(String.join(DOT, directory.concat(STAR), extension),
				  String.join(DOT, directory.concat(DOUBLE_STAR), extension),
				  String.join(DOT, directory.concat(randomStars()), extension))
			  .map(GlobRecord::new)
			  .forEach(item -> directoryAndExtensionTestCases(item, directory, extension));
	}

	@Test
	@DisplayName("*/*.[extension], **/*.[extension], ***[...]/*.[extension]")
	void mixedStarsWithExtensionTest()
	{
		var extension = randomName();
		Stream.of(DIRECTORY_PATTERN.formatted(STAR, extension),
				  DIRECTORY_PATTERN.formatted(DOUBLE_STAR, extension),
				  DIRECTORY_PATTERN.formatted(randomStars(), extension))
			  .map(GlobRecord::new)
			  .forEach(item -> extensionTestCases(item, extension));
	}

	@Test
	@DisplayName("directory/*/*.[extension], directory/**/*.[extension], directory/***[...]/*.[extension]")
	void mixedStarsUnderDirectoryWithExtensionTest()
	{
		var directory = randomDirectory();
		var extension = randomName();
		Stream.of(DIRECTORY_PATTERN.formatted(directory.concat(STAR), extension),
				  DIRECTORY_PATTERN.formatted(directory.concat(DOUBLE_STAR), extension),
				  DIRECTORY_PATTERN.formatted(directory.concat(randomStars()), extension))
			  .map(GlobRecord::new)
			  .forEach(item -> extensionTestCases(item, extension));
	}

	@Test
	@DisplayName("[file].*, [file].**, [file].***[...]")
	void fileTest()
	{
		var fileName = randomName();
		Stream.of(FILE_PATTERN.formatted(fileName, STAR),
				  FILE_PATTERN.formatted(fileName, DOUBLE_STAR),
				  FILE_PATTERN.formatted(fileName, randomStars()))
			  .map(GlobRecord::new)
			  .forEach(item -> fileTestCases(item, fileName));
	}

	@Test
	@DisplayName("?, [file].?, ?.[extension]")
	void questionMarkTest()
	{
		var fileName = randomName();
		var extension = randomName();
		Stream.of(QUESTION_MARK, FILE_PATTERN.formatted(fileName, QUESTION_MARK),
				  FILE_PATTERN.formatted(QUESTION_MARK, extension))
			  .map(GlobRecord::new)
			  .forEach(item -> questionMarkTestCases(item, fileName, extension));
	}

	private void baseTestCases(GlobRecord globRecord)
	{
		baseTestCases(globRecord.glob, globRecord.regex);
	}

	private void baseTestCases(String glob, String regex)
	{
		assertMatch(glob, regex, EMPTY_PATH);
		assertMatch(glob, regex, ROOT_PATH);
		assertMatch(glob, regex, randomCharacter());
		assertMatch(glob, regex, randomDirectory());
		assertMatch(glob, regex, randomName());
		assertMatch(glob, regex, randomPath());
		assertMatch(glob, regex, randomFile(1));
		assertMatch(glob, regex, randomDirectory(2));
		assertMatch(glob, regex, randomFile(2));
	}

	private void directoryTestCases(GlobRecord globRecord, String directory)
	{
		baseTestCases(globRecord);
		directoryTestCases(globRecord.glob, globRecord.regex, directory);
	}

	private void directoryTestCases(String glob, String regex, String directory)
	{
		assertMatch(glob, regex, directory);
		assertMatch(glob, regex, randomFile(directory));
		assertMatch(glob, regex, randomDirectory(directory));
		assertMatch(glob, regex, randomPath(1, directory, true));
	}

	private void extensionTestCases(GlobRecord globRecord, String extension)
	{
		baseTestCases(globRecord);
		extensionTestCases(globRecord.glob, globRecord.regex, extension);
	}

	private void extensionTestCases(String glob, String regex, String extension)
	{
		assertMatch(glob, regex, String.join(DOT, randomName(), extension));
		assertMatch(glob, regex, String.join(DOT, randomFile(1), extension));
		assertMatch(glob, regex, String.join(DOT, randomFile(2), extension));
	}

	private void directoryAndExtensionTestCases(GlobRecord globRecord,
												String directory,
												String extension)
	{
		baseTestCases(globRecord);
		directoryTestCases(globRecord, directory);
		extensionTestCases(globRecord, extension);
		directoryAndExtensionTestCases(globRecord.glob, globRecord.regex, directory, extension);
	}

	private void directoryAndExtensionTestCases(String glob,
												String regex,
												String directory,
												String extension)
	{
		assertMatch(glob, regex, String.join(DOT, randomFile(directory), extension));
		assertMatch(glob, regex, String.join(DOT, randomPath(1, directory, true), extension));
	}

	private void fileTestCases(GlobRecord globRecord, String fileName)
	{
		baseTestCases(globRecord);
		fileTestCases(globRecord.glob, globRecord.regex, fileName);
	}

	private void fileTestCases(String glob, String regex, String fileName)
	{
		assertMatch(glob, regex, String.join(DOT, fileName, randomName()));
		assertMatch(glob, regex, String.join(DOT, fileName, randomDirectory()));
		assertMatch(glob, regex, String.join(DOT, fileName, randomFile(1)));
		assertMatch(glob, regex,
					String.join(DOT, randomDirectory().concat(fileName), randomName()));
		assertMatch(glob, regex,
					String.join(DOT, randomDirectory().concat(fileName), randomDirectory()));
		assertMatch(glob, regex,
					String.join(DOT, randomDirectory().concat(fileName), randomPath()));
	}

	private void questionMarkTestCases(GlobRecord globRecord, String fileName, String extension)
	{
		baseTestCases(globRecord);
		questionMarkTestCases(globRecord.glob, globRecord.regex, fileName, extension);
	}

	private void questionMarkTestCases(String glob, String regex, String fileName, String extension)
	{
		assertMatch(glob, regex, String.join(DOT, fileName, randomCharacter()));
		assertMatch(glob, regex, String.join(DOT, fileName, randomName()));
		assertMatch(glob, regex, String.join(DOT, randomCharacter(), extension));
		assertMatch(glob, regex, String.join(DOT, randomName(), extension));
	}

	private void assertMatch(String glob, String regex, String path)
	{
		var expected = expectedMatch(glob, path);
		var result = path.matches(regex);
		TEST_CASES.add(MESSAGE.formatted(normalizeEmpty(glob), normalizeEmpty(path),
										 expected ? "match" : "no match"));
		Assertions.assertEquals(expected, result, () -> errorMessage(glob, regex, path, expected));
	}

	private boolean expectedMatch(String glob, String path)
	{
		var expectedPath = FILE_SYSTEM.getPath(path);
		return FILE_SYSTEM.getPathMatcher("glob:".concat(glob)).matches(expectedPath);
	}

	private String normalizeEmpty(String value)
	{
		return Optional.of(value).filter(Predicate.not(EMPTY_PATH::equals)).orElse("<empty>");
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

	private String randomCharacter()
	{
		var domain = DOMAIN.replace(".", "");
		var index = RANDOM.nextInt(domain.length());
		return String.valueOf(domain.charAt(index));
	}

	private String randomName()
	{
		var length = RANDOM.nextInt(1, 21);
		return RANDOM.ints(length, 0, DOMAIN.length())
					 .mapToObj(DOMAIN::charAt)
					 .map(String::valueOf)
					 .collect(Collectors.collectingAndThen(Collectors.joining(),
														   this::normalizeName));
	}

	private String randomFile(int depth)
	{
		return randomPath(depth, EMPTY_PATH, true);
	}

	private String randomFile(String prefix)
	{
		return randomPath(0, prefix, true);
	}

	private String randomDirectory()
	{
		return randomName().concat(ROOT_PATH);
	}

	private String randomDirectory(int depth)
	{
		return randomPath(depth, EMPTY_PATH, false);
	}

	private String randomDirectory(String prefix)
	{
		return randomPath(0, prefix, false);
	}

	private String randomPath()
	{
		return randomPath(RANDOM.nextInt(1, 6), EMPTY_PATH, RANDOM.nextBoolean());
	}

	private String randomPath(int depth, String prefix, boolean withFile)
	{
		var path = IntStream.range(Math.min(0, Math.min(depth, 5) + 1), depth)
							.mapToObj(i -> randomDirectory())
							.collect(Collectors.joining());
		return prefix.concat(path).concat(withFile ? randomName() : EMPTY_PATH);
	}

	private static String randomStars()
	{
		return IntStream.rangeClosed(1, 3 + RANDOM.nextInt(7))
						.mapToObj(i -> "*")
						.collect(Collectors.joining());
	}

	private String normalizeName(String name)
	{
		var string = name.replaceAll("\\.{2,}", DOT);
		var result = string.endsWith(DOT)
			? string.substring(0, string.length() - 1).concat(randomCharacter())
			: string;
		return result.isEmpty() ? randomCharacter() : result;
	}

	private static record GlobRecord(String glob, String regex)
	{

		GlobRecord(String glob)
		{
			this(glob, GlobPattern.of(glob).toRegex());
		}
	}

	// --------- SONO ARRIVATO QUA ----------

	//
	// @Test
	// @DisplayName("file?.txt")
	// void questionMarkWildcardTest()
	// {
	// var pattern = GlobPattern.of(QUESTION_MARK_WILDCARD).toRegexPattern();
	// Assertions.assertTrue(pattern.matcher(FILE).matches());
	// Assertions.assertFalse(pattern.matcher(OTHER_TEXT_FILE).matches());
	// Assertions.assertFalse(pattern.matcher(DIRECTORY.concat(FILE)).matches());
	// }
	//
	// @Test
	// @DisplayName("directory/fil?.txt")
	// void questionMarkWildcardUnderDirectoryTest()
	// {
	// var glob = DIRECTORY.concat(QUESTION_MARK_WILDCARD);
	// var pattern = GlobPattern.of(glob).toRegexPattern();
	// Assertions.assertFalse(pattern.matcher(FILE).matches());
	// Assertions.assertTrue(pattern.matcher(DIRECTORY.concat(FILE)).matches());
	// Assertions.assertFalse(pattern.matcher(DIRECTORY.concat(OTHER_TEXT_FILE)).matches());
	// Assertions.assertFalse(pattern.matcher(SUB_DIRECTORY.concat(FILE)).matches());
	// }
	//
	// @Test
	// @DisplayName("**/fil?.txt")
	// void doubleStarAndQuestionMarkWildcardTest()
	// {
	// var glob = DOUBLE_STAR_DIRECTORY.concat(QUESTION_MARK_WILDCARD);
	// var pattern = GlobPattern.of(glob).toRegexPattern();
	// Assertions.assertTrue(pattern.matcher(FILE).matches());
	// Assertions.assertFalse(pattern.matcher(OTHER_TEXT_FILE).matches());
	// var path = SUB_DIRECTORY.concat(FILE);
	// Assertions.assertTrue(pattern.matcher(path).matches());
	// Assertions.assertTrue(pattern.matcher(DIRECTORY.concat(path)).matches());
	// path = SUB_DIRECTORY.concat(OTHER_TEXT_FILE);
	// Assertions.assertFalse(pattern.matcher(path).matches());
	// Assertions.assertFalse(pattern.matcher(DIRECTORY.concat(path)).matches());
	// }
	//
	// @Test
	// @DisplayName("directory/**/fil?.txt")
	// void doubleStarAndQuestionMarkWildcardUnderDirectoryTest()
	// {
	// var glob = DIRECTORY.concat(DOUBLE_STAR_DIRECTORY).concat(QUESTION_MARK_WILDCARD);
	// var pattern = GlobPattern.of(glob).toRegexPattern();
	// Assertions.assertFalse(pattern.matcher(FILE).matches());
	// var path = SUB_DIRECTORY.concat(FILE);
	// Assertions.assertFalse(pattern.matcher(path).matches());
	// path = DIRECTORY.concat(path);
	// Assertions.assertTrue(pattern.matcher(path).matches());
	// path = DIRECTORY.concat(SUB_DIRECTORY).concat(OTHER_TEXT_FILE);
	// Assertions.assertFalse(pattern.matcher(path).matches());
	// }
}
