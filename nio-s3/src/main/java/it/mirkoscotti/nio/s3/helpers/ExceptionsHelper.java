package it.mirkoscotti.nio.s3.helpers;

import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;

import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.services.s3.model.S3Exception;

/**
 * @author mirko.scotti
 * @version May 24, 2025
 */
public final class ExceptionsHelper
{

	private ExceptionsHelper()
	{
		super();
	}

	public static <X extends Exception> Void sneakyThrow(X exception)
	{
		sneakyThrow(new IllegalStateException(exception));
		return null;
	}

	public static <T, X extends RuntimeException> T sneakyThrow(X exception)
	{
		throw exception;
	}

	public static <T> T redirectException(Throwable throwable)
	{
		return redirectException(throwable, S3Exception.class);
	}

	public static <T, E extends AwsServiceException> T redirectException(Throwable throwable,
																		 Class<E> exceptionClass)
	{
		var exception = toAwsServiceException(throwable, exceptionClass);
		throw new IllegalStateException(exception);
	}

	public static <T> T throwS3Exception(Throwable throwable)
	{
		throw toS3Exception(throwable);
	}

	public static S3Exception toS3Exception(Throwable throwable)
	{
		return toAwsServiceException(throwable, S3Exception.class);
	}

	public static <T extends AwsServiceException> T toAwsServiceException(Throwable throwable,
																		  Class<T> exceptionClass)
	{
		return switch (throwable)
		{
			case AwsServiceException exception -> exceptionClass.cast(exception);
			case CompletionException exception -> toAwsServiceException(exception.getCause(),
																		exceptionClass);
			case ExecutionException exception -> toAwsServiceException(exception.getCause(),
																	   exceptionClass);
			case RuntimeException exception -> throw exception;
			default -> throw new IllegalStateException(throwable);
		};
	}
}
