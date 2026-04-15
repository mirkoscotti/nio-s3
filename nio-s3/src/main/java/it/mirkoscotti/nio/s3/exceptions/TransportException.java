package it.mirkoscotti.nio.s3.exceptions;

import java.io.IOException;
import java.util.Optional;
import java.util.stream.Stream;

import it.mirkoscotti.nio.s3.enums.ErrorCode;
import it.mirkoscotti.nio.s3.functions.Transformer;
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
	public TransportException(AwsServiceException cause)
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
		Transformer.of(exception)
				   .when(RuntimeException.class::isInstance)
				   .then(this::throwAsRuntimeException)
				   .orThrow(() -> redirectToIoException(exception));
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

	public Optional<ErrorCode> toErrorCode()
	{
		return ErrorCode.of(awsErrorDetails);
	}

	private RuntimeException throwAsRuntimeException(Exception exception)
	{
		var result = ExceptionHelper.redirectException(exception);
		Stream.of(getSuppressed()).forEach(result::addSuppressed);
		throw result;
	}

	private IOException redirectToIoException(Exception exception)
	{
		var result = ExceptionHelper.toIoException(exception);
		Stream.of(getSuppressed()).forEach(result::addSuppressed);
		return result;
	}
}
