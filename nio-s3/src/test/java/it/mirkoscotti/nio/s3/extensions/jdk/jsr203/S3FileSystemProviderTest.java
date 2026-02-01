package it.mirkoscotti.nio.s3.extensions.jdk.jsr203;

import java.io.IOException;
import java.net.URI;
import java.nio.file.DirectoryStream.Filter;
import java.nio.file.FileStore;
import java.nio.file.FileSystemAlreadyExistsException;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.NoSuchFileException;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.ProviderMismatchException;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileAttributeView;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedConstruction.Context;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import it.mirkoscotti.nio.s3.configuration.BucketDescriptor;
import it.mirkoscotti.nio.s3.functions.LazyReference;
import it.mirkoscotti.nio.s3.helpers.JunitHelper;
import it.mirkoscotti.nio.s3.operations.AwsFacade;
import it.mirkoscotti.nio.s3.operations.S3Connector;
import it.mirkoscotti.nio.s3.records.AwsRecord;
import it.mirkoscotti.nio.s3.records.BucketRecord;
import it.mirkoscotti.nio.s3.records.CredentialsRecord;

import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.S3Exception;

/**
 * @author mirko.scotti
 * @version May 01, 2025
 */
@ExtendWith(MockitoExtension.class)
class S3FileSystemProviderTest
{

	private static final String ACCESS_KEY = "access-key";

	private static final String SECRET_KEY = "secret-key";

	private static final CredentialsRecord CREDENTIALS = new CredentialsRecord(ACCESS_KEY,
																			   SECRET_KEY);

	@SuppressWarnings("unchecked")
	private final Map<BucketRecord, BucketFileSystem> fileSystemsCache = JunitHelper.findStaticFieldValueByGenericType(S3FileSystemProvider.class,
																													   Map.class,
																													   BucketRecord.class,
																													   BucketFileSystem.class);

	@SuppressWarnings("unchecked")
	private final Map<AwsRecord, AwsFacade> facadeCache = JunitHelper.findStaticFieldValueByGenericType(S3FileSystemProvider.class,
																										Map.class,
																										AwsRecord.class,
																										AwsFacade.class);

	@Mock
	private BucketRecord bucketKey;

	@Mock
	private AwsRecord connectorKey;

	@Mock
	private BucketPath path;

	@AfterEach
	void afterEach()
	{
		fileSystemsCache.clear();
		facadeCache.clear();
	}

	@Test
	void getSchemeTest()
	{
		var fileSystemProvider = new S3FileSystemProvider();
		Assertions.assertEquals("s3", fileSystemProvider.getScheme());
	}

	@Test
	void newFileSystemNotYetCachedTest(@Mock URI uri,
									   @Mock AwsRecord awsRecord,
									   @Mock LazyReference<S3Connector> reference,
									   @Mock S3Connector connector)
	{
		try (var referenceMock = Mockito.mockStatic(LazyReference.class);
			 var bucketDescriptorMock = Mockito.mockConstruction(BucketDescriptor.class,
																 this::initializeBucketDescriptor);
			 var fileStoreMock = Mockito.mockConstruction(BucketFileStore.class))
		{
			referenceMock.when(() -> LazyReference.of(Mockito.any())).thenReturn(reference);
			Mockito.when(reference.get()).thenReturn(connector);
			var awsFacade = Mockito.spy(AwsFacade.create(awsRecord));
			try (var facadeMock = Mockito.mockStatic(AwsFacade.class))
			{
				facadeMock.when(() -> AwsFacade.create(Mockito.any(AwsRecord.class)))
						  .thenReturn(awsFacade);
				var fileSystemProvider = new S3FileSystemProvider();
				var fileSystem = JunitHelper.tryCall(() -> fileSystemProvider.newFileSystem(uri,
																							Map.of()));
				JunitHelper.findStaticFieldValues(S3FileSystemProvider.class, Map.class)
						   .forEach(item -> Assertions.assertEquals(1, item.size()));
				Assertions.assertEquals(fileSystem, fileSystemsCache.values().iterator().next());
				Assertions.assertEquals(awsFacade, facadeCache.values().iterator().next());
				var fileSystemFacade = JunitHelper.findFieldValueByType(fileSystem,
																		AwsFacade.class);
				Assertions.assertEquals(awsFacade, fileSystemFacade);
				var provider = JunitHelper.findFieldValueByType(fileSystem,
																S3FileSystemProvider.class);
				Assertions.assertEquals(fileSystemProvider, provider);
				var expectedFileStore = fileStoreMock.constructed().get(0);
				var resultFileStore = JunitHelper.findFieldValueByType(fileSystem,
																	   BucketFileStore.class);
				Assertions.assertEquals(expectedFileStore, resultFileStore);
			}
		}
	}

