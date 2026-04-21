package it.mirkoscotti.nio.s3.operations;

import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import io.github.mirkoscotti.nio.s3.functions.Case;

import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.ChecksumAlgorithm;
import software.amazon.awssdk.services.s3.model.CopyObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.transfer.s3.S3TransferManager;

/**
 * @author mirko.scotti
 * @version Feb 14, 2026
 */
public final class FileTransfer
{

	private final S3AsyncClient client;

	private final String bucket;

	private final String key;

	public FileTransfer(S3AsyncClient client, String bucket, String key)
	{
		this.client = Objects.requireNonNull(client, () -> "Missing client.");
		this.bucket = Objects.requireNonNull(bucket, () -> "Missing bucket.");
		this.key = Objects.requireNonNull(key, () -> "Missing key.");
	}

	public void transfer(S3AsyncClient client, String bucket, String key) throws IOException
	{
		Case.of(key)
			.when(item -> this.bucket.equals(bucket))
			.then(this::copy)
			.otherwise(item -> copy(client, bucket, key));
	}

	private void copy(String target) throws IOException
	{
		Consumer<CopyObjectRequest.Builder> requestBuilder = item -> item.checksumAlgorithm(ChecksumAlgorithm.SHA256)
																		 .sourceBucket(bucket)
																		 .sourceKey(key)
																		 .destinationBucket(bucket)
																		 .destinationKey(target);
		try (var transferManager = S3TransferManager.builder().s3Client(client).build())
		{
			transferManager.copy(item -> item.copyObjectRequest(requestBuilder))
						   .completionFuture()
						   .get(30, TimeUnit.MINUTES);
		}
		catch (InterruptedException x)
		{
			Thread.currentThread().interrupt();
			throw new IOException("Transfer interrupted.", x);
		}
		catch (Exception x)
		{
			throw new IOException("Transfer failed.", x);
		}
	}

	private void copy(S3AsyncClient client, String bucket, String key) throws IOException
	{
		try (var transferManager = S3TransferManager.builder().s3Client(client).build())
		{
			var publisher = this.client.getObject(item -> item.bucket(this.bucket).key(this.key),
												  AsyncResponseTransformer.toPublisher())
									   .get(30, TimeUnit.SECONDS);
			Consumer<PutObjectRequest.Builder> putObjectRequest = item -> item.bucket(bucket)
																			  .key(key)
																			  .checksumAlgorithm(ChecksumAlgorithm.SHA256);
			transferManager.upload(item -> item.putObjectRequest(putObjectRequest)
											   .requestBody(AsyncRequestBody.fromPublisher(publisher)))
						   .completionFuture()
						   .get(30, TimeUnit.MINUTES);
		}
		catch (InterruptedException x)
		{
			Thread.currentThread().interrupt();
			throw new IOException("Transfer interrupted.", x);
		}
		catch (Exception x)
		{
			throw new IOException("Transfer failed.", x);
		}
	}
}
