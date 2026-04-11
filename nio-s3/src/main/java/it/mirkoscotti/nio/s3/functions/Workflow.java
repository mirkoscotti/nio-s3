package it.mirkoscotti.nio.s3.functions;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * @author mirko.scotti
 * @version Apr 05, 2026
 */
public class Workflow<T, U>
{

	private final Function<T, U> function;

	private Consumer<U> consumer;

	private Workflow(Function<T, U> function)
	{
		this.function = Objects.requireNonNull(function, () -> "Missing workflow function.");
	}

	public static <T, U> Workflow<T, U> apply(Function<T, U> function)
	{
		return new Workflow<>(function);
	}

	public Workflow<T, U> thenAccept(Consumer<U> consumer)
	{
		this.consumer = Objects.requireNonNull(consumer);
		return this;
	}

	public Consumer<T> toConsumer()
	{
		return item -> consumer.accept(function.apply(item));
	}
}
