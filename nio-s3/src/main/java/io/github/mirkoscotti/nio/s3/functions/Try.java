package io.github.mirkoscotti.nio.s3.functions;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.function.Consumer;
import java.util.function.Supplier;

import it.mirkoscotti.nio.s3.helpers.ExceptionHelper;

/**
 * @author mirko.scotti
 * @version Oct 24, 2024
 */
public class Try<T>
	implements Supplier<T>, Runnable
{

	private final Callable<T> tryBlock;

	private Consumer<? super Exception> catchBlock = ExceptionHelper::sneakyThrow;

	private Callable<Void> finallyBlock = this::doNothing;

	/**
	 * @param tryBlock
	 */
	private Try(Callable<T> tryBlock)
	{
		Objects.requireNonNull(tryBlock, () -> "Missing try block.");
		this.tryBlock = tryBlock;
	}

	public static <T> Try<T> to(Callable<T> callable)
	{
		return new Try<>(callable);
	}

	public Try<T> onCatch(Consumer<? super Exception> catchBlock)
	{
		this.catchBlock = Objects.requireNonNull(catchBlock, () -> "Missing catch block.");
		return this;
	}

	public Try<T> onFinally(Callable<Void> finallyBlock)
	{
		this.finallyBlock = Objects.requireNonNull(finallyBlock, () -> "Missing finally block.");
		return this;
	}

	@Override
	public T get()
	{
		T result = null;
		Exception exception = null;
		try
		{
			result = tryBlock.call();
		}
		catch (Exception x)
		{
			exception = x;
		}
		finally
		{
			exception = toException(exception);
		}
		Optional.ofNullable(exception).ifPresent(catchBlock::accept);
		return result;
	}

	@Override
	public void run()
	{
		get();
	}

	private Exception toException(Exception exception)
	{
		var result = exception;
		try
		{
			finallyBlock.call();
		}
		catch (Exception x)
		{
			var optional = Optional.ofNullable(exception);
			optional.ifPresent(x::addSuppressed);
			result = x;
		}
		return result;
	}

	private Void doNothing()
	{
		return null;
	}
}
