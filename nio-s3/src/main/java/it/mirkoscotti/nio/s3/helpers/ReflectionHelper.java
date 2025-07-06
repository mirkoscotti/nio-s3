package it.mirkoscotti.nio.s3.helpers;

import java.util.function.Predicate;

/**
 * @author mirko.scotti
 * @version Jun 23, 2025
 */
public final class ReflectionHelper
{

	private ReflectionHelper()
	{
		super();
	}

	public static Predicate<Class<?>> isAssignableFrom(Class<?> type)
	{
		return item -> item.isAssignableFrom(type);
	}
}
