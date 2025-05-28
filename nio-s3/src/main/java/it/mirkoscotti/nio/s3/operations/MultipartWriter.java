package it.mirkoscotti.nio.s3.operations;

import it.mirkoscotti.nio.s3.functions.Try;
import it.mirkoscotti.nio.s3.helpers.ExceptionsHelper;

import java.io.Closeable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.AbortMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.CompleteMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.CompletedPart;
import software.amazon.awssdk.services.s3.model.CreateMultipartUploadResponse;
import software.amazon.awssdk.services.s3.model.UploadPartResponse;

/**
 * @author mirko.scotti
 * @version May 24, 2025
 */
public final class MultipartWriter
	implements Closeable
{

	private final List<CompletedPart> parts = new ArrayList<>();

	private final AtomicInteger partNumber = new AtomicInteger();

	private final S3AsyncClient client;

	private final String bucket;

	private final String key;

	private final String uploadId;

	/**
	 * @param client
	 */
	private MultipartWriter(S3AsyncClient client, String bucket, String key)
	{
		this.client = client;
		this.bucket = bucket;
		this.key = key;
		uploadId = Try.to(this::createUploadId).onCatch(ExceptionsHelper::redirectException).get();
	}

	@Override
	public void close()
	{
		Try.to(() -> client.completeMultipartUpload(this::createCompleteMultipartRequest))
		   .onCatch(ExceptionsHelper::redirectException)
		   .get();
	}

	public void write(byte[] buffer)
	{
		parts.add(Try.to(() -> createCompletedPart(buffer))
					 .onCatch(ExceptionsHelper::redirectException)
					 .get());
	}

	public void cancel()
	{
		Try.to(() -> client.abortMultipartUpload(this::createAbortMultipartRequest))
		   .onCatch(ExceptionsHelper::redirectException)
		   .get();
	}

	public static MultipartWriterBuilder create()
	{
		return new MultipartWriterBuilder();
	}

	private String createUploadId()
		throws TimeoutException,
			ExecutionException,
			InterruptedException
	{
		return client.createMultipartUpload(item -> item.bucket(bucket).key(key))
					 .thenApply(CreateMultipartUploadResponse::uploadId)
					 .get(30, TimeUnit.SECONDS);
	}

	private CompletedPart createCompletedPart(byte[] buffer)
		throws TimeoutException,
			ExecutionException,
			InterruptedException
	{
		var part = partNumber.incrementAndGet();
		return client.uploadPart(item -> item.bucket(bucket)
											 .key(key)
											 .uploadId(uploadId)
											 .partNumber(part),
								 AsyncRequestBody.fromBytes(buffer))
					 .thenApply(UploadPartResponse::eTag)
					 .thenApply(CompletedPart.builder()::eTag)
					 .thenApply(item -> item.partNumber(part).build())
					 .get(30, TimeUnit.SECONDS);
	}

	private void createCompleteMultipartRequest(CompleteMultipartUploadRequest.Builder builder)
	{
		builder.bucket(bucket)
			   .key(key)
			   .uploadId(uploadId)
			   .multipartUpload(item2 -> item2.parts(parts));
	}

	private void createAbortMultipartRequest(AbortMultipartUploadRequest.Builder builder)
	{
		builder.bucket(bucket).key(key).uploadId(uploadId);
	}

	public static final class MultipartWriterBuilder
	{

		private S3AsyncClient client;

		private String bucket;

		private String key;

		private MultipartWriterBuilder()
		{
			super();
		}

		public MultipartWriterBuilder withClient(S3AsyncClient client)
		{
			this.client = Objects.requireNonNull(client, () -> "Missing client");
			return this;
		}

		public MultipartWriterBuilder withBucket(String bucket)
		{
			this.bucket = Objects.requireNonNull(bucket, () -> "Missing bucket");
			return this;
		}

		public MultipartWriterBuilder withKey(String key)
		{
			this.key = Objects.requireNonNull(key, () -> "Missing key");
			return this;
		}

		public MultipartWriter start()
		{
			return new MultipartWriter(client, bucket, key);
		}
	}
}
