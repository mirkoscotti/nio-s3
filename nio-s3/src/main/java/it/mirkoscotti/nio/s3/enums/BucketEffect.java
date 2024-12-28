package it.mirkoscotti.nio.s3.enums;

import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * @author mirko.scotti
 * @version Oct 25, 2024
 */
public enum BucketEffect
{

	ALLOW,
	DENY;

	@Override
	public String toString()
	{
		var name = name();
		return name.substring(0, 1).concat(name.substring(1).toLowerCase());
	}

	public static Optional<BucketEffect> of(String effect)
	{
		return Stream.of(BucketEffect.values())
					 .filter(item -> Objects.equals(item.name(), effect.toUpperCase()))
					 .findAny();
	}
}
