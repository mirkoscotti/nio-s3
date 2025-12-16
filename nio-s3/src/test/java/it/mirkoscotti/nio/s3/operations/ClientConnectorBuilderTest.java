package it.mirkoscotti.nio.s3.operations;

import java.net.URI;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.awscore.AwsClient;
import software.amazon.awssdk.awscore.client.builder.AwsClientBuilder;
import software.amazon.awssdk.regions.Region;

/**
 * @author mirko.scotti
 * @version Dec 15, 2025
 */
@ExtendWith(MockitoExtension.class)
class ClientConnectorBuilderTest
{

	@Mock
	private AwsClientBuilder<?, AwsClient> clientBuilder;

	private ClientConnectorBuilder<?, ?, AwsConnector, AwsClient> builder;

	@BeforeEach
	@SuppressWarnings("unchecked")
	void beforeEach()
	{
		var mockSettings = Mockito.withSettings()
								  .useConstructor(clientBuilder)
								  .defaultAnswer(Mockito.CALLS_REAL_METHODS);
		builder = Mockito.mock(ClientConnectorBuilder.class, mockSettings);
	}

	@Test
	void withEndpointTest(@Mock URI uri)
	{
		builder.withEndpoint(uri);
		Mockito.verify(clientBuilder, Mockito.atLeastOnce()).endpointOverride(uri);
	}

	@Test
	void withRegionTest()
	{
		builder.withRegion("region");
		Mockito.verify(clientBuilder, Mockito.atLeastOnce()).region(Mockito.any(Region.class));
	}

	@Test
	void withCredentialsTest()
	{
		builder.withCredentials("access-key", "secret-key");
		Mockito.verify(clientBuilder, Mockito.atLeastOnce())
			   .credentialsProvider(Mockito.any(AwsCredentialsProvider.class));
	}
}
