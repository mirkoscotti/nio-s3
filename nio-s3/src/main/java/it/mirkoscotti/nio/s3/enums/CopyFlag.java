package it.mirkoscotti.nio.s3.enums;

import java.nio.file.CopyOption;
import java.nio.file.StandardCopyOption;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

/**
 * @author mirko.scotti
 * @version Feb 05, 2026
 */
public enum CopyFlag
{

	IS_REPLACEABLE(item -> item.contains(StandardCopyOption.REPLACE_EXISTING));

	private final Predicate<Set<? extends CopyOption>> predicate;

	private CopyFlag(Predicate<Set<? extends CopyOption>> predicate)
	{
		this.predicate = predicate;
	}

	public boolean matches(Set<? extends CopyOption> options)
	{
		var set = Optional.ofNullable(options).orElseGet(Set::of);
		return predicate.test(set);
	}
}
