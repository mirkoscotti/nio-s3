package io.github.mirkoscotti.nio.s3.extensions.jdk.jsr203;

import java.nio.file.attribute.FileTime;
import java.time.Instant;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import io.github.mirkoscotti.nio.s3.extensions.jdk.jsr203.ObjectBasicFileAttributes;

import software.amazon.awssdk.services.s3.model.S3Object;

/**
 * @author mirko.scotti
 * @version Dec 26, 2024
 */
@ExtendWith(MockitoExtension.class)
class ObjectsBasicFileAttributesTest
{

	private static final String KEY = "key";

	private static final String DIRECTORY = KEY.concat("/");

	@Mock
	private S3Object object;

	@Test
	void nullTest()
	{
		Assertions.assertThrows(NullPointerException.class,
								() -> new ObjectBasicFileAttributes(null));
		Assertions.assertDoesNotThrow(() -> new ObjectBasicFileAttributes(object));
	}

	@Test
	void lastModifiedTimeTest(@Mock Instant instant)
	{
		Mockito.when(object.lastModified()).thenReturn(instant);
		var objectBasicFileAttributes = new ObjectBasicFileAttributes(object);
		Assertions.assertEquals(FileTime.from(instant),
								objectBasicFileAttributes.lastModifiedTime());
	}

	@Test
	void lastAccessTimeTest(@Mock Instant instant)
	{
		Mockito.when(object.lastModified()).thenReturn(instant);
		var objectBasicFileAttributes = new ObjectBasicFileAttributes(object);
		Assertions.assertEquals(FileTime.from(instant), objectBasicFileAttributes.lastAccessTime());
	}

	@Test
	void creationTimeTest(@Mock Instant instant)
	{
		Mockito.when(object.lastModified()).thenReturn(instant);
		var objectBasicFileAttributes = new ObjectBasicFileAttributes(object);
		Assertions.assertEquals(FileTime.from(instant), objectBasicFileAttributes.creationTime());
	}

	@Test
	void isNotRegularFileTest()
	{
		Mockito.when(object.key()).thenReturn(DIRECTORY);
		var objectBasicFileAttributes = new ObjectBasicFileAttributes(object);
		Assertions.assertFalse(objectBasicFileAttributes.isRegularFile());
	}

	@Test
	void isRegularFileTest()
	{
		Mockito.when(object.key()).thenReturn(KEY);
		var objectBasicFileAttributes = new ObjectBasicFileAttributes(object);
		Assertions.assertTrue(objectBasicFileAttributes.isRegularFile());
	}

	@Test
	void isDirectoryTest()
	{
		Mockito.when(object.key()).thenReturn(DIRECTORY);
		var objectBasicFileAttributes = new ObjectBasicFileAttributes(object);
		Assertions.assertTrue(objectBasicFileAttributes.isDirectory());
	}

	@Test
	void isSymbolicLinkTest()
	{
		var objectBasicFileAttributes = new ObjectBasicFileAttributes(object);
		Assertions.assertFalse(objectBasicFileAttributes.isSymbolicLink());
	}

	@Test
	void isOtherTest()
	{
		var objectBasicFileAttributes = new ObjectBasicFileAttributes(object);
		Assertions.assertFalse(objectBasicFileAttributes.isOther());
	}

	@Test
	void sizeTest()
	{
		Mockito.when(object.size()).thenReturn(Long.valueOf(1));
		var objectBasicFileAttributes = new ObjectBasicFileAttributes(object);
		Assertions.assertEquals(1, objectBasicFileAttributes.size());
	}

	@Test
	void fileKeyTest()
	{
		Mockito.when(object.key()).thenReturn(KEY);
		var objectBasicFileAttributes = new ObjectBasicFileAttributes(object);
		Assertions.assertEquals(KEY, objectBasicFileAttributes.fileKey());
	}
}
