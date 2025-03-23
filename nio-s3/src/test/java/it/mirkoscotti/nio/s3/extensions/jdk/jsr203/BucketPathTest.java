/*
 * (C) Copyright 2019 - 2025 - Add Value S.R.L - All rights reserved.
 */

package it.mirkoscotti.nio.s3.extensions.jdk.jsr203;

import it.mirkoscotti.nio.s3.configuration.BucketDescriptor;

import java.lang.reflect.Field;
import java.nio.file.FileSystem;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.Optional;
import java.util.Random;
import java.util.random.RandomGenerator;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.platform.commons.function.Try;
import org.junit.platform.commons.support.HierarchyTraversalMode;
import org.junit.platform.commons.support.ReflectionSupport;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author mirko.scotti
 * @version Mar 13, 2025
 */
@ExtendWith(MockitoExtension.class)
class BucketPathTest
{

	private static final String ABSOLUTE = "absolute";

	private static final String RELATIVE = "relative";

	private static final String PATH = "path";

	private static final String ABSOLUTE_PATH = Stream.of(ABSOLUTE, PATH)
													  .collect(Collectors.joining("/", "/", ""));

	private static final String RELATIVE_PATH = String.join("/", RELATIVE, PATH);

	@Mock
	private BucketFileSystem fileSystem;

	@Test
	void nullTest()
	{
		Assertions.assertThrows(NullPointerException.class, () -> new BucketPath(null, null));
		Assertions.assertThrows(NullPointerException.class, () -> new BucketPath(fileSystem, null));
	}

	@Test
	void pathTooLongTest()
	{
		var path = Stream.iterate("/path/too/long",
								  item -> item.length() <= 2048,
								  item -> item.concat(item))
						 .reduce((item1, item2) -> item2)
						 .orElseThrow();
		Assertions.assertThrows(InvalidPathException.class, () -> new BucketPath(fileSystem, path));
	}

	@Test
	void consecutiveSlashesTest()
	{
		Assertions.assertThrows(InvalidPathException.class,
								() -> new BucketPath(fileSystem, "consecutive//slashes"));
	}

	@Test
	void endingWithPeriodTest()
	{
		Assertions.assertThrows(InvalidPathException.class,
								() -> new BucketPath(fileSystem, "ending/with."));
		Assertions.assertDoesNotThrow(() -> new BucketPath(fileSystem, "ending/with/."));
		Assertions.assertDoesNotThrow(() -> new BucketPath(fileSystem, "ending/with/.."));
	}

	@Test
	void forbiddenCharactersTest()
	{
		Assertions.assertThrows(InvalidPathException.class,
								() -> new BucketPath(fileSystem, "forbidden/ch%racters"));
	}

	@Test
	void specialCharactersTest()
	{
		var bucketPath = new BucketPath(fileSystem, "special/ characters");
		var field = ReflectionSupport.streamFields(BucketPath.class,
												   item -> item.getName().equals("objectKey"),
												   HierarchyTraversalMode.TOP_DOWN)
									 .findAny()
									 .orElseThrow();
		ReflectionSupport.makeAccessible(field);
		var result = Try.call(() -> field.get(bucketPath));
		Assertions.assertEquals("special/%20characters",
								result.toOptional().orElseGet(Assertions::fail));
	}

	@Test
	void fileSystemTest()
	{
		var path = new BucketPath(fileSystem, BucketDescriptor.PATH_SEPARATOR);
		Assertions.assertEquals(fileSystem, path.getFileSystem());
	}

	@Test
	void isAbsoluteTest()
	{
		var bucketPath = new BucketPath(fileSystem, ABSOLUTE_PATH);
		Assertions.assertTrue(bucketPath.isAbsolute());
	}

	@Test
	void isNotAbsoluteTest()
	{
		var bucketPath = new BucketPath(fileSystem, RELATIVE_PATH);
		Assertions.assertFalse(bucketPath.isAbsolute());
	}

	@Test
	void getRootTest()
	{
		var bucketPath = new BucketPath(fileSystem, ABSOLUTE_PATH).getRoot();
		var root = findValue("root", bucketPath).orElseGet(Assertions::fail);
		Assertions.assertEquals(bucketPath, root);
	}

