package it.mirkoscotti.nio.s3.functions;

import java.io.IOException;

/**
 * @author mirko.scotti
 * @version Feb 12, 2026
 */
@FunctionalInterface
public interface Condition<T>
{

	static Condition<?> FALSE = item -> false;

	boolean isSatisfied(T value) throws IOException;

	public static <T> Condition<T> unsatisfied()
	{
		@SuppressWarnings("unchecked")
		var result = (Condition<T>) FALSE;
		return result;
	}
}
