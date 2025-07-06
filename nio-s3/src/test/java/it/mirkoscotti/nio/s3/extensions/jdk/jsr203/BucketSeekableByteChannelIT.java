package it.mirkoscotti.nio.s3.extensions.jdk.jsr203;

import it.mirkoscotti.nio.s3.extensions.testcontainers.S3Container;
import it.mirkoscotti.nio.s3.helpers.ContainersHelper;
import it.mirkoscotti.nio.s3.helpers.IoHelper;
import it.mirkoscotti.nio.s3.helpers.JunitHelper;

import java.io.IOException;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Base64;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.api.io.CleanupMode;
import org.junit.jupiter.api.io.TempDir;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * @author mirko.scotti
 * @version May 30, 2025
 */
@Testcontainers
@TestInstance(Lifecycle.PER_CLASS)
class BucketSeekableByteChannelIT
{

	private static final String TEST_BUCKET = "test-bucket";

	private static final String TINY_FILE = "tiny.txt";

	private static final String SMALL_FILE = "small.txt";

	private static final String MEDIUM_FILE = "medium.txt";

	private static final String LARGE_FILE = "large.txt";

	private static final String TARGET_FILE = "target.txt";

	private static final URI TEST_URI = URI.create("s3://".concat(TEST_BUCKET));

	private static final String CHECKSUM_ALGORITHM = "SHA-256";

	private static final int PART_SIZE = 10 * 1024 * 1024;

	@TempDir(cleanup = CleanupMode.ALWAYS)
	private static Path baseDirectory;

	private static FileSystem fileSystem;

	private static Path tinyFile;

	private static Path smallFile;

	private static Path mediumFile;

	private static Path largeFile;

	private static Path targetFile;

	@Container
	@SuppressWarnings("resource")
	private static final S3Container CONTAINER = new S3Container().withBucket(TEST_BUCKET);

	@BeforeAll
	static void beforeAll()
	{
		tinyFile = JunitHelper.tryCall(() -> createFile(TINY_FILE, 1));
		smallFile = JunitHelper.tryCall(() -> createFile(SMALL_FILE, 1024));
		mediumFile = JunitHelper.tryCall(() -> createFile(MEDIUM_FILE, 1024 * 1024));
		largeFile = JunitHelper.tryCall(() -> createFile(LARGE_FILE, PART_SIZE * 5 / 2));
		var properties = ContainersHelper.standardProperties(CONTAINER);
		fileSystem = JunitHelper.tryCall(() -> FileSystems.newFileSystem(TEST_URI, properties));
		targetFile = fileSystem.getRootDirectories().iterator().next().resolve(TARGET_FILE);
		// var path = baseDirectory.resolve(CREATE_FILE);
		// try (var channel = Files.newByteChannel(path,
		// StandardOpenOption.WRITE,
		// StandardOpenOption.CREATE_NEW))
		// {
		// var buffer = ByteBuffer.wrap("text10".getBytes(StandardCharsets.UTF_8));
		// channel.write(buffer);
		// }
		// catch (IOException x)
		// {
		// Assertions.fail(x);
		// }
		// // File already existing
		// try (var channel = Files.newByteChannel(path, StandardOpenOption.WRITE))
		// {
		// var buffer = ByteBuffer.wrap("text2".getBytes(StandardCharsets.UTF_8));
		// channel.write(buffer);
		// }
		// catch (IOException x)
		// {
		// Assertions.fail(x);
		// }
		// throw new RuntimeException();
	}

	@AfterEach
	void afterEach()
	{
		ContainersHelper.deleteObjects(CONTAINER, TEST_BUCKET);
	}

	@Test
	void writeNotExistingFileTest()
	{
		Assertions.assertThrows(NoSuchFileException.class, () -> write(tinyFile));
	}

	@Test
	void writeExistingSinglepartFileTest()
	{
		CONTAINER.createObject(TEST_BUCKET, TARGET_FILE, smallFile);
		byte[] content;
		try (var tinyStream = Files.newInputStream(tinyFile);
			 var smallStream = Files.newInputStream(smallFile))
		{
			content = smallStream.readAllBytes();
			tinyStream.read(content);
		}
		catch (IOException x)
		{
			content = Assertions.fail(x);
		}
		var array = Arrays.copyOf(content, content.length);
		// ------ Remove -------
		try (var stream = Files.newOutputStream(baseDirectory.resolve("expected.txt"),
												StandardOpenOption.CREATE_NEW))
		{
			stream.write(array);
		}
		catch (IOException x)
		{
			content = Assertions.fail(x);
		}
		// ---------------------
		var expected = JunitHelper.tryCall(() -> singlepartChecksum(array));
		var result = JunitHelper.tryCall(() -> write(tinyFile));
		Assertions.assertEquals(expected, result);
	}

	private String write(Path file, StandardOpenOption... openOptions) throws IOException
	{
		var options = Stream.concat(Stream.of(StandardOpenOption.WRITE), Stream.of(openOptions))
							.toArray(StandardOpenOption[]::new);
		try (var channel = Files.newByteChannel(targetFile, options))
		{
			var buffer = ByteBuffer.wrap(Files.readAllBytes(file));
			channel.write(buffer);
		}
		var checksum = CONTAINER.checksum(TEST_BUCKET, TARGET_FILE);
		return checksum.get(checksum.size() - 1);
	}

	private String singlepartChecksum(byte[] array) throws GeneralSecurityException
	{
		var fileDigest = MessageDigest.getInstance(CHECKSUM_ALGORITHM);
		var digest = fileDigest.digest(array);
		return Base64.getEncoder().encodeToString(digest);
	}

	private static Path createFile(String fileName, long size) throws IOException
	{
		var result = baseDirectory.resolve(fileName);
		IoHelper.createNotEmptyFile(result, size);
		return result;
	}

	// ------------ SONO ARRIVATO QUI -------------------

	/*
	 * Write a large file using only StandardOpenOption.WRITE
	 */
	// @Test
	void multipartWriteTest()
	{
		// var fileName = "multipart-".concat(TEST_FILE);
		// var file = JunitHelper.tryCall(() -> createTestFile(fileName, PART_SIZE * 5 / 2));
		// var expected = JunitHelper.tryCall(() -> multipartChecksum(file));
		// var result = JunitHelper.tryCall(() -> write(file));
		// Assertions.assertEquals(expected, result);
	}

	private String singlepartChecksum(Path file) throws GeneralSecurityException
	{
		String result;
		try (var stream = Files.newInputStream(file))
		{
			result = singlepartChecksum(stream.readAllBytes());
		}
		catch (IOException x)
		{
			result = Assertions.fail(x);
		}
		return result;
	}

	private String multipartChecksum(Path file) throws GeneralSecurityException
	{
		String result;
		var fileDigest = MessageDigest.getInstance(CHECKSUM_ALGORITHM);
		var partDigest = MessageDigest.getInstance(CHECKSUM_ALGORITHM);
		try (var stream = Files.newInputStream(file))
		{
			var bytesLeft = Files.size(file);
			while (bytesLeft > 0)
			{
				var buffer = stream.readNBytes(PART_SIZE);
				bytesLeft -= buffer.length;
				partDigest.update(buffer);
				var digest = partDigest.digest();
				fileDigest.update(digest);
				if (bytesLeft > 0)
				{
					partDigest.reset();
				}
			}
			var digest = fileDigest.digest();
			result = Base64.getEncoder().encodeToString(digest);
		}
		catch (IOException x)
		{
			result = Assertions.fail(x);
		}
		return result;
	}
}
