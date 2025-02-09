/*
 * (C) Copyright 2019 - 2025 - Add Value S.R.L - All rights reserved.
 */

package it.mirkoscotti.nio.s3.exceptions;

import java.net.URI;

/**
 * @author mirko.scotti
 * @version Jan 25, 2025
 */
public class BucketNameException
	extends RuntimeException
{

	private static final long serialVersionUID = -1719565605695094042L;

	public BucketNameException(URI uri)
	{
		super("Invalid bucket name: %s.".formatted(uri == null ? "null" : uri.getAuthority()));
	}
}
