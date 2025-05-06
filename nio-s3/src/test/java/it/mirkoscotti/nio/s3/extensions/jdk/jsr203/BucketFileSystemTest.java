package it.mirkoscotti.nio.s3.extensions.jdk.jsr203;

import it.mirkoscotti.nio.s3.configuration.BucketDescriptor;
import it.mirkoscotti.nio.s3.exceptions.BucketNameException;
import it.mirkoscotti.nio.s3.exceptions.CredentialsException;
import it.mirkoscotti.nio.s3.operations.S3Connector;
import it.mirkoscotti.nio.s3.records.BucketRecord;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import software.amazon.awssdk.services.s3.model.BucketAlreadyExistsException;
import software.amazon.awssdk.services.s3.model.BucketAlreadyOwnedByYouException;

/**
 * @author mirko.scotti
 * @version May 06, 2025
 */
@ExtendWith(MockitoExtension.class)
class BucketFileSystemTest
{

	private static final String BUCKET_NAME = "bucket-name";

	@Mock
	private S3Connector connector;

	@Mock
	private BucketDescriptor bucketDescriptor;

	@Mock
	private BucketRecord bucketKey;

	@Mock
	private S3FileSystemProvider fileSystemProvider;

	@Test
	void bucketAlreadyOwnedByYouExceptionTest()
	{
		Mockito.doThrow(BucketAlreadyOwnedByYouException.class)
			   .when(connector)
			   .createBucket(Mockito.any(BucketDescriptor.class));
		Mockito.when(bucketDescriptor.bucketKey()).thenReturn(bucketKey);
		Mockito.when(bucketKey.bucketName()).thenReturn(BUCKET_NAME);
		Assertions.assertDoesNotThrow(() -> new BucketFileSystem(connector,
																 bucketDescriptor,
																 fileSystemProvider));
	}

	@Test
	void bucketNameExceptionTest()
	{
		Mockito.doThrow(BucketNameException.class)
			   .when(connector)
			   .createBucket(Mockito.any(BucketDescriptor.class));
		Mockito.when(bucketDescriptor.bucketKey()).thenReturn(bucketKey);
		Mockito.when(bucketKey.bucketName()).thenReturn(BUCKET_NAME);
		Assertions.assertThrows(IllegalArgumentException.class,
								() -> new BucketFileSystem(connector,
														   bucketDescriptor,
														   fileSystemProvider));
	}

	@Test
	void credentialsExceptionTest()
	{
		Mockito.doThrow(CredentialsException.class)
			   .when(connector)
			   .createBucket(Mockito.any(BucketDescriptor.class));
		Mockito.when(bucketDescriptor.bucketKey()).thenReturn(bucketKey);
		Assertions.assertThrows(IllegalArgumentException.class,
								() -> new BucketFileSystem(connector,
														   bucketDescriptor,
														   fileSystemProvider));
	}

	@Test
	void bucketAlreadyExistsExceptionTest()
	{
		Mockito.doThrow(BucketAlreadyExistsException.class)
			   .when(connector)
			   .createBucket(Mockito.any(BucketDescriptor.class));
		Mockito.when(bucketDescriptor.bucketKey()).thenReturn(bucketKey);
		Assertions.assertThrows(IllegalArgumentException.class,
								() -> new BucketFileSystem(connector,
														   bucketDescriptor,
														   fileSystemProvider));
	}

	@Test
	void unpredictedIssueTest()
	{
		Mockito.doThrow(RuntimeException.class)
			   .when(connector)
			   .createBucket(Mockito.any(BucketDescriptor.class));
		Mockito.when(bucketDescriptor.bucketKey()).thenReturn(bucketKey);
		Assertions.assertThrows(IllegalArgumentException.class,
								() -> new BucketFileSystem(connector,
														   bucketDescriptor,
														   fileSystemProvider));
	}
}
