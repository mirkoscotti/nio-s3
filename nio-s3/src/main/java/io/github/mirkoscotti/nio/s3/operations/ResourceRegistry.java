package io.github.mirkoscotti.nio.s3.operations;

import java.io.Closeable;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;

import io.github.mirkoscotti.nio.s3.functions.Case;
import io.github.mirkoscotti.nio.s3.functions.Condition;
import io.github.mirkoscotti.nio.s3.functions.Try;
import io.github.mirkoscotti.nio.s3.helpers.ExceptionHelper;

/**
 * Registry of closeable resources that closes all registered resources, in reverse registration
 * order, when the registry itself is closed.
 *
 * @author mirko.scotti
 * @version Mar 04, 2026
 */
public class ResourceRegistry
	implements Closeable
{

	private final List<AtomicReference<Closeable>> resources = new CopyOnWriteArrayList<>();

	/**
	 * Closes all currently registered resources. If closing a resource fails, the remaining
	 * resources are not closed and the exception is propagated.
	 *
	 * @throws IOException
	 *             if closing any resource fails
	 */
	@Override
	public void close() throws IOException
	{
		var reference = new AtomicReference<Exception>();
		resources.stream()
				 .filter(item -> reference.get() == null)
				 .forEach(item -> Try.to(() -> close(item)).onCatch(reference::set).run());
		Case.of(reference.get())
			.when(Condition.not(Objects::isNull))
			.thenHandle(ExceptionHelper::throwIoException);
	}

	/**
	 * Registers a resource to be closed when this registry is closed. This method has no effect if
	 * the given resource is undefined.
	 *
	 * @param resource
	 *            the resource to register
	 * @return always null
	 */
	public Void registerResource(Closeable resource)
	{
		Optional.ofNullable(resource)
				.map(AtomicReference::new)
				.ifPresent(item -> resources.add(0, item));
		return null;
	}

	/**
	 * Removes a previously registered resource so it is no longer closed by this registry.
	 *
	 * @param resource
	 *            the resource to unregister
	 * @return always null
	 */
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
