package it.mirkoscotti.nio.s3.operations;

import java.io.Closeable;
import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import it.mirkoscotti.nio.s3.functions.Try;
import it.mirkoscotti.nio.s3.helpers.ExceptionsHelper;

import software.amazon.awssdk.services.sts.StsAsyncClient;
import software.amazon.awssdk.services.sts.StsAsyncClientBuilder;
import software.amazon.awssdk.services.sts.model.GetCallerIdentityResponse;

/**
 * @author mirko.scotti
 * @version Dec 14, 2025
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

	public String arn()
	{
		return Try.to(() -> client.getCallerIdentity()
								  .thenApply(GetCallerIdentityResponse::arn)
								  .get(30, TimeUnit.SECONDS))
				  .onCatch(ExceptionsHelper::sneakyThrow)
				  .get();
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
