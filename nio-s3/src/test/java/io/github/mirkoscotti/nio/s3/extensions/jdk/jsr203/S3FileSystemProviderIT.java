package io.github.mirkoscotti.nio.s3.extensions.jdk.jsr203;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.URI;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.Random;
import java.util.function.Predicate;
import java.util.random.RandomGenerator;
import java.util.stream.Stream;

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

	private static final String A_C = A.concat("c/");

	private static final String SOURCE_FILE = "source.txt";

	private static final String TARGET_FILE = "target.txt";

	private static final String CHECKSUM_ALGORITHM = "SHA-256";

	private static final URI TEST_URI = URI.create("s3://".concat(TEST_BUCKET));

	@TempDir(cleanup = CleanupMode.ALWAYS)
	private static Path baseDirectory;

	private static Path sourceFile;

	private static Path targetFile;

	private static FileSystem fileSystem;

	@Container
	@SuppressWarnings("resource")
	private static final S3Container CONTAINER = new S3Container().withBucket(TEST_BUCKET);

	@BeforeAll
	static void beforeAll()
	{
		var properties = ContainerHelper.standardProperties(CONTAINER);
		sourceFile = JunitHelper.tryCall(() -> createSourceFile(SOURCE_FILE, 1024));
		targetFile = JunitHelper.tryCall(() -> createSourceFile(TARGET_FILE, 512));
		fileSystem = JunitHelper.tryCall(() -> FileSystems.newFileSystem(TEST_URI, properties));
	}

	@AfterEach
	void afterEach()
	{
		ContainerHelper.deleteObjects(CONTAINER, TEST_BUCKET);
	}

	@Test
	void createRootDirectoryTest()
	{
		var directory = fileSystem.getPath("/");
		Assertions.assertThrows(FileAlreadyExistsException.class,
								() -> Files.createDirectory(directory));
	}

	@Test
	void createExistingDirectoryTest()
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
	void deleteDirectoryTest() throws IOException
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
		finally
		{
			// Nothing to do
		}
	}

	@Test
	void deleteFileTest() throws IOException
	{
		CONTAINER.createObject(TEST_BUCKET, A);
		CONTAINER.createObject(TEST_BUCKET, A.concat(SOURCE_FILE));
		var file = fileSystem.getPath(A, SOURCE_FILE);
		try
		{
			Files.delete(file);
			Assertions.assertFalse(CONTAINER.objectExists(TEST_BUCKET, SOURCE_FILE));
			Assertions.assertTrue(CONTAINER.objectExists(TEST_BUCKET, A));
		}
		finally
		{
			// Nothing to do
		}
	}

	@Test
	void copyDirectoryTest() throws IOException
	{
		CONTAINER.createObject(TEST_BUCKET, A);
		CONTAINER.createObject(TEST_BUCKET, A_B);
		var source = fileSystem.getPath(A_B);
		var target = fileSystem.getPath(A_C);
		try
		{
			Files.copy(source, target);
			Assertions.assertTrue(CONTAINER.objectExists(TEST_BUCKET, A_B));
			Assertions.assertTrue(CONTAINER.objectExists(TEST_BUCKET, A_C));
		}
		finally
		{
			// Nothing to do
		}
	}

	@Test
	void copyExistingDirectoryTest()
	{
		CONTAINER.createObject(TEST_BUCKET, A);
		CONTAINER.createObject(TEST_BUCKET, A_B);
		CONTAINER.createObject(TEST_BUCKET, A_C);
		var source = fileSystem.getPath(A_B);
		var target = fileSystem.getPath(A_C);
		Assertions.assertThrows(FileAlreadyExistsException.class, () -> Files.copy(source, target));
	}

	@Test
	void copyReplacingDirectoryTest() throws IOException
	{
		CONTAINER.createObject(TEST_BUCKET, A);
		CONTAINER.createObject(TEST_BUCKET, A_B);
		CONTAINER.createObject(TEST_BUCKET, A_C);
		var source = fileSystem.getPath(A_B);
		var target = fileSystem.getPath(A_C);
		try
		{
			Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
			Assertions.assertTrue(CONTAINER.objectExists(TEST_BUCKET, A_B));
			Assertions.assertTrue(CONTAINER.objectExists(TEST_BUCKET, A_C));
		}
		finally
		{
			// Nothing to do
		}
	}

	@Test
	void copyFileTest() throws IOException
	{
		CONTAINER.createObjectWithChecksum(TEST_BUCKET, SOURCE_FILE, sourceFile);
		var source = fileSystem.getPath("/".concat(SOURCE_FILE));
		var target = fileSystem.getPath("/".concat(TARGET_FILE));
		try (var stream = Files.newInputStream(sourceFile))
		{
			Files.copy(source, target);
			Assertions.assertTrue(CONTAINER.objectExists(TEST_BUCKET, TARGET_FILE));
			CONTAINER.checksum(TEST_BUCKET, TARGET_FILE)
					 .forEach((item1, item2) -> assertChecksum(stream, item2.intValue(), item1));
		}
	}

	@Test
	void copyReplacingFileTest() throws IOException
	{
		CONTAINER.createObjectWithChecksum(TEST_BUCKET, SOURCE_FILE, sourceFile);
		CONTAINER.createObjectWithChecksum(TEST_BUCKET, TARGET_FILE, targetFile);
		var source = fileSystem.getPath("/".concat(SOURCE_FILE));
		var target = fileSystem.getPath("/".concat(TARGET_FILE));
		try (var stream = Files.newInputStream(sourceFile))
		{
			Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
			Assertions.assertTrue(CONTAINER.objectExists(TEST_BUCKET, TARGET_FILE));
			CONTAINER.checksum(TEST_BUCKET, TARGET_FILE)
					 .forEach((item1, item2) -> assertChecksum(stream, item2.intValue(), item1));
		}
	}

	@Test
	void moveFileTest() throws IOException
	{
		CONTAINER.createObjectWithChecksum(TEST_BUCKET, SOURCE_FILE, sourceFile);
		var source = fileSystem.getPath("/".concat(SOURCE_FILE));
		var target = fileSystem.getPath("/".concat(TARGET_FILE));
		try (var stream = Files.newInputStream(sourceFile))
		{
			Files.move(source, target);
			Assertions.assertFalse(CONTAINER.objectExists(TEST_BUCKET, SOURCE_FILE));
			Assertions.assertTrue(CONTAINER.objectExists(TEST_BUCKET, TARGET_FILE));
			CONTAINER.checksum(TEST_BUCKET, TARGET_FILE)
					 .forEach((item1, item2) -> assertChecksum(stream, item2.intValue(), item1));
		}
	}

	@Test
	void moveReplacingFileTest() throws IOException
	{
		CONTAINER.createObjectWithChecksum(TEST_BUCKET, SOURCE_FILE, sourceFile);
		CONTAINER.createObjectWithChecksum(TEST_BUCKET, TARGET_FILE, targetFile);
		var source = fileSystem.getPath("/".concat(SOURCE_FILE));
		var target = fileSystem.getPath("/".concat(TARGET_FILE));
		try (var stream = Files.newInputStream(sourceFile))
		{
			Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
			Assertions.assertFalse(CONTAINER.objectExists(TEST_BUCKET, SOURCE_FILE));
			Assertions.assertTrue(CONTAINER.objectExists(TEST_BUCKET, TARGET_FILE));
			CONTAINER.checksum(TEST_BUCKET, TARGET_FILE)
					 .forEach((item1, item2) -> assertChecksum(stream, item2.intValue(), item1));
		}
	}

	@Test
	void readEmptyAttributesTest()
	{
		CONTAINER.createObjectWithChecksum(TEST_BUCKET, SOURCE_FILE, sourceFile);
		var source = fileSystem.getPath("/".concat(SOURCE_FILE));
		var result = JunitHelper.tryCall(() -> Files.readAttributes(source, ""));
		Assertions.assertTrue(result.isEmpty());
	}

	@Test
	void readAllAttributesTest()
	{
		readAllAttributes("");
	}

	@Test
	void readAllAttributesWithViewNameTest()
	{
		readAllAttributes("basic:");
	}

	@Test
	void readAllAttributesFromUnsupportedViewTest()
	{
		CONTAINER.createObjectWithChecksum(TEST_BUCKET, SOURCE_FILE, sourceFile);
		var source = fileSystem.getPath("/".concat(SOURCE_FILE));
		JunitHelper.tryCall(() -> Assertions.assertThrows(UnsupportedOperationException.class,
														  () -> Files.readAttributes(source,
																					 "unsupported:*")));
	}

	@Test
	void readFilteredAttributesTest()
	{
		readFilteredAttributes("");
	}

	@Test
	void readFilteredAttributesWithViewNameTest()
	{
		readFilteredAttributes("basic:");
	}

	@Test
	void readUnsupportedAttributesWithViewNameTest()
	{
		readFilteredAttributes("basic:");
	}

	private static Path createSourceFile(String fileName, long size) throws IOException
	{
		var result = baseDirectory.resolve(fileName);
		IoHelper.createNotEmptyFile(result, size);
		return result;
	}

	private void readAllAttributes(String viewName)
	{
		CONTAINER.createObjectWithChecksum(TEST_BUCKET, SOURCE_FILE, sourceFile);
		var source = fileSystem.getPath("/".concat(SOURCE_FILE));
		var result = JunitHelper.tryCall(() -> Files.readAttributes(source, viewName.concat("*")));
		Stream.of(BasicFileAttributes.class.getDeclaredMethods())
			  .forEach(item -> Assertions.assertTrue(result.containsKey(item.getName())));
	}

	private void readFilteredAttributes(String viewName)
	{
		CONTAINER.createObjectWithChecksum(TEST_BUCKET, SOURCE_FILE, sourceFile);
		var source = fileSystem.getPath("/".concat(SOURCE_FILE));
		var random = Random.from(RandomGenerator.getDefault());
		var methods = BasicFileAttributes.class.getDeclaredMethods();
		var list = Stream.of(methods)
						 .filter(item -> random.nextBoolean())
						 .map(Method::getName)
						 .toList();
		var attributes = viewName.concat(String.join(",", list.toArray(String[]::new)));
		var result = JunitHelper.tryCall(() -> Files.readAttributes(source, attributes));
		list.forEach(item -> Assertions.assertTrue(result.containsKey(item)));
		Stream.of(methods)
			  .map(Method::getName)
			  .filter(Predicate.not(list::contains))
			  .forEach(item -> Assertions.assertFalse(result.containsKey(item)));
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
