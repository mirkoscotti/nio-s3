package it.mirkoscotti.nio.s3.exceptions;

import java.io.IOException;
import java.util.Optional;
import java.util.stream.Stream;

import it.mirkoscotti.nio.s3.enums.ErrorCode;
import it.mirkoscotti.nio.s3.helpers.ExceptionHelper;

import software.amazon.awssdk.awscore.exception.AwsErrorDetails;
import software.amazon.awssdk.awscore.exception.AwsServiceException;

/**
 * @author mirko.scotti
 * @version Apr 04, 2026
 */
public class TransportException
	extends RuntimeException
{

	private static final long serialVersionUID = 1852470812710945029L;

	private final AwsErrorDetails awsErrorDetails;

	/**
	 * @param cause
	 */
	public TransportException(Throwable cause)
	{
		super(cause);
		awsErrorDetails = Optional.ofNullable(ExceptionHelper.toAwsServiceException(cause))
								  .map(AwsServiceException::awsErrorDetails)
								  .orElse(null);
	}

	public void throwNioException() throws IOException
	{
		throwNioException(null);
	}

	public void throwNioException(String message) throws IOException
	{
		var finalMessage = Optional.ofNullable(message).orElseGet(this::getMessage);
		var exception = ErrorCode.of(awsErrorDetails)
								 .map(item -> item.nioException(finalMessage))
								 .orElseGet(() -> new IOException(finalMessage));
		if (exception instanceof RuntimeException runtimeException)
		{
			Stream.of(getSuppressed()).forEach(runtimeException::addSuppressed);
			throw runtimeException;
		}
		else
		{
			var ioException = ExceptionHelper.toIoException(exception);
			Stream.of(getSuppressed()).forEach(ioException::addSuppressed);
			throw ioException;
		}
	}

	public Exception toNioException()
	{
		return toNioException(null);
	}

	public Exception toNioException(String message)
	{
		var finalMessage = Optional.ofNullable(message).orElseGet(this::getMessage);
		return ErrorCode.of(awsErrorDetails)
						.map(item -> item.nioException(finalMessage))
						.orElseGet(() -> new IOException(finalMessage));
	}
}
