package io.github.mirkoscotti.nio.s3.enums;

import java.lang.reflect.Constructor;
/**
 * @author mirko.scotti
 * @version Apr 04, 2026
 */
import java.nio.file.AccessDeniedException;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.FileSystemAlreadyExistsException;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.InvalidPathException;
import java.nio.file.NoSuchFileException;
import java.util.Comparator;
import java.util.Optional;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import it.mirkoscotti.nio.s3.functions.Try;
import it.mirkoscotti.nio.s3.helpers.ExceptionHelper;

import software.amazon.awssdk.awscore.exception.AwsErrorDetails;

public enum ErrorCode
{

	BUCKET_ALREADY_EXISTS(FileSystemAlreadyExistsException.class),
	BUCKET_ALREADY_OWNED_BY_YOU(null),
	INVALID_ACCESS_KEY_ID(AccessDeniedException.class),
	INVALID_BUCKET_NAME(IllegalArgumentException.class),
	INVALID_REQUEST(IllegalStateException.class),
	NO_SUCH_BUCKET_POLICY(null),
	NO_SUCH_KEY(NoSuchFileException.class),
	NO_SUCH_UPLOAD(null),
	// ---- GESTITO FINO QUA ----
	ACCESS_DENIED(AccessDeniedException.class),
	ACCOUNT_PROBLEM(null),
	ALL_ACCESS_DISABLED(AccessDeniedException.class),
	AMBIGUOUS_GRANT_BY_EMAIL_ADDRESS(null),
	AUTHORIZATION_HEADER_MALFORMED(null),
	BAD_DIGEST(null),
	BUCKET_NOT_EMPTY(DirectoryNotEmptyException.class),
	CREDENTIALS_NOT_SUPPORTED(null),
	CROSS_LOCATION_LOGGING_PROHIBITED(null),
	ENTITY_TOO_LARGE(null),
	ENTITY_TOO_SMALL(null),
	EXPIRED_TOKEN(AccessDeniedException.class),
	ILLEGAL_LOCATION_CONSTRAINT_EXCEPTION(null),
	ILLEGAL_VERSIONING_CONFIGURATION_EXCEPTION(null),
	INCOMPLETE_BODY(null),
	INCORRECT_ENDPOINT(null),
	INLINE_DATA_TOO_LARGE(null),
	INTERNAL_ERROR(null),
	INVALID_ARGUMENT(null),
	INVALID_DIGEST(null),
	INVALID_LOCATION_CONSTRAINT(null),
	INVALID_OBJECT_STATE(null),
	INVALID_PART(null),
	INVALID_PART_ORDER(null),
	INVALID_POLICY_DOCUMENT(null),
	INVALID_RANGE(null),
	INVALID_SECURITY(AccessDeniedException.class),
	INVALID_STORAGE_CLASS(null),
	INVALID_URI(InvalidPathException.class),
	KEY_TOO_LONG_ERROR(InvalidPathException.class),
	MALFORMED_ACL_ERROR(null),
	MALFORMED_POST_REQUEST(null),
	MALFORMED_XML(null),
	MAX_MESSAGE_LENGTH_EXCEEDED(null),
	MAX_POST_PRE_DATA_LENGTH_EXCEEDED_ERROR(null),
	METADATA_TOO_LARGE(null),
	METHOD_NOT_ALLOWED(null),
	MISSING_CONTENT_LENGTH(null),
	MISSING_REQUEST_BODY_ERROR(null),
	NO_SUCH_BUCKET(FileSystemNotFoundException.class),
	NO_SUCH_CORS_CONFIGURATION(null),
	NO_SUCH_LIFECYCLE_CONFIGURATION(null),
	NO_SUCH_VERSION(NoSuchFileException.class),
	NOT_IMPLEMENTED(null),
	OPERATION_ABORTED(null),
	PERMANENT_REDIRECT(null),
	PRECONDITION_FAILED(null),
	REDIRECT(null),
	REQUEST_TIMEOUT(null),
	REQUEST_TIME_TOO_SKEWED(null),
	RESTORE_ALREADY_IN_PROGRESS(null),
	SERVICE_UNAVAILABLE(null),
	SIGNATURE_DOES_NOT_MATCH(AccessDeniedException.class),
	SLOW_DOWN(null),
	TEMPORARY_REDIRECT(null),
	TOKEN_REFRESH_REQUIRED(AccessDeniedException.class),
	TOO_MANY_BUCKETS(FileSystemAlreadyExistsException.class),
	UNEXPECTED_CONTENT(null),
	UNSUPPORTED_ARGUMENT(null),
	UNSUPPORTED_SIGNATURE(null),
	UNRESOLVABLE_GRANT_BY_EMAIL_ADDRESS(null);

	private static final String SEPARATOR = "_";

	private final Class<? extends Exception> nioException;

	ErrorCode(Class<? extends Exception> nioException)
	{
		this.nioException = nioException;
	}

	public Exception nioException(String... parameters)
	{
		var parameterCount = parameters == null ? 0 : parameters.length;
		var result = Try.to(() -> matchingConstructor(parameterCount).newInstance((Object[]) parameters))
						.onCatch(ExceptionHelper::sneakyThrow)
						.get();
		return (Exception) result;
	}

	public static Optional<ErrorCode> of(AwsErrorDetails awsErrorDetails)
	{
		var errorCode = awsErrorDetails.errorCode();
		var result = IntStream.range(0, errorCode.length())
							  .mapToObj(i -> transformCharacter(errorCode, i))
							  .reduce("", String::concat)
							  .toUpperCase();
		return resolve(result);
	}

	private static String transformCharacter(String source, int index)
	{
		var previous = index == 0 ? '\0' : source.charAt(index - 1);
		var current = source.charAt(index);
		var needsSeparator = Character.isLowerCase(previous) && Character.isUpperCase(current);
		return (needsSeparator ? SEPARATOR : "").concat(String.valueOf(current));
	}

	private static Optional<ErrorCode> resolve(String errorCode)
	{
		return Stream.of(ErrorCode.values())
					 .filter(item -> item.name().equals(errorCode))
					 .findAny();
	}

	private Constructor<?> matchingConstructor(int parameterCount)
	{
		return Stream.of(nioException.getDeclaredConstructors())
					 .filter(item -> Stream.of(item.getParameterTypes())
										   .allMatch(String.class::equals))
					 .sorted(Comparator.<Constructor<?>>comparingInt(item -> Math.abs(item.getParameterCount() - parameterCount))
									   .thenComparing(Comparator.<Constructor<?>>comparingInt(item -> item.getParameterCount() - parameterCount)
																.reversed()))
					 .findFirst()
					 .orElseThrow(() -> new IllegalStateException("NIO exception not instantiable."));
	}
}
