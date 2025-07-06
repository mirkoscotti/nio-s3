/*
 * (C) Copyright 2019 - 2025 - Add Value S.R.L - All rights reserved.
 */

package it.mirkoscotti.nio.s3.extensions.jdk.jsr203;

import it.mirkoscotti.nio.s3.extensions.testcontainers.S3Container;
import it.mirkoscotti.nio.s3.helpers.ContainersHelper;
import it.mirkoscotti.nio.s3.helpers.IoHelper;
import it.mirkoscotti.nio.s3.helpers.JunitHelper;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.api.io.CleanupMode;
import org.junit.jupiter.api.io.TempDir;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.AbortMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.CompleteMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.CompletedMultipartUpload;
import software.amazon.awssdk.services.s3.model.CompletedPart;
import software.amazon.awssdk.services.s3.model.CopyObjectRequest;
import software.amazon.awssdk.services.s3.model.CreateMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.UploadPartCopyRequest;
import software.amazon.awssdk.services.s3.model.UploadPartRequest;

/**
 * @author mirko.scotti
 * @version Jun 27, 2025
 */
@Testcontainers
@TestInstance(Lifecycle.PER_CLASS)
class MultipartIT
{

	private static final String TEST_BUCKET = "test-bucket";

	private static final String TINY_FILE = "tiny.txt";

	private static final String SMALL_FILE = "small.txt";

	private static final String MEDIUM_FILE = "medium.txt";

	private static final String LARGE_FILE = "large.txt";

	private static final String TARGET_FILE = "target.txt";

	private static final URI TEST_URI = URI.create("s3://".concat(TEST_BUCKET));

	private static final String CHECKSUM_ALGORITHM = "SHA-256";

	private static final int PART_SIZE = 10 * 1024 * 1024;

	@TempDir(cleanup = CleanupMode.ALWAYS)
	private static Path baseDirectory;

	@Container
	@SuppressWarnings("resource")
	private static final S3Container CONTAINER = new S3Container().withBucket(TEST_BUCKET);

	private FileSystem fileSystem;

	private Path tinyFile;

	private Path smallFile;

	private Path mediumFile;

	private Path largeFile;

	private Path targetFile;

	private S3AsyncClient client;

	@BeforeAll
	void beforeAll()
	{
		tinyFile = JunitHelper.tryCall(() -> createFile(TINY_FILE, 1));
		smallFile = JunitHelper.tryCall(() -> createFile(SMALL_FILE, 1024));
		mediumFile = JunitHelper.tryCall(() -> createFile(MEDIUM_FILE, 1024 * 1024));
		largeFile = JunitHelper.tryCall(() -> createFile(LARGE_FILE, PART_SIZE * 5 / 2));
		var properties = ContainersHelper.standardProperties(CONTAINER);
		fileSystem = JunitHelper.tryCall(() -> FileSystems.newFileSystem(TEST_URI, properties));
		targetFile = fileSystem.getRootDirectories().iterator().next().resolve(TARGET_FILE);
		CONTAINER.createObject(TEST_BUCKET, TARGET_FILE, smallFile);
		client = S3AsyncClient.builder()
							  .region(Region.US_EAST_1)
							  .credentialsProvider(() -> AwsBasicCredentials.create(CONTAINER.getAccessKey(),
																					CONTAINER.getSecretKey()))
							  .build();
	}

	@Test
	void overwriteTest()
	{
		overwrite("path/to/myfile.txt", tinyFile).whenComplete((result, throwable) ->
		{
			if (throwable != null)
			{
				System.err.println("Errore: " + throwable.getMessage());
				throwable.printStackTrace();
			}
			else
			{
				System.out.println("Operazione completata con successo!");
			}
			client.close();
		}).join(); // Aspetta il completamento per questo esempio
	}

	private Path createFile(String fileName, long size) throws IOException
	{
		var result = baseDirectory.resolve(fileName);
		IoHelper.createNotEmptyFile(result, size);
		return result;
	}

