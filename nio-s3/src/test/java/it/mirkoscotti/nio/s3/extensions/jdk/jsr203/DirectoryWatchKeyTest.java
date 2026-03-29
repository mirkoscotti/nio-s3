package it.mirkoscotti.nio.s3.extensions.jdk.jsr203;

import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchEvent.Kind;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.platform.commons.support.ReflectionSupport;
import org.mockito.Mock;
import org.mockito.MockSettings;
import org.mockito.MockedConstruction.Context;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import it.mirkoscotti.nio.s3.helpers.JunitHelper;

/**
 * @author mirko.scotti
 * @version Apr 18, 2025
 */
@Disabled
@ExtendWith(MockitoExtension.class)
class DirectoryWatchKeyTest
{

	@Mock
	private BucketPath directory;

	@Test
	void isValidTest()
	{
		var watchKey = new DirectoryWatchKey(directory);
		Assertions.assertTrue(watchKey.isValid());
	}

	@Test
	void pollEventsTest()
	{
		try (var mock = Mockito.mockConstruction(LinkedBlockingQueue.class, this::initializeEvent))
		{
			var watchKey = new DirectoryWatchKey(directory);
			var list = watchKey.pollEvents();
			Assertions.assertEquals(1, list.size());
			var queue = Assertions.assertInstanceOf(BlockingQueue.class, mock.constructed().get(0));
			Mockito.verify(queue, Mockito.atLeastOnce()).clear();
		}
	}

	@Test
	void resetInvalidKeyTest()
	{
		var field = JunitHelper.findFieldByType(DirectoryWatchKey.class, boolean.class);
		ReflectionSupport.makeAccessible(field);
		try (var mock = Mockito.mockConstruction(LinkedBlockingQueue.class))
		{
			var watchKey = new DirectoryWatchKey(directory);
			field.set(watchKey, false);
			Assertions.assertFalse(watchKey.reset());
			Mockito.verify(mock.constructed().get(0), Mockito.never()).clear();
		}
		catch (ReflectiveOperationException x)
		{
			Assertions.fail(x);
		}
	}

	@Test
	void resetValidKeyTest()
	{
		try (var mock = Mockito.mockConstruction(LinkedBlockingQueue.class))
		{
			var watchKey = new DirectoryWatchKey(directory);
			Assertions.assertTrue(watchKey.reset());
			Mockito.verify(mock.constructed().get(0), Mockito.atLeastOnce()).clear();
		}
	}

	@Test
	void cancelTest()
	{
		try (var mapMock = Mockito.mockConstruction(ConcurrentHashMap.class);
			 var queueMock = Mockito.mockConstruction(LinkedBlockingQueue.class))
		{
			var watchKey = new DirectoryWatchKey(directory);
			watchKey.cancel();
			var map = mapMock.constructed().get(0);
			Mockito.verify(map, Mockito.atLeastOnce()).clear();
			var queue = queueMock.constructed().get(0);
			Mockito.verify(queue, Mockito.atLeastOnce()).clear();
		}
	}

	@Test
	void watchableTest()
	{
		var watchKey = new DirectoryWatchKey(directory);
		Assertions.assertEquals(directory, watchKey.watchable());
	}

	@Test
	void initialUpdateEventsTest(@Mock Instant instant)
	{
		try (var mock = Mockito.mockConstruction(ConcurrentHashMap.class, initializeSettings(),
												 this::initializeEmpty))
		{
			var watchKey = new DirectoryWatchKey(directory);
			watchKey.updateEvents(Map.of("file", instant));
			if (mock.constructed().get(0) instanceof ConcurrentHashMap<?, ?> map)
			{
				Mockito.verify(map, Mockito.atLeastOnce()).clear();
				Mockito.verify(map, Mockito.atLeastOnce()).putAll(Mockito.anyMap());
			}
			else
			{
				Assertions.fail("Expected a map constructed.");
			}
		}
	}

