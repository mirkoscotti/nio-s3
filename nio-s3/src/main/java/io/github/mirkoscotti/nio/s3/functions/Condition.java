package io.github.mirkoscotti.nio.s3.functions;

import java.io.IOException;

/**
 * The equivalent of a predicate managing I/O exceptions.
 *
 * @param <T>
 *            any type
 * @author mirko.scotti
 * @version Feb 12, 2026
 */
@FunctionalInterface
public interface Condition<T>
{

	/**
	 * The specific condition never satisfied.
	 */
	static Condition<?> FALSE = item -> false;

	/**
	 * Checks whether the given object matches the condition.
	 *
	 * @param value
	 *            the object to be evaluated against the condition
	 * @return true if the given object matches the condition, false otherwise
	 * @throws IOException
	 *             if an I/O error occurs
	 */
	boolean isSatisfied(T value) throws IOException;

	/**
	 * A special condition never satisfied independently on the evaluated object.
	 *
	 * @param <T>
	 *            the type of the object
	 * @return the never satisfied condition
	 */
	public static <T> Condition<T> unsatisfied()
	{
		@SuppressWarnings("unchecked")
		var result = (Condition<T>) FALSE;
		return result;
	}

	/**
	 * The condition negating the given one.
	 *
	 * @param <T>
	 *            the type of the object
	 * @param condition
	 *            the condition to be negated
	 * @return the negating condition
	 */
	public static <T> Condition<T> not(Condition<T> condition)
	{
		return item -> !condition.isSatisfied(item);
	}
}
