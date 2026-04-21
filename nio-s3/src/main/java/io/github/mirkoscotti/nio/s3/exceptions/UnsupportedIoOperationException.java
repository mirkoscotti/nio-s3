package io.github.mirkoscotti.nio.s3.exceptions;

import java.io.IOException;

/**
 * @author mirko.scotti
 * @version Dec 26, 2024
 */
public class UnsupportedIoOperationException
	extends IOException
{

	private static final long serialVersionUID = -6513637523077884337L;

	public UnsupportedIoOperationException(String message)
	{
		super(message);
	}
}
