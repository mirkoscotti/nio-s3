package io.github.mirkoscotti.nio.s3.extensions.jdk.jsr203;

import java.io.IOException;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.channels.Channel;
import java.nio.file.AccessDeniedException;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.CleanupMode;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.junit.jupiter.MockitoExtension;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import io.github.mirkoscotti.nio.s3.extensions.testcontainers.S3Container;
import io.github.mirkoscotti.nio.s3.helpers.ContainerHelper;
import io.github.mirkoscotti.nio.s3.helpers.IoHelper;
import io.github.mirkoscotti.nio.s3.helpers.JunitHelper;

/**
 * @author mirko.scotti
 * @version Mar 18, 2026
 */
@Testcontainers
@ExtendWith(MockitoExtension.class)
class BucketFileSystemIT
{

	private static final String TEST_BUCKET = "test-bucket";

	private static final URI TEST_URI = URI.create("s3://".concat(TEST_BUCKET));

	private static final String DIRECTORY = "directory/";

	private static final String FILE = "file.txt";

	private static final int FILE_SIZE = 11 * 1024 * 1024;

	@TempDir(cleanup = CleanupMode.ALWAYS)
	private static Path baseDirectory;

	private static Path hugeFile;

	@Container
	@SuppressWarnings("resource")
	private static final S3Container CONTAINER = new S3Container().withBucket(TEST_BUCKET);

	@BeforeAll
	static void beforeAll()
	{
		hugeFile = JunitHelper.tryCall(BucketFileSystemIT::createFile);
	}

	@AfterEach
	void afterEach()
	{
		ContainerHelper.deleteObjects(CONTAINER, TEST_BUCKET);
	}

	@Test
	void createFileSystemWithWrongCredentialsTest() throws IOException
	{
		var uri = URI.create("s3://".concat(TEST_BUCKET));
		var map = ContainerHelper.unauthenticatedProperties(CONTAINER);
		var reference = new AtomicReference<FileSystem>();
		var exception = Assertions.assertThrows(IllegalArgumentException.class,
												() -> createFileSystem(uri, map, reference));
		try (var fileSystem = reference.get())
		{
			Assertions.assertInstanceOf(AccessDeniedException.class, exception.getCause());
		}
	}

	@Test
	void watchServiceForcedToCloseTest() throws IOException
	{
		var properties = ContainerHelper.standardProperties(CONTAINER);
		ScheduledExecutorService scheduler = null;
		try (var fileSystem = FileSystems.newFileSystem(TEST_URI, properties))
		{
			var watchService = fileSystem.newWatchService();
			scheduler = JunitHelper.findFieldValueByType(watchService,
														 ScheduledExecutorService.class);
			Assertions.assertFalse(scheduler.isTerminated());
		}
		finally
		{
			Optional.ofNullable(scheduler)
					.map(ScheduledExecutorService::isTerminated)
					.ifPresent(Assertions::assertTrue);
		}
	}

	@Test
	void directoryStreamForcedToCloseTest() throws IOException
	{
		CONTAINER.createObject(TEST_BUCKET, DIRECTORY);
		var properties = ContainerHelper.standardProperties(CONTAINER);
		DirectoryStream<Path> stream = null;
		try (var fileSystem = FileSystems.newFileSystem(TEST_URI, properties))
		{
			var path = fileSystem.getPath(DIRECTORY);
			stream = Files.newDirectoryStream(path);
			var isClosed = JunitHelper.findFieldValueByName(stream, "isClosed", Boolean.class);
			Assertions.assertFalse(isClosed);
		}
		finally
		{
			var isClosed = JunitHelper.findFieldValueByName(stream, "isClosed", Boolean.class);
			Optional.ofNullable(isClosed).ifPresent(Assertions::assertTrue);
		}
	}

	@Test
	@SuppressWarnings("unchecked")
	void seekableByteChannelForcedToCloseWithoutMultipartTest() throws IOException
	{
		CONTAINER.createObject(TEST_BUCKET, DIRECTORY);
		var properties = ContainerHelper.standardProperties(CONTAINER);
		Optional<BucketWritableByteChannel> optional = Optional.empty();
		try (var fileSystem = FileSystems.newFileSystem(TEST_URI, properties))
		{
			var path = fileSystem.getPath(DIRECTORY).resolve(FILE);
			var channel = Files.newByteChannel(path,
											   StandardOpenOption.WRITE,
											   StandardOpenOption.CREATE_NEW);
			optional = JunitHelper.findFieldValueByGenericType(channel,
															   Optional.class,
															   BucketWritableByteChannel.class);
			Assertions.assertTrue(optional.isPresent());
		}
		finally
		{
			optional.map(Channel::isOpen).ifPresent(Assertions::assertFalse);
		}
	}

	@Test
	@SuppressWarnings("unchecked")
	void seekableByteChannelForcedToCloseWithMultipartTest() throws IOException
	{
		CONTAINER.createObject(TEST_BUCKET, DIRECTORY);
		var properties = ContainerHelper.standardProperties(CONTAINER);
		Optional<BucketWritableByteChannel> optional = Optional.empty();
		try (var fileSystem = FileSystems.newFileSystem(TEST_URI, properties))
		{
			var path = fileSystem.getPath(DIRECTORY).resolve(FILE);
			var channel = Files.newByteChannel(path,
											   StandardOpenOption.WRITE,
											   StandardOpenOption.CREATE_NEW);
			var buffer = ByteBuffer.wrap(Files.readAllBytes(hugeFile));
			channel.write(buffer);
			optional = JunitHelper.findFieldValueByGenericType(channel,
															   Optional.class,
															   BucketWritableByteChannel.class);
			Assertions.assertTrue(optional.isPresent());
		}
		finally
		{
			optional.map(Channel::isOpen).ifPresent(Assertions::assertFalse);
		}
	}

	private void createFileSystem(URI uri,
								  Map<String, ?> properties,
								  AtomicReference<FileSystem> reference)
		throws IOException
	{
		var fileSystem = FileSystems.newFileSystem(uri, properties);
		reference.set(fileSystem);
	}

	private static Path createFile() throws IOException
	{
		var result = baseDirectory.resolve(FILE);
		IoHelper.createNotEmptyFile(result, FILE_SIZE);
		return result;
	}
}
