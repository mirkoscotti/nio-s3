package io.github.mirkoscotti.nio.s3.records;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author mirko.scotti
 * @version Mar 11, 2025
 */
@ExtendWith(MockitoExtension.class)
class CredentialsRecordTest
{

	private static final String ACCESS_KEY = "access-key";

	private static final String SECRET_KEY = "secret-key";

	@Test
	void missingCredentialsTest()
	{
		Assertions.assertThrows(NullPointerException.class,
								() -> new CredentialsRecord(null, null));
		Assertions.assertThrows(NullPointerException.class,
								() -> new CredentialsRecord(ACCESS_KEY, null));
	}

	@Test
	void credentialsTest()
	{
		var credentials = new CredentialsRecord(ACCESS_KEY, SECRET_KEY);
		var awsCredentials = credentials.awsCredentials();
		Assertions.assertEquals(ACCESS_KEY, awsCredentials.accessKeyId());
		Assertions.assertEquals(SECRET_KEY, awsCredentials.secretAccessKey());
	}
}
