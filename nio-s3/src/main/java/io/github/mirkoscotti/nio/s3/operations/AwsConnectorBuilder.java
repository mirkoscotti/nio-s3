package io.github.mirkoscotti.nio.s3.operations;

import java.net.URI;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

import io.github.mirkoscotti.nio.s3.records.CredentialsRecord;

import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.awscore.AwsClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.utils.builder.SdkBuilder;

/**
 * @author mirko.scotti
 * @version Dec 13, 2025
 */
abstract class AwsConnectorBuilder<A extends AwsConnectorBuilder<A, B, C, W>,
								   B extends SdkBuilder<B, W>,
								   C extends AwsConnector,
								   W extends AwsClient>
	implements ConnectorBuilder<A, C>
{

	private final B builder;

	protected AwsConnectorBuilder(B builder)
	{
		this.builder = Objects.requireNonNull(builder, () -> "Missing connector builder.");
	}

	@Override
	public final A withEndpoint(URI endpoint)
	{
		Optional.ofNullable(endpoint).ifPresent(item -> endpointOverride(builder, item));
		return thisBuilder();
	}

	@Override
	public final A withRegion(String region)
	{
		Optional.ofNullable(region).map(Region::of).ifPresent(item -> region(builder, item));
		return thisBuilder();
	}

	@Override
	public final A withCredentials(String accessKey, String secretKey)
	{
		Optional.ofNullable(accessKey)
				.map(item -> new CredentialsRecord(item, secretKey))
				.flatMap(item -> Optional.<AwsCredentialsProvider>of(item::awsCredentials))
				.ifPresent(item -> credentialsProvider(builder, item));
		return thisBuilder();
	}

	@Override
	public final C build()
	{
		return connectorCreator().apply(builder.build());
	}

	protected abstract void endpointOverride(B builder, URI uri);

	protected abstract void region(B builder, Region region);

	protected abstract void credentialsProvider(B builder, AwsCredentialsProvider provider);

	protected abstract Function<W, C> connectorCreator();

	protected abstract A thisBuilder();
}
