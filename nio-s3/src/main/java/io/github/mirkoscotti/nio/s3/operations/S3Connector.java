package io.github.mirkoscotti.nio.s3.operations;

import java.io.Closeable;
import java.io.IOException;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.net.URI;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Instant;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import jakarta.json.bind.JsonbBuilder;

import io.github.mirkoscotti.nio.s3.configuration.BucketDescriptor;
import io.github.mirkoscotti.nio.s3.enums.BucketProperty;
import io.github.mirkoscotti.nio.s3.enums.ErrorCode;
import io.github.mirkoscotti.nio.s3.exceptions.TransportException;
import io.github.mirkoscotti.nio.s3.extensions.jdk.collections.DirectoryIterator;
import io.github.mirkoscotti.nio.s3.extensions.jdk.jsr203.ObjectBasicFileAttributes;
import io.github.mirkoscotti.nio.s3.functions.Try;
import io.github.mirkoscotti.nio.s3.helpers.ExceptionHelper;
import io.github.mirkoscotti.nio.s3.records.PolicyRecord;

import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.core.BytesWrapper;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.S3CrtAsyncClientBuilder;
import software.amazon.awssdk.services.s3.model.ChecksumAlgorithm;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest.Builder;
import software.amazon.awssdk.services.s3.model.GetBucketAclResponse;
import software.amazon.awssdk.services.s3.model.GetBucketPolicyResponse;
import software.amazon.awssdk.services.s3.model.Grant;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.S3Object;

/**
 * Specific connector for operations on S3 buckets.
 *
 * @author mirko.scotti
 * @version Oct 22, 2024
 */
