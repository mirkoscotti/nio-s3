/*
 * (C) Copyright 2019 - 2024 - Add Value S.R.L - All rights reserved.
 */

package it.mirkoscotti.nio.s3.operations;

import it.mirkoscotti.nio.s3.records.PolicyRecord;

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
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.S3CrtAsyncClientBuilder;
import software.amazon.awssdk.services.s3.internal.crt.S3CrtAsyncClient;
import software.amazon.awssdk.services.s3.model.GetBucketPolicyResponse;

/**
 * @author mirko.scotti
 * @version Oct 22, 2024
 */
@ExtendWith(MockitoExtension.class)
class AwsConnectorTest
{

	private static final String BUCKET_NAME = "test-bucket";

	private static final String POLICY = "policy";

	@Test
	void nullTest()
	{
		Assertions.assertThrows(NullPointerException.class, () -> AwsConnector.createFor(null));
	}

	@Test
	@SuppressWarnings("unchecked")
	void isReadOnlyTest(@Mock PolicyRecord policyRecord)
	{
		var connector = AwsConnector.createFor(BUCKET_NAME);
		var builder = Mockito.mock(S3CrtAsyncClientBuilder.class);
		var client = Mockito.mock(S3CrtAsyncClient.class);
		try (var clientMock = Mockito.mockStatic(S3AsyncClient.class);
			 var jsonbMock = Mockito.mockStatic(JsonbBuilder.class))
		{
			clientMock.when(() -> S3AsyncClient.crtBuilder()).thenReturn(builder);
			Mockito.when(builder.crossRegionAccessEnabled(Mockito.anyBoolean()))
				   .thenReturn(builder);
			Mockito.when(builder.build()).thenReturn(client);
			var response = Mockito.mock(GetBucketPolicyResponse.class);
			var futureResponse = CompletableFuture.<GetBucketPolicyResponse>completedFuture(response);
			Mockito.when(client.getBucketPolicy(Mockito.any(Consumer.class)))
				   .thenReturn(futureResponse);
			Mockito.when(response.policy()).thenReturn(POLICY);
			var jsonb = Mockito.mock(Jsonb.class);
			jsonbMock.when(() -> JsonbBuilder.create()).thenReturn(jsonb);
			Mockito.when(jsonb.fromJson(Mockito.anyString(), Mockito.any())).thenReturn(builder);
			Mockito.when(jsonb.fromJson(Mockito.anyString(), Mockito.any()))
				   .thenReturn(policyRecord);
			connector.isBucketReadOnly();
			Mockito.verify(client, Mockito.atLeastOnce())
				   .getBucketPolicy(Mockito.any(Consumer.class));
		}
	}
}
