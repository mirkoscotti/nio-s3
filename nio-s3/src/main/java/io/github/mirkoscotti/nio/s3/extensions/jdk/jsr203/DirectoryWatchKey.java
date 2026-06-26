package io.github.mirkoscotti.nio.s3.extensions.jdk.jsr203;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;
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
 * A <code>WatchKey</code> thread-safe implementation representing the registration of a single
 * "directory" (an S3 key prefix) with a {@link DirectoryWatchService}, as part of an emulation of
 * the JSR-203 watch service API on top of an Amazon S3 bucket.
 * <p>
 * Each key keeps an internal snapshot of the watched directory, mapping every object key found
 * under it to its last-modified instant. On every polling cycle driven by the owning
 * {@link DirectoryWatchService}, the new snapshot is compared against the previous one in order to
 * synthesize watch events, depending on which event kinds the key was registered for. Detected
 * events are queued internally until retrieved through {@link #pollEvents()}.
 * <p>
 * Instances of this class are created exclusively by {@link DirectoryWatchService} via
 * {@link DirectoryWatchService#registerPath(BucketPath, Kind[])} and are not meant to be
 * instantiated directly by client code..
 *
 * @author mirko.scotti
 * @version Apr 04, 2025
 * @see DirectoryWatchService
 * @see BucketPath
 */
public class DirectoryWatchKey
	implements WatchKey
{

	private static final Logger LOGGER = System.getLogger(DirectoryWatchKey.class.getName());

	private final Object lock = new Object();

	private final Map<String, Instant> lastState = new ConcurrentHashMap<>();

	private final BlockingQueue<WatchEvent<?>> events = new LinkedBlockingQueue<>();

	private final BucketPath directory;

	private final Kind<?>[] kinds;

	private volatile boolean valid = true;

	@SafeVarargs
	DirectoryWatchKey(BucketPath directory, Kind<?>... kinds)
	{
		this.directory = directory;
		this.kinds = kinds;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean isValid()
	{
		return valid;
	}

	/**
	 * Returns a snapshot of the events accumulated since the last invocation of this method (or
	 * since the key was registered, if this is the first invocation), and atomically clears the
	 * internal queue. Events queued afterwards are not included in the returned list.
	 *
	 * @return the list of events detected since the last poll
	 */
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

	/**
	 * Discards any pending events without invalidating the key, allowing it to continue being
	 * polled by the owning {@link DirectoryWatchService}.
	 *
	 * @return true if the key was valid and has been reset; false if the key had already been
	 *         cancelled
	 */
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

	/**
	 * Marks this key as no longer valid, clears the recorded directory state and discards any
	 * pending events. Once cancelled, a key cannot be reactivated and will no longer be updated by
	 * the owning {@link DirectoryWatchService}.
	 */
	@Override
	public void cancel()
	{
		synchronized (lock)
		{
			LOGGER.log(Level.INFO,
					   () -> "Cancelling monitor of directory %s...".formatted(directory));
			valid = false;
			lastState.clear();
			events.clear();
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public BucketPath watchable()
	{
		return directory;
	}

	/**
	 * Compares the given up-to-date state of the watched directory against the previously recorded
	 * state, enqueues the resulting watch events for the registered {@link #kinds}, and then
	 * replaces the recorded state with the given one.
	 *
	 * @param currentState
	 *            the up-to-date snapshot of the directory, mapping each object key to its
	 *            last-modified instant
	 */
	void updateEvents(Map<String, Instant> currentState)
	{
		detectEvents(currentState);
		updateStates(currentState);
	}

	/**
	 * Replaces the recorded state of the watched directory with the given snapshot, without
	 * generating any event. Typically used to establish the initial baseline right after
	 * registration, before the first polling cycle takes place.
	 *
	 * @param currentState
	 *            the snapshot to store as the new baseline
	 */
	void updateStates(Map<String, Instant> currentState)
	{
		lastState.clear();
		lastState.putAll(currentState);
	}

	private void detectEvents(Map<String, Instant> currentState)
	{
		Stream.ofNullable(kinds)
			  .flatMap(Stream::of)
			  .forEach(item -> detectEvents(item, currentState));
	}

	private void detectEvents(Kind<?> kind, Map<String, Instant> currentState)
	{
		switch (kind)
		{
			case Kind<?> eventKind when eventKind == StandardWatchEventKinds.ENTRY_CREATE -> detectInserts(currentState);
			case Kind<?> eventKind when eventKind == StandardWatchEventKinds.ENTRY_MODIFY -> detectUpdates(currentState);
			case Kind<?> eventKind when eventKind == StandardWatchEventKinds.ENTRY_DELETE -> detectDeletions(currentState);
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
					.filter(events::add)
					.map(WatchEvent::context)
					.forEach(item -> LOGGER.log(Level.INFO,
												() -> "MONITOR - Insert event -> %s".formatted(item)));
	}

	private void detectUpdates(Map<String, Instant> currentState)
	{
		lastState.entrySet()
				 .stream()
				 .filter(item -> currentState.containsKey(item.getKey()))
				 .filter(Predicate.not(item -> item.getValue()
												   .equals(currentState.get(item.getKey()))))
				 .map(item -> createWatchEvent(StandardWatchEventKinds.ENTRY_MODIFY, item.getKey()))
				 .filter(events::add)
				 .map(WatchEvent::context)
				 .forEach(item -> LOGGER.log(Level.INFO,
											 () -> "MONITOR - Update event -> %s".formatted(item)));
	}

	private void detectDeletions(Map<String, Instant> currentState)
	{
		lastState.keySet()
				 .stream()
				 .filter(Predicate.not(currentState::containsKey))
				 .map(item -> createWatchEvent(StandardWatchEventKinds.ENTRY_DELETE, item))
				 .filter(events::add)
				 .map(WatchEvent::context)
				 .forEach(item -> LOGGER.log(Level.INFO,
											 () -> "MONITOR - Delete event -> %s".formatted(item)));
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
