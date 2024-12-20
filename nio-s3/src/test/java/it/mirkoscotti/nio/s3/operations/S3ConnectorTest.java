/*
 * (C) Copyright 2019 - 2024 - Add Value S.R.L - All rights reserved.
 */

package it.mirkoscotti.nio.s3.operations;

import it.mirkoscotti.nio.s3.helpers.JunitHelper;
import it.mirkoscotti.nio.s3.operations.S3Connector.S3ConnectorBuilder;
import it.mirkoscotti.nio.s3.records.PolicyRecord;

import java.net.URI;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.platform.commons.util.ReflectionUtils;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.S3CrtAsyncClientBuilder;
import software.amazon.awssdk.services.s3.internal.crt.S3CrtAsyncClient;
import software.amazon.awssdk.services.s3.model.GetBucketAclResponse;
import software.amazon.awssdk.services.s3.model.GetBucketPolicyResponse;
import software.amazon.awssdk.services.s3.model.Grant;

/**
 * @author mirko.scotti
 * @version Oct 22, 2024
 */
@ExtendWith(MockitoExtension.class)
class S3ConnectorTest
{

	private static final String BUCKET_NAME = "test-bucket";

	private static final String POLICY = "policy";

	@Test
	void withEndpointTest(@Mock URI uri)
	{
		var builderField = JunitHelper.findFieldByType(S3ConnectorBuilder.class,
													   S3CrtAsyncClientBuilder.class);
		ReflectionUtils.makeAccessible(builderField);
		var testBuilder = S3Connector.create().withEndpoint(uri);
		var awsBuilder = Assertions.assertInstanceOf(S3CrtAsyncClientBuilder.class,
													 JunitHelper.tryCall(() -> builderField.get(testBuilder)));
		var endpointField = JunitHelper.findFieldByName(awsBuilder.getClass(), "endpointOverride");
		ReflectionUtils.makeAccessible(endpointField);
		var endpoint = Assertions.assertInstanceOf(URI.class,
												   JunitHelper.tryCall(() -> endpointField.get(awsBuilder)));
		Assertions.assertEquals(uri, endpoint);
		builderField.setAccessible(false);
		endpointField.setAccessible(false);
	}

	@Test
	void withRegionTest()
	{
		var builderField = JunitHelper.findFieldByType(S3ConnectorBuilder.class,
													   S3CrtAsyncClientBuilder.class);
		ReflectionUtils.makeAccessible(builderField);
		var testBuilder = S3Connector.create().withRegion("us-east-1");
		var awsBuilder = Assertions.assertInstanceOf(S3CrtAsyncClientBuilder.class,
													 JunitHelper.tryCall(() -> builderField.get(testBuilder)));
		var regionField = JunitHelper.findFieldByName(awsBuilder.getClass(), "region");
		ReflectionUtils.makeAccessible(regionField);
		var region = Assertions.assertInstanceOf(Region.class,
												 JunitHelper.tryCall(() -> regionField.get(awsBuilder)));
		Assertions.assertEquals(Region.US_EAST_1, region);
		builderField.setAccessible(false);
		regionField.setAccessible(false);
	}

	@Test
	void withCredentialsTest()
	{
		var builderField = JunitHelper.findFieldByType(S3ConnectorBuilder.class,
													   S3CrtAsyncClientBuilder.class);
		ReflectionUtils.makeAccessible(builderField);
		var accessKey = "access-key";
		var secretKey = "secret-key";
		var testBuilder = S3Connector.create().withCredentials(accessKey, secretKey);
		var awsBuilder = Assertions.assertInstanceOf(S3CrtAsyncClientBuilder.class,
													 JunitHelper.tryCall(() -> builderField.get(testBuilder)));
		var credentialsField = JunitHelper.findFieldByName(awsBuilder.getClass(),
														   "credentialsProvider");
		ReflectionUtils.makeAccessible(credentialsField);
		var awsCredentials = Assertions.assertInstanceOf(AwsCredentialsProvider.class,
														 JunitHelper.tryCall(() -> credentialsField.get(awsBuilder)))
									   .resolveCredentials();
		Assertions.assertEquals(awsCredentials.accessKeyId(), accessKey);
		Assertions.assertEquals(awsCredentials.secretAccessKey(), secretKey);
		builderField.setAccessible(false);
		credentialsField.setAccessible(false);
	}

	@Test
	@SuppressWarnings("unchecked")
	void isBucketPolicyReadOnlyTest(@Mock S3CrtAsyncClientBuilder builder,
									@Mock S3CrtAsyncClient client,
									@Mock GetBucketPolicyResponse policyResponse,
									@Mock Jsonb jsonb,
									@Mock PolicyRecord policyRecord)
	{
		Mockito.when(builder.crossRegionAccessEnabled(Mockito.anyBoolean())).thenReturn(builder);
		Mockito.when(builder.build()).thenReturn(client);
		var futurePolicyResponse = CompletableFuture.<GetBucketPolicyResponse>completedFuture(policyResponse);
		Mockito.when(client.getBucketPolicy(Mockito.any(Consumer.class)))
			   .thenReturn(futurePolicyResponse);
		Mockito.when(policyResponse.policy()).thenReturn(POLICY);
		Mockito.when(jsonb.fromJson(Mockito.anyString(), Mockito.any())).thenReturn(policyRecord);
		Mockito.when(policyRecord.isReadOnly()).thenReturn(true);
		try (var clientMock = Mockito.mockStatic(S3AsyncClient.class);
			 var jsonbMock = Mockito.mockStatic(JsonbBuilder.class))
		{
			clientMock.when(S3AsyncClient::crtBuilder).thenReturn(builder);
			jsonbMock.when(JsonbBuilder::create).thenReturn(jsonb);
			var connector = S3Connector.create().build();
			Assertions.assertTrue(connector.isBucketReadOnly(BUCKET_NAME));
		}
	}