	@Test
	void newFileSystemAlreadyCachedTest(@Mock URI uri,
										@Mock AwsFacade awsFacade,
										@Mock S3Connector connector)
	{
		try (var bucketDescriptorMock = Mockito.mockConstruction(BucketDescriptor.class,
																 this::initializeBucketDescriptor);
			 var fileStoreMock = Mockito.mockConstruction(BucketFileStore.class);
			 var factoryMock = Mockito.mockStatic(AwsFacade.class))
		{
			factoryMock.when(() -> AwsFacade.create(Mockito.any(AwsRecord.class)))
					   .thenReturn(awsFacade);
			var fileSystemProvider = new S3FileSystemProvider();
			JunitHelper.tryCall(() -> fileSystemProvider.newFileSystem(uri, Map.of()));
			JunitHelper.failCall(FileSystemAlreadyExistsException.class,
								 () -> fileSystemProvider.newFileSystem(uri, Map.of()));
		}
	}

	@Test
	void getFileSystemNotAlreadyCachedTest(@Mock URI uri)
	{
		try (var bucketDescriptorMock = Mockito.mockConstruction(BucketDescriptor.class,
																 this::initializeBucketDescriptor))
		{
			var fileSystemProvider = new S3FileSystemProvider();
			Assertions.assertThrows(FileSystemNotFoundException.class,
									() -> fileSystemProvider.getFileSystem(uri));
		}
	}

	@Test
	void getFileSystemTest(@Mock URI uri,
						   @Mock BucketFileSystem fileSystem,
						   @Mock AwsFacade awsFacade)
	{
		try (var bucketDescriptorMock = Mockito.mockConstruction(BucketDescriptor.class,
																 this::initializeBucketDescriptor))
		{
			var fileSystemProvider = new S3FileSystemProvider();
			fileSystemsCache.put(bucketKey, fileSystem);
			facadeCache.put(connectorKey, awsFacade);
			var currentFileSystem = fileSystemProvider.getFileSystem(uri);
			Assertions.assertEquals(fileSystem, currentFileSystem);
		}
	}

	@Test
	void getPathCreatingNewFileSystemTest(@Mock URI uri, @Mock AwsFacade awsFacade)
	{
		Mockito.when(uri.getPath()).thenReturn("path");
		try (var bucketDescriptorMock = Mockito.mockConstruction(BucketDescriptor.class,
																 this::initializeBucketDescriptor);
			 var fileSystemMock = Mockito.mockConstruction(BucketFileSystem.class,
														   this::initializeFileSystem);
			 var factoryMock = Mockito.mockStatic(AwsFacade.class))
		{
			factoryMock.when(() -> AwsFacade.create(Mockito.any(AwsRecord.class)))
					   .thenReturn(awsFacade);
			var fileSystemProvider = new S3FileSystemProvider();
			var result = fileSystemProvider.getPath(uri);
			Assertions.assertEquals(path, result);
		}
	}

	@Test
	void newByteChannelWithUnsupportedPathTest(@Mock Path path)
	{
		var set = Set.<OpenOption>of();
		var fileSystemProvider = new S3FileSystemProvider();
		Assertions.assertThrows(ProviderMismatchException.class,
								() -> fileSystemProvider.newByteChannel(path, set));
	}

	@Test
	void newByteChannelTest()
	{
		try (var mock = Mockito.mockConstruction(BucketSeekableByteChannel.class))
		{
			var fileSystemProvider = new S3FileSystemProvider();
			var result = Assertions.assertDoesNotThrow(() -> fileSystemProvider.newByteChannel(path,
																							   Set.of()));
			var expected = mock.constructed().get(0);
			Assertions.assertEquals(expected, result);
		}
	}

