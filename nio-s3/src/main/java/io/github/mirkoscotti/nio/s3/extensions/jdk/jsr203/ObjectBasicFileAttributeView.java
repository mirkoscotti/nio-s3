package io.github.mirkoscotti.nio.s3.extensions.jdk.jsr203;

import java.io.IOException;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.Objects;

import io.github.mirkoscotti.nio.s3.exceptions.TransportException;
import io.github.mirkoscotti.nio.s3.exceptions.UnsupportedIoOperationException;
import io.github.mirkoscotti.nio.s3.helpers.ExceptionHelper;

import it.mirkoscotti.nio.s3.operations.AwsFacade;

import software.amazon.awssdk.services.s3.model.S3Exception;

/**
 * @author mirko.scotti
 * @version Oct 29, 2024
 */
public class ObjectBasicFileAttributeView
	implements BasicFileAttributeView
{

	static final String BASIC_FILE_ATTRIBUTE_VIEW = "basic";

	private final AwsFacade awsFacade;

	private final String bucketName;

	private final String objectKey;

	public ObjectBasicFileAttributeView(AwsFacade awsFacade, String bucketName, String objectKey)
	{
		this.awsFacade = Objects.requireNonNull(awsFacade, () -> "Missing AWS connector.");
		this.bucketName = Objects.requireNonNull(bucketName, () -> "Missing bucket name.");
		this.objectKey = Objects.requireNonNull(objectKey, () -> "Missing object key.");
	}

	@Override
	public String name()
	{
		return BASIC_FILE_ATTRIBUTE_VIEW;
	}

	@Override
	public BasicFileAttributes readAttributes() throws IOException
	{
		try
		{
			return awsFacade.objectMetadata(bucketName, objectKey);
		}
		catch (TransportException x)
		{
			throw ExceptionHelper.toIoException(x.toNioException());
		}
		catch (S3Exception x)
		{
			throw x;
		}
	}

	/**
	 * It is not possible to manually modify all the times of an S3 object. These values are
	 * metadata fields managed directly by Amazon S3.
	 */
	@Override
	public void setTimes(FileTime lastModifiedTime, FileTime lastAccessTime, FileTime createTime)
		throws IOException
	{
		throw new UnsupportedIoOperationException("Amazon S3 manages object times by itself without any possibility to change them.");
	}
}
