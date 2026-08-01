package io.github.mirkoscotti.nio.s3.exceptions;

import java.io.IOException;
import java.util.Optional;
import java.util.stream.Stream;

import io.github.mirkoscotti.nio.s3.enums.ErrorCode;
import io.github.mirkoscotti.nio.s3.functions.Transformer;
import io.github.mirkoscotti.nio.s3.helpers.ExceptionHelper;

import software.amazon.awssdk.awscore.exception.AwsErrorDetails;
import software.amazon.awssdk.awscore.exception.AwsServiceException;

/**
 * The exception facade converting AWS exceptions to standard NIO.2 ones.
 *
 * @author mirko.scotti
 * @version Apr 04, 2026
 */
public class TransportException
	extends RuntimeException
{

	private static final long serialVersionUID = 1852470812710945029L;

	/**
	 * The AWS error details.
	 */
	private final AwsErrorDetails awsErrorDetails;

	/**
	 * Creates an exception based on the real AWS one.
	 *
	 * @param cause
	 *            the real AWS exception
	 */
	public TransportException(AwsServiceException cause)
	{
		super(cause);
		awsErrorDetails = Optional.ofNullable(cause)
								  .map(AwsServiceException::awsErrorDetails)
								  .orElse(null);
	}

	/**
	 * Wrapper of {@link #throwNioException(String)} setting the message of this exception.
	 *
	 * @throws IOException
	 *             the NIO.2 exception or a generic I/O exception if conversion was not supported
	 */
	public void throwNioException() throws IOException
	{
		throwNioException(null);
	}

	/**
	 * Converts this exception to a NIO.2 exception or a generic I/O exception when the real AWS
	 * exception has no mapping.
	 *
	 * @param message
	 *            the original AWS message
	 * @throws IOException
	 *             the NIO.2 exception or a generic I/O exception if conversion was not supported
	 * @see ErrorCode
	 */
	public void throwNioException(String message) throws IOException
	{
		var finalMessage = Optional.ofNullable(message).orElseGet(this::getMessage);
		var exception = ErrorCode.of(awsErrorDetails)
								 .map(item -> item.nioException(finalMessage))
								 .orElseGet(() -> new IOException(finalMessage));
		Transformer.of(exception)
				   .when(RuntimeException.class::isInstance)
				   .then(ExceptionHelper::sneakyThrow)
				   .orThrow(() -> redirectToIoException(exception));
	}

	/**
	 * Same as {@link #throwNioException()} without throwing the exception.
	 *
	 * @return the NIO.2 exception
	 */
	public Exception toNioException()
	{
		return toNioException(null);
	}

	/**
	 * Same as {@link #throwNioException(String)} without throwing the exception.
	 *
	 * @param message
	 *            the error message to be propagated
	 *
	 * @return the NIO.2 exception
	 */
	public Exception toNioException(String message)
	{
		var finalMessage = Optional.ofNullable(message).orElseGet(this::getMessage);
		return ErrorCode.of(awsErrorDetails)
						.map(item -> item.nioException(finalMessage))
						.orElseGet(() -> new IOException(finalMessage));
	}

	/**
	 * The real AWS error code mapped to an {@link ErrorCode} instance.
	 *
	 * @return the error code item
	 */
	public Optional<ErrorCode> toErrorCode()
	{
		return ErrorCode.of(awsErrorDetails);
	}

	private IOException redirectToIoException(Exception exception)
	{
		var result = ExceptionHelper.toIoException(exception);
		Stream.of(getSuppressed()).forEach(result::addSuppressed);
		return result;
	}
}
