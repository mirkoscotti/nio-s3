package io.github.mirkoscotti.nio.s3.functions;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * A thread-safe container that lazily initializes a value on first access. The value is computed
 * once, on the first call to {@link #get()}, using the supplier provided at construction time.
 * Subsequent calls return the cached value without invoking the supplier again.
 *
 * @param <T>
 *            the type of the lazily initialized value
 *
 * @author mirko.scotti
 * @version Dec 21, 2025
 */
public class LazyReference<T>
	implements Supplier<T>
{

	private final Supplier<T> initializer;

	private T value;

	private LazyReference(Supplier<T> initializer)
	{
		Objects.requireNonNull(initializer, () -> "Missing reference initializer.");
		this.initializer = initializer;
	}

	/**
	 * The lazily initialized value, computing it on the first call.
	 *
	 * @return the value produced by the initializer, or null if the initializer itself returned
	 *         null
	 */
	@Override
	public synchronized T get()
	{
		value = Optional.ofNullable(value).orElseGet(initializer);
		return value;
	}

	/**
	 * The reference's factory method.
	 *
	 * @param <T>
	 *            any type
	 * @param initializer
	 *            the first value supplier
	 * @return the reference instance
	 */
	public static final <T> LazyReference<T> of(Supplier<T> initializer)
	{
		return new LazyReference<>(initializer);
	}

	/**
	 * Specifies whether the reference has been initialized.
	 *
	 * @return true if it has been initialized, false otherwise
	 */
	public boolean isPresent()
	{
		return value != null;
	}
}
