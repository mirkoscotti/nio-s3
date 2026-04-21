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

import it.mirkoscotti.nio.s3.records.AwsRecord;

import software.amazon.awssdk.regions.Region;

/**
 * @author mirko.scotti
 * @version Dec 22, 2025
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

	@Override
	public void close() throws IOException
	{
		Case.of(iam).when(LazyReference::isPresent).thenHandle(item -> item.get().close());
		Case.of(s3).when(LazyReference::isPresent).thenHandle(item -> item.get().close());
		Case.of(sts).when(LazyReference::isPresent).thenHandle(item -> item.get().close());
	}

	public static AwsFacade create(AwsRecord awsRecord)
	{
		Objects.requireNonNull(awsRecord, () -> "Missing endpoint, region and credentials.");
		return new AwsFacade(awsRecord);
	}

	public void createBucket(BucketDescriptor bucketDescriptor)
	{
		s3.get().createBucket(bucketDescriptor);
	}

	public String bucketAcl(String bucketName)
	{
		return s3.get().bucketAcl(bucketName);
	}

	public boolean isBucketReadOnly(String bucketName)
	{
		return s3.get().isBucketReadOnly(bucketName);
	}

	public String filePermission(String bucket, String key)
	{
		var arn = sts.get().arn();
		return iam.get().filePermission(arn, bucket, key);
	}

	public String directoryPermission(String bucket, String key)
	{
		var arn = sts.get().arn();
		return iam.get().directoryPermission(arn, bucket, key);
	}

	public BasicFileAttributes objectMetadata(String bucket, String key)
	{
		return s3.get().objectMetadata(bucket, key);
	}

	public Map<String, Instant> listObjects(String bucketName, String key)
	{
		return s3.get().listObjects(bucketName, key);
	}

	public Map<String, Instant> listObjects(String bucketName, String key, Integer pageSize)
	{
		return s3.get().listObjects(bucketName, key, pageSize);
	}

	public boolean isNotEmptyDirectory(String bucketName, String key)
	{
		return s3.get().isNotEmptyDirectory(bucketName, key);
	}

	public Iterator<String> scanDirectory(String bucketName, String prefix)
	{
		return s3.get().scanDirectory(bucketName, prefix);
	}

	public byte[] readObject(String bucketName, String key)
	{
		return s3.get().readObject(bucketName, key);
	}

	public byte[] readObject(String bucketName, String key, long from, long to)
	{
		return s3.get().readObject(bucketName, key, from, to);
	}

	public void writeObject(String bucketName, String key)
	{
		s3.get().writeObject(bucketName, key);
	}

	public void writeObject(String bucketName, String key, byte[] content)
	{
		s3.get().writeObject(bucketName, key, content);
	}

	public void deleteObject(String bucketName, String key)
	{
		s3.get().deleteObject(bucketName, key);
	}

	public MultipartWriter startMultipartUpload(String bucketName, String key)
	{
		return s3.get().startMultipartUpload(bucketName, key);
	}

	public FileTransfer fileTransfer(String bucketName, String key)
	{
		return s3.get().fileTransfer(bucketName, key);
	}

	public void receiveFile(String bucketName, String key, FileTransfer fileTransfer)
		throws IOException
	{
		s3.get().receiveFile(bucketName, key, fileTransfer);
	}

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