	@Test
	void newDirectoryStreamWithUnsupportedPathTest(@Mock Path path,
												   @Mock Filter<? super Path> filter)
	{
		var fileSystemProvider = new S3FileSystemProvider();
		Assertions.assertThrows(ProviderMismatchException.class,
								() -> fileSystemProvider.newDirectoryStream(path, filter));
	}

	@Test
	void newDirectoryStreamTest(@Mock Filter<? super Path> filter)
	{
		try (var mock = Mockito.mockConstruction(BucketDirectoryStream.class))
		{
			var fileSystemProvider = new S3FileSystemProvider();
			var result = Assertions.assertDoesNotThrow(() -> fileSystemProvider.newDirectoryStream(path,
																								   filter));
			var expected = mock.constructed().get(0);
			Assertions.assertEquals(expected, result);
		}
	}

	@Test
	void createDirectoryFromUnsupportedPathTest(@Mock Path path)
	{
		var fileSystemProvider = new S3FileSystemProvider();
		Assertions.assertThrows(ProviderMismatchException.class,
								() -> fileSystemProvider.createDirectory(path));
	}

	@Test
	void unpredictedExceptionWhileCreatingDirectoryTest(@Mock BucketFileSystem fileSystem,
														@Mock BucketFileStore fileStore,
														@Mock AwsFacade awsFacade)
	{
		Mockito.when(path.getFileSystem()).thenReturn(fileSystem);
		Mockito.when(path.getParent()).thenReturn(path);
		Mockito.when(path.isRootDirectory()).thenReturn(false);
		Mockito.when(fileSystem.awsFacade()).thenReturn(awsFacade);
		Mockito.when(fileSystem.getFileStores()).thenReturn(List.of(fileStore));
		Mockito.when(fileStore.name()).thenReturn("test-bucket");
		Mockito.when(awsFacade.objectMetadata(Mockito.anyString(), Mockito.anyString()))
			   .thenThrow(S3Exception.class);
		var fileSystemProvider = new S3FileSystemProvider();
		var exception = Assertions.assertThrows(IOException.class,
												() -> fileSystemProvider.createDirectory(path));
		Assertions.assertInstanceOf(S3Exception.class, exception.getCause());
	}

	@Test
	void isUnsupportedPathHiddenTest(@Mock Path path)
	{
		var fileSystemProvider = new S3FileSystemProvider();
		Assertions.assertThrows(ProviderMismatchException.class,
								() -> fileSystemProvider.isHidden(path));
	}

	@Test
	void isHiddenTest()
	{
		var fileSystemProvider = new S3FileSystemProvider();
		Assertions.assertFalse(JunitHelper.tryCall(() -> fileSystemProvider.isHidden(path)));
	}

	@Test
	void getFileStoreFromNullPathTest()
	{
		var fileSystemProvider = new S3FileSystemProvider();
		Assertions.assertThrows(NullPointerException.class,
								() -> fileSystemProvider.getFileStore(null));
	}

	@Test
	void getFileStoreFromUnsupportedPathTest(@Mock Path path)
	{
		var fileSystemProvider = new S3FileSystemProvider();
		Assertions.assertThrows(ProviderMismatchException.class,
								() -> fileSystemProvider.getFileStore(path));
	}

	@Test
	void getFileStoreTest(@Mock BucketFileSystem fileSystem, @Mock FileStore fileStore)
	{
		Mockito.when(path.getFileSystem()).thenReturn(fileSystem);
		Mockito.when(fileSystem.getFileStores()).thenReturn(List.of(fileStore));
		var fileSystemProvider = new S3FileSystemProvider();
		Assertions.assertEquals(fileStore,
								JunitHelper.tryCall(() -> fileSystemProvider.getFileStore(path)));
	}

	@Test
	void checkAccessToInvalidPathTest(@Mock Path path)
	{
		var fileSystemProvider = new S3FileSystemProvider();
		Assertions.assertThrows(ProviderMismatchException.class,
								() -> fileSystemProvider.checkAccess(path));
	}

	@Test
	void checkAccessToNotExistingFileTest(@Mock BucketFileSystem fileSystem,
										  @Mock BucketFileStore fileStore,
										  @Mock AwsFacade awsFacade)
	{
		initializePath(fileSystem, fileStore, awsFacade);
		Mockito.when(awsFacade.objectMetadata(Mockito.anyString(), Mockito.anyString()))
			   .thenThrow(NoSuchKeyException.class);
		var fileSystemProvider = new S3FileSystemProvider();
		Assertions.assertThrows(NoSuchFileException.class,
								() -> fileSystemProvider.checkAccess(path));
	}

