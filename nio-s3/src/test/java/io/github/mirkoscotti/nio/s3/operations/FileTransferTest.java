package io.github.mirkoscotti.nio.s3.operations;

import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.transfer.s3.S3TransferManager;
import software.amazon.awssdk.transfer.s3.model.CompletedCopy;
import software.amazon.awssdk.transfer.s3.model.CompletedUpload;
import software.amazon.awssdk.transfer.s3.model.Copy;
import software.amazon.awssdk.transfer.s3.model.Upload;

/**
 * @author mirko.scotti
 * @version Feb 20, 2026
 */
@ExtendWith(MockitoExtension.class)
class FileTransferTest
{

	private static final String SOURCE_BUCKET = "source-bucket";

	private static final String TARGET_BUCKET = "target-bucket";

	private static final String SOURCE_KEY = "source-key";

	private static final String TARGET_KEY = "target-key";

	@Mock
	private S3AsyncClient client;

	@Test
	void nullTest()
	{
		Assertions.assertThrows(NullPointerException.class,
								() -> new FileTransfer(null, null, null));
		Assertions.assertThrows(NullPointerException.class,
								() -> new FileTransfer(client, null, null));
		Assertions.assertThrows(NullPointerException.class,
								() -> new FileTransfer(client, SOURCE_BUCKET, null));
	}

	@Test
	void trasferWithoutTargetClientTest()
	{
		var fileTransfer = new FileTransfer(client, SOURCE_BUCKET, SOURCE_KEY);
		var exception = Assertions.assertThrows(IOException.class,
												() -> fileTransfer.transfer(null, null, null));
		Assertions.assertInstanceOf(SdkClientException.class, exception.getCause());
	}

	@Test
	void trasferWithoutTargetBucketTest()
	{
		var fileTransfer = new FileTransfer(client, SOURCE_BUCKET, SOURCE_KEY);
		var exception = Assertions.assertThrows(IOException.class,
												() -> fileTransfer.transfer(client, null, null));
		Assertions.assertInstanceOf(ExecutionException.class, exception.getCause());
	}

	@Test
	void trasferToKeyNotDefinedTest()
	{
		var fileTransfer = new FileTransfer(client, SOURCE_BUCKET, SOURCE_KEY);
		var exception = Assertions.assertThrows(IOException.class,
												() -> fileTransfer.transfer(client,
																			SOURCE_BUCKET,
																			null));
		Assertions.assertInstanceOf(ExecutionException.class, exception.getCause());
	}

	@Test
	@SuppressWarnings("unchecked")
	void interruptionDuringTransferToSameBucketTest(@Mock S3TransferManager.Builder builder,
													@Mock S3TransferManager transferManager,
													@Mock Copy copy,
													@Mock CompletableFuture<CompletedCopy> future)
		throws Exception
	{
		when(builder.s3Client(Mockito.any(S3AsyncClient.class))).thenReturn(builder);
		when(builder.build()).thenReturn(transferManager);
		when(transferManager.copy(Mockito.any(Consumer.class))).thenReturn(copy);
		when(copy.completionFuture()).thenReturn(future);
		try (var mock = Mockito.mockStatic(S3TransferManager.class))
		{
			when(future.get(Mockito.anyLong(),
							Mockito.any(TimeUnit.class))).thenThrow(InterruptedException.class);
			mock.when(S3TransferManager::builder).thenReturn(builder);
			var fileTransfer = new FileTransfer(client, SOURCE_BUCKET, SOURCE_KEY);
			var exception = Assertions.assertThrows(IOException.class,
													() -> fileTransfer.transfer(client,
																				SOURCE_BUCKET,
																				TARGET_KEY));
			Assertions.assertInstanceOf(InterruptedException.class, exception.getCause());
		}
	}

	@Test
	@SuppressWarnings("unchecked")
	void interruptionDuringTransferToDifferentBucketTest(@Mock S3TransferManager.Builder builder,
														 @Mock S3TransferManager transferManager,
														 @Mock Upload upload,
														 @Mock CompletableFuture<CompletedUpload> future)
		throws Exception
	{
		when(builder.s3Client(Mockito.any(S3AsyncClient.class))).thenReturn(builder);
		when(builder.build()).thenReturn(transferManager);
		when(transferManager.upload(Mockito.any(Consumer.class))).thenReturn(upload);
		when(upload.completionFuture()).thenReturn(future);
		try (var mock = Mockito.mockStatic(S3TransferManager.class))
		{
			when(future.get(Mockito.anyLong(),
							Mockito.any(TimeUnit.class))).thenThrow(InterruptedException.class);
			mock.when(S3TransferManager::builder).thenReturn(builder);
			var fileTransfer = new FileTransfer(client, TARGET_BUCKET, SOURCE_KEY);
			var exception = Assertions.assertThrows(IOException.class,
													() -> fileTransfer.transfer(client,
																				SOURCE_BUCKET,
																				TARGET_KEY));
			Assertions.assertInstanceOf(InterruptedException.class, exception.getCause());
		}
	}
}
