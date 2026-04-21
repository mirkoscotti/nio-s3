package io.github.mirkoscotti.nio.s3.extensions.jdk.jsr203;

import java.io.IOException;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

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

	private static final String TEXT_PATTERN = "file-%d.txt";

	private static final String PDF_PATTERN = "file-%d.pdf";

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
		createFilesInLocalDirectory(TEXT_PATTERN);
		createFilesInLocalDirectory(PDF_PATTERN);
		// Copy files to remote root directory
		copyFilesToRemoteDirectory(TEXT_PATTERN, "");
		copyFilesToRemoteDirectory(PDF_PATTERN, "");
		// Copy file to remote target directory
		copyFilesToRemoteDirectory(TEXT_PATTERN, TARGET_DIRECTORY);
		copyFilesToRemoteDirectory(PDF_PATTERN, TARGET_DIRECTORY);
		// Copy file to remote target sub-directory
		copyFilesToRemoteDirectory(TEXT_PATTERN, TARGET_SUB_DIRECTORY);
		copyFilesToRemoteDirectory(PDF_PATTERN, TARGET_SUB_DIRECTORY);
		// Copy file to remote other directory
		copyFilesToRemoteDirectory(TEXT_PATTERN, OTHER_DIRECTORY);
		copyFilesToRemoteDirectory(PDF_PATTERN, OTHER_DIRECTORY);
		// Copy file to remote other sub-directory
		copyFilesToRemoteDirectory(TEXT_PATTERN, OTHER_SUB_DIRECTORY);
		copyFilesToRemoteDirectory(PDF_PATTERN, OTHER_SUB_DIRECTORY);
		var properties = ContainerHelper.standardProperties(CONTAINER);
		fileSystem = JunitHelper.tryCall(() -> FileSystems.newFileSystem(TEST_URI, properties));
	}

	@Test
	void directoryStreamTest()
	{
		var target = fileSystem.getPath(TARGET_DIRECTORY);
		var other = fileSystem.getPath(OTHER_DIRECTORY);
		try (var stream = Files.newDirectoryStream(target))
		{
			var list = StreamSupport.stream(stream.spliterator(), false).toList();
			Assertions.assertFalse(list.contains(target));
			assertSelected(TEXT_PATTERN, target, list);
			assertSelected(PDF_PATTERN, target, list);
			assertNotSelected(TEXT_PATTERN, other, list);
			assertNotSelected(PDF_PATTERN, other, list);
			var subDirectory = target.resolve(SUB_DIRECTORY);
			Assertions.assertTrue(list.contains(subDirectory));
			assertNotSelected(TEXT_PATTERN, subDirectory, list);
			assertNotSelected(PDF_PATTERN, subDirectory, list);
			Assertions.assertFalse(list.contains(other));
			subDirectory = other.resolve(SUB_DIRECTORY);
			Assertions.assertFalse(list.contains(subDirectory));
			assertNotSelected(TEXT_PATTERN, subDirectory, list);
			assertNotSelected(PDF_PATTERN, subDirectory, list);
		}
		catch (IOException x)
		{
			Assertions.fail(x);
		}
	}

	@Test
	void filteredDirectoryStreamTest()
	{
		var target = fileSystem.getPath(TARGET_DIRECTORY);
		try (var stream = Files.newDirectoryStream(target, "*.txt"))
		{
			var list = StreamSupport.stream(stream.spliterator(), false).toList();
			Assertions.assertFalse(list.contains(target));
			assertSelected(TEXT_PATTERN, target, list);
			assertNotSelected(PDF_PATTERN, target, list);
		}
		catch (IOException x)
		{
			Assertions.fail(x);
		}
	}

	private static void createFilesInLocalDirectory(String pattern)
	{
		IntStream.rangeClosed(1, FILES_COUNT)
				 .mapToObj(pattern::formatted)
				 .map(baseDirectory::resolve)
				 .forEach(item -> JunitHelper.tryCall(() -> IoHelper.createNotEmptyFile(item)));
	}

	private static void copyFilesToRemoteDirectory(String pattern, String directory)
	{
		IntStream.rangeClosed(1, FILES_COUNT)
				 .mapToObj(pattern::formatted)
				 .forEach(item -> CONTAINER.createObject(TEST_BUCKET, directory.concat(item),
														 baseDirectory.resolve(item)));
	}

	private void assertSelected(String pattern, Path directory, List<Path> list)
	{
		assertSelection(pattern, directory, list).forEach(Assertions::assertTrue);
	}

	private void assertNotSelected(String pattern, Path directory, List<Path> list)
	{
		assertSelection(pattern, directory, list).forEach(Assertions::assertFalse);
	}

	private Stream<Boolean> assertSelection(String pattern, Path directory, List<Path> list)
	{
		return IntStream.rangeClosed(1, FILES_COUNT)
						.mapToObj(pattern::formatted)
						.map(directory::resolve)
						.map(list::contains);
	}
}
