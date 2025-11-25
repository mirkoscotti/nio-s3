package it.mirkoscotti.nio.s3.extensions.jdk.jsr203;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.WritableByteChannel;
import java.util.Optional;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

import it.mirkoscotti.nio.s3.enums.ObjectFlag;
import it.mirkoscotti.nio.s3.operations.MultipartWriter;
import it.mirkoscotti.nio.s3.operations.S3Connector;

/**
 * @author mirko.scotti
 * @version May 20, 2025
 */
class BucketWritableByteChannel
	implements WritableByteChannel
{

	private static final int MULTIPART_THRESHOLD = 10 * 1024 * 1024;

	private final ByteBuffer buffer = ByteBuffer.allocate(MULTIPART_THRESHOLD + 1);

	private Optional<MultipartWriter> multipartWriter = Optional.empty();

	private boolean isOpen = true;

	private final S3Connector connector;

	private final String bucket;

	private final String key;

	private final long oldFileSize;

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
		this(connector, path.getFileSystem().bucketName(), path.toString(), 0, false);
	}

	BucketWritableByteChannel(S3Connector connector,
							  String bucket,
							  ObjectBasicFileAttributes attributes,
							  Optional<ObjectFlag> objectFlag)
	{
		this(connector,
			 bucket,
			 attributes.fileKey(),
			 objectFlag.filter(ObjectFlag.IS_TRUNCATABLE::equals).isEmpty() ? attributes.size() : 0,
			 objectFlag.filter(ObjectFlag.IS_APPENDABLE::equals).isPresent());
	}

	private BucketWritableByteChannel(S3Connector connector,
									  String bucket,
									  String key,
									  long oldFileSize,
									  boolean isAppendable)
	{
		this.connector = connector;
		this.bucket = bucket;
		this.key = key;
		this.oldFileSize = oldFileSize;
		if (isAppendable)
		{
			Optional.of(oldFileSize)
					.filter(item -> item > MULTIPART_THRESHOLD)
					.ifPresentOrElse(item -> startMultipartUploadAndCopy(oldFileSize),
									 () -> buffer.put(connector.readObject(bucket, key)));
		}
	}

	@Override
	public boolean isOpen()
	{
		return isOpen;
	}

	@Override
	public void close() throws IOException
	{

		var newFileSize = multipartWriter.map(item -> item.bytesWritten() + buffer.position())
										 .orElseGet(() -> Long.valueOf(buffer.position()));
		Optional.ofNullable(newFileSize)
				.filter(item -> item < oldFileSize)
				.ifPresentOrElse(this::partialOverwrite, this::flushBuffer);
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

	void truncate(long size) throws ClosedChannelException
	{
		if (!isOpen)
		{
			throw new ClosedChannelException();
		}
		if (size < 0)
		{
			throw new IllegalArgumentException("Size must not be negative.");
		}
		// TODO: manage truncation for single-part files
		var message = "Truncation not supported yet. Requested to truncate at % bytes.".formatted(size);
		throw new UnsupportedOperationException(message);
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

	private void partialOverwrite(long newFileSize)
	{
		if (oldFileSize > MULTIPART_THRESHOLD)
		{
			multipartOverwrite(newFileSize);
		}
		else
		{
			singlepartOverwrite(newFileSize);
		}
	}

	private void singlepartOverwrite(long newFileSize)
	{
		var array = connector.readObject(bucket, key, newFileSize, oldFileSize - 1);
		buffer.put(array);
		flushBuffer();
	}

	private void multipartOverwrite(long newFileSize)
	{
		var remaining = buffer.remaining();
		var startLastPart = newFileSize + remaining;
		var array = connector.readObject(bucket, key, newFileSize, startLastPart - 1);
		buffer.put(array);
		flushBuffer();
		multipartWriter.ifPresent(item -> copyAndClose(item, startLastPart - 1, oldFileSize - 1));
	}

	private void flushBuffer()
	{
		buffer.flip();
		var array = new byte[Math.min(buffer.remaining(), MULTIPART_THRESHOLD)];
		buffer.get(array);
		if (buffer.hasRemaining())
		{
			startMultipartUpload();
			multipartWriter.ifPresent(item -> item.write(array));
		}
		else
		{
			multipartWriter.ifPresentOrElse(item -> writeAndClose(item, array),
											() -> connector.writeObject(bucket, key, array));
		}
		buffer.compact();
	}

	private void startMultipartUpload()
	{
		multipartWriter = multipartWriter.or(() -> Optional.of(connector.startMultipartUpload(bucket,
																							  key)));
	}

	private void startMultipartUploadAndCopy(long size)
	{
		startMultipartUpload();
		multipartWriter.ifPresent(item -> item.copy(size));
	}

	private void writeAndClose(MultipartWriter multipartWriter, byte[] buffer)
	{
		try (var writer = multipartWriter)
		{
			writer.write(buffer);
		}
		this.multipartWriter = Optional.empty();
	}

	private void copyAndClose(MultipartWriter multipartWriter, long from, long to)
	{
		try (var writer = multipartWriter)
		{
			writer.copy(from, to);
		}
		this.multipartWriter = Optional.empty();
	}
}
