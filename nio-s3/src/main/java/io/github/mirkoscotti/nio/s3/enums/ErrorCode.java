package io.github.mirkoscotti.nio.s3.enums;

import java.io.IOException;
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
import java.util.Objects;
import java.util.Optional;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import io.github.mirkoscotti.nio.s3.exceptions.TransportException;
import io.github.mirkoscotti.nio.s3.functions.Try;
import io.github.mirkoscotti.nio.s3.helpers.ExceptionHelper;
import io.github.mirkoscotti.nio.s3.operations.S3Connector;

import software.amazon.awssdk.awscore.exception.AwsErrorDetails;

/**
 * A mapper between AWS S3 error codes and semantically equivalent Java NIO.2 exceptions.
 * <p>
 * This enumeration provides a bridge between AWS SDK exceptions thrown during S3 operations and the
 * standard Java NIO.2 exception hierarchy. Each item represents a specific AWS error code that can
 * occur when interacting with S3 buckets or objects, and maps it to an appropriate NIO.2 exception
 * type.
 *
 * @author mirko.scotti
 * @version May 23, 2026
 * @see S3Connector
 * @see TransportException
 */
public enum ErrorCode
{

	/**
	 * AWS S3 error code <code>BucketAlreadyExists</code>.
	 */
	BUCKET_ALREADY_EXISTS(FileSystemAlreadyExistsException.class),
	/**
	 * AWS S3 error code <code>BucketAlreadyOwnedByYou</code>.
	 */
	BUCKET_ALREADY_OWNED_BY_YOU,
	/**
	 * AWS S3 error code <code>InvalidAccessKeyId</code>.
	 */
	INVALID_ACCESS_KEY_ID(AccessDeniedException.class),
	/**
	 * AWS S3 error code <code>InvalidBucketName</code>.
	 */
	INVALID_BUCKET_NAME(IllegalArgumentException.class),
	/**
	 * AWS S3 error code <code>InvalidRequest</code>.
	 */
	INVALID_REQUEST(IllegalStateException.class),
	/**
	 * AWS S3 error code <code>NoSuchBucketPolicy</code>.
	 */
	NO_SUCH_BUCKET_POLICY,
	/**
	 * AWS S3 error code <code>NoSuchKey</code>.
	 */
	NO_SUCH_KEY(NoSuchFileException.class),
	/**
	 * AWS S3 error code <code>NoSuchUpload</code>.
	 */
	NO_SUCH_UPLOAD,
	/**
	 * AWS S3 error code <code>AccessDenied</code>.
	 */
	ACCESS_DENIED(AccessDeniedException.class),
	/**
	 * AWS S3 error code <code>AccountProblem</code>.
	 */
	ACCOUNT_PROBLEM,
	/**
	 * AWS S3 error code <code>AllAccessDisabled</code>.
	 */
	ALL_ACCESS_DISABLED(AccessDeniedException.class),
	/**
	 * AWS S3 error code <code>AmbiguousGrantByEmailAddress</code>.
	 */
	AMBIGUOUS_GRANT_BY_EMAIL_ADDRESS,
	/**
	 * AWS S3 error code <code>AuthorizationHeaderMalformed</code>.
	 */
	AUTHORIZATION_HEADER_MALFORMED,
	/**
	 * AWS S3 error code <code>BadDigest</code>.
	 */
	BAD_DIGEST,
	/**
	 * AWS S3 error code <code>BucketNotEmpty</code>.
	 */
	BUCKET_NOT_EMPTY(DirectoryNotEmptyException.class),
	/**
	 * AWS S3 error code <code>CredentialsNotSupported</code>.
	 */
	CREDENTIALS_NOT_SUPPORTED,
	/**
	 * AWS S3 error code <code>CrossLocationLoggingProhibited</code>.
	 */
	CROSS_LOCATION_LOGGING_PROHIBITED,
	/**
	 * AWS S3 error code <code>EntityTooLarge</code>.
	 */
	ENTITY_TOO_LARGE,
	/**
	 * AWS S3 error code <code>EntityTooSmall</code>.
	 */
	ENTITY_TOO_SMALL,
	/**
	 * AWS S3 error code <code>ExpiredToken</code>.
	 */
	EXPIRED_TOKEN(AccessDeniedException.class),
	/**
	 * AWS S3 error code <code>IllegalLocationConstraintException</code>.
	 */
	ILLEGAL_LOCATION_CONSTRAINT_EXCEPTION,
	/**
	 * AWS S3 error code <code>IllegalVersioningConfigurationException</code>.
	 */
	ILLEGAL_VERSIONING_CONFIGURATION_EXCEPTION,
	/**
	 * AWS S3 error code <code>IncompleteBody</code>.
	 */
	INCOMPLETE_BODY,
	/**
	 * AWS S3 error code <code>IncorrectEndpoint</code>.
	 */
	INCORRECT_ENDPOINT,
	/**
	 * AWS S3 error code <code>InlineDataTooLarge</code>.
	 */
	INLINE_DATA_TOO_LARGE,
	/**
	 * AWS S3 error code <code>InternalError</code>.
	 */
	INTERNAL_ERROR,
	/**
	 * AWS S3 error code <code>InvalidArgument</code>.
	 */
	INVALID_ARGUMENT,
	/**
	 * AWS S3 error code <code>InvalidDigest</code>.
	 */
	INVALID_DIGEST,
	/**
	 * AWS S3 error code <code>InvalidLocationConstraint</code>.
	 */
	INVALID_LOCATION_CONSTRAINT,
	/**
	 * AWS S3 error code <code>InvalidObjectState</code>.
	 */
	INVALID_OBJECT_STATE,
	/**
	 * AWS S3 error code <code>InvalidPart</code>.
	 */
	INVALID_PART,
	/**
	 * AWS S3 error code <code>InvalidPartOrder</code>.
	 */
	INVALID_PART_ORDER,
	/**
	 * AWS S3 error code <code>InvalidPolicyDocument</code>.
	 */
	INVALID_POLICY_DOCUMENT,
	/**
	 * AWS S3 error code <code>InvalidRange</code>.
	 */
	INVALID_RANGE,
	/**
	 * AWS S3 error code <code>InvalidSecurity</code>.
	 */
	INVALID_SECURITY(AccessDeniedException.class),
	/**
	 * AWS S3 error code <code>InvalidStorageClass</code>.
	 */
	INVALID_STORAGE_CLASS,
	/**
	 * AWS S3 error code <code>InvalidURI</code>.
	 */
	INVALID_URI(InvalidPathException.class),
	/**
	 * AWS S3 error code <code>KeyTooLongError</code>.
	 */
	KEY_TOO_LONG_ERROR(InvalidPathException.class),
	/**
	 * AWS S3 error code <code>MalformedACLError</code>.
	 */
	MALFORMED_ACL_ERROR,
	/**
	 * AWS S3 error code <code>MalformedPOSTRequest</code>.
	 */
	MALFORMED_POST_REQUEST,
	/**
	 * AWS S3 error code <code>MalformedXML</code>.
	 */
	MALFORMED_XML,
	/**
	 * AWS S3 error code <code>MaxMessageLengthExceeded</code>.
	 */
	MAX_MESSAGE_LENGTH_EXCEEDED,
	/**
	 * AWS S3 error code <code>MaxPOSTPreDataLengthExceededError</code>.
	 */
	MAX_POST_PRE_DATA_LENGTH_EXCEEDED_ERROR,
	/**
	 * AWS S3 error code <code>MetadataTooLarge</code>.
	 */
	METADATA_TOO_LARGE,
	/**
	 * AWS S3 error code <code>MethodNotAllowed</code>.
	 */
	METHOD_NOT_ALLOWED,
	/**
	 * AWS S3 error code <code>MissingContentLength</code>.
	 */
	MISSING_CONTENT_LENGTH,
	/**
	 * AWS S3 error code <code>MissingRequestBodyError</code>.
	 */
	MISSING_REQUEST_BODY_ERROR,
	/**
	 * AWS S3 error code <code>NoSuchBucket</code>.
	 */
	NO_SUCH_BUCKET(FileSystemNotFoundException.class),
	/**
	 * AWS S3 error code <code>NoSuchCORSConfiguration</code>.
	 */
	NO_SUCH_CORS_CONFIGURATION,
	/**
	 * AWS S3 error code <code>NoSuchLifecycleConfiguration</code>.
	 */
	NO_SUCH_LIFECYCLE_CONFIGURATION,
	/**
	 * AWS S3 error code <code>NoSuchVersion</code>.
	 */
	NO_SUCH_VERSION(NoSuchFileException.class),
	/**
	 * AWS S3 error code <code>NotImplemented</code>.
	 */
	NOT_IMPLEMENTED,
	/**
	 * AWS S3 error code <code>OperationAborted</code>.
	 */
	OPERATION_ABORTED,
	/**
	 * AWS S3 error code <code>PermanentRedirect</code>.
	 */
	PERMANENT_REDIRECT,
	/**
	 * AWS S3 error code <code>PreconditionFailed</code>.
	 */
	PRECONDITION_FAILED,
	/**
	 * AWS S3 error code <code>Redirect</code>.
	 */
	REDIRECT,
	/**
	 * AWS S3 error code <code>RequestTimeout</code>.
	 */
	REQUEST_TIMEOUT,
	/**
	 * AWS S3 error code <code>RequestTimeTooSkewed</code>.
	 */
	REQUEST_TIME_TOO_SKEWED,
	/**
	 * AWS S3 error code <code>RestoreAlreadyInProgress</code>.
	 */
	RESTORE_ALREADY_IN_PROGRESS,
	/**
	 * AWS S3 error code <code>ServiceUnavailable</code>.
	 */
	SERVICE_UNAVAILABLE,
	/**
	 * AWS S3 error code <code>SignatureDoesNotMatch</code>.
	 */
	SIGNATURE_DOES_NOT_MATCH(AccessDeniedException.class),
	/**
	 * AWS S3 error code <code>SlowDown</code>.
	 */
	SLOW_DOWN,
	/**
	 * AWS S3 error code <code>TemporaryRedirect</code>.
	 */
	TEMPORARY_REDIRECT,
	/**
	 * AWS S3 error code <code>TokenRefreshRequired</code>.
	 */
	TOKEN_REFRESH_REQUIRED(AccessDeniedException.class),
	/**
	 * AWS S3 error code <code>TooManyBuckets</code>.
	 */
	TOO_MANY_BUCKETS(FileSystemAlreadyExistsException.class),
	/**
	 * AWS S3 error code <code>UnexpectedContent</code>.
	 */
	UNEXPECTED_CONTENT,
	/**
	 * AWS S3 error code <code>UnsupportedArgument</code>.
	 */
	UNSUPPORTED_ARGUMENT,
	/**
	 * AWS S3 error code <code>UnsupportedSignature</code>.
	 */
	UNSUPPORTED_SIGNATURE,
	/**
	 * AWS S3 error code <code>UnresolvableGrantByEmailAddress</code>.
	 */
	UNRESOLVABLE_GRANT_BY_EMAIL_ADDRESS;

