package io.github.mirkoscotti.nio.s3.extensions.jdk.jsr203;

import java.io.Closeable;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.Channel;
import java.nio.channels.NonReadableChannelException;
import java.nio.channels.NonWritableChannelException;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.NoSuchFileException;
import java.nio.file.OpenOption;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import io.github.mirkoscotti.nio.s3.enums.ObjectFlag;
import io.github.mirkoscotti.nio.s3.functions.Try;
import io.github.mirkoscotti.nio.s3.operations.AwsFacade;

/**
 * The specific {@link SeekableByteChannel} implementation providing read or write access to objects
 * stored in an Amazon S3 bucket, in compliance with the Java NIO.2 specifications (JSR-203).
 * <p>
 * Depending on the {@link OpenOption}s requested at open time, an instance wraps either a readable
 * channel or a writable channel. The two sub-channels are mutually exclusive, since an S3 object
 * cannot be read and written at the same time: requesting both <code>READ</code> and
 * <code>WRITE</code> together is rejected at construction time. When neither option is explicitly
 * requested, the channel defaults to read, consistently with the standard NIO.2 semantics of
 * {@link Files#newByteChannel(java.nio.file.Path, OpenOption...)}, which treats the absence of
 * options as a request to open for read.
 *
 * @author mirko.scotti
 * @version May 16, 2025
 * @see BucketReadableByteChannel
 * @see BucketWritableByteChannel
 * @see ObjectFlag
 * @see AwsFacade
 */
