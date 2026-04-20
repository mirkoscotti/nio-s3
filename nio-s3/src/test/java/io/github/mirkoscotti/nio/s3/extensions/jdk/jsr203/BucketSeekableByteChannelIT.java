package io.github.mirkoscotti.nio.s3.extensions.jdk.jsr203;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
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
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.CleanupMode;
import org.junit.jupiter.api.io.TempDir;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import it.mirkoscotti.nio.s3.extensions.testcontainers.S3Container;
import it.mirkoscotti.nio.s3.helpers.ContainerHelper;
import it.mirkoscotti.nio.s3.helpers.IoHelper;
import it.mirkoscotti.nio.s3.helpers.JunitHelper;

/**
 * @author mirko.scotti
 * @version May 30, 2025
 */
@Testcontainers
class BucketSeekableByteChannelIT
{

	private static final String TEST_BUCKET = "test-bucket";

	private static final String EMPTY_FILE = "empty.txt";

	private static final String TINY_FILE = "tiny.txt";

	private static final String SMALL_FILE = "small.txt";

	private static final String MEDIUM_FILE = "medium.txt";

	private static final String LARGE_FILE = "large.txt";

	private static final String HUGE_FILE = "huge.txt";

	private static final String REMOTE_FILE = "remote.txt";

	private static final URI TEST_URI = URI.create("s3://".concat(TEST_BUCKET));

	private static final String CHECKSUM_ALGORITHM = "SHA-256";

	private static final int PART_SIZE = 10 * 1024 * 1024;

	@Container
	@SuppressWarnings("resource")
	private static final S3Container CONTAINER = new S3Container().withBucket(TEST_BUCKET);

	@TempDir(cleanup = CleanupMode.ALWAYS)
	private static Path baseDirectory;

	private static FileSystem fileSystem;

	private static Path emptyFile;

	private static Path tinyFile;

	private static Path smallFile;

	private static Path mediumFile;

	private static Path largeFile;

	private static Path hugeFile;

	private static Path remoteFile;

	@BeforeAll
	static void beforeAll()
	{
		emptyFile = JunitHelper.tryCall(() -> createFile(EMPTY_FILE, 0));
		tinyFile = JunitHelper.tryCall(() -> createFile(TINY_FILE, 1));
		smallFile = JunitHelper.tryCall(() -> createFile(SMALL_FILE, 1024));
		mediumFile = JunitHelper.tryCall(() -> createFile(MEDIUM_FILE, 1024 * 1024));
		largeFile = JunitHelper.tryCall(() -> createFile(LARGE_FILE, PART_SIZE * 5 / 2));
		hugeFile = JunitHelper.tryCall(() -> createFile(HUGE_FILE, PART_SIZE * 10 / 2));
		var properties = ContainerHelper.standardProperties(CONTAINER);
		fileSystem = JunitHelper.tryCall(() -> FileSystems.newFileSystem(TEST_URI, properties));
		remoteFile = fileSystem.getRootDirectories().iterator().next().resolve(REMOTE_FILE);
	}

	@AfterEach
	void afterEach()
	{
		ContainerHelper.deleteObjects(CONTAINER, TEST_BUCKET);
	}

	@Test
	void isOpenForWriteTest()
	{
		var seekableByteChannel = JunitHelper.tryCall(() -> Files.newByteChannel(remoteFile,
																				 StandardOpenOption.WRITE,
																				 StandardOpenOption.CREATE_NEW));
		try (var channel = seekableByteChannel)
		{
			Assertions.assertTrue(channel.isOpen());
		}
		catch (IOException x)
		{
			Assertions.fail(x);
		}
		Assertions.assertFalse(seekableByteChannel.isOpen());
	}

