package it.mirkoscotti.nio.s3.extensions.jdk.jsr203;

import it.mirkoscotti.nio.s3.enums.ObjectFlag;
import it.mirkoscotti.nio.s3.functions.Try;
import it.mirkoscotti.nio.s3.operations.S3Connector;

import java.io.Closeable;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.Channel;
import java.nio.channels.NonReadableChannelException;
import java.nio.channels.NonWritableChannelException;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SeekableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.file.InvalidPathException;
import java.nio.file.OpenOption;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * @author mirko.scotti
 * @version May 16, 2025
 */
class BucketSeekableByteChannel
	implements SeekableByteChannel
{

	private final S3Connector connector;

	private final BucketPath path;

	private final Optional<ReadableByteChannel> readableByteChannel;

	private final Optional<WritableByteChannel> writableByteChannel;

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
		if (path instanceof BucketPath bucketPath)
		{
			var options = ObjectFlag.readWriteCheck(openOptions);
			readableByteChannel = ObjectFlag.IS_READABLE.matches(options)
				? Optional.of(new BucketReadableByteChannel(connector, path))
				: Optional.empty();
			if (ObjectFlag.IS_WRITABLE.matches(options))
			{
				ObjectFlag.appendTruncateCheck(options);
				ObjectFlag.truncateCheck(path, options);
				ObjectFlag.createCheck(path, options);
				writableByteChannel = Optional.of(new BucketWritableByteChannel(connector, path));
			}
			else
			{
				writableByteChannel = Optional.empty();
			}
			this.path = bucketPath;
		}
		else
		{
			var bucketPath = Objects.requireNonNull(path, () -> "Missing path.").toString();
			throw new InvalidPathException(bucketPath,
										   "Expected a path in an AWS bucket, found: %s".formatted(bucketPath));
		}
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
		readableByteChannel.ifPresent(item -> Try.to(() -> internalClose(item)).run());
		writableByteChannel.ifPresent(item -> Try.to(() -> internalClose(item)).run());
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
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public SeekableByteChannel position(long newPosition) throws IOException
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public long size() throws IOException
	{
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public SeekableByteChannel truncate(long size) throws IOException
	{
		// TODO Auto-generated method stub
		return null;
	}

	private Void internalClose(Closeable closeable) throws IOException
	{
		closeable.close();
		return null;
	}
}
