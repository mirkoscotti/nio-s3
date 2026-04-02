package it.mirkoscotti.nio.s3.functions;

import java.io.IOException;

import it.mirkoscotti.nio.s3.helpers.ExceptionHelper;

/**
 * @author mirko.scotti
 * @version Mar 11, 2026
 */
@FunctionalInterface
public interface Mapper<I, O>
{

	O map(I input) throws IOException;

	public static <I, O> Mapper<I, O> force(O value)
	{
		return item -> value;
	}

	public static <I, O> Mapper<I, O> throwing(Exception exception)
	{
		return item -> ExceptionHelper.throwIoException(exception);
	}
}
