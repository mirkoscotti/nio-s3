/*
 * (C) Copyright 2019 - 2024 - Add Value S.R.L - All rights reserved.
 */

package it.mirkoscotti.nio.s3.operations;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;
import jakarta.json.bind.JsonbException;
import software.amazon.awssdk.awscore.exception.AwsErrorDetails;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.S3CrtAsyncClientBuilder;
import software.amazon.awssdk.services.s3.internal.crt.S3CrtAsyncClient;
import software.amazon.awssdk.services.s3.model.GetBucketPolicyResponse;
import software.amazon.awssdk.services.s3.model.S3Exception;

/**
 * @author mirko.scotti
 * @version Oct 22, 2024
 */
@ExtendWith(MockitoExtension.class)
@SuppressWarnings("unchecked")
class S3ConnectorUnitTest
{

	private static final String BUCKET_NAME = "test-bucket";

	private static final String POLICY = "policy";

	@Test
	void isBucketReadOnlyWhenDeserializationFailedTest(@Mock S3CrtAsyncClientBuilder builder,
													   @Mock S3CrtAsyncClient client,
													   @Mock GetBucketPolicyResponse policyResponse,
													   @Mock Jsonb jsonb)
	{
		Mockito.when(builder.crossRegionAccessEnabled(Mockito.anyBoolean())).thenReturn(builder);
		Mockito.when(builder.build()).thenReturn(client);
		var future = CompletableFuture.<GetBucketPolicyResponse>completedFuture(policyResponse);
		Mockito.when(client.getBucketPolicy(Mockito.any(Consumer.class))).thenReturn(future);
		Mockito.when(policyResponse.policy()).thenReturn(POLICY);
		Mockito.when(jsonb.fromJson(Mockito.anyString(), Mockito.any()))
			   .thenThrow(JsonbException.class);
		try (var clientMock = Mockito.mockStatic(S3AsyncClient.class);
			 var jsonbMock = Mockito.mockStatic(JsonbBuilder.class))
		{
			clientMock.when(S3AsyncClient::crtBuilder).thenReturn(builder);
			jsonbMock.when(JsonbBuilder::create).thenReturn(jsonb);
			var connector = S3Connector.create().build();
			Assertions.assertThrows(IllegalStateException.class,
									() -> connector.isBucketReadOnly(BUCKET_NAME));
		}
	}

	@Test
	void isBucketReadOnlyOnS3ExceptionTest(@Mock S3CrtAsyncClientBuilder builder,
										   @Mock S3CrtAsyncClient client,
										   @Mock AwsErrorDetails awsErrorDetails,
										   @Mock S3Exception s3Exception)
	{
		Mockito.when(builder.crossRegionAccessEnabled(Mockito.anyBoolean())).thenReturn(builder);
		Mockito.when(builder.build()).thenReturn(client);
		Mockito.when(s3Exception.awsErrorDetails()).thenReturn(awsErrorDetails);
		Mockito.when(awsErrorDetails.errorCode()).thenReturn("Some Error");
		var future = new CompletableFuture<GetBucketPolicyResponse>();
		future.completeExceptionally(s3Exception);
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
