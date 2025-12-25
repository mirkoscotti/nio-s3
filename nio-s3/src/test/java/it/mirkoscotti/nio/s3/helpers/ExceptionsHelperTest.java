package it.mirkoscotti.nio.s3.helpers;

import java.nio.file.FileSystemNotFoundException;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;

/**
 * @author mirko.scotti
 * @version Dec 25, 2025
 */
@ExtendWith(MockitoExtension.class)
class ExceptionsHelperTest
{

	@Test
	<X extends Exception> void throwIllegalStateExceptionTest(@Mock X exception)
	{
		Assertions.assertThrows(IllegalStateException.class,
								() -> ExceptionsHelper.sneakyThrow(exception));
	}

	@Test
	<X extends RuntimeException> void throwRuntimeExceptionTest(@Mock X exception)
	{
		Assertions.assertThrows(exception.getClass(),
								() -> ExceptionsHelper.sneakyThrow(exception));
	}

	@Test
	void throwExecutionExceptionTest(@Mock ExecutionException exception,
									 @Mock AwsServiceException cause)
	{
		Mockito.when(exception.getCause()).thenReturn(cause);
		Assertions.assertThrows(cause.getClass(), () -> ExceptionsHelper.sneakyThrow(exception));
	}

	@Test
	void throwCompletionExceptionTest(@Mock CompletionException exception,
									  @Mock AwsServiceException cause)
	{
		Mockito.when(exception.getCause()).thenReturn(cause);
		Assertions.assertThrows(cause.getClass(), () -> ExceptionsHelper.sneakyThrow(exception));
	}

	@Test
	void throwNoSuchBucketExceptionTest(@Mock NoSuchBucketException exception,
										@Mock AwsServiceException cause)
	{
		Assertions.assertThrows(FileSystemNotFoundException.class,
								() -> ExceptionsHelper.sneakyThrow(exception));
	}
}
