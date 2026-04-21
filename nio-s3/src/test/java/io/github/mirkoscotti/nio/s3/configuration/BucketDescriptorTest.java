package io.github.mirkoscotti.nio.s3.configuration;

import java.net.URI;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedConstruction.Context;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import io.github.mirkoscotti.nio.s3.enums.BucketProperty;
import io.github.mirkoscotti.nio.s3.exceptions.BucketNameException;
import io.github.mirkoscotti.nio.s3.records.CredentialsRecord;

import software.amazon.awssdk.regions.Region;

/**
 * @author mirko.scotti
 * @version Mar 09, 2025
 */
@ExtendWith(MockitoExtension.class)
class BucketDescriptorTest
{

	private static final String BUCKET_NAME = "test-bucket";

	private static final String ACCESS_KEY = "access-key";

	private static final String SECRET_KEY = "secret-key";

	private static final String REGION = "us-east-1";

	@Mock
	private URI uri;

	@Test
	void wrongBucketNameTest()
	{
		Stream.of("adjacent..periods",
				  "127.0.0.1",
				  "-bucket-name",
				  "xn--bucket-name",
				  "sthree-bucket-name",
				  "bucket-name-s3alias",
				  "bucket-name--ol-s3",
				  "xy",
				  "bucket-name-too-looooooooooooooooooooooooooooooooooooooooooooooong")
			  .forEach(this::wrongBucketName);
	}

	@Test
	void uriWithOnlyBucketNameTest()
	{
		Mockito.when(uri.getHost()).thenReturn(BUCKET_NAME);
		try (var mock = Mockito.mockConstruction(UriDescriptor.class,
												 this::configureOnlyBucketName))
		{
			Assertions.assertThrows(NullPointerException.class, () -> new BucketDescriptor(uri));
		}
	}

	@Test
	void uriWithBucketNameAndCredentialsTest()
	{
		Mockito.when(uri.getHost()).thenReturn(BUCKET_NAME);
		try (var mock = Mockito.mockConstruction(UriDescriptor.class,
												 this::configureBucketNameAndCredentials))
		{
			var bucketDescriptor = new BucketDescriptor(uri);
			var bucketKey = bucketDescriptor.bucketKey();
			Assertions.assertEquals("https://s3.us-east-1.amazonaws.com",
									bucketKey.endpoint().get());
			Assertions.assertEquals(BUCKET_NAME, bucketKey.bucketName());
			var connectorKey = bucketDescriptor.connectorKey();
			var credentials = connectorKey.credentials();
			Assertions.assertEquals(ACCESS_KEY, credentials.accessKey());
			Assertions.assertEquals(SECRET_KEY, credentials.secretKey());
		}
	}

	@Test
	void configurationWithRegionTest()
	{
		Mockito.when(uri.getHost()).thenReturn(BUCKET_NAME);
		try (var mock = Mockito.mockConstruction(UriDescriptor.class,
												 this::configureEmptyCredentials))
		{
			var map = Map.of("aws.region", REGION);
			var bucketDescriptor = new BucketDescriptor(uri, map);
			var connectorKey = bucketDescriptor.connectorKey();
			var region = connectorKey.region();
			Assertions.assertTrue(region.isPresent());
			Assertions.assertEquals(Region.of(REGION), region.get());
		}
	}

	@Test
	void configurationWithCredentialsTest()
	{
		Mockito.when(uri.getHost()).thenReturn(BUCKET_NAME);
		try (var mock = Mockito.mockConstruction(UriDescriptor.class,
												 this::configureOnlyBucketName))
		{
			var map = Map.of("aws.access-key", ACCESS_KEY, "aws.secret-key", SECRET_KEY);
			var bucketDescriptor = new BucketDescriptor(uri, map);
			var connectorKey = bucketDescriptor.connectorKey();
			var credentials = connectorKey.credentials();
			Assertions.assertEquals(ACCESS_KEY, credentials.accessKey());
			Assertions.assertEquals(SECRET_KEY, credentials.secretKey());
			var configuration = bucketDescriptor.configuration();
			Assertions.assertFalse(configuration.containsKey(BucketProperty.ACCESS_KEY));
			Assertions.assertFalse(configuration.containsKey(BucketProperty.SECRET_KEY));
		}
	}

	@Test
	void configurationTest()
	{
		Mockito.when(uri.getHost()).thenReturn(BUCKET_NAME);
		try (var mock = Mockito.mockConstruction(UriDescriptor.class,
												 this::configureEmptyCredentials))
		{
			var map = Map.of("aws.region", REGION);
			var bucketDescriptor = new BucketDescriptor(uri, map);
			var configuration = bucketDescriptor.configuration();
			Assertions.assertEquals(REGION, configuration.get(BucketProperty.REGION));
		}
	}

	void wrongBucketName(String bucketName)
	{
		Mockito.when(uri.getHost()).thenReturn(bucketName);
		try (var mock = Mockito.mockConstruction(UriDescriptor.class,
												 this::configureOnlyBucketName))
		{
			Assertions.assertThrows(BucketNameException.class, () -> new BucketDescriptor(uri));
		}
	}

	private void configureOnlyBucketName(UriDescriptor uriDescriptor, Context context)
	{
		configureBucketName(uriDescriptor, context.arguments().get(0));
		Mockito.when(uriDescriptor.credentials()).thenReturn(Optional.empty());
	}

	private void configureEmptyCredentials(UriDescriptor uriDescriptor, Context context)
	{
		configureBucketName(uriDescriptor, context.arguments().get(0));
		var credentials = Mockito.mock(CredentialsRecord.class);
		Mockito.when(uriDescriptor.credentials()).thenReturn(Optional.of(credentials));
	}

	private void configureBucketNameAndCredentials(UriDescriptor uriDescriptor, Context context)
	{
		configureBucketName(uriDescriptor, context.arguments().get(0));
		var credentials = Mockito.mock(CredentialsRecord.class);
		Mockito.when(credentials.accessKey()).thenReturn(ACCESS_KEY);
		Mockito.when(credentials.secretKey()).thenReturn(SECRET_KEY);
		Mockito.when(uriDescriptor.credentials()).thenReturn(Optional.of(credentials));
	}

	private void configureBucketName(UriDescriptor uriDescriptor, Object uri)
	{
		Optional.ofNullable(uri)
				.filter(URI.class::isInstance)
				.map(URI.class::cast)
				.map(URI::getHost)
				.ifPresent(Mockito.when(uriDescriptor.bucketName())::thenReturn);
	}
}