	@Test
	void checkAccessToExistingFileTest(@Mock BucketFileSystem fileSystem,
									   @Mock BucketFileStore fileStore,
									   @Mock AwsFacade awsFacade)
	{
		initializePath(fileSystem, fileStore, awsFacade);
		var fileSystemProvider = new S3FileSystemProvider();
		Assertions.assertDoesNotThrow(() -> fileSystemProvider.checkAccess(path));
	}

	@Test
	void getInvalidFileAttributeViewTest(@Mock Path path)
	{
		var fileSystemProvider = new S3FileSystemProvider();
		Assertions.assertThrows(ProviderMismatchException.class,
								() -> fileSystemProvider.getFileAttributeView(path,
																			  FileAttributeView.class));
	}

	@Test
	void getUnsupportedFileAttributeViewTest()
	{
		var fileSystemProvider = new S3FileSystemProvider();
		Assertions.assertNull(fileSystemProvider.getFileAttributeView(path,
																	  FileAttributeView.class));
	}

	@Test
	void getFileAttributeViewTest(@Mock BucketFileSystem fileSystem,
								  @Mock BucketFileStore fileStore,
								  @Mock AwsFacade awsFacade)
	{
		initializePath(fileSystem, fileStore, awsFacade);
		var fileSystemProvider = new S3FileSystemProvider();
		Assertions.assertNotNull(fileSystemProvider.getFileAttributeView(path,
																		 ObjectBasicFileAttributeView.class));
	}

	@Test
	void readInvalidAttributesTest(@Mock Path path)
	{
		var fileSystemProvider = new S3FileSystemProvider();
		Assertions.assertThrows(ProviderMismatchException.class,
								() -> fileSystemProvider.readAttributes(path,
																		BasicFileAttributes.class));
	}

	@Test
	void readUnsupportedAttributesTest(@Mock BasicFileAttributes basicFileAttributes)
	{
		var fileSystemProvider = new S3FileSystemProvider();
		var type = basicFileAttributes.getClass();
		Assertions.assertThrows(UnsupportedOperationException.class,
								() -> fileSystemProvider.readAttributes(path, type));
	}

	@Test
	void readFileAttributesTest(@Mock BucketFileSystem fileSystem,
								@Mock BucketFileStore fileStore,
								@Mock AwsRecord awsRecord,
								@Mock LazyReference<S3Connector> reference,
								@Mock S3Connector connector)
	{
		try (var mock = Mockito.mockStatic(LazyReference.class))
		{
			mock.when(() -> LazyReference.of(Mockito.any())).thenReturn(reference);
			Mockito.when(reference.get()).thenReturn(connector);
			var awsFacade = Mockito.spy(AwsFacade.create(awsRecord));
			initializePath(fileSystem, fileStore, awsFacade);
			var fileSystemProvider = new S3FileSystemProvider();
			Assertions.assertDoesNotThrow(() -> fileSystemProvider.readAttributes(path,
																				  ObjectBasicFileAttributes.class));
		}
	}

	private void initializeBucketDescriptor(BucketDescriptor bucketDescriptor, Context context)
	{
		Mockito.when(bucketDescriptor.bucketKey()).thenReturn(bucketKey);
		Mockito.lenient().when(connectorKey.credentials()).thenReturn(CREDENTIALS);
		Mockito.when(bucketDescriptor.connectorKey()).thenReturn(connectorKey);
	}

	private void initializeFileSystem(BucketFileSystem fileSystem, Context context)
	{
		Mockito.when(fileSystem.getPath(Mockito.anyString())).thenReturn(path);
	}

	private void initializePath(BucketFileSystem fileSystem,
								BucketFileStore fileStore,
								AwsFacade awsFacade)
	{
		Mockito.when(path.getFileSystem()).thenReturn(fileSystem);
		Mockito.when(fileSystem.awsFacade()).thenReturn(awsFacade);
		Mockito.when(fileSystem.getFileStores()).thenReturn(List.of(fileStore));
		Mockito.when(fileStore.name()).thenReturn("bucket-name");
	}
}
