package io.github.mirkoscotti.nio.s3.enums;

import java.nio.file.CopyOption;
import java.nio.file.StandardCopyOption;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

/**
 * A predicate-based flag that inspects a set of <code>CopyOption</code> to determine whether a
 * specific copy behavior has been requested.
 *
 * @author mirko.scotti
 * @version Feb 05, 2026
 */
public enum CopyFlag
{

	/**
	 * The replace existing directive.
	 */
	IS_REPLACEABLE(item -> item.contains(StandardCopyOption.REPLACE_EXISTING));

	private final Predicate<Set<? extends CopyOption>> predicate;

	private CopyFlag(Predicate<Set<? extends CopyOption>> predicate)
	{
		this.predicate = predicate;
	}

	/**
	 * Checks whether the current item is contained in the given set of options.
	 *
	 * @param options
	 *            the set of options
	 * @return true if this item is one of the element of the set, false otherwise
	 */
	public boolean matches(Set<? extends CopyOption> options)
	{
		var set = Optional.ofNullable(options).orElseGet(Set::of);
		return predicate.test(set);
	}
}
