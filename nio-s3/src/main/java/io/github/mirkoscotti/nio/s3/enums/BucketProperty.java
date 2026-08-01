package io.github.mirkoscotti.nio.s3.enums;

import java.nio.file.attribute.FileStoreAttributeView;
import java.util.Optional;
import java.util.stream.Stream;

import software.amazon.awssdk.regions.Region;

/**
 * Enumeration of the configurable properties for instantiating an S3-backed file system via
 * {@link java.nio.file.FileSystems#newFileSystem(java.net.URI, java.util.Map)}.
 * <p>
 * Each constant represents a configuration key that can be passed as an entry of the map optionally
 * required while creating a new file system, following the naming convention
 * <code>aws.[property-name]</code>. Properties defining a default value are facultative.
 * <p>
 * Usage example:
 *
 * <pre>
 * var map = new HashMap&lt;String, Object>();
 * map.put(BucketProperty.ACCESS_KEY.toProperty(), "myAccessKey");
 * map.put(BucketProperty.SECRET_KEY.toProperty(), "mySecretKey");
 * var uri = URI.create("s3://my-bucket");
 * var fileSystem = Files.newFileSystem(uri, env);
 * </pre>
 *
 * @author mirko.scotti
 * @version Oct 31, 2024
 */
public enum BucketProperty
	implements FileStoreAttributeView
{

	/**
	 * The S3 endpoint URL.
	 */
	ENDPOINT("https://s3.us-east-1.amazonaws.com"),
	/**
	 * The AWS region.
	 */
	REGION(Region.US_EAST_1.toString()),
	/**
	 * The AWS access key.
	 */
	ACCESS_KEY,
	/**
	 * The AWS secret key.
	 */
	SECRET_KEY,
	/**
	 * The canned access control list to apply.
	 */
	ACL,
	/**
	 * The grantee to whom to give full control permission.
	 */
	FULL_CONTROL,
	/**
	 * The grantee to whom to give read permission.
	 */
	READ,
	/**
	 * The grantee to whom to give the permission to read the access control list.
	 */
	READ_ACP,
	/**
	 * The grantee to whom to give write permission.
	 */
	WRITE,
	/**
	 * The grantee to whom to give the permission to write the access control list.
	 */
	WRITE_ACP;

	/**
	 * The S3 URI scheme.
	 */
	public static final String SCHEME = "s3";

	private static final String PATTERN = "aws.%s";

	private final String defaultValue;

	private BucketProperty()
	{
		this(null);
	}

	private BucketProperty(String defaultValue)
	{
		this.defaultValue = defaultValue;
	}

	/**
	 * The property name.
	 *
	 * @return the name of the property
	 */
	public String toProperty()
	{
		return PATTERN.formatted(name().replace('_', '-').toLowerCase());
	}

	/**
	 * The wrapper method of the {@link #defaultValue} property.
	 *
	 * @return the value of the property
	 */
	public String defaultValue()
	{
		return defaultValue;
	}

	/**
	 * Convert the given property name to the corresponding item of this <code>enum</code>, if any.
	 *
	 * @param name
	 *            tha name of the property
	 * @return the item of this <code>enum</code> whose {@link #toProperty() name} is the given one
	 * @throws UnsupportedOperationException
	 *             if the given name does not correspond to any item of this <code>enum</code>
	 */
	public static Optional<BucketProperty> of(String name)
	{
		return Stream.of(BucketProperty.values())
					 .filter(item -> item.toProperty().equals(name))
					 .findAny();
	}
}