	@Test
	void getFileNameTest()
	{
		Mockito.when(fileSystem.getPath(Mockito.anyString())).thenCallRealMethod();
		var bucketPath = new BucketPath(fileSystem, ABSOLUTE_PATH);
		var expected = ABSOLUTE_PATH.substring(ABSOLUTE_PATH.lastIndexOf('/') + 1);
		var fileName = bucketPath.getFileName();
		var result = findValue("objectKey", fileName).orElseGet(Assertions::fail);
		Assertions.assertEquals(expected, result);
		Assertions.assertTrue(findValue("root", fileName).isEmpty());
	}

	@Test
	void getParentTest()
	{
		Mockito.when(fileSystem.getPath(Mockito.anyString())).thenCallRealMethod();
		var bucketPath = new BucketPath(fileSystem, ABSOLUTE_PATH);
		var expected = ABSOLUTE_PATH.substring(1, ABSOLUTE_PATH.lastIndexOf('/'));
		var parent = bucketPath.getParent();
		var result = findValue("objectKey", parent).orElseGet(Assertions::fail);
		Assertions.assertEquals(expected, result);
		Assertions.assertTrue(findValue("root", parent).isPresent());
	}

	@Test
	void getNameCountTest()
	{
		var bucketPath = new BucketPath(fileSystem, ABSOLUTE_PATH);
		var result = bucketPath.getNameCount();
		Assertions.assertEquals(2, result);
	}

	@Test
	void getNameWithNegativeIndexTest()
	{
		var bucketPath = new BucketPath(fileSystem, ABSOLUTE_PATH);
		Assertions.assertThrows(IllegalArgumentException.class, () -> bucketPath.getName(-1));
	}

	@Test
	void getNameWithIndexOutOfBoundTest()
	{
		var bucketPath = new BucketPath(fileSystem, ABSOLUTE_PATH);
		Assertions.assertThrows(IllegalArgumentException.class, () -> bucketPath.getName(3));
	}

	@Test
	void getNameTest()
	{
		Mockito.when(fileSystem.getPath(Mockito.anyString())).thenCallRealMethod();
		var bucketPath = new BucketPath(fileSystem, ABSOLUTE_PATH);
		var name = bucketPath.getName(0);
		var result = findValue("objectKey", name).orElseGet(Assertions::fail);
		Assertions.assertEquals(ABSOLUTE, result);
		name = bucketPath.getName(1);
		result = findValue("objectKey", name).orElseGet(Assertions::fail);
		Assertions.assertEquals(PATH, result);
	}

	@Test
	void subpathWithNegativeBeginIndexTest()
	{
		var bucketPath = new BucketPath(fileSystem, ABSOLUTE_PATH);
		Assertions.assertThrows(IllegalArgumentException.class, () -> bucketPath.subpath(-1, 1));
	}

	@Test
	void subpathWhenStartIndexHigherThanBeginIndexTest()
	{
		var bucketPath = new BucketPath(fileSystem, ABSOLUTE_PATH);
		Assertions.assertThrows(IllegalArgumentException.class, () -> bucketPath.subpath(1, 0));
	}

	@Test
	void subpathWhenBeginIndexOutOfBoundTest()
	{
		var bucketPath = new BucketPath(fileSystem, ABSOLUTE_PATH);
		Assertions.assertThrows(IllegalArgumentException.class, () -> bucketPath.subpath(5, 10));
	}

	@Test
	void subpathWhenEndIndexOutOfBoundTest()
	{
		var bucketPath = new BucketPath(fileSystem, ABSOLUTE_PATH);
		Assertions.assertThrows(IllegalArgumentException.class, () -> bucketPath.subpath(1, 5));
	}

	@RepeatedTest(5)
	void subpathTest()
	{
		var bucketPath = new BucketPath(fileSystem, RELATIVE_PATH);
		var random = Random.from(RandomGenerator.getDefault());
		var array = RELATIVE_PATH.split("/");
		var length = array.length;
		var index = random.nextInt(length);
		var subpath = bucketPath.subpath(index, index + 1);
		var result = findValue("objectKey", subpath).orElseGet(Assertions::fail);
		Assertions.assertEquals(array[index], result);
	}

	@Test
	void doesNotStartWithTest()
	{
		Mockito.when(fileSystem.getPath(Mockito.anyString())).thenCallRealMethod();
		var bucketPath = new BucketPath(fileSystem, ABSOLUTE_PATH);
		Assertions.assertFalse(bucketPath.startsWith(RELATIVE_PATH));
	}

