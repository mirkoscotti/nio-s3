package io.github.mirkoscotti.nio.s3.enums;

import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * List of effects for a bucket policy.
 *
 * @author mirko.scotti
 * @version Oct 25, 2024
 */
public enum BucketEffect
{

	/**
	 * Grants access when the associated bucket policy statement matches.
	 */
	ALLOW,
	/**
	 * Denies access when the associated bucket policy statement matches.
	 */
	DENY;

	/**
	 * Builds the tag name of this effect, as expected in a bucket policy document (e.g.
	 * <code>Allow</code>, <code>Deny</code>).
	 *
	 * @return the effect tag name
	 */
	@Override
	public String toString()
	{
		var name = name();
		return name.substring(0, 1).concat(name.substring(1).toLowerCase());
	}

	/**
	 * Resolves the effect matching the given name, case-insensitively.
	 *
	 * @param effect
	 *            the name of the effect to resolve
	 * @return the item of this <i>enum</i> corresponding to the given tag name or an empty optional
	 *         if there is no matching
	 */
	public static Optional<BucketEffect> of(String effect)
	{
		return Stream.of(BucketEffect.values())
					 .filter(item -> Objects.equals(item.name(), effect.toUpperCase()))
					 .findAny();
	}
}
