package it.mirkoscotti.nio.s3.extensions.jdk.jsr203;

import java.io.IOException;
import java.net.URI;
import java.nio.file.DirectoryNotEmptyException;
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

	private static final String A = "a/";

	private static final String A_B = A.concat("b/");

	private static final String TEST_FILE = "file.txt";

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
		var directory = fileSystem.getPath(A_B);
		Assertions.assertThrows(NoSuchFileException.class, () -> Files.createDirectory(directory));
	}

	@Test
	void createDirectoryTest()
	{
		CONTAINER.createObject(TEST_BUCKET, A);
		var directory = fileSystem.getPath(A_B);
		JunitHelper.tryCall(() -> Files.createDirectories(directory));
		Assertions.assertTrue(CONTAINER.objectExists(TEST_BUCKET, A_B));
	}

	@Test
	void deleteNotEmptyDirectoryTest()
	{
		CONTAINER.createObject(TEST_BUCKET, A);
		CONTAINER.createObject(TEST_BUCKET, A_B);
		var directory = fileSystem.getPath(A);
		Assertions.assertThrows(DirectoryNotEmptyException.class, () -> Files.delete(directory));
	}

	@Test
	void deleteDirectoryTest()
	{
		CONTAINER.createObject(TEST_BUCKET, A);
		CONTAINER.createObject(TEST_BUCKET, A_B);
		var directory = fileSystem.getPath(A_B);
		try
		{
			Files.delete(directory);
			Assertions.assertFalse(CONTAINER.objectExists(TEST_BUCKET, A_B));
			Assertions.assertTrue(CONTAINER.objectExists(TEST_BUCKET, A));
		}
		catch (IOException x)
		{
			Assertions.fail(x);
		}
	}

	@Test
	void deleteFileTest()
	{
		CONTAINER.createObject(TEST_BUCKET, A);
		CONTAINER.createObject(TEST_BUCKET, A.concat(TEST_FILE));
		var file = fileSystem.getPath(A, TEST_FILE);
		try
		{
			Files.delete(file);
			Assertions.assertFalse(CONTAINER.objectExists(TEST_BUCKET, TEST_FILE));
			Assertions.assertTrue(CONTAINER.objectExists(TEST_BUCKET, A));
		}
		catch (IOException x)
		{
			Assertions.fail(x);
		}
	}
}
