package io.github.mirkoscotti.nio.s3.extensions.jdk.jsr203;

import java.io.IOException;
import java.nio.ByteBuffer;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import io.github.mirkoscotti.nio.s3.exceptions.TransportException;
import io.github.mirkoscotti.nio.s3.helpers.JunitHelper;
import io.github.mirkoscotti.nio.s3.operations.AwsFacade;
import io.github.mirkoscotti.nio.s3.operations.MultipartWriter;

/**
 * @author mirko.scotti
 * @version Nov 29, 2025
 */
@ExtendWith(MockitoExtension.class)
class BucketWritableByteChannelTest
{

	private static final int MULTIPART_THRESHOLD = 10 * 1024 * 1024;

	@Mock
	private AwsFacade awsFacade;

	@Mock
	private BucketFileSystem fileSystem;

	@Mock
	private BucketPath path;

	@BeforeEach
	void beforeEach()
	{
		Mockito.when(path.getFileSystem()).thenReturn(fileSystem);
		Mockito.when(fileSystem.bucketName()).thenReturn("test-bucket");
	}

	@Test
	void isOpenTest()
	{
		var writableByteChannel = JunitHelper.tryCall(() -> new BucketWritableByteChannel(awsFacade,
																						  path));
		try (var channel = writableByteChannel)
		{
			Assertions.assertTrue(channel.isOpen());
		}
		catch (IOException x)
		{
			Assertions.fail(x);
		}
		Assertions.assertFalse(writableByteChannel.isOpen());
	}

	@Test
	void ioExceptionWhileWritingTest(@Mock MultipartWriter writer)
	{
		var buffer = ByteBuffer.wrap(new byte[MULTIPART_THRESHOLD + 1]);
		Mockito.when(awsFacade.startMultipartUpload(Mockito.anyString(), Mockito.anyString()))
			   .thenReturn(writer);
		Mockito.doThrow(TransportException.class).doNothing().when(writer).write(Mockito.any());
		try (var channel = JunitHelper.tryCall(() -> new BucketWritableByteChannel(awsFacade,
																				   path)))
		{
			Assertions.assertThrows(TransportException.class, () -> channel.write(buffer));
		}
		catch (IOException x)
		{
			Assertions.fail(x);
		}
	}

	@Test
	void writeThresholdBufferTest()
	{
		var buffer = ByteBuffer.wrap(new byte[MULTIPART_THRESHOLD]);
		try (var channel = JunitHelper.tryCall(() -> new BucketWritableByteChannel(awsFacade,
																				   path)))
		{
			Assertions.assertEquals(MULTIPART_THRESHOLD, channel.write(buffer));
		}
		catch (IOException x)
		{
			Assertions.fail(x);
		}
	}
}
