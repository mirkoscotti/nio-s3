package it.mirkoscotti.nio.s3.operations;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import it.mirkoscotti.nio.s3.functions.Case;
import it.mirkoscotti.nio.s3.functions.Condition;
import it.mirkoscotti.nio.s3.functions.Try;
import it.mirkoscotti.nio.s3.helpers.ExceptionsHelper;

/**
 * @author mirko.scotti
 * @version Mar 04, 2026
 */
public class ResourcesRegistry
	implements Closeable
{

	private final List<Closeable> resources = new ArrayList<>();

	private final ReadWriteLock readWriteLock = new ReentrantReadWriteLock();

	@Override
	public void close() throws IOException
	{
		var reference = new AtomicReference<Exception>();
		resources.stream()
				 .filter(item -> reference.get() != null)
				 .forEach(item -> Try.to(() -> unregisterResource(item))
									 .onCatch(reference::set)
									 .run());
		Case.of(reference.get())
			.when(Condition.not(Objects::isNull))
			.thenHandle(ExceptionsHelper::throwIoException);
	}

	public Void registerResource(Closeable resource)
	{
		var lock = readWriteLock.writeLock();
		lock.lock();
		try
		{
			Optional.ofNullable(resource).ifPresent(resources::add);
			return null;
		}
		finally
		{
			lock.unlock();
		}
	}

	public Void unregisterResource(Closeable resource) throws IOException
	{
		var lock = readWriteLock.writeLock();
		lock.lock();
		try
		{
			Case.of(resource).when(resources::remove).thenHandle(Closeable::close);
			return null;
		}
		finally
		{
			lock.unlock();
		}
	}
}
