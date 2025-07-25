package it.mirkoscotti.nio.s3.extensions.jdk.jsr203;

import java.io.ByteArrayInputStream;
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
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
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

import it.mirkoscotti.nio.s3.extensions.testcontainers.S3Container;
import it.mirkoscotti.nio.s3.helpers.ContainersHelper;
import it.mirkoscotti.nio.s3.helpers.IoHelper;
import it.mirkoscotti.nio.s3.helpers.JunitHelper;

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

	private static final String HUGE_FILE = "huge.txt";

	private static final String TARGET_FILE = "target.txt";

	private static final URI TEST_URI = URI.create("s3://".concat(TEST_BUCKET));

	private static final String CHECKSUM_ALGORITHM = "SHA-256";

	private static final int PART_SIZE = 10 * 1024 * 1024;

	@Container
	@SuppressWarnings("resource")
	private static final S3Container CONTAINER = new S3Container().withBucket(TEST_BUCKET);

	@TempDir(cleanup = CleanupMode.ALWAYS)
	private static Path baseDirectory;

	private static FileSystem fileSystem;

	private static Path tinyFile;

	private static Path smallFile;

	private static Path mediumFile;

	private static Path largeFile;

	private static Path hugeFile;

	private static Path targetFile;

	@BeforeAll
	static void beforeAll()
	{
		tinyFile = JunitHelper.tryCall(() -> createFile(TINY_FILE, 1));
		smallFile = JunitHelper.tryCall(() -> createFile(SMALL_FILE, 1024));
		mediumFile = JunitHelper.tryCall(() -> createFile(MEDIUM_FILE, 1024 * 1024));
		largeFile = JunitHelper.tryCall(() -> createFile(LARGE_FILE, PART_SIZE * 5 / 2));
		hugeFile = JunitHelper.tryCall(() -> createFile(HUGE_FILE, PART_SIZE * 10 / 2));
		var properties = ContainersHelper.standardProperties(CONTAINER);
		fileSystem = JunitHelper.tryCall(() -> FileSystems.newFileSystem(TEST_URI, properties));
		targetFile = fileSystem.getRootDirectories().iterator().next().resolve(TARGET_FILE);
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
	void singlepartOverwritingLargerSinglepartFileTest()
	{
		CONTAINER.createObject(TEST_BUCKET, TARGET_FILE, smallFile);
		JunitHelper.tryRun(() -> write(tinyFile));
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
		var expected = JunitHelper.tryCall(() -> singlepartChecksum(array));
		var result = CONTAINER.checksum(TEST_BUCKET, TARGET_FILE).lastEntry().getKey();
		Assertions.assertEquals(expected, result);
	}

	@Test
	void singlepartOverwritingLargerMultipartFileTest()
	{
		CONTAINER.createObject(TEST_BUCKET, TARGET_FILE, largeFile);
		JunitHelper.tryRun(() -> write(tinyFile));
		byte[] content;
		try (var tinyStream = Files.newInputStream(tinyFile);
			 var largeStream = Files.newInputStream(largeFile))
		{
			content = largeStream.readAllBytes();
			tinyStream.read(content);
		}
		catch (IOException x)
		{
			content = Assertions.fail(x);
		}
		var array = Arrays.copyOf(content, content.length);
		var size = JunitHelper.tryCall(() -> (int) Files.size(tinyFile));
		var expected = JunitHelper.tryCall(() -> multipartChecksum(array, size));
		var result = CONTAINER.checksum(TEST_BUCKET, TARGET_FILE);
		Assertions.assertEquals(expected, result);
	}

	@Test
	void singlepartOverwritingSmallerSinglepartFileTest()
	{
		CONTAINER.createObject(TEST_BUCKET, TARGET_FILE, smallFile);
		JunitHelper.tryRun(() -> write(mediumFile));
		byte[] content;
		try (var mediumStream = Files.newInputStream(mediumFile))
		{
			content = mediumStream.readAllBytes();
		}
		catch (IOException x)
		{
			content = Assertions.fail(x);
		}
		var array = Arrays.copyOf(content, content.length);
		var expected = JunitHelper.tryCall(() -> singlepartChecksum(array));
		var result = CONTAINER.checksum(TEST_BUCKET, TARGET_FILE).lastEntry().getKey();
		Assertions.assertEquals(expected, result);
	}

	@Test
	void multipartOverwritingLargerMultipartFileTest()
	{
		CONTAINER.createObject(TEST_BUCKET, TARGET_FILE, hugeFile);
		JunitHelper.tryRun(() -> write(largeFile));
		byte[] content;
		var length = new AtomicInteger();
		try (var largeStream = Files.newInputStream(largeFile);
			 var hugeStream = Files.newInputStream(hugeFile))
		{
			content = hugeStream.readAllBytes();
			length.addAndGet(largeStream.read(content));
		}
		catch (IOException x)
		{
			content = Assertions.fail(x);
		}
		var array = Arrays.copyOf(content, content.length);
		var size = JunitHelper.tryCall(() -> (int) Files.size(largeFile));
		var expected = JunitHelper.tryCall(() -> multipartChecksum(array, size));
		var result = CONTAINER.checksum(TEST_BUCKET, TARGET_FILE);
		Assertions.assertEquals(expected, result);
	}

	@Test
	void multipartOverwritingSmallerSinglepartFileTest()
	{
		CONTAINER.createObject(TEST_BUCKET, TARGET_FILE, tinyFile);
		JunitHelper.tryRun(() -> write(hugeFile));
		byte[] content;
		try (var hugeStream = Files.newInputStream(hugeFile))
		{
			content = hugeStream.readAllBytes();
		}
		catch (IOException x)
		{
			content = Assertions.fail(x);
		}
		var array = Arrays.copyOf(content, content.length);
		var size = JunitHelper.tryCall(() -> (int) Files.size(hugeFile));
		var expected = JunitHelper.tryCall(() -> multipartChecksum(array, size));
		var result = CONTAINER.checksum(TEST_BUCKET, TARGET_FILE);
		Assertions.assertEquals(expected, result);
	}

	@Test
	void multipartOverwritingSmallerMultipartFileTest()
	{
		CONTAINER.createObject(TEST_BUCKET, TARGET_FILE, largeFile);
		JunitHelper.tryRun(() -> write(hugeFile));
		byte[] content;
		try (var hugeStream = Files.newInputStream(hugeFile))
		{
			content = hugeStream.readAllBytes();
		}
		catch (IOException x)
		{
			content = Assertions.fail(x);
		}
		var array = Arrays.copyOf(content, content.length);
		var size = JunitHelper.tryCall(() -> (int) Files.size(hugeFile));
		var expected = JunitHelper.tryCall(() -> multipartChecksum(array, size));
		var result = CONTAINER.checksum(TEST_BUCKET, TARGET_FILE);
		Assertions.assertEquals(expected, result);
	}

	@Test
	void singlepartTruncatingLargerSinglepartFileTest()
	{
		CONTAINER.createObject(TEST_BUCKET, TARGET_FILE, smallFile);
		JunitHelper.tryRun(() -> write(tinyFile, StandardOpenOption.TRUNCATE_EXISTING));
		byte[] content;
		try (var tinyStream = Files.newInputStream(tinyFile))
		{
			content = tinyStream.readAllBytes();
		}
		catch (IOException x)
		{
			content = Assertions.fail(x);
		}
		var array = Arrays.copyOf(content, content.length);
		var expected = JunitHelper.tryCall(() -> singlepartChecksum(array));
		var result = CONTAINER.checksum(TEST_BUCKET, TARGET_FILE).lastEntry().getKey();
		Assertions.assertEquals(expected, result);
	}

	@Test
	void singlepartTruncatingLargerMultipartFileTest()
	{
		CONTAINER.createObject(TEST_BUCKET, TARGET_FILE, largeFile);
		JunitHelper.tryRun(() -> write(tinyFile, StandardOpenOption.TRUNCATE_EXISTING));
		byte[] content;
		try (var tinyStream = Files.newInputStream(tinyFile))
		{
			content = tinyStream.readAllBytes();
		}
		catch (IOException x)
		{
			content = Assertions.fail(x);
		}
		var array = Arrays.copyOf(content, content.length);
		var expected = JunitHelper.tryCall(() -> singlepartChecksum(array));
		var result = CONTAINER.checksum(TEST_BUCKET, TARGET_FILE).lastEntry().getKey();
		Assertions.assertEquals(expected, result);
	}

	@Test
	void multipartTruncatingLargerMultipartFileTest()
	{
		CONTAINER.createObject(TEST_BUCKET, TARGET_FILE, hugeFile);
		JunitHelper.tryRun(() -> write(largeFile, StandardOpenOption.TRUNCATE_EXISTING));
		byte[] content;
		try (var largeStream = Files.newInputStream(largeFile))
		{
			content = largeStream.readAllBytes();
		}
		catch (IOException x)
		{
			content = Assertions.fail(x);
		}
		var array = Arrays.copyOf(content, content.length);
		var size = JunitHelper.tryCall(() -> (int) Files.size(largeFile));
		var expected = JunitHelper.tryCall(() -> multipartChecksum(array, size));
		var result = CONTAINER.checksum(TEST_BUCKET, TARGET_FILE);
		Assertions.assertEquals(expected, result);
	}

	private Void write(Path file, StandardOpenOption... openOptions) throws IOException
	{
		var options = Stream.concat(Stream.of(StandardOpenOption.WRITE), Stream.of(openOptions))
							.toArray(StandardOpenOption[]::new);
		try (var channel = Files.newByteChannel(targetFile, options))
		{
			var buffer = ByteBuffer.wrap(Files.readAllBytes(file));
			channel.write(buffer);
		}
		return null;
	}

	private String singlepartChecksum(byte[] array) throws GeneralSecurityException
	{
		var fileDigest = MessageDigest.getInstance(CHECKSUM_ALGORITHM);
		var digest = fileDigest.digest(array);
		return Base64.getEncoder().encodeToString(digest);
	}

	private Map<String, Long> multipartChecksum(byte[] array, int size)
		throws GeneralSecurityException
	{
		var result = new LinkedHashMap<String, Long>();
		var fileDigest = MessageDigest.getInstance(CHECKSUM_ALGORITHM);
		var partDigest = MessageDigest.getInstance(CHECKSUM_ALGORITHM);
		try (var stream = new ByteArrayInputStream(array))
		{
			var bytesRead = 0;
			while (bytesRead < size)
			{
				var buffer = stream.readNBytes(PART_SIZE);
				bytesRead += buffer.length;
				partDigest.update(buffer);
				var digest = partDigest.digest();
				result.put(Base64.getEncoder().encodeToString(digest), Long.valueOf(buffer.length));
				fileDigest.update(digest);
				partDigest.reset();
			}
			var remaining = array.length - bytesRead;
			if (remaining > 0)
			{
				var buffer = stream.readNBytes(remaining);
				partDigest.update(buffer);
				var digest = partDigest.digest();
				result.put(Base64.getEncoder().encodeToString(digest), (long) remaining);
				fileDigest.update(digest);
			}
			var digest = fileDigest.digest();
			result.put(Base64.getEncoder().encodeToString(digest), (long) array.length);
		}
		catch (IOException x)
		{
			result = Assertions.fail(x);
		}
		return result;
	}

	private static Path createFile(String fileName, long size) throws IOException
	{
		var result = baseDirectory.resolve(fileName);
		IoHelper.createNotEmptyFile(result, size);
		return result;
	}
}
