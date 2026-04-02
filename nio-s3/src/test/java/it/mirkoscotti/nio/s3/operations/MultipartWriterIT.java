package it.mirkoscotti.nio.s3.operations;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.CleanupMode;
import org.junit.jupiter.api.io.TempDir;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import it.mirkoscotti.nio.s3.extensions.testcontainers.S3Container;
import it.mirkoscotti.nio.s3.helpers.ContainerHelper;
import it.mirkoscotti.nio.s3.helpers.JunitHelper;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AsyncClient;

/**
 * @author mirko.scotti
 * @version May 28, 2025
 */
@Testcontainers
class MultipartWriterIT
{

	private static final String TEST_BUCKET = "test-bucket";

	private static final String TEST_KEY = "test-key";

	private static final String TEST_FILE = "file.txt";

	@Container
	@SuppressWarnings("resource")
	private static final S3Container CONTAINER = new S3Container().withBucket(TEST_BUCKET);

	@TempDir(cleanup = CleanupMode.ALWAYS)
	private static Path baseDirectory;

	private static S3AsyncClient client;

	@BeforeAll
	static void beforeAll()
	{
		client = S3AsyncClient.crtBuilder()
							  .endpointOverride(CONTAINER.getEndpoint())
							  .region(Region.of(CONTAINER.getRegion()))
							  .credentialsProvider(() -> AwsBasicCredentials.create(CONTAINER.getAccessKey(),
																					CONTAINER.getSecretKey()))
							  .build();
	}

	@AfterEach
	void afterEach()
	{
		Optional.of(TEST_BUCKET)
				.filter(CONTAINER::bucketExists)
				.ifPresent(item -> ContainerHelper.deleteObjects(CONTAINER, item));
	}

	@Test
	void multipartUploadWithoutPartsTest()
	{
		try (var writer = new MultipartWriter(client, TEST_BUCKET, TEST_KEY))
		{
			var uploadId = JunitHelper.findFieldValueByName(writer, "uploadId", String.class);
			Assertions.assertNotNull(uploadId);
		}
		catch (IOException x)
		{
			Assertions.fail(x);
		}
	}

	@Test
	void multipartSingleUploadTest()
	{
		var test = "test";
		try (var writer = new MultipartWriter(client, TEST_BUCKET, TEST_KEY))
		{
			writer.write(test.getBytes(StandardCharsets.UTF_8));
		}
		catch (IOException x)
		{
			Assertions.fail(x);
		}
		Assertions.assertTrue(CONTAINER.objectExists(TEST_BUCKET, TEST_KEY));
		var output = baseDirectory.resolve(TEST_FILE);
		CONTAINER.readObject(TEST_BUCKET, TEST_KEY, output);
		Assertions.assertEquals(test.length(), JunitHelper.tryCall(() -> Files.size(output)));
		JunitHelper.tryCall(() -> Files.deleteIfExists(output));
	}

	@Test
	void multipartUploadTest()
	{
		var test = "test";
		var part = Stream.generate(() -> test)
						 .limit(5 * 1024 * 1024 / test.length())
						 .collect(Collectors.joining());
		try (var writer = new MultipartWriter(client, TEST_BUCKET, TEST_KEY))
		{
			writer.write(part.getBytes(StandardCharsets.UTF_8));
			writer.write(test.getBytes(StandardCharsets.UTF_8));
		}
		catch (IOException x)
		{
			Assertions.fail(x);
		}
		Assertions.assertTrue(CONTAINER.objectExists(TEST_BUCKET, TEST_KEY));
		var output = baseDirectory.resolve(TEST_FILE);
		CONTAINER.readObject(TEST_BUCKET, TEST_KEY, output);
		Assertions.assertEquals(test.length() + part.length(),
								JunitHelper.tryCall(() -> Files.size(output)));
		JunitHelper.tryCall(() -> Files.deleteIfExists(output));
	}
}
