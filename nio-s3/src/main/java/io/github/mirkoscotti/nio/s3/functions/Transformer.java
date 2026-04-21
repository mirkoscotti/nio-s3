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
 * @version Mar 12, 2026
 */
public class Transformer<I, O>
{

	private final Map<Condition<I>, Mapper<I, O>> branches = new LinkedHashMap<>();

	private final I value;

	private Transformer(I value)
	{
		this.value = value;
	}

	public static <I, O> Transformer<I, O> of(I value)
	{
		return new Transformer<>(value);
	}

	public When<I, O> whenNotNull()
	{
		return new When<>(this, Objects::nonNull);
	}

	public When<I, O> when(Condition<I> condition)
	{
		return new When<>(this, condition);
	}

	public O orReturn(Mapper<I, O> mapper) throws IOException
	{
		var reference = new AtomicReference<IOException>();
		var result = branches.entrySet()
							 .stream()
							 .filter(item -> isSatisfied(item.getKey(), reference))
							 .filter(item -> Objects.isNull(reference.get()))
							 .findFirst()
							 .map(item -> transform(item.getValue(), reference))
							 .orElseGet(() -> tryTransform(mapper, reference));
		var exception = reference.get();
		if (exception != null)
		{
			throw exception;
		}
		return result;
	}

	public O orThrow(Supplier<? extends Exception> supplier) throws IOException
	{
		return orReturn(Mapper.throwing(supplier.get()));
	}

	private O transform() throws IOException
	{
		return orReturn(Mapper.<I, O>force(null));
	}

	private Transformer<I, O> addBranch(Condition<I> when, Mapper<I, O> then)
	{
		branches.put(when, then);
		return this;
	}

	private boolean isSatisfied(Condition<I> condition, AtomicReference<IOException> reference)
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

	private O tryTransform(Mapper<I, O> mapper, AtomicReference<IOException> reference)
	{
		return Optional.of(reference)
					   .filter(item -> Objects.isNull(item.get()))
					   .map(item -> transform(mapper, reference))
					   .orElse(null);
	}

	private O transform(Mapper<I, O> mapper, AtomicReference<IOException> exception)
	{
		O result = null;
		try
		{
			result = mapper.map(value);
		}
		catch (IOException x)
		{
			exception.set(x);
		}
		return result;
	}

	public static class When<I, O>
	{

		private final Transformer<I, O> transformer;

		private final Condition<I> condition;

		private When(Transformer<I, O> transformer, Condition<I> condition)
		{
			this.transformer = transformer;
			this.condition = Optional.ofNullable(condition).orElseGet(Condition::unsatisfied);
		}

		public Transformer<I, O> then(Mapper<I, O> mapper)
		{
			return transformer.addBranch(condition, mapper);
		}

		public O thenReturn(Mapper<I, O> mapper) throws IOException
		{
			return then(mapper).transform();
		}
	}
}
