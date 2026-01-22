package it.mirkoscotti.nio.s3.enums;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import it.mirkoscotti.nio.s3.operations.GlobPattern;

/**
 * @author mirko.scotti
 * @version Jan 22, 2026
 */
@ExtendWith(MockitoExtension.class)
class PathSyntaxTest
{

	private static final String REGEX = "regex";

	@Test
	void globTest(@Mock GlobPattern globPattern)
	{
		Mockito.when(globPattern.toRegex()).thenReturn(REGEX);
		try (var globMock = Mockito.mockStatic(GlobPattern.class))
		{
			globMock.when(() -> GlobPattern.of(Mockito.anyString())).thenReturn(globPattern);
			var pattern = PathSyntax.GLOB.pattern("glob");
			Assertions.assertEquals(REGEX, pattern.toString());
		}
	}

	@Test
	void regexTest()
	{
		var pattern = PathSyntax.REGEX.pattern(REGEX);
		Assertions.assertEquals(REGEX, pattern.toString());
	}
}
