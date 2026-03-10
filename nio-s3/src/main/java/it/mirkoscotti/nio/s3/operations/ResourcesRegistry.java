package it.mirkoscotti.nio.s3.operations;

import java.io.Closeable;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;

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

	private final List<AtomicReference<Closeable>> resources = new CopyOnWriteArrayList<>();

	@Override
	public void close() throws IOException
	{
		var reference = new AtomicReference<Exception>();
		resources.stream()
				 .filter(item -> reference.get() == null)
				 .forEach(item -> Try.to(() -> close(item)).onCatch(reference::set).run());
		Case.of(reference.get())
			.when(Condition.not(Objects::isNull))
			.thenHandle(ExceptionsHelper::throwIoException);
	}

	public Void registerResource(Closeable resource)
	{
		Optional.ofNullable(resource).map(AtomicReference::new).ifPresent(resources::add);
		return null;
	}

	public Void unregisterResource(Closeable resource)
	{
		resources.removeIf(item -> item.compareAndSet(resource, null));
		return null;
	}

	private Void close(AtomicReference<Closeable> closeable) throws IOException
	{
		Case.of(closeable.getAndSet(null)).when(Objects::nonNull).thenHandle(Closeable::close);
		return null;
	}

}
