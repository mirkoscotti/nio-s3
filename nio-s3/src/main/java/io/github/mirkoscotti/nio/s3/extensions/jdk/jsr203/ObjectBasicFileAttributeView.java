package io.github.mirkoscotti.nio.s3.extensions.jdk.jsr203;

import java.io.IOException;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.Objects;

import io.github.mirkoscotti.nio.s3.exceptions.TransportException;
import io.github.mirkoscotti.nio.s3.exceptions.UnsupportedIoOperationException;
import io.github.mirkoscotti.nio.s3.helpers.ExceptionHelper;
import io.github.mirkoscotti.nio.s3.operations.AwsFacade;

import software.amazon.awssdk.services.s3.model.S3Exception;

/**
 * A <code>BasicFileAttributeView</code> for Amazon S3 objects, providing read access to the
 * standard NIO.2 basic file attributes.
 * <p>
 * This view is identified by the standard <code>basic</code> and is registered against a specific
 * S3 bucket and object key. Attribute retrieval is delegated to an
 * {@link io.github.mirkoscotti.nio.s3.operations.AwsFacade}, which issues the corresponding AWS SDK
 * call.
 * <p>
 * Modifying file timestamps via {@link #setTimes} is not supported: Amazon S3 manages object
 * timestamps internally and does not allow callers to change them directly.
 *
 * @author mirko.scotti
 * @version Oct 29, 2024
 * @see java.nio.file.attribute.BasicFileAttributeView
 * @see ObjectBasicFileAttributes
 */
public class ObjectBasicFileAttributeView
	implements BasicFileAttributeView
{

	static final String BASIC_FILE_ATTRIBUTE_VIEW = "basic";

	private final AwsFacade awsFacade;

	private final String bucketName;

	private final String objectKey;

	/**
	 * Create an instance of the file attribute view for the given object of an S3 bucket
	 *
	 * @param awsFacade
	 *            the AWS connector
	 * @param bucketName
	 *            the bucket name
	 * @param objectKey
	 *            the S3 object
	 */
	public ObjectBasicFileAttributeView(AwsFacade awsFacade, String bucketName, String objectKey)
	{
		this.awsFacade = Objects.requireNonNull(awsFacade, () -> "Missing AWS connector.");
		this.bucketName = Objects.requireNonNull(bucketName, () -> "Missing bucket name.");
		this.objectKey = Objects.requireNonNull(objectKey, () -> "Missing object key.");
	}

	/**
	 * The name of this attribute view.
	 *
	 * @return "basic"
	 */
	@Override
	public String name()
	{
		return BASIC_FILE_ATTRIBUTE_VIEW;
	}

	/**
	 * Reads the basic file attributes of the S3 object identified by the bucket name and object key
	 * supplied at construction time.
	 *
	 * @return an {@link ObjectBasicFileAttributes} instance populated with the object's metadata
	 * @throws IOException
	 *             if a network or transport error occurs while communicating with AWS, or if the
	 *             AWS SDK raises an <code>S3Exception</code>
	 */
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
