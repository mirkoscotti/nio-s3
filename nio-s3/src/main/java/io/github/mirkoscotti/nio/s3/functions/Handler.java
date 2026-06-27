package io.github.mirkoscotti.nio.s3.functions;

import java.io.IOException;

import io.github.mirkoscotti.nio.s3.helpers.ExceptionHelper;

/**
 * The equivalent of a consumer managing I/O exceptions
 *
 * @author mirko.scotti
 * @version Feb 12, 2026
 */
@FunctionalInterface
public interface Handler<T>
{

	static final Handler<?> DO_NOTHING = item ->
	{
	};

	/**
	 * Performs an action with the given value.
	 *
	 * @param value
	 *            an instance of any type
	 * @throws IOException
	 *             when an I/O error occurs
	 */
	void handle(T value) throws IOException;

	/**
	 * A special handler doing nothing
	 *
	 * @param <T>
	 *            any type
	 * @return the handler instance
	 */
	public static <T> Handler<T> doNothing()
	{
		@SuppressWarnings("unchecked")
		var result = (Handler<T>) DO_NOTHING;
		return result;
	}

	/**
	 * A special handler redirecting an exception to an I/O exception
	 *
	 * @param <T>
	 *            any type
	 * @param exception
	 *            an exception raised before
	 * @return the handler instance
	 */
	public static <T> Handler<T> throwing(Exception exception)
	{
		return item -> ExceptionHelper.throwIoException(exception);
	}
}