	@Test
	void isOpenForReadTest()
	{
		CONTAINER.createObject(TEST_BUCKET, REMOTE_FILE, tinyFile);
		var seekableByteChannel = JunitHelper.tryCall(() -> Files.newByteChannel(remoteFile));
		try (var channel = seekableByteChannel)
		{
			Assertions.assertTrue(channel.isOpen());
		}
		catch (IOException x)
		{
			Assertions.fail(x);
		}
		Assertions.assertFalse(seekableByteChannel.isOpen());
	}

	@Test
	void writeNotExistingFileTest()
	{
		Assertions.assertThrows(NoSuchFileException.class, () -> write(tinyFile));
	}

	@Test
	void singlepartOverwritingLargerSinglepartFileTest()
	{
		CONTAINER.createObject(TEST_BUCKET, REMOTE_FILE, smallFile);
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
		var result = CONTAINER.checksum(TEST_BUCKET, REMOTE_FILE).lastEntry().getKey();
		Assertions.assertEquals(expected, result);
	}

	@Test
	void singlepartOverwritingLargerMultipartFileTest()
	{
		CONTAINER.createObject(TEST_BUCKET, REMOTE_FILE, largeFile);
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
		var result = CONTAINER.checksum(TEST_BUCKET, REMOTE_FILE);
		Assertions.assertEquals(expected, result);
	}

	@Test
	void singlepartOverwritingSmallerSinglepartFileTest()
	{
		CONTAINER.createObject(TEST_BUCKET, REMOTE_FILE, smallFile);
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
		var result = CONTAINER.checksum(TEST_BUCKET, REMOTE_FILE).lastEntry().getKey();
		Assertions.assertEquals(expected, result);
	}

	@Test
	void multipartOverwritingLargerMultipartFileTest()
	{
		CONTAINER.createObject(TEST_BUCKET, REMOTE_FILE, hugeFile);
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
		var result = CONTAINER.checksum(TEST_BUCKET, REMOTE_FILE);
		Assertions.assertEquals(expected, result);
	}

	@Test
	void multipartOverwritingSmallerSinglepartFileTest()
	{
		CONTAINER.createObject(TEST_BUCKET, REMOTE_FILE, tinyFile);
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
		var result = CONTAINER.checksum(TEST_BUCKET, REMOTE_FILE);
		Assertions.assertEquals(expected, result);
	}

	@Test
	void multipartOverwritingSmallerMultipartFileTest()
	{
		CONTAINER.createObject(TEST_BUCKET, REMOTE_FILE, largeFile);
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
		var result = CONTAINER.checksum(TEST_BUCKET, REMOTE_FILE);
		Assertions.assertEquals(expected, result);
	}

	@Test
	void singlepartTruncatingLargerSinglepartFileTest()
	{
		CONTAINER.createObject(TEST_BUCKET, REMOTE_FILE, smallFile);
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
		var result = CONTAINER.checksum(TEST_BUCKET, REMOTE_FILE).lastEntry().getKey();
		Assertions.assertEquals(expected, result);
	}

	@Test
	void singlepartTruncatingLargerMultipartFileTest()
	{
		CONTAINER.createObject(TEST_BUCKET, REMOTE_FILE, largeFile);
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
		var result = CONTAINER.checksum(TEST_BUCKET, REMOTE_FILE).lastEntry().getKey();
		Assertions.assertEquals(expected, result);
	}

	@Test
	void multipartTruncatingLargerMultipartFileTest()
	{
		CONTAINER.createObject(TEST_BUCKET, REMOTE_FILE, hugeFile);
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
		var result = CONTAINER.checksum(TEST_BUCKET, REMOTE_FILE);
		Assertions.assertEquals(expected, result);
	}

