package it.mirkoscotti.nio.s3.operations;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;

import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;
import jakarta.json.bind.JsonbException;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.OngoingStubbing;

import it.mirkoscotti.nio.s3.configuration.BucketDescriptor;
import it.mirkoscotti.nio.s3.exceptions.TransportException;

import software.amazon.awssdk.awscore.exception.AwsErrorDetails;
import software.amazon.awssdk.crt.CrtRuntimeException;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.S3CrtAsyncClientBuilder;
import software.amazon.awssdk.services.s3.internal.crt.S3CrtAsyncClient;
import software.amazon.awssdk.services.s3.model.CreateBucketResponse;
import software.amazon.awssdk.services.s3.model.GetBucketPolicyResponse;
import software.amazon.awssdk.services.s3.model.S3Exception;

/**
 * @author mirko.scotti
 * @version Oct 22, 2024
 */
@ExtendWith(MockitoExtension.class)
@SuppressWarnings("unchecked")
class S3ConnectorTest
{

	private static final String BUCKET_NAME = "test-bucket";

	private static final String POLICY = "policy";

	@Test
	void closeTest(@Mock S3CrtAsyncClientBuilder builder, @Mock S3CrtAsyncClient client)
	{
		Mockito.when(builder.crossRegionAccessEnabled(Mockito.anyBoolean())).thenReturn(builder);
		Mockito.when(builder.build()).thenReturn(client);
		try (var mock = Mockito.mockStatic(S3AsyncClient.class))
		{
			mock.when(S3AsyncClient::crtBuilder).thenReturn(builder);
			var connector = S3Connector.create().build();
			connector.close();
			Mockito.verify(client, Mockito.atLeastOnce()).close();
		}
		catch (IOException x)
		{
			Assertions.fail(x);
		}
	}

	@Test
	void hashCodeWithSameInstancesTest(@Mock S3CrtAsyncClientBuilder builder,
									   @Mock S3CrtAsyncClient client)
	{
		Mockito.when(builder.crossRegionAccessEnabled(Mockito.anyBoolean())).thenReturn(builder);
		Mockito.when(builder.build()).thenReturn(client);
		try (var mock = Mockito.mockStatic(S3AsyncClient.class))
		{
			mock.when(S3AsyncClient::crtBuilder).thenReturn(builder);
			var connector1 = S3Connector.create().build();
			var connector2 = S3Connector.create().build();
			Assertions.assertEquals(connector1.hashCode(), connector2.hashCode());
		}
	}

	@Test
	void hashCodeWithDifferentInstancesTest(@Mock S3CrtAsyncClientBuilder builder,
											@Mock S3CrtAsyncClient client1,
											@Mock S3CrtAsyncClient client2)
	{
		Mockito.when(builder.crossRegionAccessEnabled(Mockito.anyBoolean())).thenReturn(builder);
		Mockito.when(builder.build()).thenReturn(client1).thenReturn(client2);
		try (var mock = Mockito.mockStatic(S3AsyncClient.class))
		{
			mock.when(S3AsyncClient::crtBuilder).thenReturn(builder);
			var connector1 = S3Connector.create().build();
			var connector2 = S3Connector.create().build();
			Assertions.assertNotEquals(connector1.hashCode(), connector2.hashCode());
		}
	}

	@Test
	void equalsToNullTest(@Mock S3CrtAsyncClientBuilder builder, @Mock S3CrtAsyncClient client)
	{
		Mockito.when(builder.crossRegionAccessEnabled(Mockito.anyBoolean())).thenReturn(builder);
		Mockito.when(builder.build()).thenReturn(client);
		try (var mock = Mockito.mockStatic(S3AsyncClient.class))
		{
			mock.when(S3AsyncClient::crtBuilder).thenReturn(builder);
			var connector = S3Connector.create().build();
			var result = connector.equals(null);
			Assertions.assertFalse(result);
		}
	}

	@Test
	void equalsToDifferentObjectTest(@Mock S3CrtAsyncClientBuilder builder,
									 @Mock S3CrtAsyncClient client,
									 @Mock Object object)
	{
		Mockito.when(builder.crossRegionAccessEnabled(Mockito.anyBoolean())).thenReturn(builder);
		Mockito.when(builder.build()).thenReturn(client);
		try (var mock = Mockito.mockStatic(S3AsyncClient.class))
		{
			mock.when(S3AsyncClient::crtBuilder).thenReturn(builder);
			var connector = S3Connector.create().build();
			var result = connector.equals(object);
			Assertions.assertFalse(result);
		}
	}

	@Test
	void equalsToConnectorWithDifferentClientTest(@Mock S3CrtAsyncClientBuilder builder,
												  @Mock S3CrtAsyncClient client1,
												  @Mock S3CrtAsyncClient client2)
	{
		Mockito.when(builder.crossRegionAccessEnabled(Mockito.anyBoolean())).thenReturn(builder);
		Mockito.when(builder.build()).thenReturn(client1).thenReturn(client2);
		try (var mock = Mockito.mockStatic(S3AsyncClient.class))
		{
			mock.when(S3AsyncClient::crtBuilder).thenReturn(builder);
			var connector1 = S3Connector.create().build();
			var connector2 = S3Connector.create().build();
			var result = connector1.equals(connector2);
			Assertions.assertFalse(result);
		}
	}

