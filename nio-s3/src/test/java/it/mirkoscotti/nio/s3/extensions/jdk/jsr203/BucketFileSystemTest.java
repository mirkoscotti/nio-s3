package it.mirkoscotti.nio.s3.extensions.jdk.jsr203;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.util.stream.StreamSupport;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import it.mirkoscotti.nio.s3.configuration.BucketDescriptor;
import it.mirkoscotti.nio.s3.exceptions.BucketNameException;
import it.mirkoscotti.nio.s3.exceptions.CredentialsException;
import it.mirkoscotti.nio.s3.operations.AwsFacade;
import it.mirkoscotti.nio.s3.operations.S3Connector;
import it.mirkoscotti.nio.s3.records.BucketRecord;

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
	private AwsFacade awsFacade;

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
			   .when(awsFacade)
			   .createBucket(Mockito.any(BucketDescriptor.class));
		Mockito.when(bucketDescriptor.bucketKey()).thenReturn(bucketKey);
		Mockito.when(bucketKey.bucketName()).thenReturn(BUCKET_NAME);
		Assertions.assertDoesNotThrow(() -> new BucketFileSystem(awsFacade,
																 bucketDescriptor,
																 fileSystemProvider));
	}

	@Test
	void bucketNameExceptionTest()
	{
		Mockito.doThrow(BucketNameException.class)
			   .when(awsFacade)
			   .createBucket(Mockito.any(BucketDescriptor.class));
		Mockito.when(bucketDescriptor.bucketKey()).thenReturn(bucketKey);
		Mockito.when(bucketKey.bucketName()).thenReturn(BUCKET_NAME);
		Assertions.assertThrows(IllegalArgumentException.class,
								() -> new BucketFileSystem(awsFacade,
														   bucketDescriptor,
														   fileSystemProvider));
	}

	@Test
	void credentialsExceptionTest()
	{
		Mockito.doThrow(CredentialsException.class)
			   .when(awsFacade)
			   .createBucket(Mockito.any(BucketDescriptor.class));
		Mockito.when(bucketDescriptor.bucketKey()).thenReturn(bucketKey);
		Assertions.assertThrows(IllegalArgumentException.class,
								() -> new BucketFileSystem(awsFacade,
														   bucketDescriptor,
														   fileSystemProvider));
	}

	@Test
	void bucketAlreadyExistsExceptionTest()
	{
		Mockito.doThrow(BucketAlreadyExistsException.class)
			   .when(awsFacade)
			   .createBucket(Mockito.any(BucketDescriptor.class));
		Mockito.when(bucketDescriptor.bucketKey()).thenReturn(bucketKey);
		Assertions.assertThrows(IllegalArgumentException.class,
								() -> new BucketFileSystem(awsFacade,
														   bucketDescriptor,
														   fileSystemProvider));
	}

	@Test
	void unpredictedIssueTest()
	{
		Mockito.doThrow(RuntimeException.class)
			   .when(awsFacade)
			   .createBucket(Mockito.any(BucketDescriptor.class));
		Mockito.when(bucketDescriptor.bucketKey()).thenReturn(bucketKey);
		Assertions.assertThrows(IllegalArgumentException.class,
								() -> new BucketFileSystem(awsFacade,
														   bucketDescriptor,
														   fileSystemProvider));
	}

	@Test
	void providerTest()
	{
		Mockito.when(bucketDescriptor.bucketKey()).thenReturn(bucketKey);
		Mockito.when(bucketKey.bucketName()).thenReturn(BUCKET_NAME);
		try (var fileSystem = new BucketFileSystem(awsFacade, bucketDescriptor, fileSystemProvider))
		{
			Assertions.assertEquals(fileSystemProvider, fileSystem.provider());
		}
		catch (IOException x)
		{
			Assertions.fail(x);
		}
	}

	@Test
	void getFileStoresTest()
	{
		Mockito.when(bucketDescriptor.bucketKey()).thenReturn(bucketKey);
		Mockito.when(bucketKey.bucketName()).thenReturn(BUCKET_NAME);
		try (var mock = Mockito.mockConstruction(BucketFileStore.class);
			 var fileSystem = new BucketFileSystem(awsFacade, bucketDescriptor, fileSystemProvider))
		{
			var list = StreamSupport.stream(fileSystem.getFileStores().spliterator(), false)
									.toList();
			Assertions.assertEquals(mock.constructed(), list);
		}
		catch (IOException x)
		{
			Assertions.fail(x);
		}
	}

	@Test
	void getPathTest()
	{
		Mockito.when(bucketDescriptor.bucketKey()).thenReturn(bucketKey);
		Mockito.when(bucketKey.bucketName()).thenReturn(BUCKET_NAME);
		try (var mock = Mockito.mockConstruction(BucketPath.class);
			 var fileSystem = new BucketFileSystem(awsFacade, bucketDescriptor, fileSystemProvider))
		{
			var path = fileSystem.getPath("path");
			Assertions.assertEquals(mock.constructed().get(0), path);
		}
		catch (IOException x)
		{
			Assertions.fail(x);
		}
	}

	@Test
	void newWatchServiceTest()
	{
		Mockito.when(bucketDescriptor.bucketKey()).thenReturn(bucketKey);
		Mockito.when(bucketKey.bucketName()).thenReturn(BUCKET_NAME);
		try (var mock = Mockito.mockConstruction(DirectoryWatchService.class);
			 var fileSystem = new BucketFileSystem(awsFacade, bucketDescriptor, fileSystemProvider))
		{
			var watchService = fileSystem.newWatchService();
			Assertions.assertEquals(mock.constructed().get(0), watchService);
		}
		catch (IOException x)
		{
			Assertions.fail(x);
		}
	}

	@Test
	void hashCodeWithSameInstancesTest()
	{
		Mockito.when(bucketDescriptor.bucketKey()).thenReturn(bucketKey);
		Mockito.when(bucketKey.bucketName()).thenReturn(BUCKET_NAME);
		try (var fileSystem1 = new BucketFileSystem(awsFacade,
													bucketDescriptor,
													fileSystemProvider);
			 var fileSystem2 = new BucketFileSystem(awsFacade,
													bucketDescriptor,
													fileSystemProvider))
		{
			Assertions.assertEquals(fileSystem1.hashCode(), fileSystem2.hashCode());
		}
		catch (IOException x)
		{
			Assertions.fail(x);
		}
	}

	@Test
	void hashCodeWithDifferentInstancesTest(@Mock AwsFacade awsFacade,
											@Mock S3Connector connector,
											@Mock BucketDescriptor bucketDescriptor,
											@Mock S3FileSystemProvider fileSystemProvider)
	{
		Mockito.when(this.bucketDescriptor.bucketKey()).thenReturn(bucketKey);
		Mockito.when(bucketKey.bucketName()).thenReturn(BUCKET_NAME);
		Mockito.when(bucketDescriptor.bucketKey()).thenReturn(bucketKey);
		try (var fileSystem1 = new BucketFileSystem(this.awsFacade,
													this.bucketDescriptor,
													this.fileSystemProvider);
			 var fileSystem2 = new BucketFileSystem(awsFacade,
													bucketDescriptor,
													fileSystemProvider))
		{
			Assertions.assertNotEquals(fileSystem1.hashCode(), fileSystem2.hashCode());
		}
		catch (IOException x)
		{
			Assertions.fail(x);
		}
	}

	@Test
	void equalsToNullTest()
	{
		Mockito.when(bucketDescriptor.bucketKey()).thenReturn(bucketKey);
		Mockito.when(bucketKey.bucketName()).thenReturn(BUCKET_NAME);
		try (var fileSystem = new BucketFileSystem(awsFacade, bucketDescriptor, fileSystemProvider))
		{
			var result = fileSystem.equals(null);
			Assertions.assertFalse(result);
		}
		catch (IOException x)
		{
			Assertions.fail(x);
		}
	}

	@Test
	void equalsToDifferentFileSystemTest(@Mock FileSystem otherFileSystem)
	{
		Mockito.when(bucketDescriptor.bucketKey()).thenReturn(bucketKey);
		Mockito.when(bucketKey.bucketName()).thenReturn(BUCKET_NAME);
		try (var thisFileSystem = new BucketFileSystem(awsFacade,
													   bucketDescriptor,
													   fileSystemProvider))
		{
			var result = thisFileSystem.equals(otherFileSystem);
			Assertions.assertFalse(result);
		}
		catch (IOException x)
		{
			Assertions.fail(x);
		}
	}

	@Test
	void equalsToFileSystemWithDifferentProviderTest(@Mock S3FileSystemProvider fileSystemProvider)
	{
		Mockito.when(bucketDescriptor.bucketKey()).thenReturn(bucketKey);
		Mockito.when(bucketKey.bucketName()).thenReturn(BUCKET_NAME);
		Mockito.when(bucketDescriptor.bucketKey()).thenReturn(bucketKey);
		try (var fileSystem1 = new BucketFileSystem(awsFacade,
													bucketDescriptor,
													this.fileSystemProvider);
			 var fileSystem2 = new BucketFileSystem(awsFacade,
													bucketDescriptor,
													fileSystemProvider))
		{
			var result = fileSystem1.equals(fileSystem2);
			Assertions.assertFalse(result);
		}
		catch (IOException x)
		{
			Assertions.fail(x);
		}
	}

	@Test
	void equalsToFileSystemWithDifferentFileStoreTest(@Mock BucketDescriptor bucketDescriptor,
													  @Mock BucketRecord bucketKey)
	{
		Mockito.when(this.bucketDescriptor.bucketKey()).thenReturn(this.bucketKey);
		Mockito.when(this.bucketKey.bucketName()).thenReturn(BUCKET_NAME);
		Mockito.when(bucketDescriptor.bucketKey()).thenReturn(bucketKey);
		Mockito.when(bucketKey.bucketName()).thenReturn("other-bucket");
		try (var fileSystem1 = new BucketFileSystem(awsFacade,
													this.bucketDescriptor,
													fileSystemProvider);
			 var fileSystem2 = new BucketFileSystem(awsFacade,
													bucketDescriptor,
													fileSystemProvider))
		{
			var result = fileSystem1.equals(fileSystem2);
			Assertions.assertFalse(result);
		}
		catch (IOException x)
		{
			Assertions.fail(x);
		}
	}

	@Test
	void equalsTest()
	{
		Mockito.when(bucketDescriptor.bucketKey()).thenReturn(bucketKey);
		Mockito.when(bucketKey.bucketName()).thenReturn(BUCKET_NAME);
		try (var fileSystem1 = new BucketFileSystem(awsFacade,
													bucketDescriptor,
													fileSystemProvider);
			 var fileSystem2 = new BucketFileSystem(awsFacade,
													bucketDescriptor,
													fileSystemProvider))
		{
			var result = fileSystem1.equals(fileSystem2);
			Assertions.assertTrue(result);
		}
		catch (IOException x)
		{
			Assertions.fail(x);
		}
	}

	@Test
	void connectorTest()
	{
		Mockito.when(bucketDescriptor.bucketKey()).thenReturn(bucketKey);
		Mockito.when(bucketKey.bucketName()).thenReturn(BUCKET_NAME);
		try (var mock = Mockito.mockConstruction(DirectoryWatchService.class);
			 var fileSystem = new BucketFileSystem(awsFacade, bucketDescriptor, fileSystemProvider))
		{
			Assertions.assertEquals(awsFacade, fileSystem.awsFacade());
		}
		catch (IOException x)
		{
			Assertions.fail(x);
		}
	}
}
