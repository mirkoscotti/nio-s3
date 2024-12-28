package it.mirkoscotti.nio.s3.functions;

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
	<T> void nullTest(@Mock Callable<T> callable, @Mock AutoCloseable resource)
	{
		Assertions.assertThrows(NullPointerException.class, () -> Try.to(null));
		var basicTry = Try.to(callable);
		Assertions.assertThrows(NullPointerException.class, () -> basicTry.onCatch(null));
		Assertions.assertThrows(NullPointerException.class, () -> basicTry.onFinally(null));
	}

	@Test
	<T> void tryWithoutCatchAndFinallyTest(@Mock Callable<T> tryCallable)
	{
		try
		{
			Mockito.when(tryCallable.call()).thenThrow(Exception.class);
			var tryStatement = Try.to(tryCallable);
			Assertions.assertThrows(IllegalStateException.class, tryStatement::run);
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