	private static final String SEPARATOR = "_";

	private final Class<? extends Exception> nioException;

	private ErrorCode()
	{
		this(null);
	}

	private ErrorCode(Class<? extends Exception> nioException)
	{
		this.nioException = nioException;
	}

	/**
	 * Creates an instance of the mapped NIO.2 exception using the given parameters to compose the
	 * message. If no NIO.2 exception type was specified for this item, a generic
	 * <code>IOException</code> is instantiated instead, ensuring the method never returns
	 * <code>null</code>.
	 *
	 * @param parameters
	 *            arguments for the exception constructor (may be null or empty)
	 * @return the NIO.2 exception
	 * @throws IllegalStateException
	 *             if the instantiation fails
	 */
	public Exception nioException(String... parameters)
	{
		var parameterCount = parameters == null ? 0 : parameters.length;
		var result = Try.to(() -> matchingConstructor(parameterCount).newInstance((Object[]) parameters))
						.onCatch(ExceptionHelper::sneakyThrow)
						.get();
		return (Exception) result;
	}

	/**
	 * Finds the item matching the given <code>AwsErrorDetails</code>.
	 *
	 * @param awsErrorDetails
	 *            the AWS error details
	 * @return an Optional containing the matching ErrorCode, or empty if not found
	 */
	public static Optional<ErrorCode> of(AwsErrorDetails awsErrorDetails)
	{
		Objects.requireNonNull(awsErrorDetails, () -> "Missing AWS error details.");
		var errorCode = awsErrorDetails.errorCode();
		var length = Optional.ofNullable(errorCode.length()).orElse(0);
		var result = IntStream.range(0, length)
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
		var constructors = Optional.<Class<? extends Exception>>ofNullable(nioException)
								   .orElse(IOException.class)
								   .getDeclaredConstructors();
		return Stream.of(constructors)
					 .flatMap(Stream::of)
					 .filter(item -> Stream.of(item.getParameterTypes())
										   .allMatch(String.class::equals))
					 .sorted(Comparator.<Constructor<?>>comparingInt(item -> Math.abs(item.getParameterCount() - parameterCount))
									   .thenComparing(Comparator.<Constructor<?>>comparingInt(item -> item.getParameterCount() - parameterCount)
																.reversed()))
					 .findFirst()
					 .orElseThrow(() -> new IllegalStateException("NIO exception not instantiable."));
	}
}