	private CompletableFuture<Void> overwrite(String key, Path sourcePath)
	{
		try
		{
			var sourceSize = Files.size(sourcePath);
			return client.headObject(HeadObjectRequest.builder()
													  .bucket(TEST_BUCKET)
													  .key(key)
													  .build())
						 .thenCompose(headResponse ->
						 {
							 long existingFileSize = headResponse.contentLength();

							 if (sourceSize >= existingFileSize)
							 {
								 // Caso 1: Sostituisci completamente
								 System.out.println("Sostituendo file completamente...");
								 return uploadCompleteFileAsync(TEST_BUCKET,
																key,
																sourcePath).thenRun(() -> System.out.println("File sostituito. Nuova dimensione: " + sourceSize + " bytes"));
							 }
							 else
							 {
								 // Caso 2: Unisci in modo ottimizzato
								 System.out.println("Unendo file con contenuto esistente...");
								 if (existingFileSize < PART_SIZE * 2)
								 {
									 return mergeSmallFileAsync(TEST_BUCKET,
																key,
																sourcePath,
																sourceSize,
																existingFileSize);
								 }
								 else
								 {
									 return mergeWithMultipartAsync(TEST_BUCKET,
																	key,
																	sourcePath,
																	sourceSize,
																	existingFileSize);
								 }
							 }
						 })
						 .exceptionallyCompose(throwable ->
						 {
							 // Gestisce il caso in cui il file non esiste su S3
							 if (throwable.getCause() instanceof NoSuchKeyException)
							 {
								 System.out.println("File non esistente su S3. Caricando nuovo file...");
								 return uploadCompleteFileAsync(TEST_BUCKET,
																key,
																sourcePath).thenRun(() -> System.out.println("Nuovo file caricato: " + sourceSize + " bytes"));
							 }
							 return CompletableFuture.failedFuture(throwable);
						 });

		}
		catch (IOException e)
		{
			return CompletableFuture.failedFuture(e);
		}
	}

	private CompletableFuture<Void> uploadCompleteFileAsync(String bucketName,
															String key,
															Path localPath)
	{
		return client.putObject(PutObjectRequest.builder().bucket(bucketName).key(key).build(),
								AsyncRequestBody.fromFile(localPath))
					 .thenRun(() ->
					 {
					 });
	}

	private CompletableFuture<Void> mergeSmallFileAsync(String bucketName,
														String key,
														Path localPath,
														long newFileSize,
														long existingFileSize)
	{
		// Scarica la parte eccedente in modo asincrono
		return downloadFileRangeAsync(bucketName,
									  key,
									  newFileSize,
									  existingFileSize - 1).thenCompose(excessiveData ->
									  {
										  try
										  {
											  // Crea file temporaneo con contenuto unito
											  File tempFile = File.createTempFile("s3_async_merge_",
																				  ".tmp");
											  tempFile.deleteOnExit();

											  try (FileOutputStream fos = new FileOutputStream(tempFile);
												   FileInputStream newFileStream = new FileInputStream(localPath.toFile()))
											  {

												  newFileStream.transferTo(fos);
												  fos.write(excessiveData);
											  }

											  // Carica il file unito
											  return client.putObject(PutObjectRequest.builder()
																					  .bucket(bucketName)
																					  .key(key)
																					  .build(),
																	  AsyncRequestBody.fromFile(tempFile.toPath()))
														   .thenRun(() ->
														   {
															   tempFile.delete();
															   System.out.println("File piccolo unito. Dimensione finale: " + existingFileSize + " bytes");
														   });

										  }
										  catch (IOException e)
										  {
											  return CompletableFuture.failedFuture(e);
										  }
									  });
	}

	private CompletableFuture<Void> mergeWithMultipartAsync(String bucketName,
															String key,
															Path localPath,
															long newFileSize,
															long existingFileSize)
	{
		// Inizia multipart upload
		return client.createMultipartUpload(CreateMultipartUploadRequest.builder()
																		.bucket(bucketName)
																		.key(key)
																		.build())
					 .thenCompose(multipartResponse ->
					 {
						 String uploadId = multipartResponse.uploadId();
						 List<CompletableFuture<CompletedPart>> partFutures = new ArrayList<>();

						 // Part 1: Carica il nuovo contenuto
						 partFutures.add(uploadNewContentPartAsync(bucketName,
																   key,
																   uploadId,
																   localPath,
																   1));

						 // Part 2+: Copia la parte rimanente usando server-side copy
						 partFutures.addAll(copyRemainingPartsAsync(bucketName,
																	key,
																	uploadId,
																	newFileSize,
																	existingFileSize,
																	2));

						 // Attendi il completamento di tutte le parti
						 return CompletableFuture.allOf(partFutures.toArray(new CompletableFuture[0]))
												 .thenCompose(v ->
												 {
													 // Raccogli i risultati delle parti
													 List<CompletedPart> completedParts = new ArrayList<>();
													 for (CompletableFuture<CompletedPart> future : partFutures)
													 {
														 completedParts.add(future.join());
													 }

													 // Completa il multipart upload
													 return client.completeMultipartUpload(CompleteMultipartUploadRequest.builder()
																														 .bucket(bucketName)
																														 .key(key)
																														 .uploadId(uploadId)
																														 .multipartUpload(CompletedMultipartUpload.builder()
																																								  .parts(completedParts)
																																								  .build())
																														 .build());
												 })
												 .thenRun(() -> System.out.println("File grande unito con multipart. Dimensione finale: " + existingFileSize + " bytes"))
												 .exceptionallyCompose(throwable ->
												 {
													 // In caso di errore, cancella il
													 // multipart upload
													 return client.abortMultipartUpload(AbortMultipartUploadRequest.builder()
																												   .bucket(bucketName)
																												   .key(key)
																												   .uploadId(uploadId)
																												   .build())
																  .thenCompose(v -> CompletableFuture.failedFuture(throwable));
												 });
					 });
	}