	@Test
	void insertEventsTest(@Mock BucketFileSystem fileSystem, @Mock Instant instant)
	{
		Mockito.when(directory.getFileSystem()).thenReturn(fileSystem);
		try (var queueMock = Mockito.mockConstruction(LinkedBlockingQueue.class);
			 var mapMock = Mockito.mockConstruction(ConcurrentHashMap.class, initializeSettings(),
													this::initializeWithOneFile))
		{
			var watchKey = new DirectoryWatchKey(directory, StandardWatchEventKinds.ENTRY_CREATE);
			watchKey.updateEvents(Map.of("new", instant));
			if (mapMock.constructed().get(0) instanceof ConcurrentHashMap<?, ?> map)
			{
				Mockito.verify(map, Mockito.atLeastOnce()).clear();
				Mockito.verify(map, Mockito.atLeastOnce()).putAll(Mockito.anyMap());
			}
			else
			{
				Assertions.fail("Expected a map constructed.");
			}
			@SuppressWarnings("unchecked")
			BlockingQueue<WatchEvent<?>> queue = queueMock.constructed().get(0);
			Mockito.verify(queue, Mockito.atLeastOnce()).add(Mockito.any(WatchEvent.class));
		}
	}

	@Test
	void noInsertEventsTest(@Mock BucketFileSystem fileSystem)
	{
		try (var queueMock = Mockito.mockConstruction(LinkedBlockingQueue.class);
			 var mapMock = Mockito.mockConstruction(ConcurrentHashMap.class, initializeSettings(),
													this::initializeWithOneFile))
		{
			var watchKey = new DirectoryWatchKey(directory, StandardWatchEventKinds.ENTRY_CREATE);
			if (mapMock.constructed().get(0) instanceof ConcurrentHashMap<?, ?> map)
			{
				var entry = map.entrySet().stream().findFirst().orElseGet(Assertions::fail);
				if (entry.getValue() instanceof Instant instant)
				{
					watchKey.updateEvents(Map.of(entry.getKey().toString(), instant));
					Mockito.verify(map, Mockito.atLeastOnce()).clear();
					Mockito.verify(map, Mockito.atLeastOnce()).putAll(Mockito.anyMap());
				}
				else
				{
					Assertions.fail("Expected an instant.");
				}
			}
			else
			{
				Assertions.fail("Expected a map constructed.");
			}
			@SuppressWarnings("unchecked")
			BlockingQueue<WatchEvent<?>> queue = queueMock.constructed().get(0);
			Mockito.verify(queue, Mockito.never()).add(Mockito.any(WatchEvent.class));
		}
	}

	@Test
	void modifyEventsTest(@Mock BucketFileSystem fileSystem, @Mock Instant instant)
	{
		Mockito.when(directory.getFileSystem()).thenReturn(fileSystem);
		try (var queueMock = Mockito.mockConstruction(LinkedBlockingQueue.class);
			 var mapMock = Mockito.mockConstruction(ConcurrentHashMap.class, initializeSettings(),
													this::initializeWithOneFile))
		{
			var watchKey = new DirectoryWatchKey(directory, StandardWatchEventKinds.ENTRY_MODIFY);
			if (mapMock.constructed().get(0) instanceof ConcurrentHashMap<?, ?> map)
			{
				var entry = map.entrySet().stream().findFirst().orElseGet(Assertions::fail);
				watchKey.updateEvents(Map.of(entry.getKey().toString(), instant));
				Mockito.verify(map, Mockito.atLeastOnce()).clear();
				Mockito.verify(map, Mockito.atLeastOnce()).putAll(Mockito.anyMap());
			}
			else
			{
				Assertions.fail("Expected a map constructed.");
			}
			@SuppressWarnings("unchecked")
			BlockingQueue<WatchEvent<?>> queue = queueMock.constructed().get(0);
			Mockito.verify(queue, Mockito.atLeastOnce()).add(Mockito.any(WatchEvent.class));
		}
	}

	@Test
	void noModifyEventsTest(@Mock BucketFileSystem fileSystem)
	{
		try (var queueMock = Mockito.mockConstruction(LinkedBlockingQueue.class);
			 var mapMock = Mockito.mockConstruction(ConcurrentHashMap.class, initializeSettings(),
													this::initializeWithOneFile))
		{
			var watchKey = new DirectoryWatchKey(directory, StandardWatchEventKinds.ENTRY_MODIFY);
			if (mapMock.constructed().get(0) instanceof ConcurrentHashMap<?, ?> map)
			{
				var entry = map.entrySet().stream().findFirst().orElseGet(Assertions::fail);
				if (entry.getValue() instanceof Instant instant)
				{
					watchKey.updateEvents(Map.of(entry.getKey().toString(), instant));
					Mockito.verify(map, Mockito.atLeastOnce()).clear();
					Mockito.verify(map, Mockito.atLeastOnce()).putAll(Mockito.anyMap());
				}
				else
				{
					Assertions.fail("Expected an instant.");
				}
			}
			else
			{
				Assertions.fail("Expected a map constructed.");
			}
			@SuppressWarnings("unchecked")
			BlockingQueue<WatchEvent<?>> queue = queueMock.constructed().get(0);
			Mockito.verify(queue, Mockito.never()).add(Mockito.any(WatchEvent.class));
		}
	}

