package it.mirkoscotti.nio.s3.operations;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * @author mirko.scotti
 * @version Dec 31, 2025
 */
class GlobPatternTest
{

	private static final String WILDCARD_FILE = "*.txt";

	private static final String DIRECTORY = "directory/";

	private static final String FILE = "file.txt";

	@Test
	void nullTest()
	{
		Assertions.assertThrows(NullPointerException.class, () -> GlobPattern.of(null));
	}

	@Test
	void fileWildcardTest()
	{
		var pattern = GlobPattern.of(WILDCARD_FILE).toRegexPattern();
		Assertions.assertTrue(pattern.matcher(FILE).matches());
		Assertions.assertFalse(pattern.matcher(DIRECTORY.concat(FILE)).matches());
	}

	@Test
	void fileWildcardUnderDirectoryTest()
	{
		var pattern = GlobPattern.of(DIRECTORY.concat(WILDCARD_FILE)).toRegexPattern();
		Assertions.assertFalse(pattern.matcher(FILE).matches());
		Assertions.assertTrue(pattern.matcher(DIRECTORY.concat(FILE)).matches());
	}
}
