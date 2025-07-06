package it.mirkoscotti.nio.s3.helpers;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;

/**
 * @author mirko.scotti
 * @version Jun 23, 2025
 */
public final class FunctionsHelper
{

	private static final Logger LOGGER = System.getLogger(FunctionsHelper.class.getName());

	private FunctionsHelper()
	{
		super();
	}

	public static Void doNothing()
	{
		return null;
	}

	public static <T> Void doNothing(T object)
	{
		LOGGER.log(Level.TRACE, object);
		return doNothing();
	}
}
