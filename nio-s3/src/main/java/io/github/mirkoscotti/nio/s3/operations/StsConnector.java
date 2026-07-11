package io.github.mirkoscotti.nio.s3.operations;

import java.io.Closeable;
import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import io.github.mirkoscotti.nio.s3.functions.Try;
import io.github.mirkoscotti.nio.s3.helpers.ExceptionHelper;

import software.amazon.awssdk.services.sts.StsAsyncClient;
import software.amazon.awssdk.services.sts.StsAsyncClientBuilder;
import software.amazon.awssdk.services.sts.model.GetCallerIdentityResponse;

/**
 * Specific connector to STS operations.
 *
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

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void close() throws IOException
	{
		client.close();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int hashCode()
	{
		return client.hashCode();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean equals(Object obj)
	{
		return obj instanceof StsConnector other && Objects.equals(client, other.client);
	}

	/**
	 * Creates a new builder for this connector.
	 *
	 * @return the builder object
	 */
	public static StsConnectorBuilder create()
	{
		return new StsConnectorBuilder();
	}

	/**
	 * Returns the ARN of the caller identity associated with the current credentials.
	 *
	 * @return the caller identity's ARN
	 */
	public String arn()
	{
		return Try.to(() -> client.getCallerIdentity()
								  .thenApply(GetCallerIdentityResponse::arn)
								  .get(30, TimeUnit.SECONDS))
				  .onCatch(ExceptionHelper::sneakyThrow)
				  .get();
	}

	static final class StsConnectorBuilder
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
