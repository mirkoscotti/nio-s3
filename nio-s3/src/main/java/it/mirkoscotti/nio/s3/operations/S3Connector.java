/*
 * (C) Copyright 2019 - 2024 - Add Value S.R.L - All rights reserved.
 */

package it.mirkoscotti.nio.s3.operations;

import it.mirkoscotti.nio.s3.enums.BucketProperty;
import it.mirkoscotti.nio.s3.functions.Try;
import it.mirkoscotti.nio.s3.records.CredentialsRecord;
import it.mirkoscotti.nio.s3.records.PolicyRecord;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.net.URI;
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
import software.amazon.awssdk.services.s3.model.GetBucketAclResponse;
import software.amazon.awssdk.services.s3.model.GetBucketPolicyResponse;
import software.amazon.awssdk.services.s3.model.Grant;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;
import software.amazon.awssdk.services.s3.model.S3Exception;

/**
 * @author mirko.scotti
 * @version Oct 22, 2024
 */
public final class S3Connector
{

	private static final Logger LOGGER = System.getLogger(S3Connector.class.getName());

	private final S3AsyncClient client;

	private S3Connector(S3AsyncClient client)
	{
		this.client = client;
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
								  .exceptionally(this::guessPermissions)
								  .get(30, TimeUnit.SECONDS))
				  .get();
	}

	public static S3ConnectorBuilder create()
	{
		return new S3ConnectorBuilder();
	}

	private boolean isBucketPolicyReadOnly(S3AsyncClient client, String bucketName)
	{
		return Try.to(() -> client.getBucketPolicy(item -> item.bucket(bucketName))
								  .thenApply(this::isBucketReadOnly)
								  .exceptionally(this::guessReadOnly)
								  .get(30, TimeUnit.SECONDS))
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
		return !toS3Exception(throwable).awsErrorDetails()
										.errorCode()
										.contains("NoSuchBucketPolicy");
	}

	private String permissions(GetBucketAclResponse response)
	{
		return response.grants()
					   .stream()
					   .map(Grant::permissionAsString)
					   .collect(Collectors.joining(";"));
	}

	private <T> T guessPermissions(Throwable throwable)
	{
		var exception = toS3Exception(throwable);
		if (exception instanceof NoSuchBucketException)
		{
			return null;
		}
		throw new IllegalStateException(toS3Exception(throwable));
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
			result = null;
		}
		return result;
	}

	private S3Exception toS3Exception(Throwable throwable)
	{
		var cause = throwable instanceof CompletionException completionException
			? completionException.getCause()
			: throwable;
		return switch (cause)
		{
			case InterruptedException exception -> interrupt(exception);
			case ExecutionException exception -> toCause(exception);
			case TimeoutException exception -> toCause(exception);
			case S3Exception exception -> exception;
			default -> throw new IllegalStateException(throwable);
		};
	}

	private <T> T interrupt(InterruptedException exception)
	{
		Thread.currentThread().interrupt();
		throw new IllegalStateException(exception);
	}

	private <T> T toCause(Exception exception)
	{
		var cause = exception.getCause();
		throw cause instanceof RuntimeException runtimeException
			? runtimeException
			: new IllegalStateException(cause);
	}

	static final class S3ConnectorBuilder
	{

		private final S3CrtAsyncClientBuilder builder = S3AsyncClient.crtBuilder();

		S3ConnectorBuilder()
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
