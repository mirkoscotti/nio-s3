package it.mirkoscotti.nio.s3.configuration;

import it.mirkoscotti.nio.s3.enums.BucketProperty;
import it.mirkoscotti.nio.s3.exceptions.BucketNameException;
import it.mirkoscotti.nio.s3.extensions.jdk.jsr203.S3FileSystemProvider;
import it.mirkoscotti.nio.s3.records.BucketRecord;
import it.mirkoscotti.nio.s3.records.AwsRecord;
import it.mirkoscotti.nio.s3.records.CredentialsRecord;

import java.net.URI;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import software.amazon.awssdk.regions.Region;

/**
 * This is a descriptor of how an AWS S3 bucket is configured, if existing, or must be configured if
 * it is not existing and must be created. It is compliant with the bucket naming convention and
 * best practices explained in the
 * <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/bucketnamingrules.html">official
 * documentation</a>.
 * <p>
 * Since it must be compliant with the Java NIO.2 specifications, the entry points of this immutable
 * object are an URI and, optionally, a set of properties to match the bucket configuration and
 * accessibility on AWS world or its emulator LocalStack.
 * <p>
 * URI and properties are provided by the specific {@link S3FileSystemProvider} through the Java
 * NIO.2 APIs:
 *
 * <ul>
 * <li>{@code FileSystems.newFileSystem(uri, properties)} to work with a bucket as it were a file
 * system</li>
 * <li>{@code Paths.get(uri)} to work with an object within a bucket as it were a directory or a
 * file</li>
 * </ul>
 * <p>
 * The URI must be compliant with the AWS allowed
 * <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/VirtualHosting.html">styles</a>,
 * but differently from what is specified in the documentation, the scheme must be {@code s3},
 * instead of {@code http} or {@code https}.
 * <p>
 * The following are valid examples:
 * <p>
 * <table border="1">
 * <caption style="text-align:left; margin-bottom:10px"><b>Compact Virtual Hosted
 * Style</b></caption>
 * <tr>
 * <th style="text-align:left">Example</th>
 * <td>{@code s3://bucket-name}</td>
 * </tr>
 * <tr>
 * <th style="text-align:left">Description</th>
 * <td>It is the compact representation of a bucket in the default region of an AWS account when
 * used with any of the Java NIO.2 APIs specified above. If it is required to refer to a custom
 * region or to access LocalStack instead of the real AWS platform, they can be configured a
 * properties.</td>
 * </tr>
 * <tr>
 * <th style="text-align:left">Java NIO APIs</th>
 * <td>{@code FileSystems.newFileSystem(uri, properties)}</td>
 * </tr>
 * </table>
 * <p>
 * <table border="1">
 * <caption style="text-align:left; margin-bottom:10px"><b>Extended Virtual Hosted
 * Style</b></caption>
 * <tr>
 * <th style="text-align:left">Example</th>
 * <td>{@code s3://bucket-name.endpoint}<br>
 * {@code s3://bucket-name.region.endpoint}</td>
 * </tr>
 * <tr>
 * <th style="text-align:left">Description</th>
 * <td>It is the extended representation of a bucket in the default or specified region at the given
 * endpoint, when used with any of the Java NIO.2 APIs specified above. If it is required to refer
 * to a custom region or to access LocalStack instead of the real AWS platform, they can be
 * configured a properties.</td>
 * </tr>
 * <tr>
 * <th style="text-align:left">Java NIO APIs</th>
 * <td>{@code FileSystems.newFileSystem(uri, properties)}</td>
 * </tr>
 * </table>
 * <p>
 * <table border="1">
 * <caption style="text-align:left; margin-bottom:10px"><b>Full Virtual Hosted Style</b></caption>
 * <tr>
 * <th style="text-align:left">Example</th>
 * <td>{@code s3://access-key:secret-key@bucket-name.endpoint}<br>
 * {@code s3://access-key:secret-key@bucket-name.region.endpoint}</td>
 * </tr>
 * <tr>
 * <th style="text-align:left">Description</th>
 * <td>It is the complete representation of a bucket in the default or specified region at the given
 * endpoint, when used with any of the Java NIO.2 APIs specified above. If it is required to refer
 * to a custom region or to access LocalStack instead of the real AWS platform, they can be
 * configured a properties.</td>
 * </tr>
 * <tr>
 * <th style="text-align:left">Java NIO APIs</th>
 * <td>{@code FileSystems.newFileSystem(uri, properties)}<br>
 * {@code Paths.get(uri)}</td>
 * </tr>
 * </table>
 *
 * @author mirko.scotti
 * @version Jan 25, 2025
 * @see BucketFileSystemProvider
 */
