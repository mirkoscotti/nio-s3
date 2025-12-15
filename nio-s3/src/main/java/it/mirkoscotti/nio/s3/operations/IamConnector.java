package it.mirkoscotti.nio.s3.operations;

import java.io.Closeable;
import java.io.IOException;
import java.util.Objects;
import java.util.function.Function;

import software.amazon.awssdk.services.iam.IamAsyncClient;
import software.amazon.awssdk.services.iam.IamAsyncClientBuilder;

/**
 * @author mirko.scotti
 * @version Oct 22, 2024
 */
public final class IamConnector
	implements AwsConnector, Closeable
{

	private final IamAsyncClient client;

	IamConnector(IamAsyncClient client)
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
		return obj instanceof IamConnector other && Objects.equals(client, other.client);
	}

	public static IamConnectorBuilder create()
	{
		return new IamConnectorBuilder();
	}

	public static final class IamConnectorBuilder
		extends
		ClientConnectorBuilder<IamConnectorBuilder, IamAsyncClientBuilder, IamConnector, IamAsyncClient>
	{

		IamConnectorBuilder()
		{
			super(IamAsyncClient.builder());
		}

		@Override
		protected Function<IamAsyncClient, IamConnector> connectorCreator()
		{
			return IamConnector::new;
		}

		@Override
		protected IamConnectorBuilder thisBuilder()
		{
			return this;
		}
	}
}
