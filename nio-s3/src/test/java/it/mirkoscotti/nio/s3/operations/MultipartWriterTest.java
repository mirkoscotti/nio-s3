package it.mirkoscotti.nio.s3.operations;

import it.mirkoscotti.nio.s3.records.OperationRecord;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
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

	@Test
	void nullTest()
	{
		Assertions.assertThrows(NullPointerException.class, () -> new MultipartWriter(null));
	}

	@Test
	void writerWithoutClientTest(@Mock OperationRecord operationRecord)
	{
		Assertions.assertThrows(NullPointerException.class,
								() -> new MultipartWriter(operationRecord));
	}

	@Test
	void writerWithoutBucketTest(@Mock OperationRecord operationRecord, @Mock S3AsyncClient client)
	{
		Mockito.when(operationRecord.client()).thenReturn(client);
		Assertions.assertThrows(NullPointerException.class,
								() -> new MultipartWriter(operationRecord));
	}

	@Test
	void writerWithoutKeyTest(@Mock OperationRecord operationRecord, @Mock S3AsyncClient client)
	{
		Mockito.when(operationRecord.client()).thenReturn(client);
		Mockito.when(operationRecord.bucket()).thenReturn(BUCKET);
		Assertions.assertThrows(NullPointerException.class,
								() -> new MultipartWriter(operationRecord));
	}
}
