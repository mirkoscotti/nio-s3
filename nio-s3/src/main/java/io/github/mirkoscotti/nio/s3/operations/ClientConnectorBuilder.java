package io.github.mirkoscotti.nio.s3.operations;

import java.net.URI;
import java.util.function.Function;

import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.awscore.AwsClient;
import software.amazon.awssdk.awscore.client.builder.AwsClientBuilder;
import software.amazon.awssdk.regions.Region;

/**
 * @author mirko.scotti
 * @version Dec 14, 2025
 */
abstract class ClientConnectorBuilder<A extends ClientConnectorBuilder<A, B, C, W>,
									  B extends AwsClientBuilder<B, W>,
									  C extends AwsConnector,
									  W extends AwsClient>
	extends AwsConnectorBuilder<A, B, C, W>
{

	ClientConnectorBuilder(B builder)
	{
		super(builder);
	}

	@Override
	protected final void endpointOverride(B builder, URI uri)
	{
		builder.endpointOverride(uri);
	}

	@Override
	protected final void region(B builder, Region region)
	{
		builder.region(region);
	}

	@Override
	protected final void credentialsProvider(B builder, AwsCredentialsProvider provider)
	{
		builder.credentialsProvider(provider);
	}

	@Override
	protected abstract Function<W, C> connectorCreator();
}
