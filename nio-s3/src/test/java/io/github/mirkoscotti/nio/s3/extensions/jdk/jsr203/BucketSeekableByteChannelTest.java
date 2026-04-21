package io.github.mirkoscotti.nio.s3.extensions.jdk.jsr203;

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
	{
		Mockito.when(path.getFileSystem()).thenReturn(fileSystem);
		Mockito.when(fileSystem.awsFacade()).thenReturn(awsFacade);
		try (var mock = Mockito.mockConstruction(BucketReadableByteChannel.class,
												 this::readableThrowsIoException))
		{
			var channel = new BucketSeekableByteChannel(path, Set.of());
			Assertions.assertThrows(IOException.class, channel::close);
		}
		catch (IOException x)
		{
			Assertions.fail(x);
		}
	}

	@Test
	void runtimeExceptionWhileClosingTest(@Mock BucketWritableByteChannel writableChannel)
	{
		Mockito.when(path.getFileSystem()).thenReturn(fileSystem);
		Mockito.when(fileSystem.provider()).thenReturn(fileSystemProvider);
		try (var mock = Mockito.mockConstruction(BucketWritableByteChannel.class,
												 this::initializeWritable))
		{
			var channel = new BucketSeekableByteChannel(path, Set.of(StandardOpenOption.WRITE));
			Assertions.assertThrows(RuntimeException.class, channel::close);
		}
		catch (IOException x)
		{
			Assertions.fail(x);
		}
	}

	@Test
	void exceptionWhileClosingTest(@Mock BucketReadableByteChannel readableChannel,
								   @Mock BucketFileSystem fileSystem,
								   @Mock AwsFacade awsFacade)
	{
		Mockito.when(path.getFileSystem()).thenReturn(fileSystem);
		Mockito.when(fileSystem.awsFacade()).thenReturn(awsFacade);
		try (var mock = Mockito.mockConstruction(BucketReadableByteChannel.class,
												 this::readableThrowsException))
		{
			var channel = new BucketSeekableByteChannel(path, Set.of());
			var exception = Assertions.assertThrows(IllegalStateException.class, channel::close);
			Assertions.assertInstanceOf(Exception.class, exception.getCause());
		}
		catch (IOException x)
		{
			Assertions.fail(x);
		}
	}

	@Test
	void unsupportedPositionTest(@Mock BucketWritableByteChannel writableChannel)
	{
		Mockito.when(path.getFileSystem()).thenReturn(fileSystem);
		Mockito.when(fileSystem.provider()).thenReturn(fileSystemProvider);
		try (var mock = Mockito.mockConstruction(BucketWritableByteChannel.class);
			 var channel = new BucketSeekableByteChannel(path, Set.of(StandardOpenOption.WRITE)))
		{
			Assertions.assertThrows(UnsupportedOperationException.class, channel::position);
		}
		catch (IOException x)
		{
			Assertions.fail(x);
		}
	}

	@Test
	void unsupportedPositionSettingTest(@Mock BucketWritableByteChannel writableChannel)
	{
		Mockito.when(path.getFileSystem()).thenReturn(fileSystem);
		Mockito.when(fileSystem.provider()).thenReturn(fileSystemProvider);
		try (var mock = Mockito.mockConstruction(BucketWritableByteChannel.class);
			 var channel = new BucketSeekableByteChannel(path, Set.of(StandardOpenOption.WRITE)))
		{
			Assertions.assertThrows(UnsupportedOperationException.class,
									() -> channel.position(10));
		}
		catch (IOException x)
		{
			Assertions.fail(x);
		}
	}

	@Test
	void unsupportedSizeTest(@Mock BucketWritableByteChannel writableChannel)
	{
		Mockito.when(path.getFileSystem()).thenReturn(fileSystem);
		Mockito.when(fileSystem.provider()).thenReturn(fileSystemProvider);
		try (var mock = Mockito.mockConstruction(BucketWritableByteChannel.class);
			 var channel = new BucketSeekableByteChannel(path, Set.of(StandardOpenOption.WRITE)))
		{
			Assertions.assertThrows(UnsupportedOperationException.class, channel::size);
		}
		catch (IOException x)
		{
			Assertions.fail(x);
		}
	}

	@Test
	void truncateTest(@Mock BucketReadableByteChannel readableChannel,
					  @Mock BucketFileSystem fileSystem,
					  @Mock AwsFacade awsFacade)
	{
		Mockito.when(path.getFileSystem()).thenReturn(fileSystem);
		Mockito.when(fileSystem.awsFacade()).thenReturn(awsFacade);
		try (var mock = Mockito.mockConstruction(BucketReadableByteChannel.class);
			 var channel = new BucketSeekableByteChannel(path, Set.of()))
		{
			Assertions.assertThrows(UnsupportedOperationException.class,
									() -> channel.truncate(10));
		}
		catch (IOException x)
		{
			Assertions.fail(x);
		}
	}

	private void readableThrowsIoException(BucketReadableByteChannel mock, Context context)
		throws IOException
	{
		Mockito.doThrow(IOException.class).when(mock).close();
	}

	private void readableThrowsException(BucketReadableByteChannel mock, Context context)
		throws IOException
	{
		Mockito.doThrow(Exception.class).when(mock).close();
	}

	private void initializeWritable(BucketWritableByteChannel mock, Context context)
		throws IOException
	{
		Mockito.doThrow(RuntimeException.class).when(mock).close();
	}
}
