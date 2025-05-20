package it.mirkoscotti.nio.s3.extensions.jdk.jsr203;

import it.mirkoscotti.nio.s3.operations.S3Connector;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;

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
		int result;
		if (position >= size)
		{
			result = -1;
		}
		else
		{
			var bytesToRead = Math.min(dst.remaining(), (int) (size - position));
			if (bytesToRead <= 0)
			{
				result = 0;
			}
			else
			{
				var to = position + bytesToRead - 1;
				var data = connector.readObject(bucket, key, position, to);
				dst.put(data);
				result = data.length;
				position += result;
			}
		}
		return result;
	}
}
