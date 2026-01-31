package it.mirkoscotti.nio.s3.extensions.jdk.jsr203;

import java.net.URI;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import it.mirkoscotti.nio.s3.extensions.testcontainers.S3Container;
import it.mirkoscotti.nio.s3.helpers.ContainersHelper;
import it.mirkoscotti.nio.s3.helpers.JunitHelper;

/**
 * @author mirko.scotti
 * @version Jan 25, 2026
 */
@Testcontainers
class S3FileSystemProviderIT
{

	private static final String TEST_BUCKET = "test-bucket";

	private static final URI TEST_URI = URI.create("s3://".concat(TEST_BUCKET));

	private static FileSystem fileSystem;

	@Container
	@SuppressWarnings("resource")
	private static final S3Container CONTAINER = new S3Container().withBucket(TEST_BUCKET);

	@BeforeAll
	static void beforeAll()
	{
		var properties = ContainersHelper.standardProperties(CONTAINER);
		fileSystem = JunitHelper.tryCall(() -> FileSystems.newFileSystem(TEST_URI, properties));
	}

	@AfterEach
	void afterEach()
	{
		ContainersHelper.deleteObjects(CONTAINER, TEST_BUCKET);
	}

	@Test
	void createRootDirectoryTest()
	{
		var directory = fileSystem.getPath("/");
		Assertions.assertThrows(FileAlreadyExistsException.class,
								() -> Files.createDirectory(directory));
	}

	@Test
	void createAlreadyExistingDirectoryTest()
	{
		var path = "a/";
		CONTAINER.createObject(TEST_BUCKET, path);
		var directory = fileSystem.getPath(path);
		Assertions.assertThrows(FileAlreadyExistsException.class,
								() -> Files.createDirectory(directory));
	}

	@Test
	void createDirectoryWhenParentDoesNotExistTest()
	{
		var path = "a/b";
		var directory = fileSystem.getPath(path);
		Assertions.assertThrows(NoSuchFileException.class, () -> Files.createDirectory(directory));
	}
}
