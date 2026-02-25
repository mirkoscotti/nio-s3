package it.mirkoscotti.nio.s3.helpers;

import java.io.IOException;
import java.nio.file.FileSystemNotFoundException;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;

import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;

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

	public static <T> T sneakyThrow(Throwable throwable)
	{
		throw redirectException(throwable);
	}

	public static RuntimeException redirectException(Throwable throwable)
	{
		return switch (throwable)
		{
			case NoSuchBucketException exception -> new FileSystemNotFoundException(exception.getMessage());
			default -> toAwsServiceException(throwable);
		};
	}

	public static IOException throwIoException(Exception exception) throws IOException
	{
		throw exception instanceof IOException ioException
			? ioException
			: new IOException(exception);
	}

	public static AwsServiceException toAwsServiceException(Throwable throwable)
	{
		return switch (throwable)
		{
			case AwsServiceException exception -> exception;
			case CompletionException exception -> toAwsServiceException(exception.getCause());
			case ExecutionException exception -> toAwsServiceException(exception.getCause());
			case RuntimeException exception -> throw exception;
			default -> throw new IllegalStateException(throwable);
		};
	}
}
