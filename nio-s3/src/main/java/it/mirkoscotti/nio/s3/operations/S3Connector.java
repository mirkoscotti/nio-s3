package it.mirkoscotti.nio.s3.operations;

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
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import jakarta.json.bind.JsonbBuilder;

import it.mirkoscotti.nio.s3.configuration.BucketDescriptor;
import it.mirkoscotti.nio.s3.enums.BucketProperty;
import it.mirkoscotti.nio.s3.extensions.jdk.collections.DirectoryIterator;
import it.mirkoscotti.nio.s3.extensions.jdk.jsr203.ObjectBasicFileAttributes;
import it.mirkoscotti.nio.s3.functions.Try;
import it.mirkoscotti.nio.s3.helpers.ExceptionsHelper;
import it.mirkoscotti.nio.s3.records.OperationRecord;
import it.mirkoscotti.nio.s3.records.PolicyRecord;

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

	@Override
	public void close() throws IOException
	{
		client.close();
	}

	@Override
	public int hashCode()
	{
		return client.hashCode();
	}

	@Override
	public boolean equals(Object obj)
	{
		return obj instanceof S3Connector other && Objects.equals(client, other.client);
	}

	public static S3ConnectorBuilder create()
	{
		return new S3ConnectorBuilder();
	}

	public void createBucket(BucketDescriptor bucketDescriptor)
	{
		try
		{
			var response = client.createBucket(item -> configureBucket(bucketDescriptor, item));
			response.get(30, TimeUnit.SECONDS);
		}
		catch (InterruptedException x)
		{
			Thread.currentThread().interrupt();
			throw new IllegalStateException(x);
		}
		catch (ExecutionException | TimeoutException x)
		{
			var cause = x.getCause();
			throw cause instanceof RuntimeException runtimeException
				? runtimeException
				: new IllegalStateException(cause);
		}
	}

	public boolean isBucketReadOnly(String bucketName)
	{
		// 1. check for user permissions
		// 2. check for policy
		// 3. check for ACL
		return isBucketPolicyReadOnly(client, bucketName)
			|| isBucketAclReadOnly(client, bucketName);
	}

	public String bucketAcl(String bucketName)
	{
		return Try.to(() -> client.getBucketAcl(item -> item.bucket(bucketName))
								  .thenApply(this::permissions)
								  .exceptionally(ExceptionsHelper::sneakyThrow)
								  .get(30, TimeUnit.SECONDS))
				  .onCatch(ExceptionsHelper::sneakyThrow)
				  .get();
	}

	public BasicFileAttributes objectMetadata(String bucketName, String key)
	{
		return Try.to(() -> client.headObject(item -> item.bucket(bucketName).key(key))
								  .thenApply(item -> S3Object.builder()
															 .key(key)
															 .size(item.contentLength())
															 .lastModified(item.lastModified())
															 .build())
								  .thenApply(ObjectBasicFileAttributes::new)
								  .exceptionally(ExceptionsHelper::sneakyThrow)
								  .get(30, TimeUnit.SECONDS))
				  .onCatch(ExceptionsHelper::sneakyThrow)
				  .get();
	}

	public Map<String, Instant> listObjects(String bucketName, String key)
	{
		return listObjects(bucketName, key, null);
	}

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

	public Iterator<String> scanDirectory(String bucketName, String prefix)
	{
		return new DirectoryIterator(client, bucketName, prefix);
	}

	public byte[] readObject(String bucketName, String key)
	{
		return Try.to(() -> client.getObject(item -> item.bucket(bucketName).key(key),
											 AsyncResponseTransformer.toBytes())
								  .thenApply(BytesWrapper::asByteArray)
								  .exceptionally(ExceptionsHelper::sneakyThrow)
								  .get(30, TimeUnit.SECONDS))
				  .onCatch(ExceptionsHelper::sneakyThrow)
				  .get();
	}

	public byte[] readObject(String bucketName, String key, long from, long to)
	{
		return Try.to(() -> client.getObject(item -> item.bucket(bucketName)
														 .key(key)
														 .range("bytes=%d-%d".formatted(from, to)),
											 AsyncResponseTransformer.toBytes())
								  .thenApply(BytesWrapper::asByteArray)
								  .exceptionally(ExceptionsHelper::sneakyThrow)
								  .get(30, TimeUnit.SECONDS))
				  .onCatch(ExceptionsHelper::sneakyThrow)
				  .get();
	}

	public void writeObject(String bucketName, String key, byte[] content)
	{
		Try.to(() -> client.putObject(item -> item.bucket(bucketName)
												  .key(key)
												  .checksumAlgorithm(ChecksumAlgorithm.SHA256),
									  AsyncRequestBody.fromBytes(content))
						   .exceptionally(ExceptionsHelper::sneakyThrow)
						   .get(30, TimeUnit.SECONDS))
		   .onCatch(ExceptionsHelper::sneakyThrow)
		   .run();
	}

	public MultipartWriter startMultipartUpload(String bucketName, String key)
	{
		var operationRecord = new OperationRecord(client, bucketName, key);
		return new MultipartWriter(operationRecord);
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
		var exception = ExceptionsHelper.toAwsServiceException(throwable);
		var errorCode = exception.awsErrorDetails().errorCode();
		return switch (errorCode)
		{
			case "NoSuchBucketPolicy" -> false;
			default -> throw new IllegalStateException(exception);
		};
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
