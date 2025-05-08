package it.mirkoscotti.nio.s3.extensions.jdk.jsr203;

import it.mirkoscotti.nio.s3.configuration.BucketDescriptor;
import it.mirkoscotti.nio.s3.helpers.JunitHelper;
import it.mirkoscotti.nio.s3.operations.S3Connector;
import it.mirkoscotti.nio.s3.operations.S3Connector.S3ConnectorBuilder;
import it.mirkoscotti.nio.s3.records.BucketRecord;
import it.mirkoscotti.nio.s3.records.ConnectorRecord;
import it.mirkoscotti.nio.s3.records.CredentialsRecord;

import java.net.URI;
import java.nio.file.FileStore;
import java.nio.file.FileSystemAlreadyExistsException;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileAttributeView;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedConstruction.Context;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

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
	private final Map<ConnectorRecord, S3Connector> connectorsCache = JunitHelper.findStaticFieldValueByGenericType(S3FileSystemProvider.class,
																													Map.class,
																													ConnectorRecord.class,
																													S3Connector.class);

	@Mock
	private BucketRecord bucketKey;

	@Mock
	private ConnectorRecord connectorKey;

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
									   @Mock S3ConnectorBuilder connectorBuilder,
									   @Mock S3Connector connector)
	{
		Mockito.when(connectorBuilder.withCredentials(Mockito.anyString(), Mockito.anyString()))
			   .thenReturn(connectorBuilder);
		Mockito.when(connectorBuilder.build()).thenReturn(connector);
		try (var bucketDescriptorMock = Mockito.mockConstruction(BucketDescriptor.class,
																 this::initializeBucketDescriptor);
			 var fileStoreMock = Mockito.mockConstruction(BucketFileStore.class);
			 var connectorMock = Mockito.mockStatic(S3Connector.class))
		{
			connectorMock.when(S3Connector::create).thenReturn(connectorBuilder);
			var fileSystemProvider = new S3FileSystemProvider();
			var fileSystem = JunitHelper.tryCall(() -> fileSystemProvider.newFileSystem(uri,
																						Map.of()));
			JunitHelper.findStaticFieldValues(S3FileSystemProvider.class, Map.class)
					   .forEach(item -> Assertions.assertEquals(1, item.size()));
			Assertions.assertEquals(fileSystem, fileSystemsCache.values().iterator().next());
			Assertions.assertEquals(connector, connectorsCache.values().iterator().next());
			var fileSystemConnector = JunitHelper.findFieldValueByType(fileSystem,
																	   S3Connector.class);
			Assertions.assertEquals(connector, fileSystemConnector);
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
										@Mock S3ConnectorBuilder connectorBuilder,
										@Mock S3Connector connector)
	{
		Mockito.when(connectorBuilder.withCredentials(Mockito.anyString(), Mockito.anyString()))
			   .thenReturn(connectorBuilder);
		Mockito.when(connectorBuilder.build()).thenReturn(connector);
		try (var bucketDescriptorMock = Mockito.mockConstruction(BucketDescriptor.class,
																 this::initializeBucketDescriptor);
			 var fileStoreMock = Mockito.mockConstruction(BucketFileStore.class);
			 var connectorMock = Mockito.mockStatic(S3Connector.class))
		{
			connectorMock.when(S3Connector::create).thenReturn(connectorBuilder);
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
						   @Mock S3Connector connector)
	{
		try (var bucketDescriptorMock = Mockito.mockConstruction(BucketDescriptor.class,
																 this::initializeBucketDescriptor))
		{
			var fileSystemProvider = new S3FileSystemProvider();
			fileSystemsCache.put(bucketKey, fileSystem);
			connectorsCache.put(connectorKey, connector);
			var currentFileSystem = fileSystemProvider.getFileSystem(uri);
			Assertions.assertEquals(fileSystem, currentFileSystem);
		}
	}

	@Test
	void getPathCreatingNewFileSystemTest(@Mock URI uri,
										  @Mock S3ConnectorBuilder connectorBuilder,
										  @Mock S3Connector connector)
	{
		Mockito.when(uri.getPath()).thenReturn("path");
		Mockito.when(connectorBuilder.withCredentials(Mockito.anyString(), Mockito.anyString()))
			   .thenReturn(connectorBuilder);
		Mockito.when(connectorBuilder.build()).thenReturn(connector);
		try (var connectorMock = Mockito.mockStatic(S3Connector.class);
			 var bucketDescriptorMock = Mockito.mockConstruction(BucketDescriptor.class,
																 this::initializeBucketDescriptor);
			 var fileSystemMock = Mockito.mockConstruction(BucketFileSystem.class,
														   this::initializeFileSystem))
		{
			connectorMock.when(S3Connector::create).thenReturn(connectorBuilder);
			var fileSystemProvider = new S3FileSystemProvider();
			var result = fileSystemProvider.getPath(uri);
			Assertions.assertEquals(path, result);
		}
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
		Assertions.assertThrows(UnsupportedOperationException.class,
								() -> fileSystemProvider.getFileStore(path));
	}

	@Test
	void getFileStoreTest(@Mock BucketPath path,
						  @Mock BucketFileSystem fileSystem,
						  @Mock FileStore fileStore)
	{
		Mockito.when(path.getFileSystem()).thenReturn(fileSystem);
		Mockito.when(fileSystem.getFileStores()).thenReturn(List.of(fileStore));
		var fileSystemProvider = new S3FileSystemProvider();
		Assertions.assertEquals(fileStore,
								JunitHelper.tryCall(() -> fileSystemProvider.getFileStore(path)));
	}

	@Test
	void getInvalidFileAttributeViewTest(@Mock Path path)
	{
		var fileSystemProvider = new S3FileSystemProvider();
		Assertions.assertThrows(UnsupportedOperationException.class,
								() -> fileSystemProvider.getFileAttributeView(path,
																			  FileAttributeView.class));
	}

	@Test
	void getUnsupportedFileAttributeViewTest(@Mock BucketPath path)
	{
		var fileSystemProvider = new S3FileSystemProvider();
		Assertions.assertNull(fileSystemProvider.getFileAttributeView(path,
																	  FileAttributeView.class));
	}

	@Test
	void getFileAttributeViewTest(@Mock BucketPath path,
								  @Mock BucketFileSystem fileSystem,
								  @Mock BucketFileStore fileStore,
								  @Mock S3Connector connector)
	{
		Mockito.when(path.getFileSystem()).thenReturn(fileSystem);
		Mockito.when(fileSystem.connector()).thenReturn(connector);
		Mockito.when(fileSystem.getFileStores()).thenReturn(List.of(fileStore));
		Mockito.when(fileStore.name()).thenReturn("bucket-name");
		var fileSystemProvider = new S3FileSystemProvider();
		Assertions.assertNotNull(fileSystemProvider.getFileAttributeView(path,
																		 ObjectBasicFileAttributeView.class));
	}

	@Test
	void readInvalidAttributesTest(@Mock Path path)
	{
		var fileSystemProvider = new S3FileSystemProvider();
		Assertions.assertThrows(UnsupportedOperationException.class,
								() -> fileSystemProvider.readAttributes(path,
																		BasicFileAttributes.class));
	}

	@Test
	void readUnsupportedAttributesTest(@Mock BucketPath path)
	{
		var fileSystemProvider = new S3FileSystemProvider();
		Assertions.assertThrows(UnsupportedOperationException.class,
								() -> fileSystemProvider.readAttributes(path,
																		BasicFileAttributes.class));
	}

	@Test
	void readFileAttributesTest(@Mock BucketPath path,
								@Mock BucketFileSystem fileSystem,
								@Mock BucketFileStore fileStore,
								@Mock S3Connector connector)
	{
		Mockito.when(path.getFileSystem()).thenReturn(fileSystem);
		Mockito.when(fileSystem.connector()).thenReturn(connector);
		Mockito.when(fileSystem.getFileStores()).thenReturn(List.of(fileStore));
		Mockito.when(fileStore.name()).thenReturn("bucket-name");
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
}
