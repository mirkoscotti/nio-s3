package io.github.mirkoscotti.nio.s3.extensions.jdk.jsr203;

import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.Objects;

import software.amazon.awssdk.services.s3.model.S3Object;

/**
 * @author mirko.scotti
 * @version Dec 26, 2024
 */
public class ObjectBasicFileAttributes
	implements BasicFileAttributes
{

	private static final String FILE_SEPARATOR = "/";

	private final S3Object object;

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

	@Override
	public boolean isRegularFile()
	{
		return !isDirectory();
	}

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

	@Override
	public long size()
	{
		return object.size();
	}

	@Override
	public String fileKey()
	{
		return object.key();
	}
}