	@Test
	void singlepartAppendingToSinglepartFileTest()
	{
		CONTAINER.createObject(TEST_BUCKET, REMOTE_FILE, tinyFile);
		JunitHelper.tryRun(() -> write(smallFile, StandardOpenOption.APPEND));
		byte[] content;
		try (var tinyStream = Files.newInputStream(tinyFile);
			 var smallStream = Files.newInputStream(smallFile);
			 var targetStream = new ByteArrayOutputStream())
		{
			targetStream.write(tinyStream.readAllBytes());
			targetStream.write(smallStream.readAllBytes());
			content = targetStream.toByteArray();
		}
		catch (IOException x)
		{
			content = Assertions.fail(x);
		}
		var array = Arrays.copyOf(content, content.length);
		var expected = JunitHelper.tryCall(() -> singlepartChecksum(array));
		var result = CONTAINER.checksum(TEST_BUCKET, REMOTE_FILE).lastEntry().getKey();
		Assertions.assertEquals(expected, result);
	}

	@Test
	void singlepartAppendingToMultipartFileTest()
	{
		CONTAINER.createObject(TEST_BUCKET, REMOTE_FILE, largeFile);
		JunitHelper.tryRun(() -> write(tinyFile, StandardOpenOption.APPEND));
		byte[] content;
		try (var tinyStream = Files.newInputStream(tinyFile);
			 var largeStream = Files.newInputStream(largeFile);
			 var targetStream = new ByteArrayOutputStream())
		{
			targetStream.write(largeStream.readAllBytes());
			targetStream.write(tinyStream.readAllBytes());
			content = targetStream.toByteArray();
		}
		catch (IOException x)
		{
			content = Assertions.fail(x);
		}
		var array = Arrays.copyOf(content, content.length);
		var oldSize = JunitHelper.tryCall(() -> (int) Files.size(largeFile));
		var newSize = JunitHelper.tryCall(() -> (int) Files.size(tinyFile));
		var expected = JunitHelper.tryCall(() -> multipartChecksum(array, oldSize, newSize));
		var result = CONTAINER.checksum(TEST_BUCKET, REMOTE_FILE);
		Assertions.assertEquals(expected, result);
	}

	@Test
	void multipartAppendingToSinglepartFileTest()
	{
		CONTAINER.createObject(TEST_BUCKET, REMOTE_FILE, tinyFile);
		JunitHelper.tryRun(() -> write(largeFile, StandardOpenOption.APPEND));
		byte[] content;
		try (var tinyStream = Files.newInputStream(tinyFile);
			 var largeStream = Files.newInputStream(largeFile);
			 var targetStream = new ByteArrayOutputStream())
		{
			targetStream.write(tinyStream.readAllBytes());
			targetStream.write(largeStream.readAllBytes());
			content = targetStream.toByteArray();
		}
		catch (IOException x)
		{
			content = Assertions.fail(x);
		}
		var array = Arrays.copyOf(content, content.length);
		var oldSize = JunitHelper.tryCall(() -> (int) Files.size(tinyFile));
		var newSize = JunitHelper.tryCall(() -> (int) Files.size(largeFile));
		var expected = JunitHelper.tryCall(() -> multipartChecksum(array, oldSize, newSize));
		var result = CONTAINER.checksum(TEST_BUCKET, REMOTE_FILE);
		Assertions.assertEquals(expected, result);
	}

	@Test
	void multipartAppendingToMultipartFileTest()
	{
		CONTAINER.createObject(TEST_BUCKET, REMOTE_FILE, largeFile);
		JunitHelper.tryRun(() -> write(hugeFile, StandardOpenOption.APPEND));
		byte[] content;
		try (var largeStream = Files.newInputStream(largeFile);
			 var hugeStream = Files.newInputStream(hugeFile);
			 var targetStream = new ByteArrayOutputStream())
		{
			targetStream.write(largeStream.readAllBytes());
			targetStream.write(hugeStream.readAllBytes());
			content = targetStream.toByteArray();
		}
		catch (IOException x)
		{
			content = Assertions.fail(x);
		}
		var array = Arrays.copyOf(content, content.length);
		var oldSize = JunitHelper.tryCall(() -> (int) Files.size(largeFile));
		var newSize = JunitHelper.tryCall(() -> (int) Files.size(hugeFile));
		var expected = JunitHelper.tryCall(() -> multipartChecksum(array, oldSize, newSize));
		var result = CONTAINER.checksum(TEST_BUCKET, REMOTE_FILE);
		Assertions.assertEquals(expected, result);
	}

