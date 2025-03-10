/*
 * (C) Copyright 2019 - 2025 - Add Value S.R.L - All rights reserved.
 */

package it.mirkoscotti.nio.s3.configuration;

import it.mirkoscotti.nio.s3.exceptions.BucketUriException;

import java.net.URI;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Add Value S.R.L by mirko.scotti
 * @version Mar 08, 2025
 */
@ExtendWith(MockitoExtension.class)
class UriDescriptorTest
{

	private static final String S3 = "s3";

	private static final String ENDPOINT = "endpoint";

	private static final String BUCKET_NAME = "test.bucket";

	private static final String ACCESS_KEY = "access-key";

	private static final String SECRET_KEY = "secret-key";

	@Test
	void nullTest()
	{
		Assertions.assertThrows(NullPointerException.class, () -> new UriDescriptor(null));
	}

	@Test
	void wrongSchemeTest(@Mock URI uri)
	{
		Assertions.assertThrows(BucketUriException.class, () -> new UriDescriptor(uri));
	}

	@Test
	void bucketStyleTest(@Mock URI uri)
	{
		Mockito.when(uri.getScheme()).thenReturn(S3);
		Mockito.when(uri.getHost()).thenReturn(BUCKET_NAME);
		var uriDescriptor = new UriDescriptor(uri);
		Assertions.assertEquals(BUCKET_NAME, uriDescriptor.bucketName());
		Assertions.assertTrue(uriDescriptor.endpoint().isEmpty());
	}

	@Test
	void bucketStyleWithEmptyPathTest(@Mock URI uri)
	{
		Mockito.when(uri.getScheme()).thenReturn(S3);
		Mockito.when(uri.getHost()).thenReturn(BUCKET_NAME);
		Mockito.when(uri.getPath()).thenReturn("/");
		var uriDescriptor = new UriDescriptor(uri);
		Assertions.assertEquals(BUCKET_NAME, uriDescriptor.bucketName());
		Assertions.assertTrue(uriDescriptor.endpoint().isEmpty());
	}

	@Test
	void virtualHostStyleTest(@Mock URI uri)
	{
		Mockito.when(uri.getScheme()).thenReturn(S3);
		Mockito.when(uri.getHost()).thenReturn(String.join(".", BUCKET_NAME, S3, ENDPOINT));
		var uriDescriptor = new UriDescriptor(uri);
		Assertions.assertEquals(BUCKET_NAME, uriDescriptor.bucketName());
		var endpoint = uriDescriptor.endpoint();
		Assertions.assertTrue(endpoint.isPresent());
		Assertions.assertEquals(String.join(".", S3, ENDPOINT), endpoint.get());
	}

	@Test
	void pathStyleTest(@Mock URI uri)
	{
		Mockito.when(uri.getScheme()).thenReturn("s3");
		Mockito.when(uri.getHost()).thenReturn(ENDPOINT);
		Mockito.when(uri.getPath()).thenReturn("/".concat(BUCKET_NAME));
		var uriDescriptor = new UriDescriptor(uri);
		Assertions.assertEquals(BUCKET_NAME, uriDescriptor.bucketName());
		var endpoint = uriDescriptor.endpoint();
		Assertions.assertTrue(endpoint.isPresent());
		Assertions.assertEquals(ENDPOINT, endpoint.get());
	}

	@Test
	void incompleteCredentialsTest(@Mock URI uri)
	{
		Mockito.when(uri.getScheme()).thenReturn(S3);
		Mockito.when(uri.getHost()).thenReturn(BUCKET_NAME);
		Mockito.when(uri.getUserInfo()).thenReturn(ACCESS_KEY);
		var uriDescriptor = new UriDescriptor(uri);
		Assertions.assertTrue(uriDescriptor.credentials().isEmpty());
	}

	@Test
	void credentialsTest(@Mock URI uri)
	{
		Mockito.when(uri.getScheme()).thenReturn(S3);
		Mockito.when(uri.getHost()).thenReturn(BUCKET_NAME);
		Mockito.when(uri.getUserInfo()).thenReturn(String.join(":", ACCESS_KEY, SECRET_KEY));
		var uriDescriptor = new UriDescriptor(uri);
		var optional = uriDescriptor.credentials();
		Assertions.assertTrue(optional.isPresent());
		var credentials = optional.get();
		Assertions.assertEquals(ACCESS_KEY, credentials.accessKey());
		Assertions.assertEquals(SECRET_KEY, credentials.secretKey());
	}
}
