package io.github.mirkoscotti.nio.s3.operations;

import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.util.function.Function;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.creation.instance.InstantiationException;
import org.mockito.exceptions.base.MockitoException;
import org.mockito.junit.jupiter.MockitoExtension;

import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.awscore.AwsClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.utils.builder.SdkBuilder;

/**
 * @author mirko.scotti
 * @version Dec 15, 2025
 */
@ExtendWith(MockitoExtension.class)
class AwsConnectorBuilderTest
{

	@Test
	void nullTest()
	{
		var mockSettings = Mockito.withSettings()
								  .useConstructor((SdkBuilder<?, AwsClient>) null)
								  .defaultAnswer(Mockito.CALLS_REAL_METHODS);
		Exception exception = Assertions.assertThrows(MockitoException.class,
													  () -> Mockito.mock(AwsConnectorBuilder.class,
																		 mockSettings));
		exception = Assertions.assertInstanceOf(InstantiationException.class, exception.getCause());
		exception = Assertions.assertInstanceOf(InvocationTargetException.class,
												exception.getCause());
		Assertions.assertInstanceOf(NullPointerException.class, exception.getCause());
	}

	@Test
	@SuppressWarnings("unchecked")
	void withEndpointTest(@Mock SdkBuilder<?, AwsClient> sdkBuilder, @Mock URI uri)
	{
		var mockSettings = Mockito.withSettings()
								  .useConstructor(sdkBuilder)
								  .defaultAnswer(Mockito.CALLS_REAL_METHODS);
		var builder = Mockito.mock(AwsConnectorBuilder.class, mockSettings);
		builder.withEndpoint(uri);
		Mockito.verify(builder, Mockito.atLeastOnce()).endpointOverride(sdkBuilder, uri);
		Mockito.verify(builder, Mockito.atLeastOnce()).thisBuilder();
	}

	@Test
	@SuppressWarnings("unchecked")
	void withRegionTest(@Mock SdkBuilder<?, AwsClient> sdkBuilder)
	{
		var mockSettings = Mockito.withSettings()
								  .useConstructor(sdkBuilder)
								  .defaultAnswer(Mockito.CALLS_REAL_METHODS);
		var builder = Mockito.mock(AwsConnectorBuilder.class, mockSettings);
		builder.withRegion("region");
		Mockito.verify(builder, Mockito.atLeastOnce())
			   .region(Mockito.any(SdkBuilder.class), Mockito.any(Region.class));
		Mockito.verify(builder, Mockito.atLeastOnce()).thisBuilder();
	}

	@Test
	@SuppressWarnings("unchecked")
	void withCredentialsTest(@Mock SdkBuilder<?, AwsClient> sdkBuilder)
	{
		var mockSettings = Mockito.withSettings()
								  .useConstructor(sdkBuilder)
								  .defaultAnswer(Mockito.CALLS_REAL_METHODS);
		var builder = Mockito.mock(AwsConnectorBuilder.class, mockSettings);
		builder.withCredentials("access-key", "secret-key");
		Mockito.verify(builder, Mockito.atLeastOnce())
			   .credentialsProvider(Mockito.any(SdkBuilder.class),
									Mockito.any(AwsCredentialsProvider.class));
		Mockito.verify(builder, Mockito.atLeastOnce()).thisBuilder();
	}

	@Test
	void buildTest(@Mock SdkBuilder<?, AwsClient> sdkBuilder,
				   @Mock Function<AwsClient, AwsConnector> connectorCreator,
				   @Mock AwsClient awsClient)
	{
		var mockSettings = Mockito.withSettings()
								  .useConstructor(sdkBuilder)
								  .defaultAnswer(Mockito.CALLS_REAL_METHODS);
		var builder = Mockito.mock(AwsConnectorBuilder.class, mockSettings);
		Mockito.when(builder.connectorCreator()).thenReturn(connectorCreator);
		Mockito.when(sdkBuilder.build()).thenReturn(awsClient);
		builder.build();
		Mockito.verify(builder, Mockito.atLeastOnce()).connectorCreator();
		Mockito.verify(connectorCreator, Mockito.atLeastOnce()).apply(Mockito.any(AwsClient.class));
	}
}
