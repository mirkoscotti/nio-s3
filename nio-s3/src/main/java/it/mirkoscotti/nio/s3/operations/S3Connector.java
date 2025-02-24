package it.mirkoscotti.nio.s3.operations;

import it.mirkoscotti.nio.s3.configuration.BucketDescriptor;
import it.mirkoscotti.nio.s3.enums.BucketProperty;
import it.mirkoscotti.nio.s3.extensions.jdk.jsr203.ObjectBasicFileAttributes;
import it.mirkoscotti.nio.s3.functions.Try;
import it.mirkoscotti.nio.s3.records.CredentialsRecord;
import it.mirkoscotti.nio.s3.records.PolicyRecord;

import java.io.Closeable;
import java.io.IOException;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.net.URI;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import jakarta.json.bind.JsonbBuilder;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.S3CrtAsyncClientBuilder;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest.Builder;
import software.amazon.awssdk.services.s3.model.GetBucketAclResponse;
import software.amazon.awssdk.services.s3.model.GetBucketPolicyResponse;
import software.amazon.awssdk.services.s3.model.Grant;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.model.S3Object;

/**
 * @author mirko.scotti
 * @version Oct 22, 2024
 */
public final class S3Connector
	implements Closeable
{

	private static final Logger LOGGER = System.getLogger(S3Connector.class.getName());

	private final S3AsyncClient client;

	private S3Connector(S3AsyncClient client)
	{
		this.client = client;
	}

	@Override
	public void close() throws IOException
	{
		client.close();
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
		return isBucketPolicyReadOnly(client, bucketName)
			|| isBucketAclReadOnly(client, bucketName);
	}

	public String bucketAcl(String bucketName)
	{
		return Try.to(() -> client.getBucketAcl(item -> item.bucket(bucketName))
								  .thenApply(this::permissions)
								  .exceptionally(this::redirectException)
								  .get(30, TimeUnit.SECONDS))
				  .onCatch(this::redirectException)
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
								  .exceptionally(this::redirectException)
								  .get(30, TimeUnit.SECONDS))
				  .onCatch(this::redirectException)
				  .get();
	}

	public static S3ConnectorBuilder create()
	{
		return new S3ConnectorBuilder();
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
		var exception = toS3Exception(throwable);
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

	private <T> T redirectException(Throwable throwable)
	{
		var exception = toS3Exception(throwable);
		throw new IllegalStateException(exception);
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

	private S3Exception toS3Exception(Throwable throwable)
	{
		return switch (throwable)
		{
			case S3Exception exception -> exception;
			case CompletionException exception -> toS3Exception(exception.getCause());
			case RuntimeException exception -> throw exception;
			default -> throw new IllegalStateException(throwable);
		};
	}

	public static final class S3ConnectorBuilder
	{

		private final S3CrtAsyncClientBuilder builder = S3AsyncClient.crtBuilder();

		private S3ConnectorBuilder()
		{
			builder.crossRegionAccessEnabled(true);
		}

		public S3ConnectorBuilder withEndpoint(URI endpoint)
		{
			Optional.ofNullable(endpoint).ifPresent(builder::endpointOverride);
			return this;
		}

		public S3ConnectorBuilder withRegion(String region)
		{
			Optional.ofNullable(region).map(Region::of).ifPresent(builder::region);
			return this;
		}

		public S3ConnectorBuilder withCredentials(String accessKey, String secretKey)
		{
			Optional.ofNullable(accessKey)
					.map(item -> new CredentialsRecord(item, secretKey))
					.flatMap(item -> Optional.<AwsCredentialsProvider>of(item::awsCredentials))
					.ifPresent(builder::credentialsProvider);
			return this;
		}

		public S3Connector build()
		{
			return new S3Connector(builder.build());
		}
	}
}
