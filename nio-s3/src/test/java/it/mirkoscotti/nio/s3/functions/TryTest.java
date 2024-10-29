/*
 * (C) Copyright 2019 - 2024 - Add Value S.R.L - All rights reserved.
 */

package it.mirkoscotti.nio.s3.functions;

import java.util.Map;
import java.util.concurrent.Callable;
import java.util.function.Consumer;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author mirko.scotti
 * @version Oct 24, 2024
 */
@ExtendWith(MockitoExtension.class)
class TryTest
{

	@Test
	<T> void nullTest(@Mock Callable<T> callable,
					  @Mock Converter<Map<String, AutoCloseable>, T> converter,
					  @Mock AutoCloseable resource)
	{
		Assertions.assertThrows(NullPointerException.class, () -> Try.to(null));
		var basicTry = Try.to(callable);
		Assertions.assertThrows(NullPointerException.class, () -> basicTry.onCatch(null));
		Assertions.assertThrows(NullPointerException.class, () -> basicTry.onFinally(null));
		Assertions.assertThrows(NullPointerException.class,
								() -> Try.withResource(null, null, null));
		Assertions.assertThrows(NullPointerException.class,
								() -> Try.withResource(converter, null, null));
		Assertions.assertThrows(NullPointerException.class,
								() -> Try.withResource(converter, "key", null));
		var tryWithResource = Try.withResource(converter, "key1", resource);
		Assertions.assertThrows(NullPointerException.class, () -> tryWithResource.and(null, null));
		Assertions.assertThrows(NullPointerException.class,
								() -> tryWithResource.and("key2", null));
	}

	@Test
	<T> void tryWithoutCatchAndFinallyTest(@Mock Callable<T> tryCallable)
	{
		try
		{
			Mockito.when(tryCallable.call()).thenThrow(Exception.class);
			var tryStatement = Try.to(tryCallable);
			Assertions.assertThrows(IllegalStateException.class, () -> tryStatement.run());
		}
		catch (Exception x)
		{
			Assertions.fail(x);
		}
	}

	@Test
	<T> void callableTest(@Mock Callable<T> tryCallable, @Mock Callable<Void> finallyCallable)
	{
		try
		{
			var tryStatement = Try.to(tryCallable).onFinally(finallyCallable);
			Mockito.verify(tryCallable, Mockito.never()).call();
			Mockito.verify(finallyCallable, Mockito.never()).call();
			tryStatement.run();
			Mockito.verify(tryCallable, Mockito.atLeastOnce()).call();
			Mockito.verify(finallyCallable, Mockito.atLeastOnce()).call();
		}
		catch (Exception x)
		{
			Assertions.fail(x);
		}
	}

	@Test
	<T> void callableThrowingExceptionTest(@Mock Callable<T> tryCallable,
										   @Mock Consumer<Exception> catchConsumer,
										   @Mock Callable<Void> finallyCallable)
	{
		try
		{
			Mockito.when(tryCallable.call()).thenThrow(Exception.class);
			Mockito.when(finallyCallable.call()).thenThrow(Exception.class);
			Try.to(tryCallable).onCatch(catchConsumer).onFinally(finallyCallable).run();
			Mockito.verify(tryCallable, Mockito.atLeastOnce()).call();
			Mockito.verify(catchConsumer, Mockito.atLeastOnce())
				   .accept(Mockito.any(Exception.class));
			Mockito.verify(finallyCallable, Mockito.atLeastOnce()).call();
		}
		catch (Exception x)
		{
			Assertions.fail(x);
		}
	}
}
