package it.mirkoscotti.nio.s3.extensions.jdk.jsr203;

import it.mirkoscotti.nio.s3.operations.S3Connector;

import java.io.IOException;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author mirko.scotti
 * @version Mar 29, 2025
 */
public class DirectoryWatchService
	implements WatchService
{

	private static final AtomicInteger THREAD_COUNTER = new AtomicInteger();

	private static final int FREQUENCY = 5;

	private final Map<BucketPath, DirectoryWatchKey> registry = new ConcurrentHashMap<>();

	private final BlockingQueue<WatchKey> events = new LinkedBlockingQueue<>();

	private final Lock lock = new ReentrantLock();

	private final S3Connector connector;

	private final ScheduledExecutorService scheduler;

	DirectoryWatchService(S3Connector connector)
	{
		this.connector = Objects.requireNonNull(connector, () -> "Missing connector.");
		scheduler = Executors.newSingleThreadScheduledExecutor(this::createDaemon);
		Runnable action = () -> registry.forEach(this::detectEvents);
		scheduler.scheduleAtFixedRate(action, 0, FREQUENCY, TimeUnit.SECONDS);
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
				throw new IOException("Timeout nella chiusura dello scheduler");
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
		return events.poll();
	}

	@Override
	public WatchKey poll(long timeout, TimeUnit unit) throws InterruptedException
	{
		return events.poll(timeout, unit);
	}

	@Override
	public WatchKey take() throws InterruptedException
	{
		return events.take();
	}

	WatchKey registerPath(BucketPath directory)
	{
		Objects.requireNonNull(directory, () -> "Missing path.");
		var result = new DirectoryWatchKey(directory);
		registry.put(directory, result);
		return result;
	}

	private Thread createDaemon(Runnable runnable)
	{
		var threadNumber = THREAD_COUNTER.incrementAndGet();
		var threadName = "s3-watcher-daemon-%d".formatted(threadNumber);
		var result = new Thread(runnable, threadName);
		result.setDaemon(true);
		return result;
	}

	private void detectEvents(BucketPath directory, DirectoryWatchKey watchKey)
	{
		var bucketName = directory.getFileSystem().getFileStores().iterator().next().name();
		var objects = connector.listObjects(bucketName, directory.toString());
		watchKey.updateEvents(objects);
	}
}
