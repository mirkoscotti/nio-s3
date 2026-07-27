package io.github.mirkoscotti.nio.s3.functions;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.function.Consumer;
import java.util.function.Supplier;

import io.github.mirkoscotti.nio.s3.helpers.ExceptionHelper;

/**
 * The try-catch pattern with functional programming.
 *
 * @param <T>
 *            any type
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

	/**
	 * Starts a new try-catch process evaluating a <code>callable</code> object functionally working
	 * as a try block.
	 *
	 * @param <T>
	 *            any type
	 * @param callable
	 *            the try block
	 * @return the try instance
	 */
	public static <T> Try<T> to(Callable<T> callable)
	{
		return new Try<>(callable);
	}

	/**
	 * Configures the catch block in the current try-catch process.
	 *
	 * @param catchBlock
	 *            the exception supplier
	 * @return the current try instance
	 */
	public Try<T> onCatch(Consumer<? super Exception> catchBlock)
	{
		this.catchBlock = Objects.requireNonNull(catchBlock, () -> "Missing catch block.");
		return this;
	}

	/**
	 * Configures the finally block in the current try-catch process.
	 *
	 * @param finallyBlock
	 *            the finally block in the form of a callable object
	 * @return the current try instance
	 */
	public Try<T> onFinally(Callable<Void> finallyBlock)
	{
		this.finallyBlock = Objects.requireNonNull(finallyBlock, () -> "Missing finally block.");
		return this;
	}

	/**
	 * The result of the try block when no errors occur.
	 */
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

	/**
	 * Performs the try-catch process when no results are expected.
	 */
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
