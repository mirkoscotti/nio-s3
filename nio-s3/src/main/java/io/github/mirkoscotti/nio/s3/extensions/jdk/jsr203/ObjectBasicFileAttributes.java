package io.github.mirkoscotti.nio.s3.extensions.jdk.jsr203;

import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.Objects;

import software.amazon.awssdk.services.s3.model.S3Object;

/**
 * A <code>BasicFileAttributes</code> implementation mapping the metadata available on an S3 object
 * to the standard NIO.2 file attribute model.
 *
 * @author mirko.scotti
 * @version Dec 26, 2024
 * @see java.nio.file.attribute.BasicFileAttributes
 * @see ObjectBasicFileAttributeView
 */
public class ObjectBasicFileAttributes
	implements BasicFileAttributes
{

	private static final String FILE_SEPARATOR = "/";

	private final S3Object object;

	/**
	 * Creates a file attribute instance from the given S3 object
	 *
	 * @param object
	 *            an S3 object
	 */
	public ObjectBasicFileAttributes(S3Object object)
	{
		this.object = Objects.requireNonNull(object, () -> "Missing S3 object.");
	}

	@Override
	public FileTime lastModifiedTime()
	{
		return FileTime.from(object.lastModified());
	}

	/**
	 * Amazon S3 does not provide this information.
	 *
	 * @return fallback to {@link #lastModifiedTime()}
	 */
	@Override
	public FileTime lastAccessTime()
	{
		return lastModifiedTime();
	}

	/**
	 * Amazon S3 does not provide this information.
	 *
	 * @return fallback to {@link #lastModifiedTime()}
	 */
	@Override
	public FileTime creationTime()
	{
		return lastModifiedTime();
	}

	/**
	 * Specifies whether the S3 object represents a file, typically if its key does not end with
	 * <i>/</i>.
	 *
	 * @return <code>true</code> if this is a regular file, <code>false</code> if it is a directory
	 */
	@Override
	public boolean isRegularFile()
	{
		return !isDirectory();
	}

	/**
	 * Specifies whether the S3 object represents a directory, typically if its key ends with
	 * <i>/</i>.
	 *
	 * @return <code>true</code> if this is a directory, <code>false</code> if it is a file
	 */
	@Override
	public boolean isDirectory()
	{
		return object.key().endsWith(FILE_SEPARATOR);
	}

	/**
	 * Buckets do not support symbolic links.
	 */
	@Override
	public boolean isSymbolicLink()
	{
		return false;
	}

	/**
	 * No additional kind of objects managed here.
	 */
	@Override
	public boolean isOther()
	{
		return false;
	}

	/**
	 * Returns the size of the S3 object in bytes, as reported by the AWS SDK.
	 */
	@Override
	public long size()
	{
		return object.size();
	}

	/**
	 * The S£ object key.
	 */
	@Override
	public String fileKey()
	{
		return object.key();
	}
}
