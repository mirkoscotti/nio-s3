package it.mirkoscotti.nio.s3.operations;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import io.github.mirkoscotti.nio.s3.extensions.testcontainers.S3Container;

/**
 * @author mirko.scotti
 * @version Dec 16, 2025
 */
@Testcontainers
class StsConnectorIT
{

	private static final String TEST_USER = "test-user";

	@Container
	@SuppressWarnings("resource")
	private static final S3Container CONTAINER = new S3Container().withUser(TEST_USER);

	@Test
	void arnTest()
	{
		var expected = CONTAINER.arn(TEST_USER);
		var result = StsConnector.create()
								 .withEndpoint(CONTAINER.getEndpoint())
								 .withRegion(CONTAINER.getRegion())
								 .withCredentials(CONTAINER.getAccessKey(TEST_USER),
												  CONTAINER.getSecretKey(TEST_USER))
								 .build()
								 .arn();
		Assertions.assertEquals(expected, result);
	}
}
