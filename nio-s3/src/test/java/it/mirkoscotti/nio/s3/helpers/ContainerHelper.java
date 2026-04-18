package it.mirkoscotti.nio.s3.helpers;

import java.net.URI;
import java.util.Map;
import java.util.stream.Stream;

import io.github.mirkoscotti.nio.s3.configuration.BucketDescriptor;
import io.github.mirkoscotti.nio.s3.enums.BucketProperty;

import it.mirkoscotti.nio.s3.extensions.testcontainers.S3Container;

/**
 * @author mirko.scotti
 * @version Jun 04, 2025
 */
public final class ContainerHelper
{

	private ContainerHelper()
	{
		super();
	}

	public static Map<String, Object> standardProperties(S3Container container)
	{
		return Map.<String, Object>of(BucketProperty.ENDPOINT.toProperty(), container.getEndpoint(),
									  BucketProperty.REGION.toProperty(), container.getRegion(),
									  BucketProperty.ACCESS_KEY.toProperty(),
									  container.getAccessKey(),
									  BucketProperty.SECRET_KEY.toProperty(),
									  container.getSecretKey());
	}

	public static Map<String, Object> unauthenticatedProperties(S3Container container)
	{
		return Map.<String, Object>of(BucketProperty.REGION.toProperty(), container.getRegion(),
									  BucketProperty.ACCESS_KEY.toProperty(), "wrongAccessKey",
									  BucketProperty.SECRET_KEY.toProperty(), "wrongPassword");
	}

	public static BucketDescriptor createBucketDescriptor(S3Container container, String bucketName)
	{
		var uri = URI.create("s3://".concat(bucketName));
		return new BucketDescriptor(uri, standardProperties(container));
	}

	public static void deleteObjects(S3Container container, String bucketName)
	{
		Stream.of(container.listObjects(bucketName))
			  .forEach(item -> container.deleteObject(bucketName, item));
	}

	public static void deleteBucket(S3Container container, String bucketName)
	{
		deleteObjects(container, bucketName);
		container.deleteBucket(bucketName);
	}
}
