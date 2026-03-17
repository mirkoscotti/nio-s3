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
		CONTAINER.createObject(BUCKET_NAME, SUB_DIRECTORY);
		var file = path.resolve("test.txt");
		JunitHelper.tryCall(() -> IoHelper.createNotEmptyFile(file));
		CONTAINER.createObject(BUCKET_NAME, FILE, file);
	}

	@Test
	void registerTest()
	{
		var pattern = "s3://%s:%s@test-bucket.s3.%s.localstack.cloud:%d/directory/";
		var uri = pattern.formatted(CONTAINER.getAccessKey(), CONTAINER.getSecretKey(),
									CONTAINER.getHost(), CONTAINER.getFirstMappedPort());
		var path = Paths.get(URI.create(uri));
		var watchService = JunitHelper.tryCall(() -> path.getFileSystem().newWatchService());
		var watchKey = JunitHelper.tryCall(() -> path.register(watchService,
															   StandardWatchEventKinds.ENTRY_CREATE,
															   StandardWatchEventKinds.ENTRY_MODIFY,
															   StandardWatchEventKinds.ENTRY_DELETE));
		System.out.println();
	}
}
