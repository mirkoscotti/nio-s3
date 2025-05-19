package it.mirkoscotti.nio.s3.extensions.jdk.jsr203;

import it.mirkoscotti.nio.s3.enums.ObjectFlag;
import it.mirkoscotti.nio.s3.operations.S3Connector;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.InvalidPathException;
import java.nio.file.OpenOption;
import java.util.Objects;
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

	/**
	 * @param connector
	 * @param path
	 */
	BucketSeekableByteChannel(S3Connector connector,
							  BucketPath path,
							  Set<? extends OpenOption> options)
		throws IOException
	{
		this.connector = Objects.requireNonNull(connector, () -> "Missing connector.");
		if (path instanceof BucketPath bucketPath)
		{
			ObjectFlag.readWriteCheck(options);
			if (ObjectFlag.IS_READABLE.matches(options))
			{
				// TODO: implement the checks for reading, if necessary, otherwise remove if block
			}
			if (ObjectFlag.IS_WRITABLE.matches(options))
			{
				ObjectFlag.appendTruncateCheck(options);
				ObjectFlag.truncateCheck(path, options);
				ObjectFlag.createCheck(path, options);
			}
			this.path = bucketPath;
		}
		var bucketPath = Objects.requireNonNull(path, () -> "Missing path.").toString();
		throw new InvalidPathException(bucketPath,
									   "Expected a path in an AWS bucket, found: %s".formatted(bucketPath));
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
	public int read(ByteBuffer dst) throws IOException
	{
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int write(ByteBuffer src) throws IOException
	{
		// TODO Auto-generated method stub
		return 0;
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
}
