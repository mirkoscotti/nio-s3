package it.mirkoscotti.nio.s3.extensions.jdk.jsr203;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.ReadableByteChannel;
import java.util.Optional;

import it.mirkoscotti.nio.s3.operations.S3Connector;

/**
 * @author mirko.scotti
 * @version May 20, 2025
 */
class BucketReadableByteChannel
	implements ReadableByteChannel
{

	private final S3Connector connector;

	private final String bucket;

	private final String key;

	private boolean isOpen = true;

	private long position = 0;

	private long size;

	BucketReadableByteChannel(S3Connector connector, BucketPath path)
	{
		this.connector = connector;
		bucket = path.getFileSystem().getFileStores().iterator().next().name();
		key = path.toString();
		size = connector.objectMetadata(bucket, key).size();
	}

	@Override
	public boolean isOpen()
	{
		return isOpen;
	}

	@Override
	public void close() throws IOException
	{
		isOpen = false;
	}

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
					   .map(item -> item.connector.objectMetadata(item.bucket, item.key))
					   .map(item -> item.size())
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
		var data = connector.readObject(bucket, key, position, to);
		buffer.put(data);
		var result = data.length;
		position += result;
		return result;
	}
}