	@Test
	@SuppressWarnings("unchecked")
	void isBucketAclReadOnlyTest(@Mock S3CrtAsyncClientBuilder builder,
								 @Mock S3CrtAsyncClient client,
								 @Mock GetBucketPolicyResponse policyResponse,
								 @Mock GetBucketAclResponse aclResponse,
								 @Mock Jsonb jsonb,
								 @Mock PolicyRecord policyRecord,
								 @Mock Grant grant)
	{
		Mockito.when(builder.crossRegionAccessEnabled(Mockito.anyBoolean())).thenReturn(builder);
		Mockito.when(builder.build()).thenReturn(client);
		var futurePolicyResponse = CompletableFuture.<GetBucketPolicyResponse>completedFuture(policyResponse);
		Mockito.when(client.getBucketPolicy(Mockito.any(Consumer.class)))
			   .thenReturn(futurePolicyResponse);
		Mockito.when(policyResponse.policy()).thenReturn(POLICY);
		var futureAclResponse = CompletableFuture.<GetBucketAclResponse>completedFuture(aclResponse);
		Mockito.when(client.getBucketAcl(Mockito.any(Consumer.class)))
			   .thenReturn(futureAclResponse);
		Mockito.when(aclResponse.grants()).thenReturn(List.of(grant));
		Mockito.when(grant.permissionAsString()).thenReturn("READ_ACP");
		try (var clientMock = Mockito.mockStatic(S3AsyncClient.class);
			 var jsonbMock = Mockito.mockStatic(JsonbBuilder.class))
		{
			clientMock.when(S3AsyncClient::crtBuilder).thenReturn(builder);
			jsonbMock.when(JsonbBuilder::create).thenReturn(jsonb);
			var connector = S3Connector.create().build();
			Assertions.assertTrue(connector.isBucketReadOnly(BUCKET_NAME));
		}
	}

	@Test
	@SuppressWarnings("unchecked")
	void isBucketNotReadOnlyTest(@Mock S3CrtAsyncClientBuilder builder,
								 @Mock S3CrtAsyncClient client,
								 @Mock GetBucketPolicyResponse policyResponse,
								 @Mock GetBucketAclResponse aclResponse,
								 @Mock Jsonb jsonb,
								 @Mock PolicyRecord policyRecord,
								 @Mock Grant grant)
	{
		Mockito.when(builder.crossRegionAccessEnabled(Mockito.anyBoolean())).thenReturn(builder);
		Mockito.when(builder.build()).thenReturn(client);
		var futurePolicyResponse = CompletableFuture.<GetBucketPolicyResponse>completedFuture(policyResponse);
		Mockito.when(client.getBucketPolicy(Mockito.any(Consumer.class)))
			   .thenReturn(futurePolicyResponse);
		Mockito.when(policyResponse.policy()).thenReturn(POLICY);
		var futureAclResponse = CompletableFuture.<GetBucketAclResponse>completedFuture(aclResponse);
		Mockito.when(client.getBucketAcl(Mockito.any(Consumer.class)))
			   .thenReturn(futureAclResponse);
		Mockito.when(aclResponse.grants()).thenReturn(List.of(grant));
		Mockito.when(grant.permissionAsString()).thenReturn("");
		try (var clientMock = Mockito.mockStatic(S3AsyncClient.class);
			 var jsonbMock = Mockito.mockStatic(JsonbBuilder.class))
		{
			clientMock.when(S3AsyncClient::crtBuilder).thenReturn(builder);
			jsonbMock.when(JsonbBuilder::create).thenReturn(jsonb);
			var connector = S3Connector.create().build();
			Assertions.assertFalse(connector.isBucketReadOnly(BUCKET_NAME));
		}
	}

	@Test
	@SuppressWarnings("unchecked")
	void interruptedExceptionOnIsBucketReadOnlyTest(@Mock S3CrtAsyncClientBuilder builder,
													@Mock S3CrtAsyncClient client,
													@Mock GetBucketPolicyResponse policyResponse,
													@Mock Jsonb jsonb)
	{
		Mockito.when(builder.crossRegionAccessEnabled(Mockito.anyBoolean())).thenReturn(builder);
		Mockito.when(builder.build()).thenReturn(client);
		var future = CompletableFuture.<GetBucketPolicyResponse>failedFuture(new InterruptedException());
		Mockito.when(client.getBucketPolicy(Mockito.any(Consumer.class))).thenReturn(future);
		try (var clientMock = Mockito.mockStatic(S3AsyncClient.class))
		{
			clientMock.when(S3AsyncClient::crtBuilder).thenReturn(builder);
			var connector = S3Connector.create().build();
			Assertions.assertThrows(IllegalStateException.class,
									() -> connector.isBucketReadOnly(BUCKET_NAME));
		}
	}
}
