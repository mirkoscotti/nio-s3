package io.github.mirkoscotti.nio.s3.helpers;

import java.io.IOException;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;

import io.github.mirkoscotti.nio.s3.exceptions.TransportException;

import software.amazon.awssdk.awscore.exception.AwsServiceException;

/**
 * Utility methods for translating and rethrowing exceptions consistently across the library,
 * unwrapping asynchronous completion exceptions and converting AWS SDK exceptions into
 * {@link TransportException}.
 *
 * @author mirko.scotti
 * @version May 24, 2025
 * @see TransportException
 */
public final class ExceptionHelper
{

	private ExceptionHelper()
	{
		super();
	}

	/**
	 * Forces a runtime exception derived from the given throwable.
	 *
	 * @param <T>
	 *            any type
	 * @param throwable
	 *            the throwable to rethrow
	 * @return nothing because an exception is forced
	 * @throws RuntimeException
	 *             always
	 */
	public static <T> T sneakyThrow(Throwable throwable)
	{
		throw redirectException(throwable);
	}

	/**
	 * Unwrapper of the most common exceptions that can be thrown during an operation on AWS.
	 *
	 * @param throwable
	 *            the throwable to translate
	 * @return an instance of {@link TransportException} in case of well recognized AWS issue, a
	 *         generic <code>IllegalStateException</code> in case of an unpredicted issue
	 */
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

	/**
	 * Forces an I/O exception derived from the given exception.
	 *
	 * @param <T>
	 *            any type
	 * @param exception
	 *            the exception to rethrow
	 * @return nothing because an exception is forced
	 * @throws IOException
	 *             always
	 */
	public static <T> T throwIoException(Exception exception) throws IOException
	{
		throw toIoException(exception);
	}

	/**
	 * Converts the given exception into a generic I/O exception.
	 *
	 * @param exception
	 *            the exception to convert
	 * @return the exception itself if it is already an I/O exception, a new instance of an I/O
	 *         exception otherwise
	 */
	public static IOException toIoException(Exception exception)
	{
		return exception instanceof IOException ioException
			? ioException
			: new IOException(exception);
	}

	private static RuntimeException interruptThread(InterruptedException exception)
	{
		Thread.currentThread().interrupt();
		return new IllegalStateException(exception);
	}
}
