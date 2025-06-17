package it.mirkoscotti.nio.s3.extensions.jdk.jsr203;

import it.mirkoscotti.nio.s3.exceptions.UnsupportedIoOperationException;
import it.mirkoscotti.nio.s3.operations.S3Connector;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.Objects;

import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.S3Exception;

/**
 * @author mirko.scotti
 * @version Oct 29, 2024
 */
public class ObjectBasicFileAttributeView
	implements BasicFileAttributeView
{

	static final String BASIC_FILE_ATTRIBUTE_VIEW = "basic";

	private final S3Connector connector;

	private final String bucketName;

	private final String objectKey;

	public ObjectBasicFileAttributeView(S3Connector connector, String bucketName, String objectKey)
	{
		this.connector = Objects.requireNonNull(connector, () -> "Missing AWS connector.");
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
			return connector.objectMetadata(bucketName, objectKey);
		}
		catch (S3Exception x)
		{
			if (x instanceof NoSuchKeyException exception)
			{
				var reason = exception.awsErrorDetails().errorMessage();
				var message = "Bucket: %s, Key: %s (%s)".formatted(bucketName, objectKey, reason);
				throw new FileNotFoundException(message);
			}
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
