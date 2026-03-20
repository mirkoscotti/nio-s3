package it.mirkoscotti.nio.s3.extensions.jdk.jsr203;

import java.io.IOException;
import java.net.URI;
import java.nio.channels.Channel;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Optional;
import java.util.concurrent.ScheduledExecutorService;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import it.mirkoscotti.nio.s3.extensions.testcontainers.S3Container;
import it.mirkoscotti.nio.s3.helpers.ContainersHelper;
import it.mirkoscotti.nio.s3.helpers.JunitHelper;

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

	@Container
	@SuppressWarnings("resource")
	private static final S3Container CONTAINER = new S3Container().withBucket(TEST_BUCKET);

	@BeforeAll
	static void beforeAll()
	{
		CONTAINER.createObject(TEST_BUCKET, DIRECTORY);
	}

	@Test
	void watchServiceForcedToCloseTest()
	{
		var properties = ContainersHelper.standardProperties(CONTAINER);
		ScheduledExecutorService scheduler = null;
		try (var fileSystem = FileSystems.newFileSystem(TEST_URI, properties))
		{
			var watchService = fileSystem.newWatchService();
			scheduler = JunitHelper.findFieldValueByType(watchService,
														 ScheduledExecutorService.class);
			Assertions.assertFalse(scheduler.isTerminated());
		}
		catch (IOException x)
		{
			Assertions.fail(x);
		}
		finally
		{
			Optional.ofNullable(scheduler)
					.map(ScheduledExecutorService::isTerminated)
					.ifPresent(Assertions::assertTrue);
		}
	}

	@Test
	void directoryStreamForcedToCloseTest()
	{
		var properties = ContainersHelper.standardProperties(CONTAINER);
		DirectoryStream<Path> stream = null;
		try (var fileSystem = FileSystems.newFileSystem(TEST_URI, properties))
		{
			var path = fileSystem.getPath(DIRECTORY);
			stream = Files.newDirectoryStream(path);
			var isClosed = JunitHelper.findFieldValueByName(stream, "isClosed", Boolean.class);
			Assertions.assertFalse(isClosed);
		}
		catch (IOException x)
		{
			Assertions.fail(x);
		}
		finally
		{
			var isClosed = JunitHelper.findFieldValueByName(stream, "isClosed", Boolean.class);
			Optional.ofNullable(isClosed).ifPresent(Assertions::assertTrue);
		}
	}

	@Test
	@SuppressWarnings("unchecked")
	void seekableByteChannelForcedToCloseTest()
	{
		var properties = ContainersHelper.standardProperties(CONTAINER);
		Optional<BucketWritableByteChannel> optional = null;
		try (var fileSystem = FileSystems.newFileSystem(TEST_URI, properties))
		{
			var path = fileSystem.getPath(DIRECTORY).resolve("file.txt");
			var channel = Files.newByteChannel(path, StandardOpenOption.WRITE,
											   StandardOpenOption.CREATE_NEW);
			optional = JunitHelper.findFieldValueByGenericType(channel, Optional.class,
															   BucketWritableByteChannel.class);
			Assertions.assertTrue(optional.isPresent());
		}
		catch (IOException x)
		{
			Assertions.fail(x);
		}
		finally
		{
			optional.map(Channel::isOpen).ifPresent(Assertions::assertFalse);
		}
	}
}
