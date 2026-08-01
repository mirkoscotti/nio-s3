package io.github.mirkoscotti.nio.s3.exceptions;

import java.io.IOException;

/**
 * Semantically equivalent to <code>UnsupportedOperationException</code> for I/O operations.
 *
 * @author mirko.scotti
 * @version Dec 26, 2024
 */
public class UnsupportedIoOperationException
	extends IOException
{

	private static final long serialVersionUID = -6513637523077884337L;

	/**
	 * Creates an instance with the given error message.
	 *
	 * @param message
	 *            the error message
	 */
	public UnsupportedIoOperationException(String message)
	{
		super(message);
	}
}
