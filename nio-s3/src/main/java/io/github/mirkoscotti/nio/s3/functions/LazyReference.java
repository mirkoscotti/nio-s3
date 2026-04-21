package io.github.mirkoscotti.nio.s3.functions;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

/**
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

	@Override
	public synchronized T get()
	{
		value = Optional.ofNullable(value).orElseGet(initializer);
		return value;
	}

	public static final <T> LazyReference<T> of(Supplier<T> initializer)
	{
		return new LazyReference<>(initializer);
	}

	public boolean isPresent()
	{
		return value != null;
	}
}
