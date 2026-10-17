package io.github.mirkoscotti.nio.s3.extensions.jdk.jsr203;

import java.io.IOException;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.CleanupMode;
import org.junit.jupiter.api.io.TempDir;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import io.github.mirkoscotti.nio.s3.extensions.testcontainers.S3Container;
import io.github.mirkoscotti.nio.s3.helpers.ContainerHelper;
import io.github.mirkoscotti.nio.s3.helpers.IoHelper;
import io.github.mirkoscotti.nio.s3.helpers.JunitHelper;

@Testcontainers
class BucketReadableByteChannelBoundaryIT
{

	private static final String TEST_BUCKET = "test-bucket";

	private static final String REMOTE_FILE = "remote.txt";

	private static final URI TEST_URI = URI.create("s3://".concat(TEST_BUCKET));

	private static final long OVERSIZED_FILE_SIZE = Integer.MAX_VALUE + 1024l;

	@Container
	@SuppressWarnings("resource")
	private static final S3Container CONTAINER = new S3Container().withBucket(TEST_BUCKET);

	@TempDir(cleanup = CleanupMode.ALWAYS)
	private static Path baseDirectory;

	private static FileSystem fileSystem;

	private static Path remoteFile;

	@BeforeAll
	static void beforeAll()
	{
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
	@Tag("slow")
	void readObjectLargerThan2GiBTest() throws IOException
	{
		var oversizedFile = JunitHelper.tryCall(() -> createFile("oversized.txt",
																 OVERSIZED_FILE_SIZE));
		try
		{
			CONTAINER.createObject(TEST_BUCKET, REMOTE_FILE, oversizedFile);
			try (var channel = Files.newByteChannel(remoteFile))
			{
				var buffer = ByteBuffer.allocate(8192);
				var resultSize = channel.read(buffer);
				// A correct implementation reads a positive number of bytes here: the object is
				// ~2 GiB and the buffer only asks for 8 KiB. The buggy implementation returns 0
				// because of the int-overflow described above, which this assertion exposes.
				Assertions.assertTrue(resultSize > 0,
									  "read() returned " + resultSize + " for an object larger than 2 GiB (F-06 int-overflow bug)");
			}
		}
		finally
		{
			Files.deleteIfExists(oversizedFile);
		}
	}

	private static Path createFile(String fileName, long size) throws IOException
	{
		var result = baseDirectory.resolve(fileName);
		IoHelper.createNotEmptyFile(result, size);
		return result;
	}
}