	@Test
	void readFileFromTheBeginningTest()
	{
		CONTAINER.createObject(TEST_BUCKET, REMOTE_FILE, tinyFile);
		try (var channel = Files.newByteChannel(remoteFile))
		{
			var expectedSize = (int) Files.size(tinyFile);
			var buffer = ByteBuffer.allocate(expectedSize);
			var resultSize = channel.read(buffer);
			Assertions.assertEquals(expectedSize, resultSize);
			var expectedContent = Files.readAllBytes(tinyFile);
			var resultContent = buffer.array();
			Assertions.assertArrayEquals(expectedContent, resultContent);
			buffer = ByteBuffer.allocate(10);
			resultSize = channel.read(buffer);
			Assertions.assertEquals(-1, resultSize);
		}
		catch (IOException x)
		{
			Assertions.fail(x);
		}
	}

	@Test
	void readFileFragmentTest()
	{
		CONTAINER.createObject(TEST_BUCKET, REMOTE_FILE, tinyFile);
		try (var channel = Files.newByteChannel(remoteFile);
			 var stream = Files.newInputStream(tinyFile))
		{
			var expectedSize = (int) Files.size(tinyFile) / 2;
			var buffer = ByteBuffer.allocate(expectedSize);
			var resultSize = channel.read(buffer);
			Assertions.assertEquals(expectedSize, resultSize);
			var position = channel.position();
			var expectedContent = stream.readNBytes(expectedSize);
			Assertions.assertEquals(expectedSize, position);
			var resultContent = buffer.array();
			Assertions.assertArrayEquals(expectedContent, resultContent);
		}
		catch (IOException x)
		{
			Assertions.fail(x);
		}
	}

	@Test
	void readFileFromTheMiddleTest()
	{
		CONTAINER.createObject(TEST_BUCKET, REMOTE_FILE, tinyFile);
		try (var channel = Files.newByteChannel(remoteFile);
			 var stream = Files.newInputStream(tinyFile))
		{
			var size = Files.size(tinyFile);
			var position = size / 2;
			channel.position(position);
			var expectedSize = (int) (size - position);
			var buffer = ByteBuffer.allocate(expectedSize);
			var resultSize = channel.read(buffer);
			Assertions.assertEquals(expectedSize, resultSize);
			stream.skip(position);
			var expectedContent = stream.readNBytes(expectedSize);
			var resultContent = buffer.array();
			Assertions.assertArrayEquals(expectedContent, resultContent);
		}
		catch (IOException x)
		{
			Assertions.fail(x);
		}
	}

	@Test
	void readFileAfterEndTest()
	{
		CONTAINER.createObject(TEST_BUCKET, REMOTE_FILE, emptyFile);
		try (var channel = Files.newByteChannel(remoteFile))
		{
			var buffer = ByteBuffer.allocate(0);
			var result = channel.read(buffer);
			Assertions.assertEquals(0, result);
		}
		catch (IOException x)
		{
			Assertions.fail(x);
		}
	}

	@Test
	void sizeTest()
	{
		CONTAINER.createObject(TEST_BUCKET, REMOTE_FILE, tinyFile);
		try (var channel = Files.newByteChannel(remoteFile))
		{
			var expectedSize = Files.size(tinyFile);
			var resultSize = channel.size();
			Assertions.assertEquals(expectedSize, resultSize);
		}
		catch (IOException x)
		{
			Assertions.fail(x);
		}
	}

	@Test
	void truncateTest()
	{
		CONTAINER.createObject(TEST_BUCKET, REMOTE_FILE, tinyFile);
		try (var channel = Files.newByteChannel(remoteFile, StandardOpenOption.WRITE))
		{
			Assertions.assertThrows(UnsupportedOperationException.class,
									() -> channel.truncate(10));
		}
		catch (IOException x)
		{
			Assertions.fail(x);
		}
	}

