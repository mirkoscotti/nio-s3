package io.github.mirkoscotti.nio.s3.helpers;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.junit.jupiter.api.Assertions;
import org.junit.platform.commons.function.Try;
import org.junit.platform.commons.support.ReflectionSupport;
import org.junit.platform.commons.util.ReflectionUtils;
import org.junit.platform.commons.util.ReflectionUtils.HierarchyTraversalMode;

/**
 * @author mirko.scotti
 * @version Dec 19, 2024
 */
public final class JunitHelper
{

	private JunitHelper()
	{
		super();
	}

	public static void tryRun(Callable<Void> callable)
	{
		Try.call(callable).ifFailure(Assertions::fail).toOptional();
	}

	public static <T> T tryCall(Callable<T> callable)
	{
		return Try.call(callable)
				  .ifFailure(Assertions::fail)
				  .toOptional()
				  .orElseGet(Assertions::fail);
	}

	public static <T> void failCall(Class<? extends Exception> exceptionType, Callable<T> callable)
	{
		var optional = Try.call(callable)
						  .ifFailure(item -> Assertions.assertInstanceOf(exceptionType, item))
						  .toOptional();
		Assertions.assertTrue(optional.isEmpty());
	}

	public static Field findFieldByName(Class<?> sourceClass, String fieldName)
	{
		return ReflectionUtils.streamFields(sourceClass, item -> item.getName().equals(fieldName),
											HierarchyTraversalMode.TOP_DOWN)
							  .findAny()
							  .orElseThrow(() -> new IllegalArgumentException("Expected one field named %s in class %s, not found.".formatted(fieldName,
																																			  sourceClass.getName())));
	}

	public static Field findStaticFieldByName(Class<?> sourceClass, String fieldName)
	{
		return ReflectionUtils.streamFields(sourceClass,
											item -> Modifier.isStatic(item.getModifiers())
												&& item.getName().equals(fieldName),
											HierarchyTraversalMode.TOP_DOWN)
							  .findAny()
							  .orElseThrow(() -> new IllegalArgumentException("Expected one static field named %s in class %s, not found.".formatted(fieldName,
																																					 sourceClass.getName())));
	}

	public static Field findFieldByType(Class<?> sourceClass, Class<?> fieldType)
	{
		var list = ReflectionUtils.findFields(sourceClass, item -> item.getType() == fieldType,
											  HierarchyTraversalMode.TOP_DOWN);
		return Optional.of(list)
					   .filter(item -> item.size() == 1)
					   .map(item -> item.get(0))
					   .orElseThrow(() -> new IllegalArgumentException("Expected one field of type %s in class %s, found %d".formatted(fieldType.getName(),
																																	   sourceClass.getName(),
																																	   list.size())));
	}

	public static Field findStaticFieldByType(Class<?> sourceClass, Class<?> fieldType)
	{
		var list = ReflectionUtils.findFields(sourceClass,
											  item -> Modifier.isStatic(item.getModifiers())
												  && item.getType() == fieldType,
											  HierarchyTraversalMode.TOP_DOWN);
		return Optional.of(list)
					   .filter(item -> item.size() == 1)
					   .map(item -> item.get(0))
					   .orElseThrow(() -> new IllegalArgumentException("Expected one static field of type %s in class %s, found %d".formatted(fieldType.getName(),
																																			  sourceClass.getName(),
																																			  list.size())));
	}

	public static Field findFieldByGenericType(Class<?> sourceClass,
											   Class<?> fieldType,
											   Class<?>... arguments)
	{
		var list = ReflectionUtils.findFields(sourceClass, item -> item.getType() == fieldType
			&& item.getGenericType() instanceof ParameterizedType parameterizedType
			&& matchesParameters(parameterizedType, arguments), HierarchyTraversalMode.TOP_DOWN);
		return Optional.of(list)
					   .filter(item -> item.size() == 1)
					   .map(item -> item.get(0))
					   .orElseThrow(() -> new IllegalArgumentException("Expected one field of generic type %s in class %s, found %d".formatted(fieldType.getName(),
																																			   sourceClass.getName(),
																																			   list.size())));
	}

