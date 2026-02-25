package it.mirkoscotti.nio.s3.functions;

import java.io.IOException;

import it.mirkoscotti.nio.s3.helpers.ExceptionsHelper;

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
		return () -> ExceptionsHelper.throwIoException(exception);
	}
}
