package it.mirkoscotti.nio.s3.helpers;

import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;

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

	public static <T> T redirectException(Throwable throwable)
	{
		var exception = toS3Exception(throwable);
		throw new IllegalStateException(exception);
	}

	public static S3Exception toS3Exception(Throwable throwable)
	{
		return switch (throwable)
		{
			case S3Exception exception -> exception;
			case CompletionException exception -> toS3Exception(exception.getCause());
			case ExecutionException exception -> toS3Exception(exception.getCause());
			case RuntimeException exception -> throw exception;
			default -> throw new IllegalStateException(throwable);
		};
	}
}
