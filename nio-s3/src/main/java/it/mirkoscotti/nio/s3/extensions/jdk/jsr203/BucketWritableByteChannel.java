package it.mirkoscotti.nio.s3.extensions.jdk.jsr203;

import it.mirkoscotti.nio.s3.operations.MultipartWriter;
import it.mirkoscotti.nio.s3.operations.S3Connector;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.WritableByteChannel;
import java.nio.file.attribute.BasicFileAttributes;
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

	private final Optional<BasicFileAttributes> metadata;

	private Optional<MultipartWriter> multipartWriter = Optional.empty();

	private boolean isOpen = true;

	private final S3Connector connector;

	private final String bucket;

	private final String key;

	/*
	 * OpenOption behaviors:
	 *
	 * - CREATE NOT EXISTING FILE => same as CREATE_NEW
	 *
	 * - CREATE EXISTING FILE AND WRITE LESS BYTES THAN THE ORIGINAL CONTENT => the file will
	 * contain the bytes written + the bytes of the original file that exceed the bytes written.
	 * Example: if the existing file contains "text123" and I write "abc", at the end of the session
	 * the file will contain "abct123". "
	 *
	 * - CREATE EXISTING FILE + TRUNCATE EXISTING => the file will contain only the bytes written.
	 * Example: if the existing file contains "text123" and I write "abcd", at the end of the
	 * session the file will contain "abcd"
	 */
	BucketWritableByteChannel(S3Connector connector, BucketPath path)
	{
		this.connector = connector;
		metadata = Optional.empty();
		bucket = path.getFileSystem().bucketName();
		key = path.toString();
	}

	BucketWritableByteChannel(S3Connector connector,
							  String bucket,
							  ObjectBasicFileAttributes attributes)
	{
		this.connector = connector;
		metadata = Optional.of(attributes);
		this.bucket = bucket;
		key = attributes.fileKey();
		metadata.map(BasicFileAttributes::size);
	}

	@Override
	public boolean isOpen()
	{
		return isOpen;
	}

	@Override
	public void close() throws IOException
	{
		/*
		 * TODO: implement these use cases:
		 *
		 * - FILE NOT EXISTING ON S3 => Do nothing and invoke directly flushBuffer
		 *
		 * - FILE EXISTING ON S3 AND MULTIPARTWRITER EMPTY => 1. GetObject from offset = number of
		 * bytes in buffer and length = remaining bytes to fill the buffer and invoke write 2. If I
		 * haven't finished to read the existing file
		 */
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
