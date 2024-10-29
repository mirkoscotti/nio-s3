/*
 * (C) Copyright 2019 - 2024 - Add Value S.R.L - All rights reserved.
 */

package it.mirkoscotti.nio.s3.operations;

import it.mirkoscotti.nio.s3.records.CredentialsRecord;
import it.mirkoscotti.nio.s3.records.PolicyRecord;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.net.URI;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import jakarta.json.bind.JsonbBuilder;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.awscore.exception.AwsErrorDetails;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.GetBucketPolicyResponse;
import software.amazon.awssdk.services.s3.model.S3Exception;

/**
 * @author mirko.scotti
 * @version Oct 22, 2024
 */
public final class AwsConnector
{

	private static final Logger LOGGER = System.getLogger(AwsConnector.class.getName());

	private final String bucketName;

	private Optional<URI> endpoint = Optional.empty();

	private Optional<Region> region = Optional.empty();

	private Optional<AwsCredentialsProvider> credentials = Optional.empty();

	/**
	 * @param bucketName
	 */
	private AwsConnector(String bucketName)
	{
		this.bucketName = bucketName;
	}

	public AwsConnector withEndpoint(URI endpoint)
	{
		this.endpoint = Optional.ofNullable(endpoint);
		return this;
	}

	public AwsConnector withRegion(String region)
	{
		this.region = Optional.ofNullable(region).map(Region::of);
		return this;
	}

	public AwsConnector withCredentials(String accessKey, String secretKey)
	{
		var credentialsRecord = new CredentialsRecord(accessKey, secretKey);
		credentials = Optional.<AwsCredentialsProvider>of(credentialsRecord::awsCredentials);
		return this;
	}

	public boolean isBucketReadOnly()
	{
		try (var client = createClient())
		{
			return client.getBucketPolicy(item -> item.bucket(bucketName))
						 .thenApply(GetBucketPolicyResponse::policy)
						 .thenApply(this::isBucketReadOnly)
						 .exceptionally(this::checkMissingPolicy)
						 .get(30, TimeUnit.SECONDS);
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

	public static AwsConnector createFor(String bucketName)
	{
		Objects.requireNonNull(bucketName, () -> "Missing bucket name.");
		return new AwsConnector(bucketName);
	}

	private S3AsyncClient createClient()
	{
		var result = S3AsyncClient.crtBuilder().crossRegionAccessEnabled(true);
		endpoint.ifPresent(result::endpointOverride);
		region.ifPresent(result::region);
		credentials.ifPresent(result::credentialsProvider);
		return result.build();
	}

	private boolean isBucketReadOnly(String policyResponse)
	{
		var policy = deserializePolicy(policyResponse);
		return Optional.ofNullable(policy).map(PolicyRecord::isReadOnly).orElse(false);
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

	private boolean checkMissingPolicy(Throwable throwable)
	{
		return Optional.of(throwable)
					   .map(Throwable::getCause)
					   .filter(S3Exception.class::isInstance)
					   .map(S3Exception.class::cast)
					   .map(S3Exception::awsErrorDetails)
					   .map(AwsErrorDetails::errorCode)
					   .filter(errorCode -> errorCode.contains("NoSuchBucketPolicy"))
					   .map(errorCode -> false)
					   .orElseThrow(() -> new IllegalStateException(throwable));
	}
}
