package io.github.mirkoscotti.nio.s3.functions;

import java.io.IOException;

import io.github.mirkoscotti.nio.s3.helpers.ExceptionHelper;

/**
 * The equivalent of a runnable managing I/O exceptions.
 *
 * @author mirko.scotti
 * @version Feb 13, 2026
 */
@FunctionalInterface
public interface Action
{

	Action DO_NOTHING = () ->
	{
	};

	/**
	 * Executes an action.
	 *
	 * @throws IOException
	 *             when an I/O error occurs
	 */
	void execute() throws IOException;

	/**
	 * An action forcing an I/O error.
	 *
	 * @param exception
	 *            the exception to be redirected to an I/O exception
	 * @return the action
	 */
	public static Action throwing(Exception exception)
	{
		return () -> ExceptionHelper.throwIoException(exception);
	}
}
