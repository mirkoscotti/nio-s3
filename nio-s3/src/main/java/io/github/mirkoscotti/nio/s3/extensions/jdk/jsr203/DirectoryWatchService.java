package io.github.mirkoscotti.nio.s3.extensions.jdk.jsr203;

import java.io.IOException;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.nio.file.ClosedWatchServiceException;
import java.nio.file.WatchEvent.Kind;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import io.github.mirkoscotti.nio.s3.operations.AwsFacade;

/**
 * @author mirko.scotti
 * @version Mar 29, 2025
 */
public class DirectoryWatchService
	implements WatchService
{

	private static final Logger LOGGER = System.getLogger(DirectoryWatchService.class.getName());

	private static final AtomicInteger THREAD_COUNTER = new AtomicInteger();

	private static final int FREQUENCY = 5;

	private final Map<BucketPath, DirectoryWatchKey> registry = new ConcurrentHashMap<>();

	private final BlockingQueue<WatchKey> events = new LinkedBlockingQueue<>();

	private final Lock lock = new ReentrantLock();

	private final AwsFacade awsFacade;

	private final ScheduledExecutorService scheduler;

	DirectoryWatchService(AwsFacade awsFacade)
	{
		this.awsFacade = Objects.requireNonNull(awsFacade, () -> "Missing connector.");
		scheduler = Executors.newSingleThreadScheduledExecutor(this::createDaemon);
		Runnable action = () -> registry.forEach(this::detectEvents);
		scheduler.scheduleAtFixedRate(action, FREQUENCY, FREQUENCY, TimeUnit.SECONDS);
	}

	@Override
	public void close() throws IOException
	{
		lock.lock();
		try
		{
			scheduler.shutdownNow();
			registry.clear();
			events.clear();
			if (!scheduler.awaitTermination(1, TimeUnit.SECONDS))
			{
				throw new IOException("Timeout during the service shutdown.");
			}
		}
		catch (InterruptedException x)
		{
			Thread.currentThread().interrupt();
			throw new IOException("Monitor interrupted during the service shutdown.", x);
		}
		finally
		{
			lock.unlock();
		}
	}

	@Override
	public WatchKey poll()
	{
		ensureIsOpen();
		return events.poll();
	}

	@Override
	public WatchKey poll(long timeout, TimeUnit unit) throws InterruptedException
	{
		ensureIsOpen();
		return events.poll(timeout, unit);
	}

	@Override
	public WatchKey take() throws InterruptedException
	{
		ensureIsOpen();
		return events.take();
	}

	WatchKey registerPath(BucketPath directory, Kind<?>... kinds)
	{
		Objects.requireNonNull(directory, () -> "Missing path.");
		var result = new DirectoryWatchKey(directory, kinds);
		LOGGER.log(Level.INFO, () -> "SERVICE - Registering directory %s...".formatted(directory));
		registry.compute(directory, (item1, item2) -> computedKey(item2, result));
		var objects = detectEvents(directory);
		result.updateStates(objects);
		return result;
	}

	private DirectoryWatchKey computedKey(DirectoryWatchKey oldWatchKey,
										  DirectoryWatchKey newWatchKey)
	{
		Optional.ofNullable(oldWatchKey).ifPresent(WatchKey::cancel);
		return newWatchKey;
	}

	private Thread createDaemon(Runnable runnable)
	{
		var threadNumber = THREAD_COUNTER.incrementAndGet();
		var threadName = "s3-watcher-daemon-%d".formatted(threadNumber);
		var result = new Thread(runnable, threadName);
		result.setDaemon(true);
		return result;
	}

	private Map<String, Instant> detectEvents(BucketPath directory)
	{
		var bucketName = directory.getFileSystem().getFileStores().iterator().next().name();
		return awsFacade.listObjects(bucketName, directory.toString());
	}

	private void detectEvents(BucketPath directory, DirectoryWatchKey watchKey)
	{
		LOGGER.log(Level.INFO,
				   () -> "SERVICE - Detecting events of directory %s...".formatted(directory));
		var objects = detectEvents(directory);
		watchKey.updateEvents(objects);
	}

	private void ensureIsOpen()
	{
		if (scheduler.isTerminated())
		{
			throw new ClosedWatchServiceException();
		}
	}
}