	@Test
	void deleteEventsTest(@Mock BucketFileSystem fileSystem, @Mock Instant instant)
	{
		Mockito.when(directory.getFileSystem()).thenReturn(fileSystem);
		try (var queueMock = Mockito.mockConstruction(LinkedBlockingQueue.class);
			 var mapMock = Mockito.mockConstruction(ConcurrentHashMap.class, initializeSettings(),
													this::initializeWithOneFile))
		{
			var watchKey = new DirectoryWatchKey(directory, StandardWatchEventKinds.ENTRY_DELETE);
			watchKey.updateEvents(Map.of("new", instant));
			if (mapMock.constructed().get(0) instanceof ConcurrentHashMap<?, ?> map)
			{
				Mockito.verify(map, Mockito.atLeastOnce()).clear();
				Mockito.verify(map, Mockito.atLeastOnce()).putAll(Mockito.anyMap());
			}
			else
			{
				Assertions.fail("Expected a map constructed.");
			}
			@SuppressWarnings("unchecked")
			BlockingQueue<WatchEvent<?>> queue = queueMock.constructed().get(0);
			Mockito.verify(queue, Mockito.atLeastOnce()).add(Mockito.any(WatchEvent.class));
		}
	}

	@Test
	void noDeleteEventsTest(@Mock BucketFileSystem fileSystem)
	{
		try (var queueMock = Mockito.mockConstruction(LinkedBlockingQueue.class);
			 var mapMock = Mockito.mockConstruction(ConcurrentHashMap.class, initializeSettings(),
													this::initializeWithOneFile))
		{
			var watchKey = new DirectoryWatchKey(directory, StandardWatchEventKinds.ENTRY_DELETE);
			if (mapMock.constructed().get(0) instanceof ConcurrentHashMap<?, ?> map)
			{
				var entry = map.entrySet().stream().findFirst().orElseGet(Assertions::fail);
				if (entry.getValue() instanceof Instant instant)
				{
					watchKey.updateEvents(Map.of(entry.getKey().toString(), instant));
					Mockito.verify(map, Mockito.atLeastOnce()).clear();
					Mockito.verify(map, Mockito.atLeastOnce()).putAll(Mockito.anyMap());
				}
				else
				{
					Assertions.fail("Expected an instant.");
				}
			}
			else
			{
				Assertions.fail("Expected a map constructed.");
			}
			@SuppressWarnings("unchecked")
			BlockingQueue<WatchEvent<?>> queue = queueMock.constructed().get(0);
			Mockito.verify(queue, Mockito.never()).add(Mockito.any(WatchEvent.class));
		}
	}

	@Test
	void unsupportedEventTest(@Mock Instant instant, @Mock Kind<Path> kind)
	{
		try (var queueMock = Mockito.mockConstruction(LinkedBlockingQueue.class);
			 var mapMock = Mockito.mockConstruction(ConcurrentHashMap.class, initializeSettings(),
													this::initializeWithOneFile))
		{
			var watchKey = new DirectoryWatchKey(directory, kind);
			var map = Map.of("new", instant);
			Assertions.assertThrows(UnsupportedOperationException.class,
									() -> watchKey.updateEvents(map));
		}
	}

	private void initializeEvent(BlockingQueue<WatchEvent<?>> queue, Context context)
	{
		var watchEvent = Mockito.mock(WatchEvent.class);
		Mockito.when(queue.toArray()).thenReturn(new WatchEvent<?>[] {watchEvent});
	}

	private void initializeEmpty(ConcurrentHashMap<String, Instant> map, Context context)
	{
		Mockito.when(map.isEmpty()).thenReturn(true);
	}

	private void initializeWithOneFile(ConcurrentHashMap<String, Instant> map, Context context)
	{
		var instant = Mockito.mock(Instant.class);
		map.put("initial", instant);
	}

	private MockSettings initializeSettings()
	{
		return Mockito.withSettings().defaultAnswer(Mockito.CALLS_REAL_METHODS);
	}
}
