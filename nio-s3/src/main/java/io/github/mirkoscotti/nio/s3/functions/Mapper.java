package io.github.mirkoscotti.nio.s3.functions;

import java.io.IOException;

import io.github.mirkoscotti.nio.s3.helpers.ExceptionHelper;

/**
 * The equivalent of a function managing I/O exceptions
 *
 * @param <I>
 *            any type for input
 * @param <O>
 *            any type for output
 * @author mirko.scotti
 * @version Mar 11, 2026
 */
@FunctionalInterface
public interface Mapper<I, O>
{

	/**
	 * Applies the function.
	 *
	 * @param input
	 *            any value
	 * @return the mapping output
	 * @throws IOException
	 *             when an I/O error occurs
	 */
	O map(I input) throws IOException;

	/**
	 * A special mapper forcing a value no matter what is the input
	 *
	 * @param <I>
	 *            the source type
	 * @param <O>
	 *            the target type
	 * @param value
	 *            the output value
	 * @return the mapper instance
	 */
	public static <I, O> Mapper<I, O> force(O value)
	{
		return item -> value;
	}

	/**
	 * A special mapper redirecting an exception to an I/O exception
	 *
	 * @param <I>
	 *            the source type
	 * @param <O>
	 *            the target type
	 * @param exception
	 *            an exception raised before
	 * @return the mapper instance
	 */
	public static <I, O> Mapper<I, O> throwing(Exception exception)
	{
		return item -> ExceptionHelper.throwIoException(exception);
	}
}
