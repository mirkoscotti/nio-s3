package it.mirkoscotti.nio.s3.extensions.jdk.jsr203;

import java.io.IOException;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

import org.awaitility.Awaitility;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.CleanupMode;
import org.junit.jupiter.api.io.TempDir;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import io.github.mirkoscotti.nio.s3.configuration.BucketDescriptor;

import it.mirkoscotti.nio.s3.extensions.testcontainers.S3Container;
import it.mirkoscotti.nio.s3.helpers.IoHelper;
import it.mirkoscotti.nio.s3.helpers.JunitHelper;

/**
 * @author mirko.scotti
 * @version Apr 03, 2025
 */
@Testcontainers
class BucketPathIT
{

	private static final Logger LOGGER = System.getLogger(BucketPathIT.class.getName());

	private static final String DIRECTORY = "directory/";

	private static final String FILE = "test.txt";

	private static final String BUCKET = "test-bucket";

	private static final String KEY = DIRECTORY.concat(FILE);

	private static final String PATTERN = "s3://%s:%s@test-bucket.s3.%s.localstack.cloud:%d";

	private static final int POLL_TIMEOUT = 5050;

	@TempDir(cleanup = CleanupMode.ALWAYS)
	private static Path baseDirectory;

	@Container
	@SuppressWarnings("resource")
	private static final S3Container CONTAINER = new S3Container().withBucket(BUCKET);

	@BeforeAll
	static void beforeAll()
	{
		CONTAINER.createObject(BUCKET, DIRECTORY);
		var file = baseDirectory.resolve(FILE);
		JunitHelper.tryCall(() -> IoHelper.createNotEmptyFile(file));
	}

	@Test
	void createEventTest()
	{
		var uri = String.join(BucketDescriptor.PATH_SEPARATOR, PATTERN, DIRECTORY)
						.formatted(CONTAINER.getAccessKey(), CONTAINER.getSecretKey(),
								   CONTAINER.getHost(), CONTAINER.getFirstMappedPort());
		var path = Paths.get(URI.create(uri));
		try (var watchService = path.getFileSystem().newWatchService())
		{
			var watchKey = path.register(watchService, StandardWatchEventKinds.ENTRY_CREATE,
										 StandardWatchEventKinds.ENTRY_MODIFY,
										 StandardWatchEventKinds.ENTRY_DELETE);
			var file = baseDirectory.resolve(FILE);
			LOGGER.log(Level.INFO, () -> "TEST - Creating file %s...".formatted(KEY));
			waitForTick(() -> CONTAINER.createObject(BUCKET, KEY, file));
			LOGGER.log(Level.INFO, () -> "TEST - Replacing file %s...".formatted(KEY));
			waitForTick(() -> CONTAINER.createObject(BUCKET, KEY, file));
			LOGGER.log(Level.INFO, () -> "TEST - Deleting file %s...".formatted(KEY));
			waitForTick(() -> CONTAINER.deleteObject(BUCKET, KEY));
			var expected = List.of(StandardWatchEventKinds.ENTRY_CREATE,
								   StandardWatchEventKinds.ENTRY_MODIFY,
								   StandardWatchEventKinds.ENTRY_DELETE);
			var result = Awaitility.await()
								   .atMost(POLL_TIMEOUT, TimeUnit.SECONDS)
								   .until(watchKey::pollEvents, Predicate.not(List::isEmpty))
								   .stream()
								   .map(WatchEvent::kind)
								   .toList();
			Assertions.assertEquals(expected.size(), result.size());
			Assertions.assertTrue(expected.containsAll(result));
			Assertions.assertTrue(result.containsAll(expected));
		}
		catch (IOException x)
		{
			Assertions.fail(x);
		}
	}

	private void waitForTick(Runnable operation)
	{
		var start = System.nanoTime();
		operation.run();
		var elapsed = System.nanoTime() - start;
		var wait = Duration.ofMillis(POLL_TIMEOUT).minus(Duration.ofNanos(elapsed));
		Awaitility.await().pollDelay(wait).until(() -> true);
	}
}
