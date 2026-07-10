package io.github.mirkoscotti.nio.s3.operations;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import io.github.mirkoscotti.nio.s3.exceptions.TransportException;
import io.github.mirkoscotti.nio.s3.functions.Case;
import io.github.mirkoscotti.nio.s3.functions.Evaluator;
import io.github.mirkoscotti.nio.s3.functions.Try;
import io.github.mirkoscotti.nio.s3.helpers.ExceptionHelper;

import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.AbortMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.ChecksumAlgorithm;
import software.amazon.awssdk.services.s3.model.CompleteMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.CompletedPart;
import software.amazon.awssdk.services.s3.model.CreateMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.CreateMultipartUploadResponse;
import software.amazon.awssdk.services.s3.model.UploadPartCopyRequest;
import software.amazon.awssdk.services.s3.model.UploadPartCopyResponse;
import software.amazon.awssdk.services.s3.model.UploadPartRequest;

/**
 * Writes an S3 object using a multipart upload, accumulating parts either from raw bytes or by
 * copying byte ranges from the same object.
 * <p>
 * An instance of this class covers the full upload workflow. The lifecycle is detailed below:
 * <ul>
 * <li>The process starts when this class is instantiated, creating the native upload identifier
 * <li>This instance allows many partial writes and each write correspond to a complete part in the
 * upload process
 * <li>The process ends completing or aborting the upload when this instance is closed
 * </ul>
 *
 * @author mirko.scotti
 * @version May 24, 2025
 */
public final class MultipartWriter
	implements Closeable
{

	private final List<CompletedPart> parts = new ArrayList<>();

	private final AtomicInteger partNumber = new AtomicInteger();

	private final List<Long> bytesWritten = new ArrayList<>();

	private final S3AsyncClient client;

	private final String bucket;

	private final String key;

	private final String uploadId;

	private boolean mustAbort = false;

	/**
	 * Starts a multipart upload for the given bucket and key.
	 *
	 * @param client
	 *            the S3 client
	 * @param bucket
	 *            the target bucket
	 * @param key
	 *            the target object key
	 */
	public MultipartWriter(S3AsyncClient client, String bucket, String key)
	{
		this.client = Objects.requireNonNull(client, () -> "Missing client.");
		this.bucket = Objects.requireNonNull(bucket, () -> "Missing bucket.");
		this.key = Objects.requireNonNull(key, () -> "Missing key.");
		uploadId = Try.to(this::createUploadId).onCatch(ExceptionHelper::sneakyThrow).get();
	}

	/**
	 * Completes the multipart upload, or aborts it if:
	 * <ul>
	 * <li>an error occurred during the process
	 * <li>the process is forced to stop by an external trigger was called or
	 * <li>no parts were written.
	 *
	 * @throws IOException
	 *             if completing or aborting the upload fails
	 */
	@Override
	public void close() throws IOException
	{
		Evaluator.when(() -> mustAbort || parts.isEmpty())
				 .then(this::cancelUpload)
				 .elseExecute(this::completeUpload);
	}

	/**
	 * Uploads the given buffer as the next part of the multipart upload.
	 *
	 * @param buffer
	 *            the part content
	 */
	public void write(byte[] buffer)
	{
		Try.to(() -> parts.add(createCompletedPart(buffer)))
		   .onCatch(ExceptionHelper::sneakyThrow)
		   .run();
	}

	/**
	 * Adds the next part by copying the specified number of bytes from the source object into this
	 * upload.
	 *
	 * @param size
	 *            the number of bytes to copy
	 */
	public void copy(long size)
	{
		parts.add(Try.to(() -> createCompletedPart(size))
					 .onCatch(ExceptionHelper::sneakyThrow)
					 .get());
	}

	/**
	 * Adds the next part by copying the specified byte range from the source object into this
	 * upload.
	 *
	 * @param from
	 *            the range start offset, inclusive
	 * @param to
	 *            the range end offset, inclusive
	 */
	public void copy(long from, long to)
	{
		parts.add(Try.to(() -> createCompletedPart(from, to))
					 .onCatch(ExceptionHelper::sneakyThrow)
					 .get());
	}

	/**
	 * The total number of bytes written so far across all parts.
	 *
	 * @return the total bytes written
	 */
	public long bytesWritten()
	{
		return bytesWritten.stream().collect(Collectors.summingLong(Long::longValue));
	}

	/**
	 * Marks this upload to be aborted instead of completed on {@link #close()}.
	 */
	public void mustAbort()
	{
		mustAbort = true;
	}

	private String createUploadId()
		throws TimeoutException,
			ExecutionException,
			InterruptedException
	{
		return client.createMultipartUpload(this::createMultipartRequest)
					 .thenApply(CreateMultipartUploadResponse::uploadId)
					 .get(30, TimeUnit.SECONDS);
	}

	private CompletedPart createCompletedPart(byte[] buffer)
		throws TimeoutException,
			ExecutionException,
			InterruptedException
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
		throws TimeoutException,
			ExecutionException,
			InterruptedException
	{
		var result = client.uploadPartCopy(this::createUploadCopyRequest)
						   .thenApply(this::createCompletedPart)
						   .get(30, TimeUnit.SECONDS);
		bytesWritten.add(size);
		return result;
	}

	private CompletedPart createCompletedPart(long from, long to)
		throws TimeoutException,
			ExecutionException,
			InterruptedException
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

	private void completeUpload() throws IOException
	{
		var reference = new AtomicReference<Exception>();
		Try.to(() -> client.completeMultipartUpload(this::createCompleteMultipartRequest)
						   .get(30, TimeUnit.SECONDS))
		   .onCatch(reference::set)
		   .run();
		Case.of(reference.get()).when(Objects::nonNull).thenHandle(this::cancelUpload);
	}

	private void cancelUpload(Exception completeException) throws IOException
	{
		var exception = ExceptionHelper.redirectException(completeException);
		Try.to(this::cancelUpload).onCatch(item -> handleException(exception, item)).run();
		Case.of(exception)
			.when(TransportException.class::isInstance)
			.then(item -> TransportException.class.cast(item).throwNioException())
			.otherwise(() -> ExceptionHelper.throwIoException(exception));
	}

	private Void cancelUpload() throws IOException
	{
		var reference = new AtomicReference<Exception>();
		Try.to(() -> client.abortMultipartUpload(this::createAbortMultipartRequest)
						   .get(30, TimeUnit.SECONDS))
		   .onCatch(reference::set)
		   .run();
		Case.of(reference.get())
			.when(Objects::nonNull)
			.thenHandle(ExceptionHelper::throwIoException);
		return null;
	}

	private void handleException(RuntimeException completeException, Exception cancelException)
	{
		Supplier<String> warning = () -> """
			A multi-part upload has failed but it was not possible to abort it.
			Ensure to enable the automatic abort of the incomplete parts after a given number of days.
			See the command put-bucket-lifecycle-configuration for further details.
			""";
		var exception = ExceptionHelper.redirectException(cancelException.getCause());
		Optional.ofNullable(exception)
				.filter(TransportException.class::isInstance)
				.map(TransportException.class::cast)
				.map(item -> item.toNioException(warning.get()))
				.ifPresent(completeException::addSuppressed);
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