	private Void write(Path file, StandardOpenOption... openOptions) throws IOException
	{
		var options = Stream.concat(Stream.of(StandardOpenOption.WRITE), Stream.of(openOptions))
							.toArray(StandardOpenOption[]::new);
		try (var channel = Files.newByteChannel(remoteFile, options))
		{
			var buffer = ByteBuffer.wrap(Files.readAllBytes(file));
			channel.write(buffer);
		}
		return null;
	}

	private String singlepartChecksum(byte[] array) throws GeneralSecurityException
	{
		var messageDigest = MessageDigest.getInstance(CHECKSUM_ALGORITHM);
		var digest = messageDigest.digest(array);
		return Base64.getEncoder().encodeToString(digest);
	}

	private Map<String, Long> multipartChecksum(byte[] array, int size)
		throws GeneralSecurityException
	{
		var map = partChecksums(array, size);
		return multipartChecksum(array, map);
	}

	private Map<String, Long> multipartChecksum(byte[] array, int oldSize, int newSize)
		throws GeneralSecurityException
	{
		var map = partChecksums(array, oldSize, newSize);
		return multipartChecksum(array, map);
	}

	private Map<String, Long> multipartChecksum(byte[] array, Map<String, Long> partChecksums)
		throws GeneralSecurityException
	{
		var messageDigest = MessageDigest.getInstance(CHECKSUM_ALGORITHM);
		var result = new LinkedHashMap<>(partChecksums);
		result.keySet().stream().map(Base64.getDecoder()::decode).forEach(messageDigest::update);
		var digest = messageDigest.digest();
		result.put(Base64.getEncoder().encodeToString(digest), (long) array.length);
		return Collections.unmodifiableMap(result);
	}

	private Map<String, Long> partChecksums(byte[] array, int size) throws GeneralSecurityException
	{
		var result = new LinkedHashMap<String, Long>();
		try (var stream = new ByteArrayInputStream(array))
		{
			var bytesRead = 0;
			while (bytesRead < size)
			{
				var buffer = stream.readNBytes(PART_SIZE);
				bytesRead += buffer.length;
				result.put(singlepartChecksum(buffer), Long.valueOf(buffer.length));
			}
			var remaining = array.length - bytesRead;
			if (remaining > 0)
			{
				var buffer = stream.readNBytes(remaining);
				result.put(singlepartChecksum(buffer), Long.valueOf(remaining));
			}
		}
		catch (IOException x)
		{
			result = Assertions.fail(x);
		}
		return Collections.unmodifiableMap(result);
	}

	private Map<String, Long> partChecksums(byte[] array, int oldSize, int newSize)
		throws GeneralSecurityException
	{
		var result = new LinkedHashMap<String, Long>();
		var size = oldSize > PART_SIZE ? newSize : oldSize + newSize;
		try (var stream = new ByteArrayInputStream(array))
		{
			var bufferLength = 0;
			if (oldSize > PART_SIZE)
			{
				var buffer = stream.readNBytes(oldSize);
				result.put(singlepartChecksum(buffer), Long.valueOf(buffer.length));
				bufferLength += buffer.length;
			}
			var buffer = stream.readNBytes(array.length - bufferLength);
			if (newSize > PART_SIZE)
			{
				var map = partChecksums(buffer, size);
				result.putAll(map);
			}
			else
			{
				result.put(singlepartChecksum(buffer), Long.valueOf(size));
			}
		}
		catch (IOException x)
		{
			result = Assertions.fail(x);
		}
		return Collections.unmodifiableMap(result);
	}

	private static Path createFile(String fileName, long size) throws IOException
	{
		var result = baseDirectory.resolve(fileName);
		IoHelper.createNotEmptyFile(result, size);
		return result;
	}
}
