package it.mirkoscotti.nio.s3.extensions.jdk.jsr203;

import it.mirkoscotti.nio.s3.enums.BucketProperty;
import it.mirkoscotti.nio.s3.operations.S3Connector;

import java.nio.file.attribute.FileStoreAttributeView;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

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

	private final S3Connector connector;

	private final String bucketName;

	public BucketFileStoreAttributeView(S3Connector connector, String bucketName)
	{
		this.connector = Objects.requireNonNull(connector, "Missing AWS connector.");
		this.bucketName = Objects.requireNonNull(bucketName, "Missing bucket name");
	}

	@Override
	public String name()
	{
		return BucketFileStoreAttributeView.class.getSimpleName();
	}

	public String get(BucketProperty bucketProperty)
	{
		return attributes.computeIfAbsent(bucketProperty, this::extract);
	}

	private String extract(BucketProperty bucketProperty)
	{
		return switch (bucketProperty)
		{
			case ACL -> connector.bucketAcl(bucketName);
			default -> null;
		};
	}
}
