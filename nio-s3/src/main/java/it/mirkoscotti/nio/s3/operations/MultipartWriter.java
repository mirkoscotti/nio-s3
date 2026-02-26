package it.mirkoscotti.nio.s3.operations;

import java.io.Closeable;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import it.mirkoscotti.nio.s3.functions.Try;
import it.mirkoscotti.nio.s3.helpers.ExceptionsHelper;

import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.AbortMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.ChecksumAlgorithm;
import software.amazon.awssdk.services.s3.model.CompleteMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.CompletedPart;
import software.amazon.awssdk.services.s3.model.CreateMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.CreateMultipartUploadResponse;
import software.amazon.awssdk.services.s3.model.InvalidRequestException;
import software.amazon.awssdk.services.s3.model.NoSuchUploadException;
import software.amazon.awssdk.services.s3.model.UploadPartCopyRequest;
import software.amazon.awssdk.services.s3.model.UploadPartCopyResponse;
import software.amazon.awssdk.services.s3.model.UploadPartRequest;

/**
 * @author mirko.scotti
 * @version May 24, 2025
 */
public final class MultipartWriter
	implements Closeable
{

	private static final Logger LOGGER = System.getLogger(MultipartWriter.class.getName());

	private final List<CompletedPart> parts = new ArrayList<>();

	private final AtomicInteger partNumber = new AtomicInteger();

	private final List<Long> bytesWritten = new ArrayList<>();

	private final S3AsyncClient client;

	private final String bucket;

	private final String key;

	private final String uploadId;

	/**
	 * @param client
	 */
	public MultipartWriter(S3AsyncClient client, String bucket, String key)
	{
		this.client = Objects.requireNonNull(client, () -> "Missing client.");
		this.bucket = Objects.requireNonNull(bucket, () -> "Missing bucket.");
		this.key = Objects.requireNonNull(key, () -> "Missing key.");
		uploadId = Try.to(this::createUploadId).onCatch(ExceptionsHelper::sneakyThrow).get();
	}

	@Override
	public void close()
	{
		Try.to(this::completeUpload).onCatch(this::cancelUpload).run();
	}

	public void write(byte[] buffer)
	{
		parts.add(Try.to(() -> createCompletedPart(buffer))
					 .onCatch(ExceptionsHelper::sneakyThrow)
					 .get());
	}

	public void copy(long size)
	{
		parts.add(Try.to(() -> createCompletedPart(size))
					 .onCatch(ExceptionsHelper::sneakyThrow)
					 .get());
	}

	public void copy(long from, long to)
	{
		parts.add(Try.to(() -> createCompletedPart(from, to))
					 .onCatch(ExceptionsHelper::sneakyThrow)
					 .get());
	}

	public long bytesWritten()
	{
		return bytesWritten.stream().collect(Collectors.summingLong(Long::longValue));
	}

	private String createUploadId()
		throws TimeoutException, ExecutionException, InterruptedException
	{
		return client.createMultipartUpload(this::createMultipartRequest)
					 .thenApply(CreateMultipartUploadResponse::uploadId)
					 .get(30, TimeUnit.SECONDS);
	}

	private CompletedPart createCompletedPart(byte[] buffer)
		throws TimeoutException, ExecutionException, InterruptedException
	{
		var part = partNumber.incrementAndGet();
		var result = client.uploadPart(item -> createUploadRequest(item, part),
									   AsyncRequestBody.fromBytes(buffer))
						   .thenApply(item -> CompletedPart.builder()
														   .eTag(item.eTag())
														   .partNumber(part)
														   .checksumSHA256(item.checksumSHA256())
														   .build())
						   .get(30, TimeUnit.SECONDS);
		bytesWritten.add(Long.valueOf(buffer.length));
		return result;
	}

	private CompletedPart createCompletedPart(long size)
		throws TimeoutException, ExecutionException, InterruptedException
	{
		var result = client.uploadPartCopy(this::createUploadCopyRequest)
						   .thenApply(this::createCompletedPart)
						   .get(30, TimeUnit.SECONDS);
		bytesWritten.add(size);
		return result;
	}

	private CompletedPart createCompletedPart(long from, long to)
		throws TimeoutException, ExecutionException, InterruptedException
	{
		return client.uploadPartCopy(item -> createUploadCopyRequest(item, from, to))
					 .thenApply(this::createCompletedPart)
					 .get(30, TimeUnit.SECONDS);
	}

	private CompletedPart createCompletedPart(UploadPartCopyResponse response)
	{
		var copyPartResult = response.copyPartResult();
		return CompletedPart.builder()
							.partNumber(partNumber.get())
							.eTag(copyPartResult.eTag())
							.checksumSHA256(copyPartResult.checksumSHA256())
							.build();
	}

	private Void completeUpload() throws TimeoutException, ExecutionException, InterruptedException
	{
		client.completeMultipartUpload(this::createCompleteMultipartRequest)
			  .get(30, TimeUnit.SECONDS);
		return null;
	}

	private void cancelUpload(Exception exception)
	{
		Supplier<String> warning = () -> """
			A multi-part upload has failed but it was not possible to abort it.
			Ensure to enable the automatic abort of the incomplete parts after a given number of days.
			See the command put-bucket-lifecycle-configuration for further details.
			""";
		Try.to(this::cancelUpload).onCatch(item -> LOGGER.log(Level.WARNING, warning, item)).run();
		var s3Exception = ExceptionsHelper.redirectException(exception);
		if (!(s3Exception instanceof NoSuchUploadException)
			&& !(s3Exception instanceof InvalidRequestException))
		{
			throw s3Exception;
		}
	}

	private Void cancelUpload() throws TimeoutException, ExecutionException, InterruptedException
	{
		client.abortMultipartUpload(this::createAbortMultipartRequest).get(30, TimeUnit.SECONDS);
		return null;
	}

	private void createMultipartRequest(CreateMultipartUploadRequest.Builder builder)
	{
		builder.bucket(bucket).key(key).checksumAlgorithm(ChecksumAlgorithm.SHA256);
	}

	private void createUploadRequest(UploadPartRequest.Builder builder, int part)
	{
		builder.bucket(bucket)
			   .key(key)
			   .uploadId(uploadId)
			   .partNumber(part)
			   .checksumAlgorithm(ChecksumAlgorithm.SHA256);
	}

	private void createUploadCopyRequest(UploadPartCopyRequest.Builder builder)
	{
		builder.sourceBucket(bucket)
			   .sourceKey(key)
			   .destinationBucket(bucket)
			   .destinationKey(key)
			   .uploadId(uploadId)
			   .partNumber(partNumber.incrementAndGet());
	}

	private void createUploadCopyRequest(UploadPartCopyRequest.Builder builder, long from, long to)
	{
		var range = "bytes=%d-%d".formatted(from, to);
		builder.sourceBucket(bucket)
			   .sourceKey(key)
			   .destinationBucket(bucket)
			   .destinationKey(key)
			   .copySourceRange(range)
			   .uploadId(uploadId)
			   .partNumber(partNumber.incrementAndGet());
	}

	private void createCompleteMultipartRequest(CompleteMultipartUploadRequest.Builder builder)
	{
		builder.bucket(bucket)
			   .key(key)
			   .uploadId(uploadId)
			   .multipartUpload(item -> item.parts(parts))
			   .checksumSHA256(bucket);
	}

	private void createAbortMultipartRequest(AbortMultipartUploadRequest.Builder builder)
	{
		builder.bucket(bucket).key(key).uploadId(uploadId);
	}
}
