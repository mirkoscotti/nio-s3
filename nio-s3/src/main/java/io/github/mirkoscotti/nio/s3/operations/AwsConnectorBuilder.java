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
 * Template based on AWS root SDK builders.
 *
 * @author mirko.scotti
 * @since Dec 13, 2025
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

	/**
	 * Overrides the default AWS service endpoint with the given URI. This typically occurs during
	 * tests when possibly Localstack is accessed instead of AWS.
	 *
	 * @param endpoint
	 *            the custom endpoint URI
	 * @return this builder
	 */
	@Override
	public final A withEndpoint(URI endpoint)
	{
		Optional.ofNullable(endpoint).ifPresent(item -> endpointOverride(builder, item));
		return thisBuilder();
	}

	/**
	 * Overrides the AWS region for the service client. Default is <code>us-east-1</code>
	 *
	 * @param region
	 *            the AWS region identifier
	 * @return this builder instance for fluent chaining
	 */
	@Override
	public final A withRegion(String region)
	{
		Optional.ofNullable(region).map(Region::of).ifPresent(item -> region(builder, item));
		return thisBuilder();
	}

	/**
	 * Static AWS client credentials configuration.
	 *
	 * @param accessKey
	 *            the AWS access key identifier
	 * @param secretKey
	 *            the AWS secret access key
	 * @return this builder
	 */
	@Override
	public final A withCredentials(String accessKey, String secretKey)
	{
		Optional.ofNullable(accessKey)
				.map(item -> new CredentialsRecord(item, secretKey))
				.flatMap(item -> Optional.<AwsCredentialsProvider>of(item::awsCredentials))
				.ifPresent(item -> credentialsProvider(builder, item));
		return thisBuilder();
	}

	/**
	 * Creates an instance of the connector according to the <code>with</code> methods
	 * configuration.
	 *
	 * @return the connector instance
	 */
	@Override
	public final C build()
	{
		return connectorCreator().apply(builder.build());
	}

	/**
	 * Custom endpoint configuration.
	 *
	 * @param builder
	 *            the AWS client builder
	 * @param uri
	 *            the custom URI
	 */
	protected abstract void endpointOverride(B builder, URI uri);

	/**
	 * Custom region configuration.
	 *
	 * @param builder
	 *            the AWS client builder
	 * @param region
	 *            the custom AWS region
	 */
	protected abstract void region(B builder, Region region);

	/**
	 * Credentials to access AWS resources
	 *
	 * @param builder
	 *            the AWS client builder
	 * @param provider
	 *            the AWS credentials provider
	 */
	protected abstract void credentialsProvider(B builder, AwsCredentialsProvider provider);

	/**
	 * The connector instance provider.
	 *
	 * @return the connector instance
	 */
	protected abstract Function<W, C> connectorCreator();

	/**
	 * This builder.
	 *
	 * @return this builder
	 */
	protected abstract A thisBuilder();
}
