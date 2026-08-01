package io.github.mirkoscotti.nio.s3.extensions.jdk.jsr203;

import java.nio.file.attribute.FileStoreAttributeView;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

import io.github.mirkoscotti.nio.s3.enums.BucketProperty;
import io.github.mirkoscotti.nio.s3.operations.AwsFacade;

/**
 * The whole set of properties of the bucket mapped to the file store defining this view.
 *
 * @author mirko.scotti
 * @version Oct 29, 2024
 * @see BucketFileStore
 */
public class BucketFileStoreAttributeView
	implements FileStoreAttributeView
{

	private final Map<BucketProperty, String> attributes = new EnumMap<>(BucketProperty.class);

	private final AwsFacade awsFacade;

	private final String bucketName;

	/**
	 * Creates a view of the given S3 bucket.
	 *
	 * @param awsFacade
	 *            the AWS connector
	 * @param bucketName
	 *            the name of the bucket
	 */
	public BucketFileStoreAttributeView(AwsFacade awsFacade, String bucketName)
	{
		this.awsFacade = Objects.requireNonNull(awsFacade, "Missing AWS connector.");
		this.bucketName = Objects.requireNonNull(bucketName, "Missing bucket name");
	}

	/**
	 * The name of this view, corresponding to the simple name of this class.
	 */
	@Override
	public String name()
	{
		return BucketFileStoreAttributeView.class.getSimpleName();
	}

	/**
	 * Extracts the value of the given property.
	 *
	 * @param bucketProperty
	 *            the property item
	 * @return the value of the property or <code>null</code> if the property is not set
	 */
	public String get(BucketProperty bucketProperty)
	{
		return attributes.computeIfAbsent(bucketProperty, this::extract);
	}

	private String extract(BucketProperty bucketProperty)
	{
		return switch (bucketProperty)
		{
			case ACL -> awsFacade.bucketAcl(bucketName);
			default -> null;
		};
	}
}
