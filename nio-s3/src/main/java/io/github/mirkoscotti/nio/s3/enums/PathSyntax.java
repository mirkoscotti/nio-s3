package io.github.mirkoscotti.nio.s3.enums;

import java.util.regex.Pattern;
import java.util.stream.Stream;

import io.github.mirkoscotti.nio.s3.operations.GlobPattern;

/**
 * Defines the syntax variants available for matching file paths within the S3 NIO file system.
 *
 * <p>
 * Each constant represents a distinct matching strategy that can be applied when opening or
 * filtering files. The selected syntax determines how a pattern string is interpreted and compiled
 * into a <code>java.util.regex.Pattern</code>.
 *
 * @author mirko.scotti
 * @version Jan 12, 2026
 * @see GlobPattern
 */
public enum PathSyntax
{

	/**
	 * Glob syntax, following the pattern language defined by the Java NIO.2 specification.
	 */
	GLOB
	{

		@Override
		public Pattern pattern(String pattern)
		{
			var regex = GlobPattern.of(pattern).toRegex();
			return super.pattern(regex);
		}
	},
	/**
	 * Regular-expression syntax, following the <code>regex</code> pattern language.
	 */
	REGEX;

	/**
	 * Returns the item whose name matches the given string, compared case-insensitively, according
	 * to the Java NIO.2 specification.
	 *
	 * @param syntax
	 *            "glob" or "regex"
	 * @return the item whose name matches the given syntax.
	 * @throws UnsupportedOperationException
	 *             if syntax does not match any item.
	 */
	public static PathSyntax of(String syntax)
	{
		return Stream.of(PathSyntax.values())
					 .filter(item -> item.name().toLowerCase().equals(syntax))
					 .findFirst()
					 .orElseThrow(() -> new UnsupportedOperationException("Expected Glob o regex syntax, found %s".formatted(syntax)));
	}

	/**
	 * Generates the pattern object from the given raw pattern.
	 *
	 * @param pattern
	 *            the raw pattern
	 * @return the pattern object
	 */
	public Pattern pattern(String pattern)
	{
		return Pattern.compile(pattern);
	}
}