public final class S3Connector
	implements AwsConnector, Closeable
{

	private static final Logger LOGGER = System.getLogger(S3Connector.class.getName());

	private final S3AsyncClient client;

	S3Connector(S3AsyncClient client)
	{
		this.client = client;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void close() throws IOException
	{
		client.close();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int hashCode()
	{
		return client.hashCode();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean equals(Object obj)
	{
		return obj instanceof S3Connector other && Objects.equals(client, other.client);
	}

	/**
	 * Creates a new builder for this connector.
	 *
	 * @return the builder object
	 */
	public static S3ConnectorBuilder create()
	{
		return new S3ConnectorBuilder();
	}

	/**
	 * Creates a bucket according to the given descriptor
	 *
	 * @param bucketDescriptor
	 *            the bucket configuration
	 * @see BucketDescriptor
	 */
	public void createBucket(BucketDescriptor bucketDescriptor)
	{
		Try.to(() -> client.createBucket(item -> configureBucket(bucketDescriptor, item))
						   .get(30, TimeUnit.SECONDS))
		   .onCatch(ExceptionHelper::sneakyThrow)
		   .run();
	}

	/**
	 * Determines whether the given bucket is effectively read-only, based on its bucket policy or,
	 * failing that, its ACL.
	 *
	 * @param bucketName
	 *            the bucket name
	 * @return true if the bucket is read-only, false otherwise
	 */
	public boolean isBucketReadOnly(String bucketName)
	{
		// 1. check for user permissions
		// 2. check for policy
		// 3. check for ACL
		return isBucketPolicyReadOnly(client, bucketName)
			|| isBucketAclReadOnly(client, bucketName);
	}

	/**
	 * The grants of the given bucket.
	 *
	 * @param bucketName
	 *            the bucket name
	 * @return the grants as a semicolon-separated list of permissions
	 */
	public String bucketAcl(String bucketName)
	{
		return Try.to(() -> client.getBucketAcl(item -> item.bucket(bucketName))
								  .thenApply(this::permissions)
								  .exceptionally(ExceptionHelper::sneakyThrow)
								  .get(30, TimeUnit.SECONDS))
				  .onCatch(ExceptionHelper::sneakyThrow)
				  .get();
	}

	/**
	 * Retrieves the basic file attributes (size, last modified time) of the given object.
	 *
	 * @param bucketName
	 *            the bucket name
	 * @param key
	 *            the object key
	 * @return the object's basic file attributes
	 */
	public BasicFileAttributes objectMetadata(String bucketName, String key)
	{
		return Try.to(() -> client.headObject(item -> item.bucket(bucketName).key(key))
								  .thenApply(item -> S3Object.builder()
															 .key(key)
															 .size(item.contentLength())
															 .lastModified(item.lastModified())
															 .build())
								  .thenApply(ObjectBasicFileAttributes::new)
								  .exceptionally(ExceptionHelper::sneakyThrow)
								  .get(30, TimeUnit.SECONDS))
				  .onCatch(ExceptionHelper::sneakyThrow)
				  .get();
	}

	/**
	 * Lists all objects under the given key, treated as a directory prefix, excluding the key
	 * itself.
	 *
	 * @param bucketName
	 *            the bucket name
	 * @param key
	 *            the directory prefix
	 * @return a map of object keys to their last modified instant
	 */
	public Map<String, Instant> listObjects(String bucketName, String key)
	{
		return listObjects(bucketName, key, null);
	}

	/**
	 * A single page of the specified size containing objects objects under the given key, treated
	 * as a directory prefix, excluding the key itself, paginating requests with the given page
	 * size.
	 *
	 * @param bucketName
	 *            the bucket name
	 * @param key
	 *            the directory prefix
	 * @param pageSize
	 *            the maximum number of keys per page, or an empty map to use the default
	 * @return a map of object keys to their last modified instant
	 */
	public Map<String, Instant> listObjects(String bucketName, String key, Integer pageSize)
	{
		var separator = BucketDescriptor.PATH_SEPARATOR;
		var prefix = key.endsWith(separator) ? key : key.concat(separator);
		var result = new ConcurrentHashMap<String, Instant>();
		client.listObjectsV2Paginator(item -> item.bucket(bucketName)
												  .prefix(prefix)
												  .maxKeys(pageSize))
			  .subscribe(item -> reportObjects(item, result))
			  .join();
		return result.entrySet()
					 .stream()
					 // Excluding the given key
					 .filter(Predicate.not(item -> item.getKey().equals(key)))
					 .collect(Collectors.toMap(Entry::getKey, Entry::getValue));
	}

	/**
	 * Determines whether the directory identified by the given key} contains at least one object
	 * other than the directory marker itself.
	 *
	 * @param bucketName
	 *            the bucket name
	 * @param key
	 *            the directory prefix
	 * @return true if the directory is not empty, false otherwise
	 */
	public boolean isNotEmptyDirectory(String bucketName, String key)
	{
		var separator = BucketDescriptor.PATH_SEPARATOR;
		var prefix = key.endsWith(separator) ? key : key.concat(separator);
		return Try.to(() -> client.listObjectsV2(item -> item.bucket(bucketName)
															 .prefix(prefix)
															 .maxKeys(2))
								  .thenApply(item -> item.contents().size() == 2)
								  .exceptionally(ExceptionHelper::sneakyThrow)
								  .get(30, TimeUnit.SECONDS))
				  .onCatch(ExceptionHelper::sneakyThrow)
				  .get();
	}

	/**
	 * An iterator over the object keys found under the given prefix.
	 *
	 * @param bucketName
	 *            the bucket name
	 * @param prefix
	 *            the key prefix to scan
	 * @return an iterator of matching object keys
	 */
	public Iterator<String> scanDirectory(String bucketName, String prefix)
	{
		return new DirectoryIterator(client, bucketName, prefix);
	}

	/**
	 * Reads the full content of an object.
	 *
	 * @param bucketName
	 *            the bucket name
	 * @param key
	 *            the object key
	 * @return the object's content as a byte array
	 */
	public byte[] readObject(String bucketName, String key)
	{
		return Try.to(() -> client.getObject(item -> item.bucket(bucketName).key(key),
											 AsyncResponseTransformer.toBytes())
								  .thenApply(BytesWrapper::asByteArray)
								  .exceptionally(ExceptionHelper::sneakyThrow)
								  .get(30, TimeUnit.SECONDS))
				  .onCatch(ExceptionHelper::sneakyThrow)
				  .get();
	}

	/**
	 * Reads the byte range of an object.
	 *
	 * @param bucketName
	 *            the bucket name
	 * @param key
	 *            the object key
	 * @param from
	 *            the range start offset, inclusive
	 * @param to
	 *            the range end offset, inclusive
	 * @return the requested byte range as a byte array
	 */
	public byte[] readObject(String bucketName, String key, long from, long to)
	{
		return Try.to(() -> client.getObject(item -> item.bucket(bucketName)
														 .key(key)
														 .range("bytes=%d-%d".formatted(from, to)),
											 AsyncResponseTransformer.toBytes())
								  .thenApply(BytesWrapper::asByteArray)
								  .exceptionally(ExceptionHelper::sneakyThrow)
								  .get(30, TimeUnit.SECONDS))
				  .onCatch(ExceptionHelper::sneakyThrow)
				  .get();
	}

	/**
	 * Creates an empty object at the given key.
	 *
	 * @param bucketName
	 *            the bucket name
	 * @param key
	 *            the object key
	 */
	public void writeObject(String bucketName, String key)
	{
		writeObject(bucketName, key, new byte[0]);
	}

	/**
	 * Writes the given content to the specified object, using SHA-256 checksum validation.
	 *
	 * @param bucketName
	 *            the bucket name
	 * @param key
	 *            the object key
	 * @param content
	 *            the content to write
	 */
	public void writeObject(String bucketName, String key, byte[] content)
	{
		Try.to(() -> client.putObject(item -> item.bucket(bucketName)
												  .key(key)
												  .checksumAlgorithm(ChecksumAlgorithm.SHA256),
									  AsyncRequestBody.fromBytes(content))
						   .exceptionally(ExceptionHelper::sneakyThrow)
						   .get(30, TimeUnit.SECONDS))
		   .onCatch(ExceptionHelper::sneakyThrow)
		   .run();
	}

	/**
	 * Deletes the given object.
	 *
	 * @param bucketName
	 *            the bucket name
	 * @param key
	 *            the object key
	 */
	public void deleteObject(String bucketName, String key)
	{
		Try.to(() -> client.deleteObject(item -> item.bucket(bucketName).key(key))
						   .exceptionally(ExceptionHelper::sneakyThrow)
						   .get(30, TimeUnit.SECONDS))
		   .onCatch(ExceptionHelper::sneakyThrow)
		   .run();
	}

	/**
	 * Starts a new multipart upload for the given object.
	 *
	 * @param bucketName
	 *            the destination bucket
	 * @param key
	 *            the destination object key
	 * @return the multipart process manager
	 */
	public MultipartWriter startMultipartUpload(String bucketName, String key)
	{
		return new MultipartWriter(client, bucketName, key);
	}

	/**
	 * Starts a file transfer for copying or moving the given object to another bucket or key.
	 *
	 * @param bucketName
	 *            the source bucket
	 * @param key
	 *            the source object key
	 * @return the file transfer process manager
	 */
	public FileTransfer fileTransfer(String bucketName, String key)
	{
		return new FileTransfer(client, bucketName, key);
	}

	/**
	 * Transfers an external object into the given bucket and key.
	 *
	 * @param bucketName
	 *            the destination bucket
	 * @param key
	 *            the destination object key
	 * @param fileTransfer
	 *            the transfer manager
	 * @throws IOException
	 *             if the transfer fails
	 */
	public void receiveFile(String bucketName, String key, FileTransfer fileTransfer)
		throws IOException
	{
		fileTransfer.transfer(client, bucketName, key);
	}

	private void configureBucket(BucketDescriptor bucketDescriptor, Builder builder)
	{
		builder.bucket(bucketDescriptor.bucketKey().bucketName());
		var properties = bucketDescriptor.configuration();
		Optional.ofNullable(properties.get(BucketProperty.ACL))
				.map(Object::toString)
				.ifPresent(builder::acl);
		Optional.ofNullable(properties.get(BucketProperty.FULL_CONTROL))
				.map(Object::toString)
				.ifPresent(builder::grantFullControl);
		Optional.ofNullable(properties.get(BucketProperty.READ))
				.map(Object::toString)
				.ifPresent(builder::grantRead);
		Optional.ofNullable(properties.get(BucketProperty.READ_ACP))
				.map(Object::toString)
				.ifPresent(builder::grantReadACP);
		Optional.ofNullable(properties.get(BucketProperty.WRITE))
				.map(Object::toString)
				.ifPresent(builder::grantWrite);
		Optional.ofNullable(properties.get(BucketProperty.WRITE_ACP))
				.map(Object::toString)
				.ifPresent(builder::grantWriteACP);
	}

	private boolean isBucketPolicyReadOnly(S3AsyncClient client, String bucketName)
	{
		return Try.to(() -> client.getBucketPolicy(item -> item.bucket(bucketName))
								  .thenApply(this::isBucketReadOnly)
								  .exceptionally(this::guessReadOnly)
								  .get(30, TimeUnit.SECONDS))
				  .onCatch(this::guessReadOnly)
				  .get();
	}

	private boolean isBucketReadOnly(GetBucketPolicyResponse response)
	{
		var policy = deserializePolicy(response.policy());
		return Optional.ofNullable(policy).map(PolicyRecord::isReadOnly).orElse(false);
	}

	private boolean isBucketAclReadOnly(S3AsyncClient client, String bucketName)
	{
		return Try.to(() -> client.getBucketAcl(item -> item.bucket(bucketName))
								  .thenApply(this::isBucketReadOnly)
								  .exceptionally(this::guessReadOnly)
								  .get(30, TimeUnit.SECONDS))
				  .onCatch(this::guessReadOnly)
				  .get();
	}

	private boolean isBucketReadOnly(GetBucketAclResponse response)
	{
		var grants = response.grants();
		return grants.stream()
					 .map(Grant::permissionAsString)
					 .anyMatch(List.of(BucketProperty.READ.name(),
									   BucketProperty.READ_ACP.name())::contains);
	}

	private boolean guessReadOnly(Throwable throwable)
	{
		var exception = ExceptionHelper.redirectException(throwable);
		if (exception instanceof TransportException transportException)
		{
			return transportException.toErrorCode()
									 .filter(Predicate.isEqual(ErrorCode.NO_SUCH_BUCKET_POLICY))
									 .map(item -> false)
									 .orElseThrow(() -> new IllegalStateException(exception));
		}
		throw exception;
	}

	private String permissions(GetBucketAclResponse response)
	{
		return response.grants()
					   .stream()
					   .map(Grant::permissionAsString)
					   .collect(Collectors.joining(";"));
	}

	private PolicyRecord deserializePolicy(String policy)
	{
		PolicyRecord result;
		try (var jsonb = JsonbBuilder.create())
		{
			result = jsonb.fromJson(policy, PolicyRecord.class);
		}
		catch (Exception x)
		{
			LOGGER.log(Level.ERROR, () -> "Policy deserialization error.%n%s".formatted(policy), x);
			throw new IllegalStateException("Failed to read the bucket policy.", x);
		}
		return result;
	}

	private void reportObjects(ListObjectsV2Response response, Map<String, Instant> report)
	{
		response.contents().forEach(item -> report.put(item.key(), item.lastModified()));
	}

	/**
	 * The specific {@link ConnectorBuilder} implementation creating connector to access S3 buckets.
	 *
	 * @author mirko.scotti
	 * @version Jun 29, 2026
	 */
	static class S3ConnectorBuilder
		extends
		AwsConnectorBuilder<S3ConnectorBuilder, S3CrtAsyncClientBuilder, S3Connector, S3AsyncClient>
	{

		S3ConnectorBuilder()
		{
			super(S3AsyncClient.crtBuilder().crossRegionAccessEnabled(true));
		}

		@Override
		protected void endpointOverride(S3CrtAsyncClientBuilder builder, URI uri)
		{
			builder.endpointOverride(uri);
		}

		@Override
		protected void region(S3CrtAsyncClientBuilder builder, Region region)
		{
			builder.region(region);
		}

		@Override
		protected void credentialsProvider(S3CrtAsyncClientBuilder builder,
										   AwsCredentialsProvider provider)
		{
			builder.credentialsProvider(provider);
		}

		@Override
		protected Function<S3AsyncClient, S3Connector> connectorCreator()
		{
			return S3Connector::new;
		}

		@Override
		protected S3ConnectorBuilder thisBuilder()
		{
			return this;
		}
	}
}
