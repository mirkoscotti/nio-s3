package io.github.mirkoscotti.nio.s3.enums;

import java.util.regex.Pattern;
import java.util.stream.Stream;

import it.mirkoscotti.nio.s3.operations.GlobPattern;

/**
 * @author mirko.scotti
 * @version Jan 12, 2026
 */
public enum PathSyntax
{

	GLOB
	{

		@Override
		public Pattern pattern(String pattern)
		{
			var regex = GlobPattern.of(pattern).toRegex();
			return super.pattern(regex);
		}
	},
	REGEX;

	public static PathSyntax of(String syntax)
	{
		return Stream.of(PathSyntax.values())
					 .filter(item -> item.name().toLowerCase().equals(syntax))
					 .findFirst()
					 .orElseThrow(() -> new UnsupportedOperationException("Expected Glob o regex syntax, found %s".formatted(syntax)));
	}

	public Pattern pattern(String pattern)
	{
		return Pattern.compile(pattern);
	}
}
