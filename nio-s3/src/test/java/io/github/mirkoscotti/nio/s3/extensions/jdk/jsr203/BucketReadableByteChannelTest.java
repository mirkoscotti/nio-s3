package io.github.mirkoscotti.nio.s3.extensions.jdk.jsr203;

import static org.mockito.Mockito.when;

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

import io.github.mirkoscotti.nio.s3.operations.AwsFacade;

/**
 * @author mirko.scotti
 * @version Nov 29, 2025
 */
@ExtendWith(MockitoExtension.class)
class BucketReadableByteChannelTest
{

	@Mock
	private AwsFacade awsFacade;

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
		when(awsFacade.objectMetadata(Mockito.anyString(),
									  Mockito.anyString())).thenReturn(fileAttributes);
		when(path.getFileSystem()).thenReturn(fileSystem);
		when(fileSystem.getFileStores()).thenReturn(List.of(fileStore));
		when(fileStore.name()).thenReturn("file-store");
		when(path.toString()).thenReturn("path");
	}

	@Test
	void isOpenTest() throws IOException
	{
		var readableByteChannel = new BucketReadableByteChannel(awsFacade, path);
		try (var channel = readableByteChannel)
		{
			Assertions.assertTrue(channel.isOpen());
		}
		Assertions.assertFalse(readableByteChannel.isOpen());
	}

	@Test
	void positionWhileChannelIsClosedTest() throws IOException
	{
		var readableByteChannel = new BucketReadableByteChannel(awsFacade, path);
		try (var channel = readableByteChannel)
		{
			// Nothing to do
		}
		Assertions.assertThrows(ClosedChannelException.class, readableByteChannel::position);
		Assertions.assertThrows(ClosedChannelException.class,
								() -> readableByteChannel.position(10));
	}

	@Test
	void negativePositionTest() throws IOException
	{
		try (var channel = new BucketReadableByteChannel(awsFacade, path))
		{
			Assertions.assertThrows(IllegalArgumentException.class, () -> channel.position(-1));
		}
	}
}