	public static Field findStaticFieldByGenericType(Class<?> sourceClass,
													 Class<?> fieldType,
													 Class<?>... arguments)
	{
		var list = ReflectionUtils.findFields(sourceClass, item -> Modifier
																		   .isStatic(item.getModifiers())
			&& item.getType() == fieldType
			&& item.getGenericType() instanceof ParameterizedType parameterizedType
			&& matchesParameters(parameterizedType, arguments), HierarchyTraversalMode.TOP_DOWN);
		return Optional.of(list)
					   .filter(item -> item.size() == 1)
					   .map(item -> item.get(0))
					   .orElseThrow(() -> new IllegalArgumentException("Expected one field of generic type %s in class %s, found %d".formatted(fieldType.getName(),
																																			   sourceClass.getName(),
																																			   list.size())));
	}

	public static <T> T findFieldValueByName(Object instance, String fieldName, Class<T> fieldType)
	{
		var field = findFieldByName(instance.getClass(), fieldName);
		var result = findFieldValue(instance, field);
		return Assertions.assertInstanceOf(fieldType, result);
	}

	public static <T> T findStaticFieldValueByName(Class<?> sourceClass,
												   String fieldName,
												   Class<T> fieldType)
	{
		var field = findStaticFieldByName(sourceClass, fieldName);
		var result = findStaticFieldValue(field);
		return Assertions.assertInstanceOf(fieldType, result);
	}

	public static <T> T findFieldValueByType(Object instance, Class<T> fieldType)
	{
		var field = findFieldByType(instance.getClass(), fieldType);
		var result = findFieldValue(instance, field);
		return Assertions.assertInstanceOf(fieldType, result);
	}

	public static <T> T findStaticFieldValueByType(Class<?> sourceClass, Class<T> fieldType)
	{
		var field = findFieldByType(sourceClass, fieldType);
		var result = findStaticFieldValue(field);
		return Assertions.assertInstanceOf(fieldType, result);
	}

	public static <T> T findFieldValueByGenericType(Object instance,
													Class<T> fieldType,
													Class<?>... arguments)
	{
		var field = findFieldByGenericType(instance.getClass(), fieldType, arguments);
		var result = findFieldValue(instance, field);
		return Assertions.assertInstanceOf(fieldType, result);
	}

	public static <T> T findStaticFieldValueByGenericType(Class<?> sourceClass,
														  Class<T> fieldType,
														  Class<?>... arguments)
	{
		var field = findStaticFieldByGenericType(sourceClass, fieldType, arguments);
		var result = findStaticFieldValue(field);
		return Assertions.assertInstanceOf(fieldType, result);
	}

	public static <T> Stream<T> findFieldValues(Object instance, Class<T> fieldType)
	{
		return ReflectionUtils.streamFields(instance.getClass(),
											item -> item.getType() == fieldType,
											HierarchyTraversalMode.TOP_DOWN)
							  .map(item -> findFieldValue(instance, item))
							  .map(item -> Assertions.assertInstanceOf(fieldType, item));
	}

	public static <T> Stream<T> findStaticFieldValues(Class<?> sourceClass, Class<T> fieldType)
	{
		return ReflectionUtils.streamFields(sourceClass,
											item -> Modifier.isStatic(item.getModifiers())
												&& item.getType() == fieldType,
											HierarchyTraversalMode.TOP_DOWN)
							  .map(JunitHelper::findStaticFieldValue)
							  .map(item -> Assertions.assertInstanceOf(fieldType, item));
	}

	public static Object findFieldValue(Object instance, Field field)
	{
		ReflectionSupport.makeAccessible(field);
		return tryCall(() -> field.get(instance));
	}

	public static Object findStaticFieldValue(Field field)
	{
		ReflectionSupport.makeAccessible(field);
		return tryCall(() -> field.get(null));
	}

	private static boolean matchesParameters(ParameterizedType type, Class<?>... arguments)
	{
		var expectedArguments = type.getActualTypeArguments();
		return expectedArguments.length == arguments.length
			&& IntStream.range(0, expectedArguments.length)
						.allMatch(i -> expectedArguments[i].equals(arguments[i]));
	}
}
