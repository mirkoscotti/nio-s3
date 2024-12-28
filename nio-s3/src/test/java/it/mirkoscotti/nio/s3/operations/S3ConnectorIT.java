package it.mirkoscotti.nio.s3.operations;

import it.mirkoscotti.nio.s3.extensions.testcontainers.S3Container;

import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * @author mirko.scotti
 * @version Oct 24, 2024
 */
@Testcontainers
@ExtendWith(MockitoExtension.class)
class S3ConnectorIT
{

	private static final String BUCKET_NAME = "test-bucket";

	private static final String PUBLIC_READ = "public-read";

	private static final String KEY = "key";

	@Container
	private static final S3Container CONTAINER = new S3Container();

	private static S3Connector connector;

	@BeforeAll
	static void beforeAll()
	{
		connector = S3Connector.create()
							   .withEndpoint(CONTAINER.getEndpoint())
							   .withRegion(CONTAINER.getRegion())
							   .withCredentials(CONTAINER.getAccessKey(), CONTAINER.getSecretKey())
							   .build();
	}

	@AfterEach
	void afterEach()
	{
		Optional.of(BUCKET_NAME).filter(CONTAINER::bucketExists).ifPresent(this::deleteBucket);
	}

	@Test
	void isBucketReadOnlyWithoutPolicyTest()
	{
		CONTAINER.createBucket(BUCKET_NAME);
		Assertions.assertFalse(connector.isBucketReadOnly(BUCKET_NAME));
	}

	@Test
	void isBucketReadOnlyWhenReadOnlyPolicyTest()
	{
		CONTAINER.createBucket(BUCKET_NAME);
		CONTAINER.createBucketPolicy(BUCKET_NAME);
		Assertions.assertTrue(connector.isBucketReadOnly(BUCKET_NAME));
	}

	@Test
	void isBucketReadOnlyWhenReadOnlyAclTest()
	{
		CONTAINER.createBucket(BUCKET_NAME);
		CONTAINER.createBucketAcl(BUCKET_NAME, PUBLIC_READ);
		Assertions.assertTrue(connector.isBucketReadOnly(BUCKET_NAME));
	}

	@Test
	void bucketAclTest()
	{
		CONTAINER.createBucket(BUCKET_NAME);
		CONTAINER.createBucketAcl(BUCKET_NAME, PUBLIC_READ);
		Assertions.assertTrue(connector.bucketAcl(BUCKET_NAME).contains("READ"));
	}

	@Test
	void bucketAclWhenBucketDoesNotExistTest()
	{
		Assertions.assertThrows(IllegalStateException.class,
								() -> connector.bucketAcl(BUCKET_NAME));
	}

	@Test
	void objectMetadataTest()
	{
		CONTAINER.createBucket(BUCKET_NAME);
		CONTAINER.createObject(BUCKET_NAME, KEY);
		var basicFileAttributes = connector.objectMetadata(BUCKET_NAME, KEY);
		var lastModified = basicFileAttributes.lastModifiedTime();
		Assertions.assertEquals(CONTAINER.lastModified(BUCKET_NAME, KEY), lastModified.toInstant());
		Assertions.assertEquals(0, basicFileAttributes.size());
		Assertions.assertEquals(KEY, basicFileAttributes.fileKey());
	}

	private void deleteBucket(String bucketName)
	{
		Optional.of(KEY)
				.filter(item -> CONTAINER.objectExists(bucketName, item))
				.ifPresent(item -> CONTAINER.deleteObject(bucketName, item));
		CONTAINER.deleteBucket(bucketName);
	}
}
