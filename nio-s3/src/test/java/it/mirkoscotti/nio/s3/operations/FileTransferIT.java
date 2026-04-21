package it.mirkoscotti.nio.s3.operations;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.DigestInputStream;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.Base64;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.CleanupMode;
import org.junit.jupiter.api.io.TempDir;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import io.github.mirkoscotti.nio.s3.extensions.testcontainers.S3Container;
import io.github.mirkoscotti.nio.s3.helpers.ContainerHelper;
import io.github.mirkoscotti.nio.s3.helpers.IoHelper;
import io.github.mirkoscotti.nio.s3.helpers.JunitHelper;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3CrtAsyncClientBuilder;
import software.amazon.awssdk.services.s3.internal.crt.S3CrtAsyncClient;

/**
 * @author mirko.scotti
 * @version Feb 17, 2026
 */
@Testcontainers
class FileTransferIT
{

	private static final String CHECKSUM_ALGORITHM = "SHA-256";

	private static final String SOURCE_USER = "source-user";

	private static final String TARGET_USER = "target-user";

	private static final String SOURCE_BUCKET = "source-bucket";

	private static final String TARGET_BUCKET = "target-bucket";

	private static final Region SOURCE_REGION = Region.US_EAST_1;

	private static final Region TARGET_REGION = Region.US_WEST_1;

	private static final String SOURCE_FILE = "source.txt";

	private static final String TARGET_FILE = "target.txt";

	@TempDir(cleanup = CleanupMode.ALWAYS)
	private static Path baseDirectory;

	private static Path sourceFile;

	@Container
	@SuppressWarnings("resource")
	private static final S3Container CONTAINER = new S3Container().withUser(SOURCE_USER)
																  .withUser(TARGET_USER)
																  .withBucket(SOURCE_BUCKET)
																  .withBucket(TARGET_BUCKET);

	@BeforeAll
	static void beforeAll()
	{
		sourceFile = JunitHelper.tryCall(() -> createSourceFile(SOURCE_FILE, 1024));
	}

	@AfterEach
	void afterEach()
	{
		ContainerHelper.deleteObjects(CONTAINER, SOURCE_BUCKET);
		ContainerHelper.deleteObjects(CONTAINER, TARGET_BUCKET);
	}

	@Test
	void copyToSameBucket()
	{
		CONTAINER.createObjectWithChecksum(SOURCE_BUCKET, SOURCE_FILE, sourceFile);
		try (var client = clientBuilder(SOURCE_USER, SOURCE_REGION).build();
			 var stream = Files.newInputStream(sourceFile))
		{
			var fileTransfer = new FileTransfer(client, SOURCE_BUCKET, SOURCE_FILE);
			fileTransfer.transfer(client, SOURCE_BUCKET, TARGET_FILE);
			CONTAINER.checksum(SOURCE_BUCKET, TARGET_FILE)
					 .forEach((item1, item2) -> assertChecksum(stream, item2.intValue(), item1));
		}
		catch (IOException x)
		{
			Assertions.fail(x);
		}
	}

	@Test
	void copyToDifferentBucket()
	{
		CONTAINER.createObjectWithChecksum(SOURCE_BUCKET, SOURCE_FILE, sourceFile);
		try (var sourceClient = clientBuilder(SOURCE_USER, SOURCE_REGION).build();
			 var targetClient = clientBuilder(TARGET_USER, TARGET_REGION).build();
			 var stream = Files.newInputStream(sourceFile))
		{
			var fileTransfer = new FileTransfer(sourceClient, SOURCE_BUCKET, SOURCE_FILE);
			fileTransfer.transfer(targetClient, TARGET_BUCKET, TARGET_FILE);
			var map = CONTAINER.checksum(TARGET_BUCKET, TARGET_FILE);
			var lastEntry = map.pollLastEntry();
			var outputStream = new ByteArrayOutputStream();
			map.entrySet()
			   .stream()
			   .forEach(item -> writeDigest(stream, outputStream, item.getKey(), item.getValue()));
			var expected = Base64.getEncoder()
								 .encodeToString(MessageDigest.getInstance(CHECKSUM_ALGORITHM)
															  .digest(outputStream.toByteArray()));
			var result = lastEntry.getKey();
			Assertions.assertEquals(expected, result);
		}
		catch (Exception x)
		{
			Assertions.fail(x);
		}
	}

	private static Path createSourceFile(String fileName, long size) throws IOException
	{
		var result = baseDirectory.resolve(fileName);
		IoHelper.createNotEmptyFile(result, size);
		return result;
	}

	private static S3CrtAsyncClientBuilder clientBuilder(String user, Region region)
	{
		return S3CrtAsyncClient.builder()
							   .crossRegionAccessEnabled(true)
							   .endpointOverride(CONTAINER.getEndpoint())
							   .region(region)
							   .credentialsProvider(() -> AwsBasicCredentials.create(CONTAINER.getAccessKey(user),
																					 CONTAINER.getSecretKey(user)));
	}

	private void writeDigest(InputStream input, OutputStream output, String checksum, Long size)
	{
		try
		{
			var messageDigest = MessageDigest.getInstance(CHECKSUM_ALGORITHM);
			var stream = new DigestInputStream(input, messageDigest);
			stream.readNBytes(size.intValue());
			var digest = messageDigest.digest();
			Assertions.assertEquals(Base64.getEncoder().encodeToString(digest), checksum);
			output.write(digest);
		}
		catch (Exception x)
		{
			Assertions.fail(x);
		}
	}

	private void assertChecksum(InputStream stream, int size, String result)
	{
		try
		{
			var expected = checksum(stream.readNBytes(size));
			Assertions.assertEquals(expected, result);
		}
		catch (Exception x)
		{
			Assertions.fail(x);
		}
	}

	private String checksum(byte[] array) throws GeneralSecurityException
	{
		var messageDigest = MessageDigest.getInstance(CHECKSUM_ALGORITHM);
		var digest = messageDigest.digest(array);
		return Base64.getEncoder().encodeToString(digest);
	}
}