	@Test
	void equalsTest(@Mock S3CrtAsyncClientBuilder builder, @Mock S3CrtAsyncClient client)
	{
		Mockito.when(builder.crossRegionAccessEnabled(Mockito.anyBoolean())).thenReturn(builder);
		Mockito.when(builder.build()).thenReturn(client);
		try (var mock = Mockito.mockStatic(S3AsyncClient.class))
		{
			mock.when(S3AsyncClient::crtBuilder).thenReturn(builder);
			var connector1 = S3Connector.create().build();
			var connector2 = S3Connector.create().build();
			var result = connector1.equals(connector2);
			Assertions.assertTrue(result);
		}
	}

	@Test
	void interruptedExceptionWhileCreatingBucketTest(@Mock S3CrtAsyncClientBuilder builder,
													 @Mock S3CrtAsyncClient client,
													 @Mock CreateBucketResponse createBucketResponse,
													 @Mock CompletableFuture<CreateBucketResponse> future,
													 @Mock BucketDescriptor bucketDescriptor)
	{
		Mockito.when(builder.crossRegionAccessEnabled(Mockito.anyBoolean())).thenReturn(builder);
		Mockito.when(builder.build()).thenReturn(client);
		OngoingStubbing<CompletableFuture<CreateBucketResponse>> ongoingStubbing = Mockito.when(client.createBucket(Mockito.any(Consumer.class)));
		ongoingStubbing.thenReturn(future);
		try
		{
			Mockito.doThrow(InterruptedException.class)
				   .when(future)
				   .get(Mockito.anyLong(), Mockito.any(TimeUnit.class));
		}
		catch (Exception x)
		{
			Assertions.fail(x);
		}
		try (var clientMock = Mockito.mockStatic(S3AsyncClient.class))
		{
			clientMock.when(S3AsyncClient::crtBuilder).thenReturn(builder);
			var connector = S3Connector.create().build();
			var exception = Assertions.assertThrows(TransportException.class,
													() -> connector.createBucket(bucketDescriptor));
			Assertions.assertInstanceOf(InterruptedException.class, exception.getCause());
		}
	}

	@Test
	void timeoutExceptionWhileCreatingBucketTest(@Mock S3CrtAsyncClientBuilder builder,
												 @Mock S3CrtAsyncClient client,
												 @Mock CreateBucketResponse createBucketResponse,
												 @Mock CompletableFuture<CreateBucketResponse> future,
												 @Mock BucketDescriptor bucketDescriptor)
	{
		Mockito.when(builder.crossRegionAccessEnabled(Mockito.anyBoolean())).thenReturn(builder);
		Mockito.when(builder.build()).thenReturn(client);
		OngoingStubbing<CompletableFuture<CreateBucketResponse>> ongoingStubbing = Mockito.when(client.createBucket(Mockito.any(Consumer.class)));
		ongoingStubbing.thenReturn(future);
		try
		{
			Mockito.doThrow(TimeoutException.class)
				   .when(future)
				   .get(Mockito.anyLong(), Mockito.any(TimeUnit.class));
		}
		catch (Exception x)
		{
			Assertions.fail(x);
		}
		try (var clientMock = Mockito.mockStatic(S3AsyncClient.class))
		{
			clientMock.when(S3AsyncClient::crtBuilder).thenReturn(builder);
			var connector = S3Connector.create().build();
			Assertions.assertThrows(IllegalStateException.class,
									() -> connector.createBucket(bucketDescriptor));
		}
	}

	@Test
	void executionExceptionWhileCreatingBucketTest(@Mock S3CrtAsyncClientBuilder builder,
												   @Mock S3CrtAsyncClient client,
												   @Mock CreateBucketResponse createBucketResponse,
												   @Mock CompletableFuture<CreateBucketResponse> future,
												   @Mock BucketDescriptor bucketDescriptor)
	{
		Mockito.when(builder.crossRegionAccessEnabled(Mockito.anyBoolean())).thenReturn(builder);
		Mockito.when(builder.build()).thenReturn(client);
		OngoingStubbing<CompletableFuture<CreateBucketResponse>> ongoingStubbing = Mockito.when(client.createBucket(Mockito.any(Consumer.class)));
		ongoingStubbing.thenReturn(future);
		try
		{
			Mockito.doThrow(new ExecutionException(new CrtRuntimeException("Failure")))
				   .when(future)
				   .get(Mockito.anyLong(), Mockito.any(TimeUnit.class));
		}
		catch (Exception x)
		{
			Assertions.fail(x);
		}
		try (var clientMock = Mockito.mockStatic(S3AsyncClient.class))
		{
			clientMock.when(S3AsyncClient::crtBuilder).thenReturn(builder);
			var connector = S3Connector.create().build();
			Assertions.assertThrows(CrtRuntimeException.class,
									() -> connector.createBucket(bucketDescriptor));
		}
	}

	@Test
	void isBucketReadOnlyWhenDeserializationFailedTest(@Mock S3CrtAsyncClientBuilder builder,
													   @Mock S3CrtAsyncClient client,
													   @Mock GetBucketPolicyResponse getBucketPolicyResponse,
													   @Mock Jsonb jsonb)
	{
		Mockito.when(builder.crossRegionAccessEnabled(Mockito.anyBoolean())).thenReturn(builder);
		Mockito.when(builder.build()).thenReturn(client);
		var future = CompletableFuture.<GetBucketPolicyResponse>completedFuture(getBucketPolicyResponse);
		Mockito.when(client.getBucketPolicy(Mockito.any(Consumer.class))).thenReturn(future);
		Mockito.when(getBucketPolicyResponse.policy()).thenReturn(POLICY);
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
