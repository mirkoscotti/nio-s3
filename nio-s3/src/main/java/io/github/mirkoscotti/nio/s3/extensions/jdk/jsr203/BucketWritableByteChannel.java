package io.github.mirkoscotti.nio.s3.extensions.jdk.jsr203;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.WritableByteChannel;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

import io.github.mirkoscotti.nio.s3.enums.ObjectFlag;
import io.github.mirkoscotti.nio.s3.exceptions.TransportException;
import io.github.mirkoscotti.nio.s3.functions.Case;
import io.github.mirkoscotti.nio.s3.functions.Evaluator;
import io.github.mirkoscotti.nio.s3.functions.Expression;
import io.github.mirkoscotti.nio.s3.functions.Transformer;
import io.github.mirkoscotti.nio.s3.helpers.ExceptionHelper;
import io.github.mirkoscotti.nio.s3.operations.AwsFacade;
import io.github.mirkoscotti.nio.s3.operations.MultipartWriter;

/**
 * A <code>WritableByteChannel</code> implementation that writes the content to objects of an
 * S3-compatible bucket.
 * <p>
 * Bytes are accumulated in an internal buffer. As long as the overall amount of data stays within
 * an internal fixed threshold of 10MB, the content is uploaded to S3 with a single put operation
 * (single-part upload) when the channel is closed; once the threshold is exceeded, the channel
 * automatically switches to a multipart upload, delegated to a {@link MultipartWriter} instance,
 * splitting the content into parts that are uploaded progressively as the buffer fills up.
 * <p>
 * The class also reproduces, for already existing S3 objects, the write semantics typical of
 * traditional file systems with respect to NIO.2 <code>OpenOption</code> configuration set:
 * <ul>
 * <li><b>Creating a non-existing object</b>: behaves like <code>CREATE_NEW</code>
 * <li><b>Writing to an existing object, without <code>TRUNCATE_EXISTING</code>, with fewer bytes
 * written than the original content's size</b>: the resulting object will contain the bytes written
 * followed by the remaining bytes of the original content that exceed what was written. Example: if
 * the existing object contains <i>text123</i> and the bytes <i>abc</i> are written, the resulting
 * object will be <i>abct123"</i>
 * <li><b>Writing to an existing object with <code>TRUNCATE_EXISTING</code></b> (or, more generally,
 * with a number of bytes written equal to or greater than the original size): the resulting object
 * will contain only the bytes written. Example: if the existing object contains <i>text123</i> and
 * the bytes <i>abc</i> are written, the resulting object will be <i>abc</i>.
 * </ul>
 *
 * @author mirko.scotti
 * @version May 20, 2025
 * @see WritableByteChannel
 * @see MultipartWriter
 * @see AwsFacade
 * @see BucketPath
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

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int write(ByteBuffer src) throws IOException
	{
		return Transformer.<BucketWritableByteChannel, Integer>of(this)
						  .when(item -> item.isOpen)
						  .then(item -> item.writeBuffer(src))
						  .orThrow(ClosedChannelException::new);
	}

	/**
	 * Signals the forced cancellation of the write operation in progress, aborting any multipart
	 * upload started on the S3 bucket, so as not to leave orphaned parts behind.
	 * <p>
	 * Invoked in case of abnormal interruption of the operation (for example following an unhandled
	 * exception during a write), as an alternative to the normal closing via {@link #close()}.
	 */

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
		catch (TransportException x)
		{
			throw ExceptionHelper.toIoException(x.toNioException());
		}
		this.multipartWriter = null;
	}
}
