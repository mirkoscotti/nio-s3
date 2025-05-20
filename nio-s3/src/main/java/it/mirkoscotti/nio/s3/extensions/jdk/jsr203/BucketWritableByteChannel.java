package it.mirkoscotti.nio.s3.extensions.jdk.jsr203;

import it.mirkoscotti.nio.s3.operations.S3Connector;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.WritableByteChannel;

/**
 * @author mirko.scotti
 * @version May 20, 2025
 */
class BucketWritableByteChannel
	implements WritableByteChannel
{

	private final S3Connector connector;

	private final String bucket;

	private final String key;

	BucketWritableByteChannel(S3Connector connector, BucketPath path)
	{
		this.connector = connector;
		bucket = path.getFileSystem().getFileStores().iterator().next().name();
		key = path.toString();
	}

	@Override
	public boolean isOpen()
	{
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void close() throws IOException
	{
		// TODO Auto-generated method stub

	}

	@Override
	public int write(ByteBuffer src) throws IOException
	{
		// TODO Auto-generated method stub
		return 0;
	}
}
