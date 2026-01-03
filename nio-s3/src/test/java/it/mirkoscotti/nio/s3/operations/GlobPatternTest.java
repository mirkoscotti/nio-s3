package it.mirkoscotti.nio.s3.operations;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * @author mirko.scotti
 * @version Dec 31, 2025
 */
class GlobPatternTest
{

	private static final String STAR_WILDCARD = "*.txt";

	private static final String DOUBLE_STAR_WILDCARD = "**";

	private static final String DOUBLE_STAR_WILDCARD_WITH_SLASH = "**".concat("/");

	private static final String QUESTION_MARK_WILDCARD = "file?.txt";

	private static final String DIRECTORY = "directory/";

	private static final String SUB_DIRECTORY = "sub-directory/";

	private static final String FILE_PATTERN = "file%d.txt";

	private static final String FILE = FILE_PATTERN.formatted(1);

	private static final String OTHER_FILE = FILE_PATTERN.formatted(10);

	@Test
	void nullTest()
	{
		Assertions.assertThrows(NullPointerException.class, () -> GlobPattern.of(null));
	}

	@Test
	@DisplayName("*.txt")
	void starWildcardTest()
	{
		var pattern = GlobPattern.of(STAR_WILDCARD).toRegexPattern();
		Assertions.assertTrue(pattern.matcher(FILE).matches());
		Assertions.assertFalse(pattern.matcher(DIRECTORY.concat(FILE)).matches());
	}

	@Test
	@DisplayName("directory/*.txt")
	void starWildcardUnderDirectoryTest()
	{
		var glob = DIRECTORY.concat(STAR_WILDCARD);
		var pattern = GlobPattern.of(glob).toRegexPattern();
		Assertions.assertFalse(pattern.matcher(FILE).matches());
		Assertions.assertTrue(pattern.matcher(DIRECTORY.concat(FILE)).matches());
		Assertions.assertFalse(pattern.matcher(SUB_DIRECTORY.concat(FILE)).matches());
	}

	@Test
	@DisplayName("**/*.txt")
	void doubleStarWildcardTest()
	{
		var glob = DOUBLE_STAR_WILDCARD_WITH_SLASH.concat(STAR_WILDCARD);
		var pattern = GlobPattern.of(glob).toRegexPattern();
		Assertions.assertTrue(pattern.matcher(FILE).matches());
		var path = SUB_DIRECTORY.concat(FILE);
		Assertions.assertTrue(pattern.matcher(path).matches());
		Assertions.assertTrue(pattern.matcher(DIRECTORY.concat(path)).matches());
	}

	@Test
	@DisplayName("directory/**/*.txt")
	void doubleStarWildcardUnderDirectoryTest()
	{
		var glob = DIRECTORY.concat(DOUBLE_STAR_WILDCARD_WITH_SLASH).concat(STAR_WILDCARD);
		var pattern = GlobPattern.of(glob).toRegexPattern();
		Assertions.assertFalse(pattern.matcher(FILE).matches());
		var path = SUB_DIRECTORY.concat(FILE);
		Assertions.assertFalse(pattern.matcher(path).matches());
		Assertions.assertTrue(pattern.matcher(DIRECTORY.concat(path)).matches());
	}

	@Test
	@DisplayName("directory/**")
	void doubleStarWildcardAtEndTest()
	{
		var glob = DIRECTORY.concat(DOUBLE_STAR_WILDCARD);
		var pattern = GlobPattern.of(glob).toRegexPattern();
		var path = SUB_DIRECTORY.concat(FILE);
		Assertions.assertFalse(pattern.matcher(path).matches());
		Assertions.assertTrue(pattern.matcher(DIRECTORY.concat(path)).matches());
	}

	@Test
	@DisplayName("file?.txt")
	void questionMarkWildcardTest()
	{
		var pattern = GlobPattern.of(QUESTION_MARK_WILDCARD).toRegexPattern();
		Assertions.assertTrue(pattern.matcher(FILE).matches());
		Assertions.assertFalse(pattern.matcher(OTHER_FILE).matches());
		Assertions.assertFalse(pattern.matcher(DIRECTORY.concat(FILE)).matches());
	}

	@Test
	@DisplayName("directory/fil?.txt")
	void questionMarkWildcardUnderDirectoryTest()
	{
		var glob = DIRECTORY.concat(QUESTION_MARK_WILDCARD);
		var pattern = GlobPattern.of(glob).toRegexPattern();
		Assertions.assertFalse(pattern.matcher(FILE).matches());
		Assertions.assertTrue(pattern.matcher(DIRECTORY.concat(FILE)).matches());
		Assertions.assertFalse(pattern.matcher(DIRECTORY.concat(OTHER_FILE)).matches());
		Assertions.assertFalse(pattern.matcher(SUB_DIRECTORY.concat(FILE)).matches());
	}

	@Test
	@DisplayName("**/fil?.txt")
	void doubleStarAndQuestionMarkWildcardTest()
	{
		var glob = DOUBLE_STAR_WILDCARD_WITH_SLASH.concat(QUESTION_MARK_WILDCARD);
		var pattern = GlobPattern.of(glob).toRegexPattern();
		Assertions.assertTrue(pattern.matcher(FILE).matches());
		Assertions.assertFalse(pattern.matcher(OTHER_FILE).matches());
		var path = SUB_DIRECTORY.concat(FILE);
		Assertions.assertTrue(pattern.matcher(path).matches());
		Assertions.assertTrue(pattern.matcher(DIRECTORY.concat(path)).matches());
		path = SUB_DIRECTORY.concat(OTHER_FILE);
		Assertions.assertFalse(pattern.matcher(path).matches());
		Assertions.assertFalse(pattern.matcher(DIRECTORY.concat(path)).matches());
	}

	@Test
	@DisplayName("directory/**/fil?.txt")
	void doubleStarAndQuestionMarkWildcardUnderDirectoryTest()
	{
		var glob = DIRECTORY.concat(DOUBLE_STAR_WILDCARD_WITH_SLASH).concat(QUESTION_MARK_WILDCARD);
		var pattern = GlobPattern.of(glob).toRegexPattern();
		Assertions.assertFalse(pattern.matcher(FILE).matches());
		var path = SUB_DIRECTORY.concat(FILE);
		Assertions.assertFalse(pattern.matcher(path).matches());
		path = DIRECTORY.concat(path);
		Assertions.assertTrue(pattern.matcher(path).matches());
		path = DIRECTORY.concat(SUB_DIRECTORY).concat(OTHER_FILE);
		Assertions.assertFalse(pattern.matcher(path).matches());
	}
}
