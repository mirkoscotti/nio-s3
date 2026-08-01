package io.github.mirkoscotti.nio.s3.operations;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import software.amazon.awssdk.services.iam.IamAsyncClient;
import software.amazon.awssdk.services.iam.IamAsyncClientBuilder;
import software.amazon.awssdk.services.iam.model.EvaluationResult;
import software.amazon.awssdk.services.iam.model.PolicyEvaluationDecisionType;
import software.amazon.awssdk.services.iam.model.SimulatePrincipalPolicyRequest;
import software.amazon.awssdk.services.iam.model.SimulatePrincipalPolicyResponse;

/**
 * @author mirko.scotti
 * @version Dec 16, 2025
 */
@ExtendWith(MockitoExtension.class)
class IamConnectorTest
{

	@Test
	void closeTest(@Mock IamAsyncClientBuilder builder, @Mock IamAsyncClient client)
		throws IOException
	{
		when(builder.build()).thenReturn(client);
		try (var mock = Mockito.mockStatic(IamAsyncClient.class))
		{
			mock.when(IamAsyncClient::builder).thenReturn(builder);
			var connector = IamConnector.create().build();
			connector.close();
			verify(client, Mockito.atLeastOnce()).close();
		}
	}

	@Test
	void hashCodeWithSameInstancesTest(@Mock IamAsyncClientBuilder builder,
									   @Mock IamAsyncClient client)
	{
		when(builder.build()).thenReturn(client);
		try (var mock = Mockito.mockStatic(IamAsyncClient.class))
		{
			mock.when(IamAsyncClient::builder).thenReturn(builder);
			var connector1 = IamConnector.create().build();
			var connector2 = IamConnector.create().build();
			Assertions.assertEquals(connector1.hashCode(), connector2.hashCode());
		}
	}

	@Test
	void hashCodeWithDifferentInstancesTest(@Mock IamAsyncClientBuilder builder,
											@Mock IamAsyncClient client1,
											@Mock IamAsyncClient client2)
	{
		when(builder.build()).thenReturn(client1).thenReturn(client2);
		try (var mock = Mockito.mockStatic(IamAsyncClient.class))
		{
			mock.when(IamAsyncClient::builder).thenReturn(builder);
			var connector1 = IamConnector.create().build();
			var connector2 = IamConnector.create().build();
			Assertions.assertNotEquals(connector1.hashCode(), connector2.hashCode());
		}
	}

	@Test
	void equalsToNullTest(@Mock IamAsyncClientBuilder builder, @Mock IamAsyncClient client)
	{
		when(builder.build()).thenReturn(client);
		try (var mock = Mockito.mockStatic(IamAsyncClient.class))
		{
			mock.when(IamAsyncClient::builder).thenReturn(builder);
			var connector = IamConnector.create().build();
			var result = connector.equals(null);
			Assertions.assertFalse(result);
		}
	}

	@Test
	void equalsToDifferentObjectTest(@Mock IamAsyncClientBuilder builder,
									 @Mock IamAsyncClient client,
									 @Mock Object object)
	{
		when(builder.build()).thenReturn(client);
		try (var mock = Mockito.mockStatic(IamAsyncClient.class))
		{
			mock.when(IamAsyncClient::builder).thenReturn(builder);
			var connector = IamConnector.create().build();
			var result = connector.equals(object);
			Assertions.assertFalse(result);
		}
	}

	@Test
	void equalsToConnectorWithDifferentClientTest(@Mock IamAsyncClientBuilder builder,
												  @Mock IamAsyncClient client1,
												  @Mock IamAsyncClient client2)
	{
		when(builder.build()).thenReturn(client1).thenReturn(client2);
		try (var mock = Mockito.mockStatic(IamAsyncClient.class))
		{
			mock.when(IamAsyncClient::builder).thenReturn(builder);
			var connector1 = IamConnector.create().build();
			var connector2 = IamConnector.create().build();
			var result = connector1.equals(connector2);
			Assertions.assertFalse(result);
		}
	}

	@Test
	void equalsTest(@Mock IamAsyncClientBuilder builder, @Mock IamAsyncClient client)
	{
		Mockito.when(builder.build()).thenReturn(client);
		try (var mock = Mockito.mockStatic(IamAsyncClient.class))
		{
			mock.when(IamAsyncClient::builder).thenReturn(builder);
			var connector1 = IamConnector.create().build();
			var connector2 = IamConnector.create().build();
			var result = connector1.equals(connector2);
			Assertions.assertTrue(result);
		}
	}

	/*
	 * TODO: Remove this test once the bug reported here is fixed. It will be replaced by the
	 * integration test in IamConnectorIT currently disabled.
	 * https://github.com/localstack/localstack/issues/13073
	 */
	@Test
	void permissionOnFileTest(@Mock IamAsyncClientBuilder builder,
							  @Mock IamAsyncClient client,
							  @Mock SimulatePrincipalPolicyRequest request,
							  @Mock SimulatePrincipalPolicyResponse response,
							  @Mock EvaluationResult result)
	{
		when(builder.build()).thenReturn(client);
		@SuppressWarnings("unchecked")
		Consumer<SimulatePrincipalPolicyRequest.Builder> consumer = Mockito.any(Consumer.class);
		when(client.simulatePrincipalPolicy(consumer)).thenReturn(CompletableFuture.completedFuture(response));
		when(response.evaluationResults()).thenReturn(List.of(result));
		when(result.evalDecision()).thenReturn(PolicyEvaluationDecisionType.ALLOWED);
		try (var mock = Mockito.mockStatic(IamAsyncClient.class))
		{
			mock.when(IamAsyncClient::builder).thenReturn(builder);
			var connector = IamConnector.create().build();
			Assertions.assertNull(connector.filePermission("arn", "bucket", "key"));
		}
	}

	/*
	 * TODO: Remove this test once the bug reported here is fixed. It will be replaced by the
	 * integration test in IamConnectorIT currently disabled.
	 * https://github.com/localstack/localstack/issues/13073
	 */
	@Test
	void permissionOnDirectoryTest(@Mock IamAsyncClientBuilder builder,
								   @Mock IamAsyncClient client,
								   @Mock SimulatePrincipalPolicyRequest request,
								   @Mock SimulatePrincipalPolicyResponse response,
								   @Mock EvaluationResult result)
	{
		when(builder.build()).thenReturn(client);
		@SuppressWarnings("unchecked")
		Consumer<SimulatePrincipalPolicyRequest.Builder> consumer = Mockito.any(Consumer.class);
		when(client.simulatePrincipalPolicy(consumer)).thenReturn(CompletableFuture.completedFuture(response));
		when(response.evaluationResults()).thenReturn(List.of(result));
		when(result.evalDecision()).thenReturn(PolicyEvaluationDecisionType.ALLOWED);
		try (var mock = Mockito.mockStatic(IamAsyncClient.class))
		{
			mock.when(IamAsyncClient::builder).thenReturn(builder);
			var connector = IamConnector.create().build();
			Assertions.assertNull(connector.directoryPermission("arn", "bucket", "key"));
		}
	}
}
