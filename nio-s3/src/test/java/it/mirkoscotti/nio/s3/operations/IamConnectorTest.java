package it.mirkoscotti.nio.s3.operations;

import java.io.IOException;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import software.amazon.awssdk.services.iam.IamAsyncClient;
import software.amazon.awssdk.services.iam.IamAsyncClientBuilder;

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
}
