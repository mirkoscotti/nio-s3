package io.github.mirkoscotti.nio.s3.extensions.jdk.jsr203;

import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.StandardCopyOption;

import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import it.mirkoscotti.nio.s3.helpers.JunitHelper;

/**
 * @author mirko.scotti
 * @version Feb 12, 2026
 */
class CopyTest
{

	private static final String SOURCE_DIRECTORY = "source/";

	private static final String TARGET_DIRECTORY = "target/";

	private static final String SOURCE_FILE = SOURCE_DIRECTORY.concat("source.txt");

	private static final String TARGET_FILE = TARGET_DIRECTORY.concat("target.txt");

	@Test
	void copyNotExistingDirectoryTest()
	{
		var fileSystem = Jimfs.newFileSystem(Configuration.unix());
		var source = fileSystem.getPath(SOURCE_DIRECTORY);
		var target = fileSystem.getPath(TARGET_DIRECTORY);
		Assertions.assertThrows(NoSuchFileException.class, () -> Files.copy(source, target));
	}

	@Test
	void copyNotExistingFileTest()
	{
		var fileSystem = Jimfs.newFileSystem(Configuration.unix());
		var source = fileSystem.getPath(SOURCE_FILE);
		var target = fileSystem.getPath(TARGET_FILE);
		Assertions.assertThrows(NoSuchFileException.class, () -> Files.copy(source, target));
	}

	@Test
	void copyDirectoryTest()
	{
		var fileSystem = Jimfs.newFileSystem(Configuration.unix());
		var source = JunitHelper.tryCall(() -> Files.createDirectory(fileSystem.getPath(SOURCE_DIRECTORY)));
		Assertions.assertTrue(Files.exists(source));
		Assertions.assertTrue(Files.isDirectory(source));
		var target = fileSystem.getPath("/target");
		Assertions.assertFalse(Files.isDirectory(target));
		JunitHelper.tryCall(() -> Files.copy(source, target));
		Assertions.assertTrue(Files.exists(target));
		Assertions.assertTrue(Files.isDirectory(target));
	}

	@Test
	void copyFileTest()
	{
		var fileSystem = Jimfs.newFileSystem(Configuration.unix());
		JunitHelper.tryCall(() -> Files.createDirectory(fileSystem.getPath(SOURCE_DIRECTORY)));
		var source = JunitHelper.tryCall(() -> Files.createFile(fileSystem.getPath(SOURCE_FILE)));
		Assertions.assertTrue(Files.exists(source));
		Assertions.assertTrue(Files.isRegularFile(source));
		JunitHelper.tryCall(() -> Files.createDirectory(fileSystem.getPath(TARGET_DIRECTORY)));
		var target = fileSystem.getPath(TARGET_FILE);
		Assertions.assertFalse(Files.exists(target));
		JunitHelper.tryCall(() -> Files.copy(source, target));
		Assertions.assertTrue(Files.exists(target));
		Assertions.assertTrue(Files.isRegularFile(target));
	}

	@Test
	void copyFileToNotExistingDirectoryTest()
	{
		var fileSystem = Jimfs.newFileSystem(Configuration.unix());
		JunitHelper.tryCall(() -> Files.createDirectory(fileSystem.getPath(SOURCE_DIRECTORY)));
		var source = JunitHelper.tryCall(() -> Files.createFile(fileSystem.getPath(SOURCE_FILE)));
		Assertions.assertTrue(Files.exists(source));
		Assertions.assertTrue(Files.isRegularFile(source));
		var target = fileSystem.getPath(TARGET_DIRECTORY).resolve(SOURCE_FILE);
		Assertions.assertFalse(Files.exists(target));
		Assertions.assertThrows(NoSuchFileException.class, () -> Files.copy(source, target));
	}

	@Test
	void copyFileToDirectoryTest()
	{
		var fileSystem = Jimfs.newFileSystem(Configuration.unix());
		JunitHelper.tryCall(() -> Files.createDirectory(fileSystem.getPath(SOURCE_DIRECTORY)));
		var source = JunitHelper.tryCall(() -> Files.createFile(fileSystem.getPath(SOURCE_FILE)));
		Assertions.assertTrue(Files.exists(source));
		Assertions.assertTrue(Files.isRegularFile(source));
		var target = fileSystem.getPath(TARGET_DIRECTORY);
		JunitHelper.tryCall(() -> Files.createDirectory(target));
		Assertions.assertTrue(Files.isDirectory(target));
		Assertions.assertThrows(FileAlreadyExistsException.class, () -> Files.copy(source, target));
	}

	@Test
	void copyFileToAlreadyExistingFileTest()
	{
		var fileSystem = Jimfs.newFileSystem(Configuration.unix());
		JunitHelper.tryCall(() -> Files.createDirectory(fileSystem.getPath(SOURCE_DIRECTORY)));
		var source = JunitHelper.tryCall(() -> Files.createFile(fileSystem.getPath(SOURCE_FILE)));
		Assertions.assertTrue(Files.exists(source));
		Assertions.assertTrue(Files.isRegularFile(source));
		JunitHelper.tryCall(() -> Files.createDirectory(fileSystem.getPath(TARGET_DIRECTORY)));
		var target = JunitHelper.tryCall(() -> Files.createFile(fileSystem.getPath(TARGET_FILE)));
		Assertions.assertTrue(Files.exists(target));
		Assertions.assertTrue(Files.isRegularFile(target));
		Assertions.assertThrows(FileAlreadyExistsException.class, () -> Files.copy(source, target));
	}

