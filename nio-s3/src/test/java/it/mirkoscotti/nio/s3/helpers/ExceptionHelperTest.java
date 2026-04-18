package it.mirkoscotti.nio.s3.helpers;

import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import io.github.mirkoscotti.nio.s3.exceptions.TransportException;

import software.amazon.awssdk.awscore.exception.AwsServiceException;

/**
 * @author mirko.scotti
 * @version Dec 25, 2025
 */
@ExtendWith(MockitoExtension.class)
class ExceptionHelperTest
{

	@Test
	<T extends Throwable> void throwInterruptedExceptionTest(@Mock InterruptedException exception)
	{
		var runtimeException = Assertions.assertThrows(IllegalStateException.class,
													   () -> ExceptionHelper.sneakyThrow(exception));
		Assertions.assertEquals(exception, runtimeException.getCause());
	}

	@Test
	<T extends Throwable> void throwIllegalStateExceptionTest(@Mock T throwable)
	{
		var exception = Assertions.assertThrows(IllegalStateException.class,
												() -> ExceptionHelper.sneakyThrow(throwable));
		Assertions.assertEquals(throwable, exception.getCause());
	}

	@Test
	<X extends RuntimeException> void throwRuntimeExceptionTest(@Mock X exception)
	{
		Assertions.assertThrows(exception.getClass(), () -> ExceptionHelper.sneakyThrow(exception));
	}

	@Test
	void throwExecutionExceptionTest(@Mock ExecutionException exception,
									 @Mock AwsServiceException cause)
	{
		throwExceptionWithCauseTest(exception, cause);
	}

	@Test
	void throwCompletionExceptionTest(@Mock CompletionException exception,
									  @Mock AwsServiceException cause)
	{
		throwExceptionWithCauseTest(exception, cause);
	}

	void throwExceptionWithCauseTest(Exception exception, AwsServiceException cause)
	{
		Mockito.when(exception.getCause()).thenReturn(cause);
		var transportException = Assertions.assertThrows(TransportException.class,
														 () -> ExceptionHelper.sneakyThrow(exception));
		Assertions.assertEquals(cause, transportException.getCause());
	}
}
