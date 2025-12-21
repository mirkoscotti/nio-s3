package it.mirkoscotti.nio.s3.operations;

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
	{
		Mockito.when(builder.build()).thenReturn(client);
		try (var mock = Mockito.mockStatic(IamAsyncClient.class))
		{
			mock.when(IamAsyncClient::builder).thenReturn(builder);
			var connector = IamConnector.create().build();
			connector.close();
			Mockito.verify(client, Mockito.atLeastOnce()).close();
		}
		catch (IOException x)
		{
			Assertions.fail(x);
		}
	}

	@Test
	void hashCodeWithSameInstancesTest(@Mock IamAsyncClientBuilder builder,
									   @Mock IamAsyncClient client)
	{
		Mockito.when(builder.build()).thenReturn(client);
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
		Mockito.when(builder.build()).thenReturn(client1).thenReturn(client2);
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
		Mockito.when(builder.build()).thenReturn(client);
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
		Mockito.when(builder.build()).thenReturn(client);
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
		Mockito.when(builder.build()).thenReturn(client1).thenReturn(client2);
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
		Mockito.when(builder.build()).thenReturn(client);
		@SuppressWarnings("unchecked")
		Consumer<SimulatePrincipalPolicyRequest.Builder> consumer = Mockito.any(Consumer.class);
		Mockito.when(client.simulatePrincipalPolicy(consumer))
			   .thenReturn(CompletableFuture.completedFuture(response));
		Mockito.when(response.evaluationResults()).thenReturn(List.of(result));
		Mockito.when(result.evalDecision()).thenReturn(PolicyEvaluationDecisionType.ALLOWED);
		try (var mock = Mockito.mockStatic(IamAsyncClient.class))
		{
			mock.when(IamAsyncClient::builder).thenReturn(builder);
			var connector = IamConnector.create().build();
			Assertions.assertNull(connector.permissionOnFile("arn", "bucket", "key"));
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
		Mockito.when(builder.build()).thenReturn(client);
		@SuppressWarnings("unchecked")
		Consumer<SimulatePrincipalPolicyRequest.Builder> consumer = Mockito.any(Consumer.class);
		Mockito.when(client.simulatePrincipalPolicy(consumer))
			   .thenReturn(CompletableFuture.completedFuture(response));
		Mockito.when(response.evaluationResults()).thenReturn(List.of(result));
		Mockito.when(result.evalDecision()).thenReturn(PolicyEvaluationDecisionType.ALLOWED);
		try (var mock = Mockito.mockStatic(IamAsyncClient.class))
		{
			mock.when(IamAsyncClient::builder).thenReturn(builder);
			var connector = IamConnector.create().build();
			Assertions.assertNull(connector.permissionOnDirectory("arn", "bucket", "key"));
		}
	}
}
