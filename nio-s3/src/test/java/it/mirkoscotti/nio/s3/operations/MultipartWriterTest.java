package it.mirkoscotti.nio.s3.operations;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;
import java.util.function.Function;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.CompletedPart;
import software.amazon.awssdk.services.s3.model.CreateMultipartUploadResponse;
import software.amazon.awssdk.services.s3.model.UploadPartResponse;

/**
 * @author mirko.scotti
 * @version May 28, 2025
 */
@ExtendWith(MockitoExtension.class)
class MultipartWriterTest
{

	private static final String BUCKET = "bucket";

	private static final String KEY = "key";

	private static final String UPLOAD_ID = "uploadId";

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

	@Test
	@SuppressWarnings("unchecked")
	void timeoutWhileWritingTest(@Mock CreateMultipartUploadResponse createMultipartUploadResponse,
								 @Mock CompletableFuture<UploadPartResponse> uploadFuture,
								 @Mock CompletableFuture<CompletedPart> partFuture)
	{
		Mockito.when(client.createMultipartUpload(Mockito.any(Consumer.class)))
			   .thenReturn(CompletableFuture.completedFuture(createMultipartUploadResponse));
		Mockito.when(createMultipartUploadResponse.uploadId()).thenReturn(UPLOAD_ID);
		Mockito.when(client.uploadPart(Mockito.any(Consumer.class),
									   Mockito.any(AsyncRequestBody.class)))
			   .thenReturn(uploadFuture);
		Mockito.when(uploadFuture.thenApply(Mockito.any(Function.class))).thenReturn(partFuture);
		try (var writer = new MultipartWriter(client, BUCKET, KEY))
		{
			Mockito.when(partFuture.get(Mockito.anyLong(), Mockito.any(TimeUnit.class)))
				   .thenThrow(TimeoutException.class);
			var buffer = new byte[0];
			var exception = Assertions.assertThrows(IOException.class, () -> writer.write(buffer));
			Assertions.assertInstanceOf(TimeoutException.class, exception.getCause());
		}
		catch (Exception x)
		{
			Assertions.fail(x);
		}
	}

	@Test
	@SuppressWarnings("unchecked")
	void interruptedExceptionWhileWritingTest(@Mock CreateMultipartUploadResponse createMultipartUploadResponse,
											  @Mock CompletableFuture<UploadPartResponse> uploadFuture,
											  @Mock CompletableFuture<CompletedPart> partFuture)
	{
		Mockito.when(client.createMultipartUpload(Mockito.any(Consumer.class)))
			   .thenReturn(CompletableFuture.completedFuture(createMultipartUploadResponse));
		Mockito.when(createMultipartUploadResponse.uploadId()).thenReturn(UPLOAD_ID);
		Mockito.when(client.uploadPart(Mockito.any(Consumer.class),
									   Mockito.any(AsyncRequestBody.class)))
			   .thenReturn(uploadFuture);
		Mockito.when(uploadFuture.thenApply(Mockito.any(Function.class))).thenReturn(partFuture);
		try (var writer = new MultipartWriter(client, BUCKET, KEY))
		{
			Mockito.when(partFuture.get(Mockito.anyLong(), Mockito.any(TimeUnit.class)))
				   .thenThrow(InterruptedException.class);
			var buffer = new byte[0];
			var exception = Assertions.assertThrows(IOException.class, () -> writer.write(buffer));
			Assertions.assertInstanceOf(InterruptedException.class, exception.getCause());
		}
		catch (Exception x)
		{
			Assertions.fail(x);
		}
	}
}
