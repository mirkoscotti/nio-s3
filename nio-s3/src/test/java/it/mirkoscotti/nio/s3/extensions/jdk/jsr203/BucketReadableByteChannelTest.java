package it.mirkoscotti.nio.s3.extensions.jdk.jsr203;

import java.io.IOException;
import java.nio.channels.ClosedChannelException;
import java.nio.file.FileStore;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import it.mirkoscotti.nio.s3.operations.S3Connector;

/**
 * @author mirko.scotti
 * @version Nov 29, 2025
 */
@ExtendWith(MockitoExtension.class)
class BucketReadableByteChannelTest
{

	@Mock
	private S3Connector connector;

	@Mock
	private BucketPath path;

	@Mock
	private BucketFileSystem fileSystem;

	@Mock
	private FileStore fileStore;

	@Mock
	private BasicFileAttributes fileAttributes;

	@BeforeEach
	void beforeEach()
	{
		Mockito.when(connector.objectMetadata(Mockito.anyString(), Mockito.anyString()))
			   .thenReturn(fileAttributes);
		Mockito.when(path.getFileSystem()).thenReturn(fileSystem);
		Mockito.when(fileSystem.getFileStores()).thenReturn(List.of(fileStore));
		Mockito.when(fileStore.name()).thenReturn("file-store");
		Mockito.when(path.toString()).thenReturn("path");
	}

	@Test
	void isOpenTest()
	{
		var readableByteChannel = new BucketReadableByteChannel(connector, path);
		try (var channel = readableByteChannel)
		{
			Assertions.assertTrue(channel.isOpen());
		}
		catch (IOException x)
		{
			Assertions.fail(x);
		}
		Assertions.assertFalse(readableByteChannel.isOpen());
	}

	@Test
	void positionWhenChannelIsClosedTest()
	{
		var readableByteChannel = new BucketReadableByteChannel(connector, path);
		try (var channel = readableByteChannel)
		{
			// Nothing to do
		}
		catch (IOException x)
		{
			Assertions.fail(x);
		}
		Assertions.assertThrows(ClosedChannelException.class, readableByteChannel::position);
	}

	@Test
	void negativePositionTest()
	{
		try (var channel = new BucketReadableByteChannel(connector, path))
		{
			Assertions.assertThrows(IllegalArgumentException.class, () -> channel.position(-1));
		}
		catch (IOException x)
		{
			Assertions.fail(x);
		}
	}
}
