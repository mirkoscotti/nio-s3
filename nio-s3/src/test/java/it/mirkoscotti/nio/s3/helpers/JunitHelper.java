/*
 * (C) Copyright 2019 - 2024 - Add Value S.R.L - All rights reserved.
 */

package it.mirkoscotti.nio.s3.helpers;

import java.lang.reflect.Field;
import java.util.concurrent.Callable;

import org.junit.jupiter.api.Assertions;
import org.junit.platform.commons.function.Try;
import org.junit.platform.commons.util.ReflectionUtils;
import org.junit.platform.commons.util.ReflectionUtils.HierarchyTraversalMode;

/**
 * @author Add Value S.R.L by mirko.scotti
 * @version Dec 19, 2024
 */
public final class JunitHelper
{

	private JunitHelper()
	{
		super();
	}

	public static Field findFieldByName(Class<?> sourceClass, String fieldName)
	{
		return ReflectionUtils.streamFields(sourceClass,
											item -> item.getName().equals(fieldName),
											HierarchyTraversalMode.TOP_DOWN)
							  .findAny()
							  .orElseGet(Assertions::fail);
	}

	public static Field findFieldByType(Class<?> sourceClass, Class<?> fieldType)
	{
		return ReflectionUtils.streamFields(sourceClass,
											item -> item.getType() == fieldType,
											HierarchyTraversalMode.TOP_DOWN)
							  .findAny()
							  .orElseGet(Assertions::fail);
	}

	public static <T> T tryCall(Callable<T> callable)
	{
		return Try.call(callable)
				  .ifFailure(Assertions::fail)
				  .toOptional()
				  .orElseGet(Assertions::fail);
	}
}
