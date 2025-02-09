/*
 * (C) Copyright 2019 - 2025 - Add Value S.R.L - All rights reserved.
 */

package it.mirkoscotti.nio.s3.exceptions;

/**
 * @author mirko.scotti
 * @version Jan 25, 2025
 */
public class CredentialsException
	extends RuntimeException
{

	private static final long serialVersionUID = 3120329843968078958L;

	public CredentialsException()
	{
		super();
	}

	public CredentialsException(String message, Throwable cause)
	{
		super(message, cause);
	}

	public CredentialsException(String message)
	{
		super(message);
	}

	public CredentialsException(Throwable cause)
	{
		super(cause);
	}
}
