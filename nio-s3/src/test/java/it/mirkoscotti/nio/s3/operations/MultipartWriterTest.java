package it.mirkoscotti.nio.s3.operations;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import software.amazon.awssdk.services.s3.S3AsyncClient;

/**
 * @author mirko.scotti
 * @version May 28, 2025
 */
@ExtendWith(MockitoExtension.class)
class MultipartWriterTest
{

	private static final String BUCKET = "bucket";

	@Mock
	private S3AsyncClient client;

	@Test
	void instantiationTest()
	{
		Assertions.assertThrows(NullPointerException.class,
								() -> new MultipartWriter(null, null, null));
		Assertions.assertThrows(NullPointerException.class,
								() -> new MultipartWriter(client, null, null));
		Assertions.assertThrows(NullPointerException.class,
								() -> new MultipartWriter(client, BUCKET, null));
	}
}