public class BucketDescriptor
{

	public static final String PATH_SEPARATOR = "/";

	private static final String BUCKET_NAME_NOT_ADJACENT_PERIODS = "(?!.*\\.{2})";

	private static final String BUCKET_NAME_NOT_IP = "(?!\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}$)";

	private static final String BUCKET_NAME_DOES_NOT_START_WITH_DASH = "(?!-)";

	private static final String BUCKET_NAME_DOES_NOT_START_WITH_XN_DOUBLE_DASH = "((?!xn--).*)";

	private static final String BUCKET_NAME_DOES_NOT_START_WITH_S3 = "(^(?!sthree-))";

	private static final String BUCKET_NAME_DOES_NOT_END_WITH_S3ALIAS = "(?!.*-s3alias$)";

	private static final String BUCKET_NAME_DOES_NOT_END_WITH_OL_S3 = "(?!.*--ol-s3$)";

	private static final String BUCKET_LENGTH_BETWEEN_3_AND_63 = "[a-z0-9][a-z0-9.-]{1,61}[a-z0-9]";

	private static final String BUCKET_PATTERN = Stream.of(BUCKET_NAME_NOT_ADJACENT_PERIODS,
														   BUCKET_NAME_NOT_IP,
														   BUCKET_NAME_DOES_NOT_START_WITH_DASH,
														   BUCKET_NAME_DOES_NOT_START_WITH_XN_DOUBLE_DASH,
														   BUCKET_NAME_DOES_NOT_START_WITH_S3,
														   BUCKET_NAME_DOES_NOT_END_WITH_S3ALIAS,
														   BUCKET_NAME_DOES_NOT_END_WITH_OL_S3,
														   BUCKET_LENGTH_BETWEEN_3_AND_63)
													   .collect(Collectors.joining("", "^", "$"));

	private final Map<BucketProperty, String> configuration = new EnumMap<>(BucketProperty.class);

	private final BucketRecord bucketKey;

	private final AwsRecord connectorKey;

	public BucketDescriptor(URI uri)
	{
		this(uri, null);
	}

	public BucketDescriptor(URI uri, Map<String, ?> configuration)
	{
		var uriDescriptor = new UriDescriptor(uri);
		var map = Optional.ofNullable(configuration).orElseGet(Map::of);
		Stream.of(BucketProperty.values())
			  .forEach(item -> addProperty(item, map.get(item.toProperty())));
		bucketKey = createBucketKey(uriDescriptor);
		var endpoint = bucketKey.endpoint();
		var region = Optional.ofNullable(this.configuration.get(BucketProperty.REGION))
							 .map(Region::of);
		var credentials = createCredentials(uriDescriptor);
		connectorKey = new AwsRecord(endpoint, region, credentials);
	}

	/**
	 * The wrapper method of the {@link #configuration} property.
	 *
	 * @return the value of the property
	 */
	public Map<BucketProperty, String> configuration()
	{
		return Collections.unmodifiableMap(configuration);
	}

	/**
	 * The wrapper method of the {@link #bucketKey} property.
	 *
	 * @return the value of the property
	 */
	public BucketRecord bucketKey()
	{
		return bucketKey;
	}

	/**
	 * The wrapper method of the {@link #connectorKey} property.
	 *
	 * @return the value of the property
	 */
	public AwsRecord connectorKey()
	{
		return connectorKey;
	}

	private void addProperty(BucketProperty property, Object object)
	{
		Stream.<Supplier<?>>of(() -> object,
							   () -> System.getProperty(property.toProperty()),
							   () -> System.getenv(property.toProperty()),
							   property::defaultValue)
			  .map(Supplier::get)
			  .filter(Objects::nonNull)
			  .findFirst()
			  .map(Object::toString)
			  .ifPresent(item -> configuration.put(property, item));
	}

	private BucketRecord createBucketKey(UriDescriptor uriDescriptor)
	{
		var bucket = uriDescriptor.bucketName();
		var bucketName = Optional.of(bucket)
								 .filter(item -> Pattern.matches(BUCKET_PATTERN, item))
								 .orElseThrow(() -> new BucketNameException(bucket));
		var endpoint = uriDescriptor.endpoint()
									.orElseGet(() -> configuration.remove(BucketProperty.ENDPOINT));
		return new BucketRecord(Optional.ofNullable(endpoint), bucketName);
	}

	private CredentialsRecord createCredentials(UriDescriptor uriDescriptor)
	{
		return uriDescriptor.credentials()
							.orElseGet(() -> new CredentialsRecord(configuration.remove(BucketProperty.ACCESS_KEY),
																   configuration.remove(BucketProperty.SECRET_KEY)));
	}
}
