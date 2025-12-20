package it.mirkoscotti.nio.s3.operations;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import it.mirkoscotti.nio.s3.extensions.testcontainers.S3Container;

/**
 * @author mirko.scotti
 * @version Dec 17, 2025
 */
@Testcontainers
class IamConnectorIT
{

	private static final String READ_USER = "read-user";

	private static final String WRITE_USER = "write-user";

	private static final String TEST_BUCKET = "test-bucket";

	private static final String TEST_FILE = "test-file.txt";

	private static final String TEST_DIRECTORY = "test-directory";

	@Container
	@SuppressWarnings("resource")
	private static final S3Container CONTAINER = new S3Container().withReadOnlyUser(READ_USER)
																  .withUser(WRITE_USER)
																  .withBucket(TEST_BUCKET);

	@Test
	void cannotWriteFileTest()
	{
		var readArn = CONTAINER.arn(READ_USER);
		var result = IamConnector.create()
								 .withEndpoint(CONTAINER.getEndpoint())
								 .withRegion(CONTAINER.getRegion())
								 .withCredentials(CONTAINER.getAccessKey(READ_USER),
												  CONTAINER.getSecretKey(READ_USER))
								 .build()
								 .canWriteFile(readArn, TEST_BUCKET, TEST_FILE);
		Assertions.assertFalse(result);
	}

	@Test
	@Disabled("Waiting for LocalStack bug fix.")
	void canWriteFileTest()
	{
		/*
		 * This test cannot be performed until the bug reported here is fixed:
		 * https://github.com/localstack/localstack/issues/13073
		 */
		var readArn = CONTAINER.arn(WRITE_USER);
		var result = IamConnector.create()
								 .withEndpoint(CONTAINER.getEndpoint())
								 .withRegion(CONTAINER.getRegion())
								 .withCredentials(CONTAINER.getAccessKey(WRITE_USER),
												  CONTAINER.getSecretKey(WRITE_USER))
								 .build()
								 .canWriteFile(readArn, TEST_BUCKET, TEST_FILE);
		Assertions.assertTrue(result);
	}

	@Test
	void cannotWriteDirectoryTest()
	{
		var readArn = CONTAINER.arn(READ_USER);
		var result = IamConnector.create()
								 .withEndpoint(CONTAINER.getEndpoint())
								 .withRegion(CONTAINER.getRegion())
								 .withCredentials(CONTAINER.getAccessKey(READ_USER),
												  CONTAINER.getSecretKey(READ_USER))
								 .build()
								 .canWriteDirectory(readArn, TEST_BUCKET, TEST_DIRECTORY);
		Assertions.assertFalse(result);
	}
}
