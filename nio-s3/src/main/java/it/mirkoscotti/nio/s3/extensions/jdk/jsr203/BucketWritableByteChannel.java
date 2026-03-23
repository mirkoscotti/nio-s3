package it.mirkoscotti.nio.s3.extensions.jdk.jsr203;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.WritableByteChannel;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

import it.mirkoscotti.nio.s3.enums.ObjectFlag;
import it.mirkoscotti.nio.s3.functions.Case;
import it.mirkoscotti.nio.s3.functions.Evaluator;
import it.mirkoscotti.nio.s3.functions.Expression;
import it.mirkoscotti.nio.s3.functions.Transformer;
import it.mirkoscotti.nio.s3.operations.AwsFacade;
import it.mirkoscotti.nio.s3.operations.MultipartWriter;

/**
 * @author mirko.scotti
 * @version May 20, 2025
 */
class BucketWritableByteChannel
	implements WritableByteChannel
{

	private static final int MULTIPART_THRESHOLD = 10 * 1024 * 1024;

	private final ByteBuffer buffer = ByteBuffer.allocate(MULTIPART_THRESHOLD + 1);

	private boolean isOpen = true;

	private MultipartWriter multipartWriter;

	private final AwsFacade awsFacade;

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
	BucketWritableByteChannel(AwsFacade awsFacade, BucketPath path) throws IOException
	{
		this(awsFacade, path.getFileSystem().bucketName(), path.toString(), 0, false);
	}

	BucketWritableByteChannel(AwsFacade awsFacade,
							  String bucket,
							  ObjectBasicFileAttributes attributes,
							  Optional<ObjectFlag> objectFlag)
		throws IOException
	{
		this(awsFacade,
			 bucket,
			 attributes.fileKey(),
			 objectFlag.filter(ObjectFlag.IS_TRUNCATABLE::equals).isEmpty() ? attributes.size() : 0,
			 objectFlag.filter(ObjectFlag.IS_APPENDABLE::equals).isPresent());
	}

	private BucketWritableByteChannel(AwsFacade awsFacade,
									  String bucket,
									  String key,
									  long oldFileSize,
									  boolean isAppendable)
		throws IOException
	{
		this.awsFacade = awsFacade;
		this.bucket = bucket;
		this.key = key;
		this.oldFileSize = oldFileSize;
		Evaluator.when(() -> isAppendable).thenExecute(this::evaluateMultipart);
	}

	@Override
	public boolean isOpen()
	{
		return isOpen;
	}

	@Override
	public void close() throws IOException
	{
		var newFileSize = Transformer.<MultipartWriter, Long>of(multipartWriter)
									 .whenNotNull()
									 .then(item -> item.bytesWritten() + buffer.position())
									 .orReturn(item -> Long.valueOf(buffer.position()));
		Case.of(newFileSize)
			.when(item -> item < oldFileSize)
			.then(this::partialOverwrite)
			.otherwise(this::flushBuffer);
		isOpen = false;
	}

	@Override
	public int write(ByteBuffer src) throws IOException
	{
		return Transformer.<BucketWritableByteChannel, Integer>of(this)
						  .when(item -> item.isOpen)
						  .then(item -> item.writeBuffer(src))
						  .orThrow(ClosedChannelException::new);
	}

	void mustAbort()
	{
		Optional.ofNullable(multipartWriter).ifPresent(MultipartWriter::mustAbort);
	}

	private int writeBuffer(ByteBuffer input) throws IOException
	{
		var reference = new AtomicReference<IOException>();
		var position = input.position();
		Stream.iterate(input, UnaryOperator.identity())
			  .takeWhile(item -> reference.get() == null && item.hasRemaining())
			  .forEach(item -> writeRemaining(item, reference));
		var exception = reference.get();
		Case.of(exception).whenNotNull().thenThrow(() -> exception);
		return input.position() - position;
	}

	private void writeRemaining(ByteBuffer input, AtomicReference<IOException> reference)
	{
		Optional.of(Math.min(input.remaining(), buffer.remaining()))
				.filter(item -> item > 0)
				.ifPresent(item -> writeRemaining(input, item));
		try
		{
			Evaluator.when(Expression.not(buffer::hasRemaining)).thenExecute(this::flushBuffer);
		}
		catch (IOException x)
		{
			reference.set(x);
		}
	}

	private void writeRemaining(ByteBuffer input, int remaining)
	{
		var chunk = new byte[remaining];
		input.get(chunk);
		buffer.put(chunk);
	}

	private void partialOverwrite(long newFileSize) throws IOException
	{
		Evaluator.when(() -> oldFileSize > MULTIPART_THRESHOLD)
				 .then(() -> multipartOverwrite(newFileSize))
				 .elseExecute(() -> singlepartOverwrite(newFileSize));
	}

	private void singlepartOverwrite(long newFileSize) throws IOException
	{
		var array = awsFacade.readObject(bucket, key, newFileSize, oldFileSize - 1);
		buffer.put(array);
		flushBuffer();
	}

	private void multipartOverwrite(long newFileSize) throws IOException
	{
		var remaining = buffer.remaining();
		var startLastPart = newFileSize + remaining;
		var array = awsFacade.readObject(bucket, key, newFileSize, startLastPart - 1);
		buffer.put(array);
		flushBuffer();
		Case.of(multipartWriter)
			.whenNotNull()
			.thenHandle(item -> copyAndClose(item, startLastPart - 1, oldFileSize - 1));
	}

	private void flushBuffer() throws IOException
	{
		buffer.flip();
		var array = new byte[Math.min(buffer.remaining(), MULTIPART_THRESHOLD)];
		buffer.get(array);
		if (buffer.hasRemaining())
		{
			startMultipartUpload();
			multipartWriter.write(array);
		}
		else
		{
			Case.of(multipartWriter)
				.whenNotNull()
				.then(item -> writeAndClose(item, array))
				.otherwise(item -> awsFacade.writeObject(bucket, key, array));
		}
		buffer.compact();
	}

	private void evaluateMultipart() throws IOException
	{
		Case.of(oldFileSize)
			.when(item -> item > MULTIPART_THRESHOLD)
			.then(this::startMultipartUploadAndCopy)
			.otherwise(() -> buffer.put(awsFacade.readObject(bucket, key)));
	}

	private void startMultipartUpload()
	{
		multipartWriter = Optional.ofNullable(multipartWriter)
								  .orElseGet(() -> awsFacade.startMultipartUpload(bucket, key));
	}

	private void startMultipartUploadAndCopy(long size) throws IOException
	{
		startMultipartUpload();
		Case.of(multipartWriter).whenNotNull().thenHandle(item -> item.copy(size));
	}

	private void writeAndClose(MultipartWriter multipartWriter, byte[] buffer) throws IOException
	{
		try (var writer = multipartWriter)
		{
			writer.write(buffer);
		}
		this.multipartWriter = null;
	}

	private void copyAndClose(MultipartWriter multipartWriter, long from, long to)
		throws IOException
	{
		try (var writer = multipartWriter)
		{
			writer.copy(from, to);
		}
		this.multipartWriter = null;
	}
}
