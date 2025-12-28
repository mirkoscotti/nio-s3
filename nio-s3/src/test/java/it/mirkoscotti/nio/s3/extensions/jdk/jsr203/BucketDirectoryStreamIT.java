package it.mirkoscotti.nio.s3.extensions.jdk.jsr203;

import java.io.IOException;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.IntStream;
import java.util.stream.StreamSupport;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.CleanupMode;
import org.junit.jupiter.api.io.TempDir;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import it.mirkoscotti.nio.s3.extensions.testcontainers.S3Container;
import it.mirkoscotti.nio.s3.helpers.ContainersHelper;
import it.mirkoscotti.nio.s3.helpers.IoHelper;
import it.mirkoscotti.nio.s3.helpers.JunitHelper;

/**
 * @author mirko.scotti
 * @version Dec 27, 2025
 */
@Testcontainers
class BucketDirectoryStreamIT
{

	private static final String TEST_BUCKET = "test-bucket";

	private static final String TARGET_DIRECTORY = "target-directory/";

	private static final String OTHER_DIRECTORY = "other-directory/";

	private static final String SUB_DIRECTORY = "sub-directory/";

	private static final String TARGET_SUB_DIRECTORY = TARGET_DIRECTORY.concat(SUB_DIRECTORY);

	private static final String OTHER_SUB_DIRECTORY = OTHER_DIRECTORY.concat(SUB_DIRECTORY);

	private static final String FILE_PATTERN = "file-%d.txt";

	private static final int FILES_COUNT = 5;

	private static final URI TEST_URI = URI.create("s3://".concat(TEST_BUCKET));

	private static FileSystem fileSystem;

	@Container
	@SuppressWarnings("resource")
	private static final S3Container CONTAINER = new S3Container().withBucket(TEST_BUCKET);

	@TempDir(cleanup = CleanupMode.ALWAYS)
	private static Path baseDirectory;

	@BeforeAll
	static void beforeAll()
	{
		// Create the root directories
		CONTAINER.createObject(TEST_BUCKET, TARGET_DIRECTORY);
		CONTAINER.createObject(TEST_BUCKET, OTHER_DIRECTORY);
		// Create the sub-directories
		CONTAINER.createObject(TEST_BUCKET, TARGET_SUB_DIRECTORY);
		CONTAINER.createObject(TEST_BUCKET, OTHER_SUB_DIRECTORY);
		// Create files in local directory
		IntStream.rangeClosed(1, FILES_COUNT)
				 .mapToObj(FILE_PATTERN::formatted)
				 .map(baseDirectory::resolve)
				 .forEach(item -> JunitHelper.tryCall(() -> IoHelper.createNotEmptyFile(item)));
		// Copy files to remote root directory
		IntStream.rangeClosed(1, FILES_COUNT)
				 .mapToObj(FILE_PATTERN::formatted)
				 .map("/"::concat)
				 .forEach(item -> CONTAINER.createObject(TEST_BUCKET, item));
		// Copy file to remote target directory
		IntStream.rangeClosed(1, FILES_COUNT)
				 .mapToObj(FILE_PATTERN::formatted)
				 .map(TARGET_DIRECTORY::concat)
				 .forEach(item -> CONTAINER.createObject(TEST_BUCKET, item));
		// Copy file to remote target sub-directory
		IntStream.rangeClosed(1, FILES_COUNT)
				 .mapToObj(FILE_PATTERN::formatted)
				 .map(TARGET_SUB_DIRECTORY::concat)
				 .forEach(item -> CONTAINER.createObject(TEST_BUCKET, item));
		// Copy file to remote other directory
		IntStream.rangeClosed(1, FILES_COUNT)
				 .mapToObj(FILE_PATTERN::formatted)
				 .map(OTHER_DIRECTORY::concat)
				 .forEach(item -> CONTAINER.createObject(TEST_BUCKET, item));
		// Copy file to remote other sub-directory
		IntStream.rangeClosed(1, FILES_COUNT)
				 .mapToObj(FILE_PATTERN::formatted)
				 .map(OTHER_SUB_DIRECTORY::concat)
				 .forEach(item -> CONTAINER.createObject(TEST_BUCKET, item));
		var properties = ContainersHelper.standardProperties(CONTAINER);
		fileSystem = JunitHelper.tryCall(() -> FileSystems.newFileSystem(TEST_URI, properties));
	}

	@Test
	void directoryStreamTest()
	{
		var target = fileSystem.getPath(TARGET_DIRECTORY);
		var other = fileSystem.getPath(OTHER_DIRECTORY);
		try (var stream = Files.newDirectoryStream(target))
		{
			var spliterator = Spliterators.spliteratorUnknownSize(stream.iterator(),
																  Spliterator.ORDERED);
			var list = StreamSupport.stream(spliterator, false).toList();
			Assertions.assertFalse(list.contains(target));
			var subDirectory = target.resolve(SUB_DIRECTORY);
			Assertions.assertTrue(list.contains(subDirectory));
			IntStream.rangeClosed(1, FILES_COUNT)
					 .mapToObj(FILE_PATTERN::formatted)
					 .map(target::resolve)
					 .forEach(item -> Assertions.assertTrue(list.contains(item)));
			IntStream.rangeClosed(1, FILES_COUNT)
					 .mapToObj(FILE_PATTERN::formatted)
					 .map(subDirectory::resolve)
					 .forEach(item -> Assertions.assertFalse(list.contains(item)));
			Assertions.assertFalse(list.contains(other));
			subDirectory = other.resolve(SUB_DIRECTORY);
			Assertions.assertFalse(list.contains(subDirectory));
			IntStream.rangeClosed(1, FILES_COUNT)
					 .mapToObj(FILE_PATTERN::formatted)
					 .map(other::resolve)
					 .forEach(item -> Assertions.assertFalse(list.contains(item)));
			IntStream.rangeClosed(1, FILES_COUNT)
					 .mapToObj(FILE_PATTERN::formatted)
					 .map(subDirectory::resolve)
					 .forEach(item -> Assertions.assertFalse(list.contains(item)));
		}
		catch (IOException x)
		{
			Assertions.fail(x);
		}
	}
}
