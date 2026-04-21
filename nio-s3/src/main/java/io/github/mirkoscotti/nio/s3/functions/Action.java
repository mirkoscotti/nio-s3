package io.github.mirkoscotti.nio.s3.functions;

import java.io.IOException;

import it.mirkoscotti.nio.s3.helpers.ExceptionHelper;

/**
 * @author mirko.scotti
 * @version Feb 13, 2026
 */
@FunctionalInterface
public interface Action
{

	Action DO_NOTHING = () -> {};

	void execute() throws IOException;

	public static Action throwing(Exception exception)
	{
		return () -> ExceptionHelper.throwIoException(exception);
	}
}
