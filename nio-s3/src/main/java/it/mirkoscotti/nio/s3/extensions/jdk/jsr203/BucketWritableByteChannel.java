package it.mirkoscotti.nio.s3.extensions.jdk.jsr203;

import it.mirkoscotti.nio.s3.operations.MultipartWriter;
import it.mirkoscotti.nio.s3.operations.S3Connector;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.WritableByteChannel;
import java.util.Optional;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

/**
 * @author mirko.scotti
 * @version May 20, 2025
 */
class BucketWritableByteChannel
	implements WritableByteChannel
{

	private static final int MULTIPART_THRESHOLD = 10 * 1024 * 1024;

	private final ByteBuffer buffer = ByteBuffer.allocate(MULTIPART_THRESHOLD + 1);

	private final S3Connector connector;

	private final String bucket;

	private final String key;

	private boolean isOpen = true;

	private Optional<MultipartWriter> multipartWriter = Optional.empty();

	BucketWritableByteChannel(S3Connector connector, BucketPath path)
	{
		this.connector = connector;
		bucket = path.getFileSystem().getFileStores().iterator().next().name();
		key = path.toString();
	}

	@Override
	public boolean isOpen()
	{
		return isOpen;
	}

	@Override
	public void close() throws IOException
	{
		flushBuffer();
		isOpen = false;
	}

	@Override
	public int write(ByteBuffer src) throws IOException
	{
		return Optional.of(this)
					   .filter(item -> item.isOpen)
					   .map(item -> item.writeBuffer(src))
					   .orElseThrow(ClosedChannelException::new);
	}

	private int writeBuffer(ByteBuffer input)
	{
		var position = input.position();
		Stream.iterate(input, UnaryOperator.identity())
			  .takeWhile(ByteBuffer::hasRemaining)
			  .forEach(this::writeRemaining);
		return input.position() - position;
	}

	private void writeRemaining(ByteBuffer input)
	{
		Optional.of(Math.min(input.remaining(), buffer.remaining()))
				.filter(item -> item > 0)
				.ifPresent(item -> writeRemaining(input, item));
		if (!buffer.hasRemaining())
		{
			flushBuffer();
		}
	}

	private void writeRemaining(ByteBuffer input, int remaining)
	{
		var chunk = new byte[remaining];
		input.get(chunk);
		buffer.put(chunk);
	}

	private void flushBuffer()
	{
		buffer.flip();
		var array = new byte[Math.min(buffer.remaining(), MULTIPART_THRESHOLD)];
		buffer.get(array);
		if (buffer.hasRemaining())
		{
			multipartWriter = multipartWriter.or(() -> Optional.of(connector.startMultipartUpload(bucket,
																								  key)));
			multipartWriter.ifPresent(item -> item.write(array));
		}
		else
		{
			multipartWriter.ifPresentOrElse(item -> writeAndClose(item, array),
											() -> connector.writeObject(bucket, key, array));
		}
		buffer.compact();
	}

	private void writeAndClose(MultipartWriter multipartWriter, byte[] buffer)
	{
		try (var writer = multipartWriter)
		{
			writer.write(buffer);
		}
		this.multipartWriter = Optional.empty();
	}
}