	private CompletableFuture<byte[]> downloadFileRangeAsync(String bucketName,
															 String key,
															 long startByte,
															 long endByte)
	{
		GetObjectRequest getRequest = GetObjectRequest.builder()
													  .bucket(bucketName)
													  .key(key)
													  .range("bytes=" + startByte + "-" + endByte)
													  .build();
		return client.getObject(getRequest, AsyncResponseTransformer.toBytes())
					 .thenApply(response -> response.asByteArray());
	}

	private CompletableFuture<CompletedPart> uploadNewContentPartAsync(String bucketName,
																	   String key,
																	   String uploadId,
																	   Path localPath,
																	   int partNumber)
	{
		return client.uploadPart(UploadPartRequest.builder()
												  .bucket(bucketName)
												  .key(key)
												  .uploadId(uploadId)
												  .partNumber(partNumber)
												  .build(),
								 AsyncRequestBody.fromFile(localPath))
					 .thenApply(response -> CompletedPart.builder()
														 .partNumber(partNumber)
														 .eTag(response.eTag())
														 .build());
	}

	private List<CompletableFuture<CompletedPart>> copyRemainingPartsAsync(String bucketName,
																		   String key,
																		   String uploadId,
																		   long newFileSize,
																		   long existingFileSize,
																		   int startPartNumber)
	{

		List<CompletableFuture<CompletedPart>> futures = new ArrayList<>();
		String tempKey = key + ".temp_" + System.currentTimeMillis();

		// Prima copia il file originale in una chiave temporanea
		CompletableFuture<Void> copyOriginal = client.copyObject(CopyObjectRequest.builder()
																				  .sourceBucket(bucketName)
																				  .sourceKey(key)
																				  .destinationBucket(bucketName)
																				  .destinationKey(tempKey)
																				  .build())
													 .thenRun(() ->
													 {
													 });

		long remainingBytes = existingFileSize - newFileSize;
		long currentOffset = newFileSize;
		int partNumber = startPartNumber;

		while (remainingBytes > 0)
		{
			long partSize = Math.min(remainingBytes, PART_SIZE);
			long endByte = currentOffset + partSize - 1;
			final int currentPartNumber = partNumber;
			final long currentStart = currentOffset;

			// Crea future per ogni parte che dipende dalla copia originale
			CompletableFuture<CompletedPart> partFuture = copyOriginal.thenCompose(v -> client.uploadPartCopy(UploadPartCopyRequest.builder()
																																   .sourceBucket(bucketName)
																																   .sourceKey(tempKey)
																																   .destinationBucket(bucketName)
																																   .destinationKey(key)
																																   .uploadId(uploadId)
																																   .partNumber(currentPartNumber)
																																   .copySourceRange("bytes=" + currentStart + "-" + endByte)
																																   .build()))
																	  .thenApply(copyResponse -> CompletedPart.builder()
																											  .partNumber(currentPartNumber)
																											  .eTag(copyResponse.copyPartResult()
																																.eTag())
																											  .build());

			futures.add(partFuture);

			currentOffset += partSize;
			remainingBytes -= partSize;
			partNumber++;
		}

		// Aggiungi cleanup della chiave temporanea dopo tutte le operazioni
		CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
						 .whenComplete((result, throwable) ->
						 {
							 client.deleteObject(DeleteObjectRequest.builder()
																	.bucket(bucketName)
																	.key(tempKey)
																	.build())
								   .exceptionally(ex ->
								   {
									   System.err.println("Avviso: Impossibile eliminare chiave temporanea: " + tempKey);
									   return null;
								   });
						 });

		return futures;
	}
}
