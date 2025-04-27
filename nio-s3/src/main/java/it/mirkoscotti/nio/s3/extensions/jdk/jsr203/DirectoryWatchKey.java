package it.mirkoscotti.nio.s3.extensions.jdk.jsr203;

import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchEvent.Kind;
import java.nio.file.WatchKey;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * @author mirko.scotti
 * @version Apr 04, 2025
 */
public class DirectoryWatchKey
	implements WatchKey
{

	private final Object lock = new Object();

	private final Map<String, Instant> lastState = new ConcurrentHashMap<>();

	private final BlockingQueue<WatchEvent<?>> events = new LinkedBlockingQueue<>();

	private final BucketPath directory;

	private final Kind<Path>[] kinds;

	private volatile boolean valid = true;

	@SafeVarargs
	DirectoryWatchKey(BucketPath directory, Kind<Path>... kinds)
	{
		this.directory = directory;
		this.kinds = kinds;
	}

	@Override
	public boolean isValid()
	{
		return valid;
	}

	@Override
	public List<WatchEvent<?>> pollEvents()
	{
		synchronized (lock)
		{
			var result = new ArrayList<>(events);
			events.clear();
			return result;
		}
	}

	@Override
	public boolean reset()
	{
		synchronized (lock)
		{
			boolean result = false;
			if (isValid())
			{
				events.clear();
				result = true;
			}
			return result;
		}
	}

	@Override
	public synchronized void cancel()
	{
		synchronized (lock)
		{
			valid = false;
			lastState.clear();
			events.clear();
		}
	}

	@Override
	public BucketPath watchable()
	{
		return directory;
	}

	void updateEvents(Map<String, Instant> currentState)
	{
		if (!lastState.isEmpty())
		{
			detectEvents(currentState);
		}
		lastState.clear();
		lastState.putAll(currentState);
	}

	private void detectEvents(Map<String, Instant> currentState)
	{
		Stream.ofNullable(kinds)
			  .flatMap(Stream::of)
			  .forEach(item -> detectEvents(item, currentState));
	}

	private void detectEvents(Kind<Path> kind, Map<String, Instant> currentState)
	{
		switch (kind)
		{
			case Kind<Path> eventKind when eventKind == StandardWatchEventKinds.ENTRY_CREATE -> detectInserts(currentState);
			case Kind<Path> eventKind when eventKind == StandardWatchEventKinds.ENTRY_MODIFY -> detectUpdates(currentState);
			case Kind<Path> eventKind when eventKind == StandardWatchEventKinds.ENTRY_DELETE -> detectDeletions(currentState);
			default -> throw new UnsupportedOperationException("Event kind not supported: %s".formatted(kind.getClass()
																											.getName()));
		}
	}

	private void detectInserts(Map<String, Instant> currentState)
	{
		currentState.keySet()
					.stream()
					.filter(Predicate.not(lastState::containsKey))
					.map(item -> createWatchEvent(StandardWatchEventKinds.ENTRY_CREATE, item))
					.forEach(events::add);
	}

	private void detectUpdates(Map<String, Instant> currentState)
	{
		lastState.entrySet()
				 .stream()
				 .filter(item -> currentState.containsKey(item.getKey()))
				 .filter(Predicate.not(item -> item.getValue()
												   .equals(currentState.get(item.getKey()))))
				 .map(item -> createWatchEvent(StandardWatchEventKinds.ENTRY_MODIFY, item.getKey()))
				 .forEach(events::add);
	}

	private void detectDeletions(Map<String, Instant> currentState)
	{
		lastState.keySet()
				 .stream()
				 .filter(Predicate.not(currentState::containsKey))
				 .map(item -> createWatchEvent(StandardWatchEventKinds.ENTRY_DELETE, item))
				 .forEach(events::add);
	}

	private WatchEvent<Path> createWatchEvent(Kind<Path> kind, String key)
	{
		var path = new BucketPath(directory.getFileSystem(), key);
		return new DirectoryWatchEvent(kind, 1, path);
	}

	private static record DirectoryWatchEvent(Kind<Path> kind, int count, Path context)
		implements WatchEvent<Path>
	{

	}
}
