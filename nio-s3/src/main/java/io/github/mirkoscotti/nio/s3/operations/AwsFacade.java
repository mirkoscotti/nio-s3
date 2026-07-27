package io.github.mirkoscotti.nio.s3.operations;

import java.io.Closeable;
import java.io.IOException;
import java.net.URI;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Instant;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

import io.github.mirkoscotti.nio.s3.configuration.BucketDescriptor;
import io.github.mirkoscotti.nio.s3.functions.Case;
import io.github.mirkoscotti.nio.s3.functions.LazyReference;
import io.github.mirkoscotti.nio.s3.records.AwsRecord;

import software.amazon.awssdk.regions.Region;

/**
 * The unified entry point for AWS operations. This facade orchestrates the AWS connectors, where
 * necessary, to cover the Java NIO.2 specifications while accessing S3 buckets and objects.
 *
 * @author mirko.scotti
 * @since Dec 22, 2025
 */
public class AwsFacade
	implements Closeable
{

	private final AwsRecord awsRecord;

	private final LazyReference<IamConnector> iam = LazyReference.of(() -> createConnector(IamConnector::create));

	private final LazyReference<S3Connector> s3 = LazyReference.of(() -> createConnector(S3Connector::create));

	private final LazyReference<StsConnector> sts = LazyReference.of(() -> createConnector(StsConnector::create));

	private AwsFacade(AwsRecord awsRecord)
	{
		this.awsRecord = awsRecord;
	}

	/**
	 * Closes all AWS service connectors that have been initialized during an I/O operation.
	 *
	 * @throws IOException
	 *             if any connector fails to close
	 */
	@Override
	public void close() throws IOException
	{
		Case.of(iam).when(LazyReference::isPresent).thenHandle(item -> item.get().close());
		Case.of(s3).when(LazyReference::isPresent).thenHandle(item -> item.get().close());
		Case.of(sts).when(LazyReference::isPresent).thenHandle(item -> item.get().close());
	}

	/**
	 * Creates a new instance of this facade, configuring the internal connectors as specified by
	 * the given input.
	 *
	 * @param awsRecord
	 *            endpoint, region and credentials container
	 * @return a new instance of this facade
	 * @throws NullPointerException
	 *             if no input is provided
	 */
	public static AwsFacade create(AwsRecord awsRecord)
	{
		Objects.requireNonNull(awsRecord, () -> "Missing endpoint, region and credentials.");
		return new AwsFacade(awsRecord);
	}

	/**
	 * Creates a new S3 bucket as described by the given {@link BucketDescriptor}.
	 *
	 * @param bucketDescriptor
	 *            the bucket descriptor
	 */
	public void createBucket(BucketDescriptor bucketDescriptor)
	{
		s3.get().createBucket(bucketDescriptor);
	}

	/**
	 * Returns the ACL (Access Control List) of the specified S3 bucket.
	 *
	 * @param bucketName
	 *            the name of the bucket
	 * @return a string representation of the bucket ACL
	 */
	public String bucketAcl(String bucketName)
	{
		return s3.get().bucketAcl(bucketName);
	}

	/**
	 * Checks whether the specified S3 bucket is read-only.
	 *
	 * @param bucketName
	 *            the name of the bucket
	 * @return true if the bucket policy allows only read access, false otherwise
	 */
	public boolean isBucketReadOnly(String bucketName)
	{
		return s3.get().isBucketReadOnly(bucketName);
	}

	/**
	 * Returns the effective IAM permission for the current principal on the specified S3 object.
	 *
	 * @param bucket
	 *            the name of the S3 bucket
	 * @param key
	 *            the object key within the bucket
	 * @return a string describing the effective permission
	 */
	public String filePermission(String bucket, String key)
	{
		var arn = sts.get().arn();
		return iam.get().filePermission(arn, bucket, key);
	}

	/**
	 * Returns the effective IAM permission for the current principal on the specified S3 prefix
	 * (logical directory).
	 *
	 * @param bucket
	 *            the name of the S3 bucket
	 * @param key
	 *            the key prefix representing the logical directory
	 * @return a string describing the effective permission
	 */
	public String directoryPermission(String bucket, String key)
	{
		var arn = sts.get().arn();
		return iam.get().directoryPermission(arn, bucket, key);
	}

	/**
	 * Returns the metadata of the specified S3 object in the form of Java NIO.2 compliant
	 * attributes.
	 *
	 * @param bucket
	 *            the name of the S3 bucket
	 * @param key
	 *            the object key
	 * @return the file attributes of the object AWS makes available
	 */
	public BasicFileAttributes objectMetadata(String bucket, String key)
	{
		return s3.get().objectMetadata(bucket, key);
	}

	/**
	 * Lists all objects under the given key prefix in the specified bucket.
	 *
	 * @param bucketName
	 *            the name of the S3 bucket
	 * @param key
	 *            the key prefix to list
	 * @return the mapping of object keys matching the prefix to its last-modified instant
	 */
	public Map<String, Instant> listObjects(String bucketName, String key)
	{
		return s3.get().listObjects(bucketName, key);
	}

	/**
	 * The paginated version of the method above.
	 *
	 * @param bucketName
	 *            the name of the S3 bucket
	 * @param key
	 *            the key prefix to list
	 * @param pageSize
	 *            the maximum number of objects per page or the AWS default if not specified
	 * @return the mapping of object keys matching the prefix to its last-modified instant
	 */
	public Map<String, Instant> listObjects(String bucketName, String key, Integer pageSize)
	{
		return s3.get().listObjects(bucketName, key, pageSize);
	}

	/**
	 * Specifies whether the logical directory identified by the given prefix is non-empty.
	 *
	 * @param bucketName
	 *            the name of the S3 bucket
	 * @param key
	 *            the key prefix representing the logical directory
	 * @return true if at least one object exists under the prefix, false otherwise
	 */
	public boolean isNotEmptyDirectory(String bucketName, String key)
	{
		return s3.get().isNotEmptyDirectory(bucketName, key);
	}

	/**
	 * A lazy iterator that scans all object keys under the given prefix.
	 *
	 * @param bucketName
	 *            the name of the S3 bucket
	 * @param prefix
	 *            the key prefix to scan
	 * @return the iterator of object keys matching the prefix
	 */
	public Iterator<String> scanDirectory(String bucketName, String prefix)
	{
		return s3.get().scanDirectory(bucketName, prefix);
	}

	/**
	 * Reads the full content of the specified S3 object.
	 *
	 * @param bucketName
	 *            the name of the S3 bucket
	 * @param key
	 *            the object key
	 * @return the raw bytes of the object
	 */
	public byte[] readObject(String bucketName, String key)
	{
		return s3.get().readObject(bucketName, key);
	}

	/**
	 * Reads a portion of the specified S3 object.
	 *
	 * @param bucketName
	 *            the name of the S3 bucket
	 * @param key
	 *            the object key
	 * @param from
	 *            the start offset
	 * @param to
	 *            the end offset
	 * @return the raw bytes of the object included in the byte range
	 */
	public byte[] readObject(String bucketName, String key, long from, long to)
	{
		return s3.get().readObject(bucketName, key, from, to);
	}

	/**
	 * Creates an empty object at the specified key in the given S3 bucket.
	 *
	 * @param bucketName
	 *            the name of the S3 bucket
	 * @param key
	 *            the object key to create
	 */
	public void writeObject(String bucketName, String key)
	{
		s3.get().writeObject(bucketName, key);
	}

	/**
	 * Writes the given content to the specified key in the given S3 bucket. If the object already
	 * exists, it is overwritten.
	 *
	 * @param bucketName
	 *            the name of the S3 bucket
	 * @param key
	 *            the object key to write
	 * @param content
	 *            the bytes to upload
	 */
	public void writeObject(String bucketName, String key, byte[] content)
	{
		s3.get().writeObject(bucketName, key, content);
	}

	/**
	 * Deletes the object at the specified key from the given S3 bucket. If the object does not
	 * exist, no errors are raised.
	 *
	 * @param bucketName
	 *            the name of the S3 bucket
	 * @param key
	 *            the object key to delete
	 */
	public void deleteObject(String bucketName, String key)
	{
		s3.get().deleteObject(bucketName, key);
	}

	/**
	 * Starts a new multi-part upload for the specified S3 object.
	 *
	 * @param bucketName
	 *            the name of the S3 bucket
	 * @param key
	 *            the destination object key
	 * @return an instance of the multi-part upload process
	 */
	public MultipartWriter startMultipartUpload(String bucketName, String key)
	{
		return s3.get().startMultipartUpload(bucketName, key);
	}

	/**
	 * Starts a new file transfer from a bucket to another.
	 *
	 * @param bucketName
	 *            the name of the S3 source bucket
	 * @param key
	 *            the object key
	 * @return a {@link FileTransfer} instance for the requested object
	 */
	public FileTransfer fileTransfer(String bucketName, String key)
	{
		return s3.get().fileTransfer(bucketName, key);
	}

	/**
	 * Receives an S3 object using the given file transfer. This is the counterpart to
	 * {@link #fileTransfer(String, String)}: it performs the actual data transfer for a file
	 * transfer previously obtained from another facade instance or an external source.
	 *
	 * @param bucketName
	 *            the name of the S3 bucket
	 * @param key
	 *            the object key
	 * @param fileTransfer
	 *            the file transfer managing the external file
	 * @throws IOException
	 *             if an I/O error occurs during the transfer
	 */
	public void receiveFile(String bucketName, String key, FileTransfer fileTransfer)
		throws IOException
	{
		s3.get().receiveFile(bucketName, key, fileTransfer);
	}

	/**
	 * The wrapper method of the {@link #awsRecord} property.
	 *
	 * @return the value of the property
	 */
	public AwsRecord awsRecord()
	{
		return awsRecord;
	}

	private <T extends AwsConnectorBuilder<T, ?, C, ?>,
			 C extends AwsConnector> C createConnector(Supplier<T> builder)
	{
		var credentials = awsRecord.credentials();
		var connectorBuilder = builder.get()
									  .withCredentials(credentials.accessKey(),
													   credentials.secretKey());
		awsRecord.endpoint().map(URI::create).ifPresent(connectorBuilder::withEndpoint);
		awsRecord.region().map(Region::toString).ifPresent(connectorBuilder::withRegion);
		return connectorBuilder.build();
	}
}