	@Test
	void copyNotEmptyDirectoryTest()
	{
		var fileSystem = Jimfs.newFileSystem(Configuration.unix());
		var source = JunitHelper.tryCall(() -> Files.createDirectory(fileSystem.getPath(SOURCE_DIRECTORY)));
		Assertions.assertTrue(Files.exists(source));
		Assertions.assertTrue(Files.isDirectory(source));
		JunitHelper.tryCall(() -> Files.createFile(fileSystem.getPath(SOURCE_FILE)));
		var target = fileSystem.getPath(TARGET_DIRECTORY);
		Assertions.assertFalse(Files.exists(target));
		JunitHelper.tryCall(() -> Files.copy(source, target));
		Assertions.assertTrue(Files.exists(target));
		Assertions.assertTrue(Files.isDirectory(target));
		Assertions.assertEquals(0, JunitHelper.tryCall(() -> Files.list(target).count()));
	}

	@Test
	void copyDirectoryReplacingExistingTest()
	{
		var fileSystem = Jimfs.newFileSystem(Configuration.unix());
		var source = JunitHelper.tryCall(() -> Files.createDirectory(fileSystem.getPath(SOURCE_DIRECTORY)));
		Assertions.assertTrue(Files.exists(source));
		Assertions.assertTrue(Files.isDirectory(source));
		var target = JunitHelper.tryCall(() -> Files.createDirectory(fileSystem.getPath(TARGET_DIRECTORY)));
		Assertions.assertTrue(Files.exists(target));
		Assertions.assertTrue(Files.isDirectory(target));
		JunitHelper.tryCall(() -> Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING));
		Assertions.assertTrue(Files.exists(target));
		Assertions.assertTrue(Files.isDirectory(target));
	}

	@Test
	void copyDirectoryReplacingExistingFileTest()
	{
		var fileSystem = Jimfs.newFileSystem(Configuration.unix());
		var source = JunitHelper.tryCall(() -> Files.createDirectory(fileSystem.getPath(SOURCE_DIRECTORY)));
		Assertions.assertTrue(Files.exists(source));
		Assertions.assertTrue(Files.isDirectory(source));
		JunitHelper.tryCall(() -> Files.createDirectory(fileSystem.getPath(TARGET_DIRECTORY)));
		var target = JunitHelper.tryCall(() -> Files.createFile(fileSystem.getPath(TARGET_FILE)));
		Assertions.assertTrue(Files.exists(target));
		Assertions.assertTrue(Files.isRegularFile(target));
		JunitHelper.tryCall(() -> Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING));
		Assertions.assertTrue(Files.exists(target));
		Assertions.assertTrue(Files.isDirectory(target));
	}

	@Test
	void copyFileReplacingExistingTest()
	{
		var fileSystem = Jimfs.newFileSystem(Configuration.unix());
		JunitHelper.tryCall(() -> Files.createDirectory(fileSystem.getPath(SOURCE_DIRECTORY)));
		var source = JunitHelper.tryCall(() -> Files.createFile(fileSystem.getPath(SOURCE_FILE)));
		Assertions.assertTrue(Files.exists(source));
		Assertions.assertTrue(Files.isRegularFile(source));
		JunitHelper.tryCall(() -> Files.createDirectory(fileSystem.getPath(TARGET_DIRECTORY)));
		var target = JunitHelper.tryCall(() -> Files.createFile(fileSystem.getPath(TARGET_FILE)));
		Assertions.assertTrue(Files.exists(target));
		Assertions.assertTrue(Files.isRegularFile(target));
		JunitHelper.tryCall(() -> Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING));
		Assertions.assertTrue(Files.exists(target));
		Assertions.assertTrue(Files.isRegularFile(target));
	}

	@Test
	void copyFileReplacingExistingDirectoryTest()
	{
		var fileSystem = Jimfs.newFileSystem(Configuration.unix());
		JunitHelper.tryCall(() -> Files.createDirectory(fileSystem.getPath(SOURCE_DIRECTORY)));
		var source = JunitHelper.tryCall(() -> Files.createFile(fileSystem.getPath(SOURCE_FILE)));
		Assertions.assertTrue(Files.exists(source));
		Assertions.assertTrue(Files.isRegularFile(source));
		var target = JunitHelper.tryCall(() -> Files.createDirectory(fileSystem.getPath(TARGET_DIRECTORY)));
		Assertions.assertTrue(Files.exists(target));
		Assertions.assertTrue(Files.isDirectory(target));
		JunitHelper.tryCall(() -> Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING));
		Assertions.assertTrue(Files.exists(target));
		Assertions.assertTrue(Files.isRegularFile(target));
	}

	@Test
	void copyDirectoryToNotEmptyDirectoryTest()
	{
		var fileSystem = Jimfs.newFileSystem(Configuration.unix());
		var source = JunitHelper.tryCall(() -> Files.createDirectory(fileSystem.getPath(SOURCE_DIRECTORY)));
		Assertions.assertTrue(Files.exists(source));
		Assertions.assertTrue(Files.isDirectory(source));
		JunitHelper.tryCall(() -> Files.createFile(fileSystem.getPath(SOURCE_FILE)));
		var target = JunitHelper.tryCall(() -> Files.createDirectory(fileSystem.getPath(TARGET_DIRECTORY)));
		Assertions.assertTrue(Files.exists(target));
		Assertions.assertTrue(Files.isDirectory(target));
		JunitHelper.tryCall(() -> Files.createFile(fileSystem.getPath(TARGET_FILE)));
		Assertions.assertThrows(DirectoryNotEmptyException.class,
								() -> Files.copy(source, target,
												 StandardCopyOption.REPLACE_EXISTING));
	}
}