class BucketSeekableByteChannel
	implements SeekableByteChannel
{

	private final BucketPath path;

	private final AwsFacade awsFacade;

	private final Optional<BucketReadableByteChannel> readableByteChannel;

	private final Optional<BucketWritableByteChannel> writableByteChannel;

	/**
	 * Creates a new channel for the S3 object identified by its path, opening either the readable
	 * or the writable sub-channel according to the requested <code>openOptions</code>.
	 *
	 * @param path
	 *            the path, within the bucket, of the object to open
	 * @param openOptions
	 *            the requested open options
	 * @throws IOException
	 *             if an error occurs while accessing the S3 bucket during the opening of the
	 *             writable channel
	 * @throws NullPointerException
	 *             if the path is not specified
	 */
	BucketSeekableByteChannel(BucketPath path, Set<? extends OpenOption> openOptions)
		throws IOException
	{
		this.path = Objects.requireNonNull(path, () -> "Missing path.");
		awsFacade = path.getFileSystem().awsFacade();
		ObjectFlag.readWriteCheck(openOptions);
		readableByteChannel = createReadableByteChannel(openOptions);
		writableByteChannel = createWritableByteChannel(openOptions);
	}

	@Override
	public boolean isOpen()
	{
		return readableByteChannel.filter(Channel::isOpen).isPresent()
			|| writableByteChannel.filter(Channel::isOpen).isPresent();
	}

	/**
	 * Closes whichever sub-channel — readable or writable — is present, and unregisters this
	 * instance from the owning {@link BucketFileSystem}.
	 * <p>
	 * If the file system is being closed and the writable channel is the one in use, any
	 * in-progress multipart upload on it is aborted before it is closed. If closing the sub-channel
	 * raises an exception, it is propagated; exceptions of an unexpected type are wrapped in an
	 * {@link IllegalStateException}.
	 *
	 * @throws IOException
	 *             if an error occurs while closing the sub-channel
	 * @throws IllegalStateException
	 *             when an unexpected error occurs
	 */
	@Override
	public synchronized void close() throws IOException
	{
		var readable = readableByteChannel.map(this::tryClose);
		var writable = writableByteChannel.map(this::tryClose);
		var exception = readable.orElseGet(() -> writable.orElse(null));
		if (exception != null)
		{
			switch (exception)
			{
				case IOException x -> throw x;
				case RuntimeException x -> throw x;
				default -> throw new IllegalStateException(exception);
			}
		}
		path.getFileSystem().unregisterResource(this);
	}

	/**
	 * Reads a sequence of bytes from this channel into the given buffer, delegating the operation
	 * to the writable sub-channel.
	 *
	 * @param dst
	 *            the destination buffer
	 * @return the number of bytes read, or -1 if the end of the stream has been reached
	 * @throws NonReadableChannelException
	 *             if the channel was not opened for read
	 * @throws IOException
	 *             if an I/O error occurs
	 */
	@Override
	public int read(ByteBuffer dst) throws IOException
	{
		return readableByteChannel.orElseThrow(NonReadableChannelException::new).read(dst);
	}

	/**
	 * Writes a sequence of bytes to this channel from the given buffer, delegating the operation to
	 * the writable sub-channel.
	 *
	 * @param src
	 *            the source buffer
	 * @return the number of bytes written
	 * @throws NonWritableChannelException
	 *             if the channel was not opened for write
	 * @throws IOException
	 *             if an I/O error occurs
	 */
	@Override
	public int write(ByteBuffer src) throws IOException
	{
		return writableByteChannel.orElseThrow(NonWritableChannelException::new).write(src);
	}

	/**
	 * Returns this channel's current position.
	 * <p>
	 * Only available when the channel has been opened for read: when the channel is open for write,
	 * S3 does not allow tracking a position on the object being uploaded, so this method is not
	 * supported — especially while a multipart upload is in progress.
	 *
	 * @return the current position, in bytes from the beginning of the object
	 * @throws UnsupportedOperationException
	 *             if the channel was not opened for read
	 * @throws IOException
	 *             if an I/O error occurs
	 */
	@Override
	public long position() throws IOException
	{
		return readableByteChannel.orElseThrow(() -> new UnsupportedOperationException("Position is available only when channel is open for read."))
								  .position();
	}

	/**
	 * Sets this channel's position.
	 * <p>
	 * Only available when the channel has been opened for read: when the channel is open for write,
	 * S3 does not allow seeking to a position on the object being uploaded, so this method is not
	 * supported — especially while a multipart upload is in progress.
	 *
	 * @param newPosition
	 *            the new position, in bytes from the beginning of the object
	 * @return this instance
	 * @throws UnsupportedOperationException
	 *             if the channel was not opened for read
	 * @throws IOException
	 *             if an I/O error occurs
	 */
	@Override
	public SeekableByteChannel position(long newPosition) throws IOException
	{
		readableByteChannel.orElseThrow(() -> new UnsupportedOperationException("Position can be set only when channel is open for read."))
						   .position(newPosition);
		return this;
	}

	/**
	 * Returns the current size, in bytes, of the underlying object.
	 * <p>
	 * Only available when the channel has been opened for read.
	 *
	 * @return the size of the object, in bytes
	 * @throws UnsupportedOperationException
	 *             if the channel was not opened for read
	 * @throws IOException
	 *             if an I/O error occurs
	 */
	@Override
	public long size() throws IOException
	{
		return readableByteChannel.orElseThrow(() -> new UnsupportedOperationException("Size is available only when channel is open for read."))
								  .size();
	}

	/**
	 * Not yet supported.
	 * <p>
	 * Several implementation strategies are currently under evaluation (in-RAM truncation for
	 * single-part uploads, temporary-file truncation for multi-part uploads, or a dedicated open
	 * option to enable truncation when needed).
	 *
	 * @param size
	 *            the requested size (currently unused)
	 * @return never: this method always throws
	 * @throws UnsupportedOperationException
	 *             always, since truncation is not yet implemented
	 */
	@Override
	public SeekableByteChannel truncate(long size) throws IOException
	{
		// TODO - under evaluation:
		// 1. in-ram truncation for singlepart uploads
		// 2. temporary files truncation for multi-part uploads
		// 3. custom OpenOption enum to enable truncation when needed
		throw new UnsupportedOperationException("Truncate not supported yet.");
	}

	private Optional<BucketReadableByteChannel> createReadableByteChannel(Set<? extends OpenOption> options)
	{
		return ObjectFlag.IS_READABLE.matches(options)
			? Optional.of(new BucketReadableByteChannel(awsFacade, path))
			: Optional.empty();
	}

	private Optional<BucketWritableByteChannel> createWritableByteChannel(Set<? extends OpenOption> options)
		throws IOException
	{
		return ObjectFlag.IS_WRITABLE.matches(options)
			? Optional.of(createSafeWritableByteChannel(options))
			: Optional.empty();
	}

	private BucketWritableByteChannel createSafeWritableByteChannel(Set<? extends OpenOption> options)
		throws IOException
	{
		BucketWritableByteChannel result;
		var objectFlag = ObjectFlag.appendTruncateCheck(options);
		try
		{
			var fileAttributes = Files.readAttributes(path,
													  ObjectBasicFileAttributes.class,
													  LinkOption.NOFOLLOW_LINKS);
			ObjectFlag.creationWhenFileExistingCheck(options, path);
			var bucketName = path.getFileSystem().bucketName();
			result = new BucketWritableByteChannel(awsFacade,
												   bucketName,
												   fileAttributes,
												   objectFlag);
		}
		catch (NoSuchFileException x)
		{
			ObjectFlag.creationWhenFileNotFoundCheck(options, path);
			result = new BucketWritableByteChannel(awsFacade, path);
		}
		return result;
	}

	private Exception tryClose(BucketWritableByteChannel channel)
	{
		Optional.of(path.getFileSystem())
				.filter(BucketFileSystem::isClosing)
				.ifPresent(item -> channel.mustAbort());
		return tryClose((Closeable) channel);
	}

	private Exception tryClose(Closeable closeable)
	{
		var result = new AtomicReference<Exception>();
		Try.to(() -> close(closeable)).onCatch(result::set).run();
		return result.get();
	}

	private Void close(Closeable closeable) throws IOException
	{
		closeable.close();
		return null;
	}
}
