package io.github.mirkoscotti.nio.s3.enums;

import java.nio.file.attribute.FileStoreAttributeView;
import java.util.Optional;
import java.util.stream.Stream;

import software.amazon.awssdk.regions.Region;

/**
 * @author mirko.scotti
 * @version Oct 31, 2024
 */
public enum BucketProperty
	implements FileStoreAttributeView
{

	ENDPOINT("https://s3.us-east-1.amazonaws.com"),
	REGION(Region.US_EAST_1.toString()),
	ACCESS_KEY,
	SECRET_KEY,
	CREDENTIALS,
	ACL,
	FULL_CONTROL,
	READ,
	READ_ACP,
	WRITE,
	WRITE_ACP;

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