	@Test
	void startsWithUndefinedPathTest()
	{
		var bucketPath = new BucketPath(fileSystem, ABSOLUTE_PATH);
		Assertions.assertFalse(bucketPath.startsWith((BucketPath) null));
	}

	@Test
	void startsWithLongerPathTest()
	{
		Mockito.when(fileSystem.getPath(Mockito.anyString())).thenCallRealMethod();
		var bucketPath = new BucketPath(fileSystem, ABSOLUTE_PATH);
		Assertions.assertFalse(bucketPath.startsWith(ABSOLUTE_PATH.concat("/more")));
	}

	@Test
	void startsWithPathFromDifferentFileSystemTest(@Mock Path path, @Mock FileSystem fileSystem)
	{
		Mockito.when(path.getFileSystem()).thenReturn(fileSystem);
		var bucketPath = new BucketPath(this.fileSystem, ABSOLUTE_PATH);
		Assertions.assertFalse(bucketPath.startsWith(path));
	}

	@Test
	void startsWithTest()
	{
		Mockito.when(fileSystem.getPath(Mockito.anyString())).thenCallRealMethod();
		var bucketPath = new BucketPath(fileSystem, ABSOLUTE_PATH);
		Assertions.assertTrue(bucketPath.startsWith(ABSOLUTE));
	}

	@Test
	void doesNotEndWithTest()
	{
		Mockito.when(fileSystem.getPath(Mockito.anyString())).thenCallRealMethod();
		var bucketPath = new BucketPath(fileSystem, ABSOLUTE_PATH);
		Assertions.assertFalse(bucketPath.endsWith(RELATIVE_PATH));
	}

	@Test
	void endsWithUndefinedPathTest()
	{
		var bucketPath = new BucketPath(fileSystem, ABSOLUTE_PATH);
		Assertions.assertFalse(bucketPath.endsWith((BucketPath) null));
	}

	@Test
	void endsWithLongerPathTest()
	{
		Mockito.when(fileSystem.getPath(Mockito.anyString())).thenCallRealMethod();
		var bucketPath = new BucketPath(fileSystem, ABSOLUTE_PATH);
		Assertions.assertFalse(bucketPath.endsWith("/more".concat(ABSOLUTE_PATH)));
	}

	@Test
	void endsWithPathFromDifferentFileSystemTest(@Mock Path path, @Mock FileSystem fileSystem)
	{
		Mockito.when(path.getFileSystem()).thenReturn(fileSystem);
		var bucketPath = new BucketPath(this.fileSystem, ABSOLUTE_PATH);
		Assertions.assertFalse(bucketPath.endsWith(path));
	}

	@Test
	void endsWithTest()
	{
		Mockito.when(fileSystem.getPath(Mockito.anyString())).thenCallRealMethod();
		var bucketPath = new BucketPath(fileSystem, ABSOLUTE_PATH);
		Assertions.assertTrue(bucketPath.endsWith(PATH));
	}

	@Test
	void normalizeRelativePathTest()
	{
		Mockito.when(fileSystem.getPath(Mockito.anyString())).thenCallRealMethod();
		var bucketPath = new BucketPath(fileSystem, RELATIVE_PATH);
		var normalizedPath = bucketPath.normalize();
		var expected = findValue("objectKey", bucketPath).orElseGet(Assertions::fail);
		var result = findValue("objectKey", normalizedPath).orElseGet(Assertions::fail);
		Assertions.assertEquals(expected, result);
	}

	@Test
	void normalizeAbsolutePathTest()
	{
		Mockito.when(fileSystem.getPath(Mockito.anyString())).thenCallRealMethod();
		var bucketPath = new BucketPath(fileSystem, ABSOLUTE_PATH);
		var normalizedPath = bucketPath.normalize();
		var expected = findValue("objectKey", bucketPath).orElseGet(Assertions::fail);
		var result = findValue("objectKey", normalizedPath).orElseGet(Assertions::fail);
		Assertions.assertEquals(expected, result);
	}

	@Test
	void normalizePathWithCurrentDirectoryTest()
	{
		Mockito.when(fileSystem.getPath(Mockito.anyString())).thenCallRealMethod();
		var bucketPath = new BucketPath(fileSystem, String.join("/./", ABSOLUTE, PATH));
		var normalizedPath = bucketPath.normalize();
		var expected = ABSOLUTE_PATH.substring(1);
		var result = findValue("objectKey", normalizedPath).orElseGet(Assertions::fail);
		Assertions.assertEquals(expected, result);
	}

