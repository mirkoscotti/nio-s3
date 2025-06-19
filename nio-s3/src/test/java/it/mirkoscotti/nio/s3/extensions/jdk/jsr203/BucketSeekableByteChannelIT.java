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
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.Base64;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.CleanupMode;
import org.junit.jupiter.api.io.TempDir;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * @author mirko.scotti
 * @version May 30, 2025
 */
@Testcontainers
class BucketSeekableByteChannelIT
{

	private static final String TEST_BUCKET = "test-bucket";

	private static final String TEST_FILE = "file.txt";

	private static final URI TEST_URI = URI.create("s3://".concat(TEST_BUCKET));

	private static final String CHECKSUM_ALGORITHM = "SHA-256";

	private static final int PART_SIZE = 10 * 1024 * 1024;

	@TempDir(cleanup = CleanupMode.ALWAYS)
	private static Path baseDirectory;

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

	@Test
	void singlepartWriteTest()
	{
		var fileName = "singlepart-".concat(TEST_FILE);
		var file = JunitHelper.tryCall(() -> createTestFile(fileName, PART_SIZE / 2));
		var expected = JunitHelper.tryCall(() -> singlepartChecksum(file));
		var result = JunitHelper.tryCall(() -> write(file));
		Assertions.assertEquals(expected, result);
	}

	@Test
	void multipartWriteTest()
	{
		var fileName = "multipart-".concat(TEST_FILE);
		var file = JunitHelper.tryCall(() -> createTestFile(fileName, PART_SIZE * 5 / 2));
		var expected = JunitHelper.tryCall(() -> multipartChecksum(file));
		var result = JunitHelper.tryCall(() -> write(file));
		Assertions.assertEquals(expected, result);
	}

	private Path createTestFile(String fileName, long size) throws IOException
	{
		var result = baseDirectory.resolve(fileName);
		IoHelper.createNotEmptyFile(result, size);
		return result;
	}

	private String write(Path file) throws IOException
	{
		var fileName = file.getFileName().toString();
		var path = fileSystem.getRootDirectories().iterator().next().resolve(fileName);
		try (var channel = Files.newByteChannel(path,
												StandardOpenOption.CREATE_NEW,
												StandardOpenOption.WRITE))
		{
			var buffer = ByteBuffer.wrap(Files.readAllBytes(file));
			channel.write(buffer);
		}
		return CONTAINER.checksum(TEST_BUCKET, fileName);
	}

	private String singlepartChecksum(Path file) throws GeneralSecurityException
	{
		String result;
		var fileDigest = MessageDigest.getInstance(CHECKSUM_ALGORITHM);
		try (var stream = Files.newInputStream(file))
		{
			var digest = fileDigest.digest(stream.readAllBytes());
			result = Base64.getEncoder().encodeToString(digest);
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
