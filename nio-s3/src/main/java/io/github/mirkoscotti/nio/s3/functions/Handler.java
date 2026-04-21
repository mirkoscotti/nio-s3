package io.github.mirkoscotti.nio.s3.functions;

import java.io.IOException;

import it.mirkoscotti.nio.s3.helpers.ExceptionHelper;

/**
 * @author mirko.scotti
 * @version Feb 12, 2026
 */
@FunctionalInterface
public interface Handler<T>
{

	static final Handler<?> DO_NOTHING = item -> {};

	void handle(T value) throws IOException;

	public static <T> Handler<T> doNothing()
	{
		@SuppressWarnings("unchecked")
		var result = (Handler<T>) DO_NOTHING;
		return result;
	}

	public static <T> Handler<T> throwing(Exception exception)
	{
		return item -> ExceptionHelper.throwIoException(exception);
	}
}
