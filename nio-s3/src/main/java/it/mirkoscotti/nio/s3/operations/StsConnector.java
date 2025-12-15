package it.mirkoscotti.nio.s3.operations;

import java.io.Closeable;
import java.io.IOException;
import java.util.Objects;
import java.util.function.Function;

import software.amazon.awssdk.services.sts.StsAsyncClient;
import software.amazon.awssdk.services.sts.StsAsyncClientBuilder;

/**
 * @author mirko.scotti
 * @version Oct 22, 2024
 */
public final class StsConnector
	implements AwsConnector, Closeable
{

	private final StsAsyncClient client;

	StsConnector(StsAsyncClient client)
	{
		this.client = client;
	}

	@Override
	public void close() throws IOException
	{
		client.close();
	}

	@Override
	public int hashCode()
	{
		return client.hashCode();
	}

	@Override
	public boolean equals(Object obj)
	{
		return obj instanceof StsConnector other && Objects.equals(client, other.client);
	}

	public static StsConnectorBuilder create()
	{
		return new StsConnectorBuilder();
	}

	public static final class StsConnectorBuilder
		extends
		ClientConnectorBuilder<StsConnectorBuilder, StsAsyncClientBuilder, StsConnector, StsAsyncClient>
	{

		StsConnectorBuilder()
		{
			super(StsAsyncClient.builder());
		}

		@Override
		protected Function<StsAsyncClient, StsConnector> connectorCreator()
		{
			return StsConnector::new;
		}

		@Override
		protected StsConnectorBuilder thisBuilder()
		{
			return this;
		}
	}
}
