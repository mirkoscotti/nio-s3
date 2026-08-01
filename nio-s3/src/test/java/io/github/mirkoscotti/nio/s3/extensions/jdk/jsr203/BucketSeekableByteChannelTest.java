package io.github.mirkoscotti.nio.s3.extensions.jdk.jsr203;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Set;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedConstruction.Context;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import io.github.mirkoscotti.nio.s3.operations.AwsFacade;

/**
 * @author mirko.scotti
 * @version Nov 30, 2025
 */
@ExtendWith(MockitoExtension.class)
class BucketSeekableByteChannelTest
{

	@Mock
	private AwsFacade awsFacade;

	@Mock
	private BucketPath path;

	@Mock
	private BucketFileSystem fileSystem;

	@Mock
	private S3FileSystemProvider fileSystemProvider;

	@Test
	void invalidInstantiationsTest(@Mock Path invalidPath)
	{
		Assertions.assertThrows(NullPointerException.class,
								() -> new BucketSeekableByteChannel(null, null));
		Assertions.assertThrows(NullPointerException.class,
								() -> new BucketSeekableByteChannel(path, null));
	}

	@Test
	void ioExceptionWhileClosingTest(@Mock BucketReadableByteChannel readableChannel,
									 @Mock BucketFileSystem fileSystem,
									 @Mock AwsFacade awsFacade)
		throws IOException
	{
		when(path.getFileSystem()).thenReturn(fileSystem);
		when(fileSystem.awsFacade()).thenReturn(awsFacade);
		try (var mock = Mockito.mockConstruction(BucketReadableByteChannel.class,
												 this::readableThrowsIoException))
		{
			var channel = new BucketSeekableByteChannel(path, Set.of());
			Assertions.assertThrows(IOException.class, channel::close);
		}
	}

	@Test
	void runtimeExceptionWhileClosingTest(@Mock BucketWritableByteChannel writableChannel)
		throws IOException
	{
		when(path.getFileSystem()).thenReturn(fileSystem);
		when(fileSystem.provider()).thenReturn(fileSystemProvider);
		try (var mock = Mockito.mockConstruction(BucketWritableByteChannel.class,
												 this::initializeWritable))
		{
			var channel = new BucketSeekableByteChannel(path, Set.of(StandardOpenOption.WRITE));
			Assertions.assertThrows(RuntimeException.class, channel::close);
		}
	}

	@Test
	void exceptionWhileClosingTest(@Mock BucketReadableByteChannel readableChannel,
								   @Mock BucketFileSystem fileSystem,
								   @Mock AwsFacade awsFacade)
		throws IOException
	{
		when(path.getFileSystem()).thenReturn(fileSystem);
		when(fileSystem.awsFacade()).thenReturn(awsFacade);
		try (var mock = Mockito.mockConstruction(BucketReadableByteChannel.class,
												 this::readableThrowsException))
		{
			var channel = new BucketSeekableByteChannel(path, Set.of());
			var exception = Assertions.assertThrows(IllegalStateException.class, channel::close);
			Assertions.assertInstanceOf(Exception.class, exception.getCause());
		}
	}

	@Test
	void unsupportedPositionTest(@Mock BucketWritableByteChannel writableChannel) throws IOException
	{
		when(path.getFileSystem()).thenReturn(fileSystem);
		when(fileSystem.provider()).thenReturn(fileSystemProvider);
		try (var mock = Mockito.mockConstruction(BucketWritableByteChannel.class);
			 var channel = new BucketSeekableByteChannel(path, Set.of(StandardOpenOption.WRITE)))
		{
			Assertions.assertThrows(UnsupportedOperationException.class, channel::position);
		}
	}

	@Test
	void unsupportedPositionSettingTest(@Mock BucketWritableByteChannel writableChannel)
		throws IOException
	{
		when(path.getFileSystem()).thenReturn(fileSystem);
		when(fileSystem.provider()).thenReturn(fileSystemProvider);
		try (var mock = Mockito.mockConstruction(BucketWritableByteChannel.class);
			 var channel = new BucketSeekableByteChannel(path, Set.of(StandardOpenOption.WRITE)))
		{
			Assertions.assertThrows(UnsupportedOperationException.class,
									() -> channel.position(10));
		}
	}

	@Test
	void unsupportedSizeTest(@Mock BucketWritableByteChannel writableChannel) throws IOException
	{
		when(path.getFileSystem()).thenReturn(fileSystem);
		when(fileSystem.provider()).thenReturn(fileSystemProvider);
		try (var mock = Mockito.mockConstruction(BucketWritableByteChannel.class);
			 var channel = new BucketSeekableByteChannel(path, Set.of(StandardOpenOption.WRITE)))
		{
			Assertions.assertThrows(UnsupportedOperationException.class, channel::size);
		}
	}

	@Test
	void truncateTest(@Mock BucketReadableByteChannel readableChannel,
					  @Mock BucketFileSystem fileSystem,
					  @Mock AwsFacade awsFacade)
		throws IOException
	{
		when(path.getFileSystem()).thenReturn(fileSystem);
		when(fileSystem.awsFacade()).thenReturn(awsFacade);
		try (var mock = Mockito.mockConstruction(BucketReadableByteChannel.class);
			 var channel = new BucketSeekableByteChannel(path, Set.of()))
		{
			Assertions.assertThrows(UnsupportedOperationException.class,
									() -> channel.truncate(10));
		}
	}

	private void readableThrowsIoException(BucketReadableByteChannel mock, Context context)
		throws IOException
	{
		doThrow(IOException.class).when(mock).close();
	}

	private void readableThrowsException(BucketReadableByteChannel mock, Context context)
		throws IOException
	{
		doThrow(Exception.class).when(mock).close();
	}

	private void initializeWritable(BucketWritableByteChannel mock, Context context)
		throws IOException
	{
		doThrow(RuntimeException.class).when(mock).close();
	}
}
