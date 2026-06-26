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
 * A <code>WatchService</code> thread-safe implementation that allows one or more "directories" (key
 * prefixes) within an Amazon S3 bucket to be monitored for object creation, modification and
 * deletion, surfacing the changes through the standard JSR-203 watch service API.
 * <p>
 * This service does not depend on Amazon S3's own event notification mechanism (notifications to
 * SNS, SQS, EventBridge or Lambda), which would require provisioning additional AWS resources
 * outside of the S3 API itself. Instead, it emulates a comparable watch behaviour through polling:
 * a single background daemon thread, scheduled at a fixed frequency, periodically lists the objects
 * of every registered path and delegates to each directory's {@link DirectoryWatchKey} the
 * comparison against its previous state in order to detect and queue the corresponding events.
 * <p>
 * Directories are registered through {@link #registerPath(BucketPath, Kind[])} and monitored until
 * their key is cancelled or this service is {@link #close() closed}. Instances are normally
 * obtained through the associated file system provider rather than constructed directly.
 *
 * @author mirko.scotti
 * @version Mar 29, 2025
 * @see DirectoryWatchKey
 * @see AwsFacade
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

	/**
	 * Stops the background polling scheduler, clears the registry of watched directories and
	 * discards any pending events. After this method returns successfully, the service is closed.
	 *
	 * @throws IOException
	 *             if the scheduler does not terminate within the shutdown timeout, or if the
	 *             calling thread is interrupted while waiting for termination
	 */
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

	/**
	 * {@inheritDoc}
	 */
	@Override
	public WatchKey poll()
	{
		ensureIsOpen();
		return events.poll();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public WatchKey poll(long timeout, TimeUnit unit) throws InterruptedException
	{
		ensureIsOpen();
		return events.poll(timeout, unit);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public WatchKey take() throws InterruptedException
	{
		ensureIsOpen();
		return events.take();
	}

	/**
	 * Registers the given directory with this watch service for the specified event kinds and
	 * returns the corresponding {@link WatchKey}.
	 * <p>
	 * If the directory was already registered, the previous key is cancelled and replaced by the
	 * new one. The current state of the directory is captured immediately as the registration
	 * baseline, so that the first subsequent polling cycle only reports changes that occurred after
	 * registration.
	 *
	 * @param directory
	 *            the path representing the S3 key prefix to watch
	 * @param kinds
	 *            the event kinds to watch for
	 * @return the watch key representing the registration
	 * @throws NullPointerException
	 *             if the path is not defined
	 */
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
