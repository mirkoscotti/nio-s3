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
	void writerWithoutClientTest()
	{
		var writer = MultipartWriter.create();
		Assertions.assertThrows(NullPointerException.class, writer::start);
	}

	@Test
	void writerWithoutBucketTest(@Mock S3AsyncClient client)
	{
		var writer = MultipartWriter.create().withClient(client);
		Assertions.assertThrows(NullPointerException.class, writer::start);
	}

	@Test
	void writerWithoutKeyTest()
	{
		var writer = MultipartWriter.create().withClient(client).withBucket(BUCKET);
		Assertions.assertThrows(NullPointerException.class, writer::start);
	}
}
