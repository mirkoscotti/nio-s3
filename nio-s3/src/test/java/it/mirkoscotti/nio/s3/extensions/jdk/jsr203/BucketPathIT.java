package it.mirkoscotti.nio.s3.extensions.jdk.jsr203;

import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardWatchEventKinds;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.CleanupMode;
import org.junit.jupiter.api.io.TempDir;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import it.mirkoscotti.nio.s3.configuration.BucketDescriptor;
import it.mirkoscotti.nio.s3.extensions.testcontainers.S3Container;
import it.mirkoscotti.nio.s3.helpers.IoHelper;
import it.mirkoscotti.nio.s3.helpers.JunitHelper;

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

	private static final String FILE = DIRECTORY.concat("test.txt");

	private static final String PATTERN = "s3://%s:%s@test-bucket.s3.%s.localstack.cloud:%d";

	@TempDir(cleanup = CleanupMode.ALWAYS)
	private static Path path;

	@Container
	@SuppressWarnings("resource")
	private static final S3Container CONTAINER = new S3Container().withBucket(BUCKET_NAME);

	@BeforeAll
	static void beforeAll()
	{
		CONTAINER.createObject(BUCKET_NAME, DIRECTORY);
		var file = path.resolve("test.txt");
		JunitHelper.tryCall(() -> IoHelper.createNotEmptyFile(file));
		CONTAINER.createObject(BUCKET_NAME, FILE, file);
	}

	@Test
	void registerTest()
	{
		var uri = String.join(BucketDescriptor.PATH_SEPARATOR, PATTERN, DIRECTORY)
						.formatted(CONTAINER.getAccessKey(), CONTAINER.getSecretKey(),
								   CONTAINER.getHost(), CONTAINER.getFirstMappedPort());
		var path = Paths.get(URI.create(uri));
		var fileSystem = path.getFileSystem();
		var watchService = JunitHelper.tryCall(() -> path.getFileSystem().newWatchService());
		var watchKey = JunitHelper.tryCall(() -> path.register(watchService,
															   StandardWatchEventKinds.ENTRY_CREATE,
															   StandardWatchEventKinds.ENTRY_MODIFY,
															   StandardWatchEventKinds.ENTRY_DELETE));
		System.out.println();
	}
}
