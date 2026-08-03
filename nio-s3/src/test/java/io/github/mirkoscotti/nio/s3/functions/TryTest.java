package io.github.mirkoscotti.nio.s3.functions;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
	<T> void tryWithoutCatchAndFinallyTest(@Mock Callable<T> tryCallable) throws Exception
	{
		when(tryCallable.call()).thenThrow(Exception.class);
		var tryStatement = Try.to(tryCallable);
		Assertions.assertThrows(IllegalStateException.class, tryStatement::run);
	}

	@Test
	<T> void callableTest(@Mock Callable<T> tryCallable, @Mock Callable<Void> finallyCallable)
		throws Exception
	{
		var tryStatement = Try.to(tryCallable).onFinally(finallyCallable);
		verify(tryCallable, never()).call();
		verify(finallyCallable, never()).call();
		tryStatement.run();
		verify(tryCallable, Mockito.atLeastOnce()).call();
		verify(finallyCallable, Mockito.atLeastOnce()).call();
	}

	@Test
	<T> void callableThrowingExceptionTest(@Mock Callable<T> tryCallable,
										   @Mock Consumer<Exception> catchConsumer,
										   @Mock Callable<Void> finallyCallable)
		throws Exception
	{
		when(tryCallable.call()).thenThrow(Exception.class);
		when(finallyCallable.call()).thenThrow(Exception.class);
		Try.to(tryCallable).onCatch(catchConsumer).onFinally(finallyCallable).run();
		verify(tryCallable, Mockito.atLeastOnce()).call();
		verify(catchConsumer, Mockito.atLeastOnce()).accept(Mockito.any(Exception.class));
		verify(finallyCallable, Mockito.atLeastOnce()).call();
	}
}
