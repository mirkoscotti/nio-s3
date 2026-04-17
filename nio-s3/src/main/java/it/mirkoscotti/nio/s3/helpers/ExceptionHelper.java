package it.mirkoscotti.nio.s3.helpers;

import java.io.IOException;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;

import it.mirkoscotti.nio.s3.exceptions.TransportException;

import software.amazon.awssdk.awscore.exception.AwsServiceException;

/**
 * @author mirko.scotti
 * @version May 24, 2025
 */
public final class ExceptionHelper
{

	private ExceptionHelper()
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
			case InterruptedException exception -> interruptThread(exception);
			case CompletionException exception -> redirectException(exception.getCause());
			case ExecutionException exception -> redirectException(exception.getCause());
			case AwsServiceException exception -> new TransportException(exception);
			case RuntimeException exception -> exception;
			default -> new IllegalStateException(throwable);
		};
	}

	public static <T> T throwIoException(Exception exception) throws IOException
	{
		throw toIoException(exception);
	}

	public static IOException toIoException(Exception exception)
	{
		return exception instanceof IOException ioException
			? ioException
			: new IOException(exception);
	}

	public static RuntimeException interruptThread(InterruptedException exception)
	{
		Thread.currentThread().interrupt();
		return new IllegalStateException(exception);
	}
}
