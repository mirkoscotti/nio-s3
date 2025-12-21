package it.mirkoscotti.nio.s3.extensions.jdk.jsr203;

import java.net.URI;
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
import it.mirkoscotti.nio.s3.helpers.JunitHelper;
import it.mirkoscotti.nio.s3.operations.ConnectorFactory;
import it.mirkoscotti.nio.s3.operations.S3Connector;
import it.mirkoscotti.nio.s3.records.BucketRecord;
import it.mirkoscotti.nio.s3.records.CredentialsRecord;
import it.mirkoscotti.nio.s3.records.FactoryRecord;

import software.amazon.awssdk.services.s3.model.NoSuchKeyException;

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
	private final Map<FactoryRecord, ConnectorFactory> connectorsCache = JunitHelper.findStaticFieldValueByGenericType(S3FileSystemProvider.class,
																													   Map.class,
																													   FactoryRecord.class,
																													   ConnectorFactory.class);

	@Mock
	private BucketRecord bucketKey;

	@Mock
	private FactoryRecord connectorKey;

	@Mock
	private BucketPath path;

	@AfterEach
	void afterEach()
	{
		fileSystemsCache.clear();
		connectorsCache.clear();
	}

	@Test
	void getSchemeTest()
	{
		var fileSystemProvider = new S3FileSystemProvider();
		Assertions.assertEquals("s3", fileSystemProvider.getScheme());
	}

	@Test
	void newFileSystemNotYetCachedTest(@Mock URI uri,
									   @Mock ConnectorFactory connectorFactory,
									   @Mock S3Connector connector)
	{
		Mockito.when(connectorFactory.s3Connector()).thenReturn(connector);
		try (var bucketDescriptorMock = Mockito.mockConstruction(BucketDescriptor.class,
																 this::initializeBucketDescriptor);
			 var fileStoreMock = Mockito.mockConstruction(BucketFileStore.class);
			 var factoryMock = Mockito.mockStatic(ConnectorFactory.class))
		{
			factoryMock.when(() -> ConnectorFactory.create(Mockito.any(FactoryRecord.class)))
					   .thenReturn(connectorFactory);
			var fileSystemProvider = new S3FileSystemProvider();
			var fileSystem = JunitHelper.tryCall(() -> fileSystemProvider.newFileSystem(uri,
																						Map.of()));
			JunitHelper.findStaticFieldValues(S3FileSystemProvider.class, Map.class)
					   .forEach(item -> Assertions.assertEquals(1, item.size()));
			Assertions.assertEquals(fileSystem, fileSystemsCache.values().iterator().next());
			Assertions.assertEquals(connectorFactory, connectorsCache.values().iterator().next());
			var fileSystemConnector = JunitHelper.findFieldValueByType(fileSystem,
																	   ConnectorFactory.class);
			Assertions.assertEquals(connectorFactory, fileSystemConnector);
			var provider = JunitHelper.findFieldValueByType(fileSystem, S3FileSystemProvider.class);
			Assertions.assertEquals(fileSystemProvider, provider);
			var expectedFileStore = fileStoreMock.constructed().get(0);
			var resultFileStore = JunitHelper.findFieldValueByType(fileSystem,
																   BucketFileStore.class);
			Assertions.assertEquals(expectedFileStore, resultFileStore);
		}
	}

	@Test
	void newFileSystemAlreadyCachedTest(@Mock URI uri,
										@Mock ConnectorFactory connectorFactory,
										@Mock S3Connector connector)
	{
		Mockito.when(connectorFactory.s3Connector()).thenReturn(connector);
		try (var bucketDescriptorMock = Mockito.mockConstruction(BucketDescriptor.class,
																 this::initializeBucketDescriptor);
			 var fileStoreMock = Mockito.mockConstruction(BucketFileStore.class);
			 var factoryMock = Mockito.mockStatic(ConnectorFactory.class))
		{
			factoryMock.when(() -> ConnectorFactory.create(Mockito.any(FactoryRecord.class)))
					   .thenReturn(connectorFactory);
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
						   @Mock ConnectorFactory connectorFactory)
	{
		try (var bucketDescriptorMock = Mockito.mockConstruction(BucketDescriptor.class,
																 this::initializeBucketDescriptor))
		{
			var fileSystemProvider = new S3FileSystemProvider();
			fileSystemsCache.put(bucketKey, fileSystem);
			connectorsCache.put(connectorKey, connectorFactory);
			var currentFileSystem = fileSystemProvider.getFileSystem(uri);
			Assertions.assertEquals(fileSystem, currentFileSystem);
		}
	}

	@Test
	void getPathCreatingNewFileSystemTest(@Mock URI uri, @Mock ConnectorFactory connectorFactory)
	{
		Mockito.when(uri.getPath()).thenReturn("path");
		try (var bucketDescriptorMock = Mockito.mockConstruction(BucketDescriptor.class,
																 this::initializeBucketDescriptor);
			 var fileSystemMock = Mockito.mockConstruction(BucketFileSystem.class,
														   this::initializeFileSystem);
			 var factoryMock = Mockito.mockStatic(ConnectorFactory.class))
		{
			factoryMock.when(() -> ConnectorFactory.create(Mockito.any(FactoryRecord.class)))
					   .thenReturn(connectorFactory);
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
	void newByteChannelTest(@Mock BucketFileSystem fileSystem,
							@Mock ConnectorFactory connectorFactory)
	{
		Mockito.when(path.getFileSystem()).thenReturn(fileSystem);
		Mockito.when(fileSystem.connectorFactory()).thenReturn(connectorFactory);
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
										  @Mock S3Connector connector,
										  @Mock ConnectorFactory connectorFactory)
	{
		initializePath(fileSystem, fileStore, connectorFactory);
		Mockito.when(connectorFactory.s3Connector()).thenReturn(connector);
		Mockito.when(connector.objectMetadata(Mockito.anyString(), Mockito.anyString()))
			   .thenThrow(NoSuchKeyException.class);
		var fileSystemProvider = new S3FileSystemProvider();
		Assertions.assertThrows(NoSuchFileException.class,
								() -> fileSystemProvider.checkAccess(path));
	}

	@Test
	void checkAccessToExistingFileTest(@Mock BucketFileSystem fileSystem,
									   @Mock BucketFileStore fileStore,
									   @Mock ConnectorFactory connectorFactory,
									   @Mock S3Connector connector)
	{
		Mockito.when(connectorFactory.s3Connector()).thenReturn(connector);
		initializePath(fileSystem, fileStore, connectorFactory);
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
								  @Mock ConnectorFactory connectorFactory,
								  @Mock S3Connector connector)
	{
		Mockito.when(connectorFactory.s3Connector()).thenReturn(connector);
		initializePath(fileSystem, fileStore, connectorFactory);
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
								@Mock ConnectorFactory connectorFactory,
								@Mock S3Connector connector)
	{
		Mockito.when(connectorFactory.s3Connector()).thenReturn(connector);
		initializePath(fileSystem, fileStore, connectorFactory);
		var fileSystemProvider = new S3FileSystemProvider();
		Assertions.assertDoesNotThrow(() -> fileSystemProvider.readAttributes(path,
																			  ObjectBasicFileAttributes.class));
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
								ConnectorFactory connectorFactory)
	{
		Mockito.when(path.getFileSystem()).thenReturn(fileSystem);
		Mockito.when(fileSystem.connectorFactory()).thenReturn(connectorFactory);
		Mockito.when(fileSystem.getFileStores()).thenReturn(List.of(fileStore));
		Mockito.when(fileStore.name()).thenReturn("bucket-name");
	}
}
