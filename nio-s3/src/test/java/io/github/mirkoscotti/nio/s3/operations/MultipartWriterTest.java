package io.github.mirkoscotti.nio.s3.operations;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
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

import io.github.mirkoscotti.nio.s3.enums.ErrorCode;
import io.github.mirkoscotti.nio.s3.exceptions.TransportException;

import software.amazon.awssdk.awscore.exception.AwsErrorDetails;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.AbortMultipartUploadResponse;
import software.amazon.awssdk.services.s3.model.CompleteMultipartUploadResponse;
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
			var exception = Assertions.assertThrows(IllegalStateException.class,
													() -> writer.write(buffer));
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
			var exception = Assertions.assertThrows(IllegalStateException.class,
													() -> writer.write(buffer));
			Assertions.assertInstanceOf(InterruptedException.class, exception.getCause());
		}
		catch (Exception x)
		{
			Assertions.fail(x);
		}
	}

	@Test
	@SuppressWarnings("unchecked")
	void failedCloseTest(@Mock CreateMultipartUploadResponse createMultipartUploadResponse,
						 @Mock CompleteMultipartUploadResponse completeMultipartUploadResponse)
	{
		Mockito.when(client.createMultipartUpload(Mockito.any(Consumer.class)))
			   .thenReturn(CompletableFuture.completedFuture(createMultipartUploadResponse));
		Mockito.when(createMultipartUploadResponse.uploadId()).thenReturn(UPLOAD_ID);
		try (var writer = new MultipartWriter(client, BUCKET, KEY))
		{
			// Nothing to do
		}
		catch (Exception x)
		{
			var exception = Assertions.assertInstanceOf(TransportException.class, x);
			Assertions.assertInstanceOf(AwsServiceException.class, exception.getCause());
		}
	}

	@Test
	@SuppressWarnings("unchecked")
	void failedAbortTest(@Mock CreateMultipartUploadResponse createMultipartUploadResponse,
						 @Mock CompletedPart completedPart,
						 @Mock CompletableFuture<UploadPartResponse> uploadFuture,
						 @Mock CompletableFuture<CompletedPart> partFuture,
						 @Mock CompletableFuture<CompleteMultipartUploadResponse> completeFuture,
						 @Mock CompletableFuture<AbortMultipartUploadResponse> abortFuture,
						 @Mock ExecutionException executionException,
						 @Mock AwsServiceException awsServiceException,
						 @Mock AwsErrorDetails awsErrorDetails,
						 @Mock ErrorCode errorCode)
	{
		Mockito.when(client.createMultipartUpload(Mockito.any(Consumer.class)))
			   .thenReturn(CompletableFuture.completedFuture(createMultipartUploadResponse));
		Mockito.when(createMultipartUploadResponse.uploadId()).thenReturn(UPLOAD_ID);
		Mockito.when(client.uploadPart(Mockito.any(Consumer.class),
									   Mockito.any(AsyncRequestBody.class)))
			   .thenReturn(uploadFuture);
		Mockito.when(uploadFuture.thenApply(Mockito.any(Function.class))).thenReturn(partFuture);
		Mockito.when(client.completeMultipartUpload(Mockito.any(Consumer.class)))
			   .thenReturn(completeFuture);
		Mockito.when(client.abortMultipartUpload(Mockito.any(Consumer.class)))
			   .thenReturn(abortFuture);
		Mockito.when(executionException.getCause()).thenReturn(awsServiceException);
		Mockito.when(awsServiceException.awsErrorDetails()).thenReturn(awsErrorDetails);
		var exception = new RuntimeException();
		Mockito.when(errorCode.nioException(Mockito.any(String[].class))).thenReturn(exception);
		try (var errorMock = Mockito.mockStatic(ErrorCode.class))
		{
			errorMock.when(() -> ErrorCode.of(Mockito.nullable(AwsErrorDetails.class)))
					 .thenReturn(Optional.of(errorCode));
			try (var writer = new MultipartWriter(client, BUCKET, KEY))
			{
				Mockito.when(partFuture.get(Mockito.anyLong(), Mockito.any(TimeUnit.class)))
					   .thenReturn(completedPart);
				Mockito.when(completeFuture.get(Mockito.anyLong(), Mockito.any(TimeUnit.class)))
					   .thenThrow(new ExecutionException(AwsServiceException.builder().build()));
				Mockito.when(abortFuture.get(Mockito.anyLong(), Mockito.any(TimeUnit.class)))
					   .thenThrow(executionException);
				writer.write(new byte[0]);
			}
			catch (Exception x)
			{
				var ioException = Assertions.assertInstanceOf(RuntimeException.class, x);
				Assertions.assertEquals(0, ioException.getSuppressed().length);
			}
		}
	}
}
