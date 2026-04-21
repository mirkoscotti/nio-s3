package io.github.mirkoscotti.nio.s3.functions;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

/**
 * @author mirko.scotti
 * @version Feb 12, 2026
 */
public class Case<T>
{

	private final Map<Condition<T>, Handler<T>> branches = new LinkedHashMap<>();

	private final T value;

	private Case(T value)
	{
		this.value = value;
	}

	public static <T> Case<T> of(T value)
	{
		return new Case<>(value);
	}

	public When<T> when(Condition<T> condition)
	{
		return new When<>(this, condition);
	}

	public When<T> whenNotNull()
	{
		return new When<>(this, Objects::nonNull);
	}

	public void otherwise(Handler<T> handler) throws IOException
	{
		var reference = new AtomicReference<IOException>();
		branches.entrySet()
				.stream()
				.filter(item -> isSatisfied(item.getKey(), reference))
				.filter(item -> Objects.isNull(reference.get()))
				.findFirst()
				.ifPresentOrElse(item -> handle(item.getValue(), reference),
								 () -> tryHandle(handler, reference));
		var exception = reference.get();
		if (exception != null)
		{
			throw exception;
		}
	}

	public void otherwise(Action action) throws IOException
	{
		otherwise(item -> action.execute());
	}

	private void run() throws IOException
	{
		otherwise(Handler.doNothing());
	}

	private Case<T> addBranch(Condition<T> when, Handler<T> then)
	{
		branches.put(when, then);
		return this;
	}

	private boolean isSatisfied(Condition<T> condition, AtomicReference<IOException> reference)
	{
		boolean result = false;
		try
		{
			result = condition.isSatisfied(value);
		}
		catch (IOException x)
		{
			reference.set(x);
		}
		return result;
	}

	private void tryHandle(Handler<T> handler, AtomicReference<IOException> reference)
	{
		Optional.of(reference)
				.filter(item -> Objects.isNull(item.get()))
				.ifPresent(item -> handle(handler, reference));
	}

	private void handle(Handler<T> handler, AtomicReference<IOException> exception)
	{
		try
		{
			handler.handle(value);
		}
		catch (IOException x)
		{
			exception.set(x);
		}
	}

	public static class When<T>
	{

		private final Case<T> evaluator;

		private final Condition<T> condition;

		private When(Case<T> evaluator, Condition<T> condition)
		{
			this.evaluator = evaluator;
			this.condition = Optional.ofNullable(condition).orElseGet(Condition::unsatisfied);
		}

		public Case<T> then(Handler<T> handler)
		{
			return evaluator.addBranch(condition, handler);
		}

		public void thenHandle(Handler<T> handler) throws IOException
		{
			then(handler).run();
		}

		public void thenThrow(Supplier<? extends Exception> supplier) throws IOException
		{
			then(Handler.throwing(supplier.get())).run();
		}
	}
}