	@Test
	void normalizePathWithParentDirectoryTest()
	{
		Mockito.when(fileSystem.getPath(Mockito.anyString())).thenCallRealMethod();
		var bucketPath = new BucketPath(fileSystem, String.join("/../", ABSOLUTE, PATH));
		var normalizedPath = bucketPath.normalize();
		var expected = PATH;
		var result = findValue("objectKey", normalizedPath).orElseGet(Assertions::fail);
		Assertions.assertEquals(expected, result);
	}

	@Test
	void normalizePathWithBackwardDirectoryTest()
	{
		Mockito.when(fileSystem.getPath(Mockito.anyString())).thenCallRealMethod();
		var path = ABSOLUTE_PATH.concat("/");
		var bucketPath = new BucketPath(fileSystem, path);
		var normalizedPath = bucketPath.normalize();
		var expected = path.substring(1);
		var result = findValue("objectKey", normalizedPath).orElseGet(Assertions::fail);
		Assertions.assertEquals(expected, result);
	}

	@Test
	void resolveMissingPathTest()
	{
		var bucketPath = new BucketPath(fileSystem, ABSOLUTE_PATH);
		Assertions.assertThrows(NullPointerException.class, () -> bucketPath.resolve((Path) null));
	}

	@Test
	void resolveWithNotBucketPathTest(@Mock Path path)
	{
		var bucketPath = new BucketPath(fileSystem, ABSOLUTE_PATH);
		Assertions.assertThrows(IllegalArgumentException.class, () -> bucketPath.resolve(path));
	}

	@Test
	void resolveWithBucketBathHavingDifferentFileSystemTest(@Mock BucketPath path,
															@Mock FileSystem fileSystem)
	{
		Mockito.when(path.getFileSystem()).thenReturn(fileSystem);
		var bucketPath = new BucketPath(this.fileSystem, ABSOLUTE_PATH);
		Assertions.assertThrows(IllegalArgumentException.class, () -> bucketPath.resolve(path));
	}

	@Test
	void resolveWithRootTest()
	{
		var bucketPath = new BucketPath(fileSystem, ABSOLUTE_PATH);
		var expected = findValue("objectKey", bucketPath).orElseGet(Assertions::fail);
		var resolvedPath = bucketPath.resolve(bucketPath.getRoot());
		var result = findValue("objectKey", resolvedPath).orElseGet(Assertions::fail);
		Assertions.assertEquals(expected, result);
	}

	@Test
	void resolveWithAbsolutePathTest()
	{
		var bucketPath = new BucketPath(fileSystem, ABSOLUTE_PATH);
		var path = new BucketPath(fileSystem, "/other/path");
		Assertions.assertEquals(path, bucketPath.resolve(path));
	}

	@Test
	void resolveWithRelativePathTest()
	{
		var bucketPath = new BucketPath(fileSystem, ABSOLUTE_PATH);
		var path = bucketPath.resolve(new BucketPath(fileSystem, RELATIVE_PATH));
		var expected = String.join("/", ABSOLUTE_PATH, RELATIVE_PATH).substring(1);
		var result = findValue("objectKey", path).orElseGet(Assertions::fail);
		Assertions.assertEquals(expected, result);
	}

	@Test
	void resolvePathEndingWithSeparatorWithRelativePathTest()
	{
		var bucketPath = new BucketPath(fileSystem, ABSOLUTE_PATH.concat("/"));
		var path = bucketPath.resolve(new BucketPath(fileSystem, RELATIVE_PATH));
		var expected = String.join("/", ABSOLUTE_PATH, RELATIVE_PATH).substring(1);
		var result = findValue("objectKey", path).orElseGet(Assertions::fail);
		Assertions.assertEquals(expected, result);
	}

	private Field findField(String fieldName)
	{
		var result = ReflectionSupport.streamFields(BucketPath.class,
													item -> item.getName().equals(fieldName),
													HierarchyTraversalMode.TOP_DOWN)
									  .findAny()
									  .orElseGet(Assertions::fail);
		ReflectionSupport.makeAccessible(result);
		return result;
	}

	private Optional<Object> findValue(String fieldName, Object instance)
	{
		var field = findField(fieldName);
		return Try.call(() -> field.get(instance)).toOptional();
	}
}
