package io.github.mirkoscotti.nio.s3.operations;

import java.io.IOException;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import software.amazon.awssdk.services.sts.StsAsyncClient;
import software.amazon.awssdk.services.sts.StsAsyncClientBuilder;

/**
 * @author mirko.scotti
 * @version Dec 16, 2025
 */
@ExtendWith(MockitoExtension.class)
class StsConnectorTest
{

	@Test
	void closeTest(@Mock StsAsyncClientBuilder builder, @Mock StsAsyncClient client)
	{
		Mockito.when(builder.build()).thenReturn(client);
		try (var mock = Mockito.mockStatic(StsAsyncClient.class))
		{
			mock.when(StsAsyncClient::builder).thenReturn(builder);
			var connector = StsConnector.create().build();
			connector.close();
			Mockito.verify(client, Mockito.atLeastOnce()).close();
		}
		catch (IOException x)
		{
			Assertions.fail(x);
		}
	}

	@Test
	void hashCodeWithSameInstancesTest(@Mock StsAsyncClientBuilder builder,
									   @Mock StsAsyncClient client)
	{
		Mockito.when(builder.build()).thenReturn(client);
		try (var mock = Mockito.mockStatic(StsAsyncClient.class))
		{
			mock.when(StsAsyncClient::builder).thenReturn(builder);
			var connector1 = StsConnector.create().build();
			var connector2 = StsConnector.create().build();
			Assertions.assertEquals(connector1.hashCode(), connector2.hashCode());
		}
	}

	@Test
	void hashCodeWithDifferentInstancesTest(@Mock StsAsyncClientBuilder builder,
											@Mock StsAsyncClient client1,
											@Mock StsAsyncClient client2)
	{
		Mockito.when(builder.build()).thenReturn(client1).thenReturn(client2);
		try (var mock = Mockito.mockStatic(StsAsyncClient.class))
		{
			mock.when(StsAsyncClient::builder).thenReturn(builder);
			var connector1 = StsConnector.create().build();
			var connector2 = StsConnector.create().build();
			Assertions.assertNotEquals(connector1.hashCode(), connector2.hashCode());
		}
	}

	@Test
	void equalsToNullTest(@Mock StsAsyncClientBuilder builder, @Mock StsAsyncClient client)
	{
		Mockito.when(builder.build()).thenReturn(client);
		try (var mock = Mockito.mockStatic(StsAsyncClient.class))
		{
			mock.when(StsAsyncClient::builder).thenReturn(builder);
			var connector = StsConnector.create().build();
			var result = connector.equals(null);
			Assertions.assertFalse(result);
		}
	}

	@Test
	void equalsToDifferentObjectTest(@Mock StsAsyncClientBuilder builder,
									 @Mock StsAsyncClient client,
									 @Mock Object object)
	{
		Mockito.when(builder.build()).thenReturn(client);
		try (var mock = Mockito.mockStatic(StsAsyncClient.class))
		{
			mock.when(StsAsyncClient::builder).thenReturn(builder);
			var connector = StsConnector.create().build();
			var result = connector.equals(object);
			Assertions.assertFalse(result);
		}
	}

	@Test
	void equalsToConnectorWithDifferentClientTest(@Mock StsAsyncClientBuilder builder,
												  @Mock StsAsyncClient client1,
												  @Mock StsAsyncClient client2)
	{
		Mockito.when(builder.build()).thenReturn(client1).thenReturn(client2);
		try (var mock = Mockito.mockStatic(StsAsyncClient.class))
		{
			mock.when(StsAsyncClient::builder).thenReturn(builder);
			var connector1 = StsConnector.create().build();
			var connector2 = StsConnector.create().build();
			var result = connector1.equals(connector2);
			Assertions.assertFalse(result);
		}
	}

	@Test
	void equalsTest(@Mock StsAsyncClientBuilder builder, @Mock StsAsyncClient client)
	{
		Mockito.when(builder.build()).thenReturn(client);
		try (var mock = Mockito.mockStatic(StsAsyncClient.class))
		{
			mock.when(StsAsyncClient::builder).thenReturn(builder);
			var connector1 = StsConnector.create().build();
			var connector2 = StsConnector.create().build();
			var result = connector1.equals(connector2);
			Assertions.assertTrue(result);
		}
	}
}
