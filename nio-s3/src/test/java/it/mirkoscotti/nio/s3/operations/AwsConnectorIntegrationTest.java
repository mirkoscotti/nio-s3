/*
 * (C) Copyright 2019 - 2024 - Add Value S.R.L - All rights reserved.
 */

package it.mirkoscotti.nio.s3.operations;

import it.mirkoscotti.nio.s3.extensions.testcontainers.S3Container;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * @author mirko.scotti
 * @version Oct 24, 2024
 */
@Testcontainers
class AwsConnectorIntegrationTest
{

	private static final String BUCKET_NAME = "test-bucket";

	@Container
	@SuppressWarnings("resource")
	private static final S3Container CONTAINER = new S3Container().withBucket(BUCKET_NAME);

	@Test
	@Disabled
	void testTest()
	{
		Assertions.assertTrue(CONTAINER.bucketExists(BUCKET_NAME));
		var endpoint = CONTAINER.getEndpoint();
		CONTAINER.createBucketPolicy(BUCKET_NAME);
		var policy = CONTAINER.bucketPolicy(BUCKET_NAME);
		var connector = S3Connector.create()
								   .withEndpoint(endpoint)
								   .withRegion(CONTAINER.getRegion())
								   .withCredentials(CONTAINER.getAccessKey(),
													CONTAINER.getSecretKey())
								   .build()
								   .isBucketReadOnly(BUCKET_NAME);
	}
}
