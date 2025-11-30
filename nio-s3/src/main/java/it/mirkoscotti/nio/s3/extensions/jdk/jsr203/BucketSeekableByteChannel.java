package it.mirkoscotti.nio.s3.extensions.jdk.jsr203;

import java.io.Closeable;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.Channel;
import java.nio.channels.NonReadableChannelException;
import java.nio.channels.NonWritableChannelException;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.NoSuchFileException;
import java.nio.file.OpenOption;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import it.mirkoscotti.nio.s3.enums.ObjectFlag;
import it.mirkoscotti.nio.s3.functions.Try;
import it.mirkoscotti.nio.s3.operations.S3Connector;

/**
 * @author mirko.scotti
 * @version May 16, 2025
 */
class BucketSeekableByteChannel
	implements SeekableByteChannel
{

	private final S3Connector connector;

	private final BucketPath path;

	private final Optional<BucketReadableByteChannel> readableByteChannel;

	private final Optional<BucketWritableByteChannel> writableByteChannel;

	/**
	 * @param connector
	 * @param path
	 */
	BucketSeekableByteChannel(S3Connector connector,
							  BucketPath path,
							  Set<? extends OpenOption> openOptions)
		throws IOException
	{
		this.connector = Objects.requireNonNull(connector, () -> "Missing connector.");
		Objects.requireNonNull(path, () -> "Missing path.");
		this.path = path;
		ObjectFlag.readWriteCheck(openOptions);
		readableByteChannel = createReadableByteChannel(openOptions);
		writableByteChannel = createWritableByteChannel(openOptions);
	}

	@Override
	public boolean isOpen()
	{
		return readableByteChannel.filter(Channel::isOpen).isPresent()
			|| writableByteChannel.filter(Channel::isOpen).isPresent();
	}

	@Override
	public void close() throws IOException
	{
		var readable = readableByteChannel.map(this::tryClose);
		var writable = writableByteChannel.map(this::tryClose);
		var exception = readable.orElseGet(() -> writable.orElse(null));
		if (exception != null)
		{
			switch (exception)
			{
				case IOException x -> throw x;
				case RuntimeException x -> throw x;
				default -> throw new IllegalStateException(exception);
			}
		}
	}

	@Override
	public int read(ByteBuffer dst) throws IOException
	{
		return readableByteChannel.orElseThrow(NonReadableChannelException::new).read(dst);
	}

	@Override
	public int write(ByteBuffer src) throws IOException
	{
		return writableByteChannel.orElseThrow(NonWritableChannelException::new).write(src);
	}

	@Override
	public long position() throws IOException
	{
		return readableByteChannel.orElseThrow(() -> new UnsupportedOperationException("Position is available only when channel is open for read."))
								  .position();
	}

	@Override
	public SeekableByteChannel position(long newPosition) throws IOException
	{
		readableByteChannel.orElseThrow(() -> new UnsupportedOperationException("Position can be set only when channel is open for read."))
						   .position(newPosition);
		return this;
	}

	@Override
	public long size() throws IOException
	{
		return readableByteChannel.orElseThrow(() -> new UnsupportedOperationException("Size is available only when channel is open for read."))
								  .size();
	}

	@Override
	public SeekableByteChannel truncate(long size) throws IOException
	{
		writableByteChannel.orElseThrow(NonWritableChannelException::new).truncate(size);
		return this;
	}

	private Optional<BucketReadableByteChannel> createReadableByteChannel(Set<? extends OpenOption> options)
	{
		return ObjectFlag.IS_READABLE.matches(options)
			? Optional.of(new BucketReadableByteChannel(connector, path))
			: Optional.empty();
	}

	private Optional<BucketWritableByteChannel> createWritableByteChannel(Set<? extends OpenOption> options)
		throws IOException
	{
		return ObjectFlag.IS_WRITABLE.matches(options)
			? Optional.of(createSafeWritableByteChannel(options))
			: Optional.empty();
	}

	private BucketWritableByteChannel createSafeWritableByteChannel(Set<? extends OpenOption> options)
		throws IOException
	{
		BucketWritableByteChannel result;
		var objectFlag = ObjectFlag.appendTruncateCheck(options);
		try
		{
			var fileAttributes = Files.readAttributes(path,
													  ObjectBasicFileAttributes.class,
													  LinkOption.NOFOLLOW_LINKS);
			ObjectFlag.creationWhenFileExistingCheck(options, path);
			var bucketName = path.getFileSystem().bucketName();
			result = new BucketWritableByteChannel(connector,
												   bucketName,
												   fileAttributes,
												   objectFlag);
		}
		catch (NoSuchFileException x)
		{
			ObjectFlag.creationWhenFileNotFoundCheck(options, path);
			result = new BucketWritableByteChannel(connector, path);
		}
		return result;
	}

	private Exception tryClose(Closeable closeable)
	{
		var result = new AtomicReference<Exception>();
		Try.to(() -> close(closeable)).onCatch(result::set).run();
		return result.get();
	}

	private Void close(Closeable closeable) throws IOException
	{
		closeable.close();
		return null;
	}
}
