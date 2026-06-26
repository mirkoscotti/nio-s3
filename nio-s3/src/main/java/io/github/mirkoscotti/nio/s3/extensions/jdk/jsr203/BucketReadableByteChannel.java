package io.github.mirkoscotti.nio.s3.extensions.jdk.jsr203;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.ReadableByteChannel;
import java.util.Optional;

import io.github.mirkoscotti.nio.s3.operations.AwsFacade;

/**
 * A <code>ReadableByteChannel</code> that reads the content of an object stored in an S3-compatible
 * bucket, supporting sequential and repositioned reads over the object's byte range.
 *
 * <p>
 * An instance is bound at construction time to a specific bucket and key, derived from the supplied
 * {@link BucketPath}. The object size is resolved at construction time and remains fixed for the
 * lifetime of the channel.
 *
 * @see BucketPath
 * @see BucketSeekableByteChannel
 */
class BucketReadableByteChannel
	implements ReadableByteChannel
{

	private final AwsFacade awsFacade;

	private final String bucket;

	private final String key;

	private final long size;

	private boolean isOpen = true;

	private long position = 0;

	BucketReadableByteChannel(AwsFacade awsFacade, BucketPath path)
	{
		this.awsFacade = awsFacade;
		bucket = path.getFileSystem().getFileStores().iterator().next().name();
		key = path.toString();
		size = awsFacade.objectMetadata(bucket, key).size();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean isOpen()
	{
		return isOpen;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void close() throws IOException
	{
		isOpen = false;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int read(ByteBuffer dst) throws IOException
	{
		return Optional.of(this)
					   .filter(item -> item.isOpen)
					   .map(item -> item.position < item.size ? readRemaining(dst) : -1)
					   .orElseThrow(ClosedChannelException::new);
	}

	long position() throws ClosedChannelException
	{
		return Optional.of(this)
					   .filter(item -> item.isOpen)
					   .map(item -> item.position)
					   .orElseThrow(ClosedChannelException::new);
	}

	long size() throws ClosedChannelException
	{
		return Optional.of(this)
					   .filter(item -> item.isOpen)
					   .map(item -> item.size)
					   .orElseThrow(ClosedChannelException::new);
	}

	Void position(long position) throws ClosedChannelException
	{
		if (!isOpen)
		{
			throw new ClosedChannelException();
		}
		if (position < 0)
		{
			throw new IllegalArgumentException("Position must not be negative.");
		}
		this.position = position;
		return null;
	}

	private int readRemaining(ByteBuffer buffer)
	{
		var remaining = Math.min(buffer.remaining(), (int) (size - position));
		return remaining > 0 ? readRemaining(buffer, remaining) : 0;
	}

	private int readRemaining(ByteBuffer buffer, int remaining)
	{
		var to = position + remaining - 1;
		var data = awsFacade.readObject(bucket, key, position, to);
		buffer.put(data);
		var result = data.length;
		position += result;
		return result;
	}
}
