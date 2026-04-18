package it.mirkoscotti.nio.s3.extensions.jdk.jsr203;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.StreamSupport;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedConstruction.Context;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import io.github.mirkoscotti.nio.s3.configuration.BucketDescriptor;

import it.mirkoscotti.nio.s3.enums.PathSyntax;
import it.mirkoscotti.nio.s3.exceptions.BucketNameException;
import it.mirkoscotti.nio.s3.operations.AwsFacade;
import it.mirkoscotti.nio.s3.operations.S3Connector;
import it.mirkoscotti.nio.s3.records.BucketRecord;

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
	void bucketNameExceptionTest()
	{
		Mockito.doThrow(BucketNameException.class)
			   .when(awsFacade)
			   .createBucket(Mockito.any(BucketDescriptor.class));
		Assertions.assertThrows(IllegalArgumentException.class, () -> doWithFileSystem(item -> {}));
	}

	@Test
	void unpredictedIssueTest()
	{
		Mockito.doThrow(RuntimeException.class)
			   .when(awsFacade)
			   .createBucket(Mockito.any(BucketDescriptor.class));
		Assertions.assertThrows(IllegalArgumentException.class, () -> doWithFileSystem(item -> {}));
	}

	@Test
	void providerTest()
	{
		Mockito.when(bucketDescriptor.bucketKey()).thenReturn(bucketKey);
		Mockito.when(bucketKey.bucketName()).thenReturn(BUCKET_NAME);
		doWithFileSystem(item -> Assertions.assertEquals(fileSystemProvider, item.provider()));
	}

	@Test
	void isFileSystemOpen()
	{
		Mockito.when(bucketDescriptor.bucketKey()).thenReturn(bucketKey);
		Mockito.when(bucketKey.bucketName()).thenReturn(BUCKET_NAME);
		Mockito.when(fileSystemProvider.isFileSystemOpen(Mockito.any(BucketFileSystem.class)))
			   .thenReturn(true);
		doWithFileSystem(item -> Assertions.assertTrue(item.isOpen()));
	}

	@Test
	void isReadOnlyTest()
	{
		Mockito.when(bucketDescriptor.bucketKey()).thenReturn(bucketKey);
		Mockito.when(bucketKey.bucketName()).thenReturn(BUCKET_NAME);
		try (var mock = Mockito.mockConstruction(BucketFileStore.class, this::readOnlyFileStore);
			 var fileSystem = new BucketFileSystem(awsFacade, bucketDescriptor, fileSystemProvider))
		{
			Assertions.assertTrue(fileSystem.isReadOnly());
		}
		catch (IOException x)
		{
			Assertions.fail(x);
		}
	}

	@Test
	void getSeparatorTest()
	{
		Mockito.when(bucketDescriptor.bucketKey()).thenReturn(bucketKey);
		Mockito.when(bucketKey.bucketName()).thenReturn(BUCKET_NAME);
		doWithFileSystem(item -> Assertions.assertEquals("/", item.getSeparator()));
	}

	@Test
	void getRootDirectoriesTest()
	{
		Mockito.when(bucketDescriptor.bucketKey()).thenReturn(bucketKey);
		Mockito.when(bucketKey.bucketName()).thenReturn(BUCKET_NAME);
		doWithFileSystem(this::getRootDirectoriesTest);
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
	void supportedFileAttributeViewsTest()
	{
		Mockito.when(bucketDescriptor.bucketKey()).thenReturn(bucketKey);
		Mockito.when(bucketKey.bucketName()).thenReturn(BUCKET_NAME);
		doWithFileSystem(this::supportedFileAttributeViewsTest);
	}

	@Test
	void getPathMatcherFromNullPatternTest()
	{
		Mockito.when(bucketDescriptor.bucketKey()).thenReturn(bucketKey);
		Mockito.when(bucketKey.bucketName()).thenReturn(BUCKET_NAME);
		doWithFileSystem(item -> Assertions.assertThrows(NullPointerException.class,
														 () -> item.getPathMatcher(null)));
	}

	@Test
	void getPathMatcherFromMalformedPatternTest()
	{
		Mockito.when(bucketDescriptor.bucketKey()).thenReturn(bucketKey);
		Mockito.when(bucketKey.bucketName()).thenReturn(BUCKET_NAME);
		doWithFileSystem(item -> Assertions.assertThrows(IllegalArgumentException.class,
														 () -> item.getPathMatcher("malformed-pattern")));
	}

	@Test
	void getPathMatcherTest(@Mock PathSyntax pathSyntax,
							@Mock Pattern pattern,
							@Mock Matcher matcher,
							@Mock BucketPath path)
	{
		Mockito.when(bucketDescriptor.bucketKey()).thenReturn(bucketKey);
		Mockito.when(bucketKey.bucketName()).thenReturn(BUCKET_NAME);
		Mockito.when(pathSyntax.pattern(Mockito.anyString())).thenReturn(pattern);
		Mockito.when(pattern.matcher(Mockito.anyString())).thenReturn(matcher);
		try (var mock = Mockito.mockStatic(PathSyntax.class);
			 var fileSystem = new BucketFileSystem(awsFacade, bucketDescriptor, fileSystemProvider))
		{
			mock.when(() -> PathSyntax.of(Mockito.anyString())).thenReturn(pathSyntax);
			var glob = "glob";
			var globPattern = "pattern";
			var pathMatcher = fileSystem.getPathMatcher(String.join(":", glob, globPattern));
			pathMatcher.matches(path);
			mock.verify(() -> PathSyntax.of(glob), Mockito.atLeastOnce());
			Mockito.verify(pathSyntax, Mockito.atLeastOnce()).pattern(globPattern);
		}
		catch (IOException x)
		{
			Assertions.fail(x);
		}
	}

	@Test
	void getUserPrincipalLookupServiceTest()
	{
		Mockito.when(bucketDescriptor.bucketKey()).thenReturn(bucketKey);
		Mockito.when(bucketKey.bucketName()).thenReturn(BUCKET_NAME);
		doWithFileSystem(item -> Assertions.assertThrows(UnsupportedOperationException.class,
														 item::getUserPrincipalLookupService));
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
		doWithFileSystem(this::equalsToNullTest);
	}

	@Test
	void equalsToDifferentFileSystemTest(@Mock FileSystem otherFileSystem)
	{
		Mockito.when(bucketDescriptor.bucketKey()).thenReturn(bucketKey);
		Mockito.when(bucketKey.bucketName()).thenReturn(BUCKET_NAME);
		doWithFileSystem(item -> equalsToDifferentFileSystemTest(item, otherFileSystem));
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
	void bucketNameTest()
	{
		Mockito.when(bucketDescriptor.bucketKey()).thenReturn(bucketKey);
		Mockito.when(bucketKey.bucketName()).thenReturn(BUCKET_NAME);
		doWithFileSystem(item -> Assertions.assertEquals(BUCKET_NAME, item.bucketName()));
	}

	@Test
	void awsFacadeTest()
	{
		Mockito.when(bucketDescriptor.bucketKey()).thenReturn(bucketKey);
		Mockito.when(bucketKey.bucketName()).thenReturn(BUCKET_NAME);
		doWithFileSystem(item -> Assertions.assertEquals(awsFacade, item.awsFacade()));
	}

	private void readOnlyFileStore(BucketFileStore fileStore, Context context)
	{
		Mockito.when(fileStore.isReadOnly()).thenReturn(true);
	}

	private void getRootDirectoriesTest(BucketFileSystem fileSystem)
	{
		var paths = fileSystem.getRootDirectories();
		var list = StreamSupport.stream(paths.spliterator(), false).toList();
		Assertions.assertEquals(1, list.size());
		Assertions.assertEquals("/", list.get(0).toString());
	}

	private void supportedFileAttributeViewsTest(BucketFileSystem fileSystem)
	{
		var set = fileSystem.supportedFileAttributeViews();
		Assertions.assertEquals(1, set.size());
		Assertions.assertEquals("basic", set.iterator().next());
	}

	private void equalsToNullTest(BucketFileSystem fileSystem)
	{
		var result = fileSystem.equals(null);
		Assertions.assertFalse(result);
	}

	private void equalsToDifferentFileSystemTest(BucketFileSystem fileSystem1,
												 FileSystem fileSystem2)
	{
		var result = fileSystem1.equals(fileSystem2);
		Assertions.assertFalse(result);
	}

	private void doWithFileSystem(Consumer<BucketFileSystem> consumer)
	{
		try (var fileSystem = new BucketFileSystem(awsFacade, bucketDescriptor, fileSystemProvider))
		{
			consumer.accept(fileSystem);
		}
		catch (IOException x)
		{
			Assertions.fail(x);
		}
	}
}
