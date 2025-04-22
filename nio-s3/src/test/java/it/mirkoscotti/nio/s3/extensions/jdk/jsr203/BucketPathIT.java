package it.mirkoscotti.nio.s3.extensions.jdk.jsr203;

import it.mirkoscotti.nio.s3.extensions.testcontainers.S3Container;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.nio.file.StandardWatchEventKinds;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.CleanupMode;
import org.junit.jupiter.api.io.TempDir;
import org.junit.platform.commons.function.Try;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * @author mirko.scotti
 * @version Apr 03, 2025
 */
@Disabled
@Testcontainers
class BucketPathIT
{

	private static final String BUCKET_NAME = "test-bucket";

	private static final String DIRECTORY = "directory/";

	private static final String SUB_DIRECTORY = DIRECTORY.concat("a/");

	private static final String FILE = DIRECTORY.concat("test.txt");

	@TempDir(cleanup = CleanupMode.ALWAYS)
	private static Path path;

	@Container
	private static final S3Container CONTAINER = new S3Container();

	@BeforeAll
	static void beforeAll()
	{
		CONTAINER.createBucket(BUCKET_NAME);
		// CONTAINER.createObject(BUCKET_NAME, DIRECTORY);
		CONTAINER.createObject(BUCKET_NAME, SUB_DIRECTORY);
		var file = Try.call(BucketPathIT::writeFile).getOrThrow(IllegalStateException::new);
		CONTAINER.createObject(BUCKET_NAME, FILE, file);
	}

	@Test
	void registerTest()
	{
		var pattern = "s3://%s:%s@test-bucket.s3.%s.localstack.cloud:%d/directory/";
		var uri = pattern.formatted(CONTAINER.getAccessKey(),
									CONTAINER.getSecretKey(),
									CONTAINER.getHost(),
									CONTAINER.getFirstMappedPort());
		var path = Paths.get(URI.create(uri));
		var watchService = Try.call(() -> path.getFileSystem().newWatchService())
							  .toOptional()
							  .orElseGet(Assertions::fail);
		var watchKey = Try.call(() -> path.register(watchService,
													StandardWatchEventKinds.ENTRY_CREATE,
													StandardWatchEventKinds.ENTRY_MODIFY,
													StandardWatchEventKinds.ENTRY_DELETE))
						  .toOptional()
						  .orElseGet(Assertions::fail);
		System.out.println();
	}

	private static Path writeFile() throws IOException
	{
		var result = path.resolve("test.txt");
		try (var writer = Files.newBufferedWriter(result, StandardOpenOption.CREATE_NEW))
		{
			writer.write("test");
		}
		return result;
	}
}
