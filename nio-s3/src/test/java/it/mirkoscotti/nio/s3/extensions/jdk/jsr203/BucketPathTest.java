package it.mirkoscotti.nio.s3.extensions.jdk.jsr203;

import java.io.FileNotFoundException;
import java.lang.reflect.Field;
import java.nio.file.FileStore;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.NotDirectoryException;
import java.nio.file.Path;
import java.nio.file.ProviderMismatchException;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent.Kind;
import java.nio.file.WatchEvent.Modifier;
import java.nio.file.WatchService;
import java.nio.file.spi.FileSystemProvider;
import java.util.List;
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

import it.mirkoscotti.nio.s3.configuration.BucketDescriptor;
import it.mirkoscotti.nio.s3.helpers.JunitHelper;

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

	private static final String TEST_BUCKET = "test-bucket";

	private static final String S3 = "s3";

	@Mock
	private BucketFileSystem fileSystem;

	@Test
	void nullTest()
	{
		Assertions.assertThrows(NullPointerException.class, () -> new BucketPath(null, null));
		Assertions.assertThrows(NullPointerException.class, () -> new BucketPath(fileSystem, null));
		Assertions.assertDoesNotThrow(() -> new BucketPath(fileSystem, ""));
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
															@Mock BucketFileSystem fileSystem)
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

	@Test
	void relativizeAbsoluteAndRelativePathsTest()
	{
		var absolutePath = new BucketPath(fileSystem, ABSOLUTE_PATH);
		var relativePath = new BucketPath(fileSystem, RELATIVE_PATH);
		Assertions.assertThrows(IllegalArgumentException.class,
								() -> absolutePath.relativize(relativePath));
		Assertions.assertThrows(IllegalArgumentException.class,
								() -> relativePath.relativize(absolutePath));
	}

	@Test
	void relativizeToEmptyPathTest()
	{
		var bucketPath = new BucketPath(fileSystem, ABSOLUTE_PATH);
		var path = bucketPath.relativize(bucketPath);
		var root = findValue("root", path);
		Assertions.assertTrue(root.isEmpty());
		var result = findValue("objectKey", path).orElseGet(Assertions::fail);
		Assertions.assertTrue(result.toString().isEmpty());
	}

	@Test
	void relativizeToSiblingPathTest()
	{
		var bucketPath = new BucketPath(fileSystem, "/a/b");
		var siblingPath = new BucketPath(fileSystem, "/a/x");
		var relativizedPath = bucketPath.relativize(siblingPath);
		var root = findValue("root", relativizedPath);
		Assertions.assertTrue(root.isEmpty());
		var result = findValue("objectKey", relativizedPath).orElseGet(Assertions::fail);
		Assertions.assertEquals("a/b/../x", result);
	}

	@Test
	void relativizePartialPathTest()
	{
		var partialPath = new BucketPath(fileSystem, "/".concat(ABSOLUTE));
		var completePath = new BucketPath(fileSystem, ABSOLUTE_PATH);
		var relativizedPath = partialPath.relativize(completePath);
		var root = findValue("root", relativizedPath);
		Assertions.assertTrue(root.isEmpty());
		var result = findValue("objectKey", relativizedPath).orElseGet(Assertions::fail);
		Assertions.assertEquals(PATH, result);
	}

	@Test
	void toUriTest(@Mock FileSystemProvider fileSystemProvider, @Mock FileStore fileStore)
	{
		Mockito.when(fileSystemProvider.getScheme()).thenReturn(S3);
		Mockito.when(fileStore.name()).thenReturn(TEST_BUCKET);
		Mockito.when(fileSystem.provider()).thenReturn(fileSystemProvider);
		Mockito.when(fileSystem.getFileStores()).thenReturn(List.of(fileStore));
		var bucketPath = new BucketPath(fileSystem, ABSOLUTE_PATH);
		var uri = bucketPath.toUri();
		Assertions.assertEquals(S3, uri.getScheme());
		Assertions.assertEquals(TEST_BUCKET, uri.getHost());
		Assertions.assertEquals(ABSOLUTE_PATH, uri.getPath());
	}

	@Test
	void fromAbsolutePathToAbsolutePathTest()
	{
		var bucketPath = new BucketPath(fileSystem, ABSOLUTE_PATH);
		var absolutePath = bucketPath.toAbsolutePath();
		var root = findValue("root", absolutePath);
		Assertions.assertTrue(root.isPresent());
		var expected = ABSOLUTE_PATH.substring(1);
		var result = findValue("objectKey", absolutePath).orElseGet(Assertions::fail);
		Assertions.assertEquals(expected, result);
	}

	@Test
	void fromRelativePathToAbsolutePathTest()
	{
		var bucketPath = new BucketPath(fileSystem, RELATIVE_PATH);
		var absolutePath = bucketPath.toAbsolutePath();
		var root = findValue("root", absolutePath);
		Assertions.assertTrue(root.isPresent());
		var result = findValue("objectKey", absolutePath).orElseGet(Assertions::fail);
		Assertions.assertEquals(RELATIVE_PATH, result);
	}

	@Test
	void toRealNotExistingPathTest(@Mock FileSystemProvider fileSystemProvider,
								   @Mock FileStore fileStore)
	{
		Mockito.when(fileSystemProvider.exists(Mockito.any(Path.class))).thenReturn(false);
		Mockito.when(fileStore.name()).thenReturn(TEST_BUCKET);
		Mockito.when(fileSystem.provider()).thenReturn(fileSystemProvider);
		Mockito.when(fileSystem.getFileStores()).thenReturn(List.of(fileStore));
		Mockito.when(fileSystem.getPath(Mockito.anyString())).thenCallRealMethod();
		var bucketPath = new BucketPath(fileSystem, ABSOLUTE_PATH);
		Assertions.assertThrows(FileNotFoundException.class, bucketPath::toRealPath);
	}

	@Test
	void toRealPathTest(@Mock FileSystemProvider fileSystemProvider)
	{
		Mockito.when(fileSystemProvider.exists(Mockito.any(Path.class))).thenReturn(true);
		Mockito.when(fileSystem.provider()).thenReturn(fileSystemProvider);
		Mockito.when(fileSystem.getPath(Mockito.anyString())).thenCallRealMethod();
		var bucketPath = new BucketPath(fileSystem, ABSOLUTE_PATH);
		var realPath = JunitHelper.tryCall(bucketPath::toRealPath);
		var root = findValue("root", realPath);
		Assertions.assertTrue(root.isPresent());
		var expected = ABSOLUTE_PATH.substring(1);
		var result = findValue("objectKey", realPath).orElseGet(Assertions::fail);
		Assertions.assertEquals(expected, result);
	}

	@Test
	void compareAbsolutePathToAbsolutePathTest()
	{
		var leftPath = new BucketPath(fileSystem, ABSOLUTE_PATH);
		var rightPath = new BucketPath(fileSystem, ABSOLUTE_PATH);
		Assertions.assertEquals(0, leftPath.compareTo(rightPath));
	}

	@Test
	void compareRelativePathToRelativePathTest()
	{
		var leftPath = new BucketPath(fileSystem, RELATIVE_PATH);
		var rightPath = new BucketPath(fileSystem, RELATIVE_PATH);
		Assertions.assertEquals(0, leftPath.compareTo(rightPath));
	}

	@Test
	void compareAbsolutePathToRelativePathTest()
	{
		var leftPath = new BucketPath(fileSystem, ABSOLUTE_PATH);
		var rightPath = new BucketPath(fileSystem, RELATIVE_PATH);
		Assertions.assertTrue(leftPath.compareTo(rightPath) < 0);
	}

	@Test
	void compareRelativePathToAbsolutePathTest()
	{
		var leftPath = new BucketPath(fileSystem, RELATIVE_PATH);
		var rightPath = new BucketPath(fileSystem, ABSOLUTE_PATH);
		Assertions.assertTrue(leftPath.compareTo(rightPath) > 0);
	}

	@Test
	void registerFileTest(@Mock WatchService watchService)
	{
		try (var mock = Mockito.mockStatic(Files.class))
		{
			mock.when(() -> Files.isDirectory(Mockito.any(Path.class))).thenReturn(false);
			var bucketPath = new BucketPath(fileSystem, PATH);
			Assertions.assertThrows(NotDirectoryException.class,
									() -> bucketPath.register(watchService));
		}
	}

	@Test
	void registerNotExistingDirectoryTest(@Mock WatchService watchService)
	{
		try (var mock = Mockito.mockStatic(Files.class))
		{
			mock.when(() -> Files.isDirectory(Mockito.any(Path.class))).thenReturn(true);
			mock.when(() -> Files.exists(Mockito.any(Path.class))).thenReturn(false);
			var bucketPath = new BucketPath(fileSystem, PATH);
			Assertions.assertThrows(NotDirectoryException.class,
									() -> bucketPath.register(watchService));
		}
	}

	@Test
	void registerPathForUnsupportedEventTest(@Mock WatchService watchService)
	{
		try (var mock = Mockito.mockStatic(Files.class))
		{
			mock.when(() -> Files.isDirectory(Mockito.any(Path.class))).thenReturn(true);
			mock.when(() -> Files.exists(Mockito.any(Path.class))).thenReturn(true);
			var bucketPath = new BucketPath(fileSystem, PATH);
			var exception = Assertions.assertThrows(UnsupportedOperationException.class,
													() -> bucketPath.register(watchService,
																			  StandardWatchEventKinds.OVERFLOW));
			var suppressed = exception.getSuppressed();
			Assertions.assertEquals(1, suppressed.length);
			Assertions.assertInstanceOf(IllegalArgumentException.class, suppressed[0]);
		}
	}

	@Test
	void registerPathForUnsupportedModifierTest(@Mock WatchService watchService,
												@Mock Modifier modifier)
	{
		try (var mock = Mockito.mockStatic(Files.class))
		{
			mock.when(() -> Files.isDirectory(Mockito.any(Path.class))).thenReturn(true);
			mock.when(() -> Files.exists(Mockito.any(Path.class))).thenReturn(true);
			var bucketPath = new BucketPath(fileSystem, PATH);
			var kinds = new Kind[] {StandardWatchEventKinds.ENTRY_CREATE};
			var exception = Assertions.assertThrows(UnsupportedOperationException.class,
													() -> bucketPath.register(watchService,
																			  kinds,
																			  modifier));
			var suppressed = exception.getSuppressed();
			Assertions.assertEquals(1, suppressed.length);
			Assertions.assertInstanceOf(IllegalArgumentException.class, suppressed[0]);
		}
	}

	@Test
	void registerPathWithUnexpectedWatchServiceTest(@Mock WatchService watchService)
	{
		try (var mock = Mockito.mockStatic(Files.class))
		{
			mock.when(() -> Files.isDirectory(Mockito.any(Path.class))).thenReturn(true);
			mock.when(() -> Files.exists(Mockito.any(Path.class))).thenReturn(true);
			var bucketPath = new BucketPath(fileSystem, PATH);
			Assertions.assertThrows(ProviderMismatchException.class,
									() -> bucketPath.register(watchService,
															  StandardWatchEventKinds.ENTRY_CREATE));
		}
	}

	@Test
	void registerPathWithUnexpectedWatchServiceTest(@Mock DirectoryWatchService watchService)
	{
		try (var mock = Mockito.mockStatic(Files.class))
		{
			mock.when(() -> Files.isDirectory(Mockito.any(Path.class))).thenReturn(true);
			mock.when(() -> Files.exists(Mockito.any(Path.class))).thenReturn(true);
			var bucketPath = new BucketPath(fileSystem, PATH);
			Assertions.assertDoesNotThrow(() -> bucketPath.register(watchService,
																	StandardWatchEventKinds.ENTRY_CREATE));
		}
	}

	@Test
	void equalsToPathOfDifferentTypeTest(@Mock Path path)
	{
		var bucketPath = new BucketPath(fileSystem, ABSOLUTE_PATH);
		var result = bucketPath.equals(path);
		Assertions.assertFalse(result);
	}

	@Test
	void equalsToPathWithDifferentFileSystemTest(@Mock FileSystem fileSystem, @Mock BucketPath path)
	{
		var bucketPath = new BucketPath(this.fileSystem, ABSOLUTE_PATH);
		var result = bucketPath.equals(path);
		Assertions.assertFalse(result);
	}

	@Test
	void equalsToDifferentPathTest(@Mock BucketPath path)
	{
		Mockito.when(path.getFileSystem()).thenReturn(fileSystem);
		var bucketPath = new BucketPath(fileSystem, ABSOLUTE_PATH);
		var result = bucketPath.equals(path);
		Assertions.assertFalse(result);
	}

	@Test
	void equalsToRelativePathWithSameKeyTest()
	{
		var absolutePath = new BucketPath(fileSystem, ABSOLUTE_PATH);
		var relativePath = new BucketPath(fileSystem, ABSOLUTE.concat(PATH));
		var result = absolutePath.equals(relativePath);
		Assertions.assertFalse(result);
	}

	@Test
	void equalsToAbsolutePathWithSameKeyTest()
	{
		var relativePath = new BucketPath(fileSystem, RELATIVE_PATH);
		var absolutePath = new BucketPath(fileSystem, "/".concat(RELATIVE_PATH));
		var result = relativePath.equals(absolutePath);
		Assertions.assertFalse(result);
	}

	@Test
	void equalsToAbsolutePathTest()
	{
		var bucketPath = new BucketPath(fileSystem, ABSOLUTE_PATH);
		var result = bucketPath.equals(bucketPath);
		Assertions.assertTrue(result);
	}

	@Test
	void equalsToRelativePathTest()
	{
		var bucketPath = new BucketPath(fileSystem, RELATIVE_PATH);
		var result = bucketPath.equals(bucketPath);
		Assertions.assertTrue(result);
	}

	@Test
	void hashCodeTest()
	{
		var bucketPath1 = new BucketPath(fileSystem, ABSOLUTE_PATH);
		var bucketPath2 = new BucketPath(fileSystem, ABSOLUTE_PATH);
		Assertions.assertEquals(bucketPath1, bucketPath2);
		Assertions.assertEquals(bucketPath1.hashCode(), bucketPath2.hashCode());
	}

	@Test
	void pathToString()
	{
		var bucketPath1 = new BucketPath(fileSystem, ABSOLUTE_PATH);
		Assertions.assertEquals(ABSOLUTE_PATH.substring(1), bucketPath1.toString());
	}

	@Test
	void rootToString()
	{
		var bucketPath1 = new BucketPath(fileSystem, ABSOLUTE_PATH);
		Assertions.assertEquals("/", bucketPath1.getRoot().toString());
	}

	private Field findField(String fieldName)
	{
		var result = JunitHelper.findFieldByName(BucketPath.class, fieldName);
		ReflectionSupport.makeAccessible(result);
		return result;
	}

	private Optional<Object> findValue(String fieldName, Object instance)
	{
		var field = findField(fieldName);
		return Try.call(() -> field.get(instance)).toOptional();
	}
}
