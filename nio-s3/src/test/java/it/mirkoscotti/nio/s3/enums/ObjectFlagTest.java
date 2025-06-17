package it.mirkoscotti.nio.s3.enums;

import it.mirkoscotti.nio.s3.extensions.jdk.jsr203.BucketPath;

import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Set;
import java.util.stream.Stream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author mirko.scotti
 * @version May 17, 2025
 */
@ExtendWith(MockitoExtension.class)
class ObjectFlagTest
{

	@Test
	void matchesNullTest()
	{
		Assertions.assertTrue(ObjectFlag.IS_READABLE.matches(null));
		Stream.of(ObjectFlag.values())
			  .filter(item -> item != ObjectFlag.IS_READABLE)
			  .forEach(item -> Assertions.assertFalse(item.matches(null)));
	}

	@Test
	void matchesEmptyTest()
	{
		var set = Set.<OpenOption>of();
		Assertions.assertTrue(ObjectFlag.IS_READABLE.matches(set));
		Stream.of(ObjectFlag.values())
			  .filter(item -> item != ObjectFlag.IS_READABLE)
			  .forEach(item -> Assertions.assertFalse(item.matches(set)));
	}

	@Test
	void doesNotMatchTest(@Mock OpenOption openOption)
	{
		Assertions.assertFalse(ObjectFlag.IS_READABLE.matches(Set.of(StandardOpenOption.WRITE)));
		Stream.of(ObjectFlag.values())
			  .filter(item -> item != ObjectFlag.IS_READABLE)
			  .forEach(item -> Assertions.assertFalse(item.matches(Set.of(openOption))));
	}

	@Test
	void isReadableMatchesTest(@Mock OpenOption openOption)
	{
		Assertions.assertTrue(ObjectFlag.IS_READABLE.matches(Set.of(StandardOpenOption.READ)));
		Assertions.assertTrue(ObjectFlag.IS_READABLE.matches(Set.of(openOption)));
	}

	@Test
	void isAppendableMatchesTest()
	{
		var set = Set.of(StandardOpenOption.APPEND);
		Assertions.assertTrue(ObjectFlag.IS_APPENDABLE.matches(set));
	}

	@Test
	void isTruncatableMatchesTest()
	{
		var set = Set.of(StandardOpenOption.TRUNCATE_EXISTING);
		Assertions.assertTrue(ObjectFlag.IS_TRUNCATABLE.matches(set));
	}

	@Test
	void isWritableMatchesTest()
	{
		Assertions.assertTrue(ObjectFlag.IS_WRITABLE.matches(Set.of(StandardOpenOption.APPEND)));
		Assertions.assertTrue(ObjectFlag.IS_WRITABLE.matches(Set.of(StandardOpenOption.WRITE)));
	}

	@Test
	void readWriteCheckFailureTest()
	{
		var set1 = Set.of(StandardOpenOption.READ, StandardOpenOption.WRITE);
		Assertions.assertThrows(IllegalArgumentException.class,
								() -> ObjectFlag.readWriteCheck(set1));
	}

	@Test
	void readWriteCheckTest()
	{
		Assertions.assertDoesNotThrow(() -> ObjectFlag.readWriteCheck(Set.of()));
		Assertions.assertDoesNotThrow(() -> ObjectFlag.readWriteCheck(Set.of(StandardOpenOption.WRITE)));
		Assertions.assertDoesNotThrow(() -> ObjectFlag.readWriteCheck(Set.of(StandardOpenOption.APPEND)));
		Assertions.assertDoesNotThrow(() -> ObjectFlag.readWriteCheck(Set.of(StandardOpenOption.TRUNCATE_EXISTING)));
	}

	@Test
	void appendTruncateCheckFailureTest()
	{
		var set = Set.of(StandardOpenOption.APPEND, StandardOpenOption.TRUNCATE_EXISTING);
		Assertions.assertThrows(IllegalArgumentException.class,
								() -> ObjectFlag.appendTruncateCheck(set));
	}

	@Test
	void appendTruncateCheckTest()
	{
		Assertions.assertDoesNotThrow(() -> ObjectFlag.appendTruncateCheck(Set.of()));
		Assertions.assertDoesNotThrow(() -> ObjectFlag.appendTruncateCheck(Set.of(StandardOpenOption.APPEND)));
		Assertions.assertDoesNotThrow(() -> ObjectFlag.appendTruncateCheck(Set.of(StandardOpenOption.TRUNCATE_EXISTING)));
	}

	@Test
	void notCreatableTruncateCheckTest(@Mock BucketPath path)
	{
		var set = Set.of(StandardOpenOption.CREATE_NEW, StandardOpenOption.TRUNCATE_EXISTING);
		try (var mock = Mockito.mockStatic(Files.class))
		{
			mock.when(() -> Files.exists(Mockito.any(Path.class))).thenReturn(true);
			Assertions.assertThrows(IllegalArgumentException.class,
									() -> ObjectFlag.truncateCheck(path, set));
		}
	}

	@Test
	void truncateCheckTest(@Mock BucketPath path)
	{
		Assertions.assertDoesNotThrow(() -> ObjectFlag.truncateCheck(path, Set.of()));
	}

	@Test
	void createExistingFileCheckTest(@Mock BucketPath path)
	{
		var set = Set.of(StandardOpenOption.CREATE_NEW);
		try (var mock = Mockito.mockStatic(Files.class))
		{
			mock.when(() -> Files.exists(Mockito.any(Path.class), Mockito.any(LinkOption.class)))
				.thenReturn(true);
			Assertions.assertThrows(FileAlreadyExistsException.class,
									() -> ObjectFlag.createCheck(path, set));
		}
	}

	@Test
	void notCreatableTruncatableCheckTest(@Mock BucketPath path)
	{
		Assertions.assertDoesNotThrow(() -> ObjectFlag.createCheck(path, Set.of()));
	}
}
