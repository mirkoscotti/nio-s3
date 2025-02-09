/*
 * (C) Copyright 2019 - 2025 - Add Value S.R.L - All rights reserved.
 */

package it.mirkoscotti.nio.s3.exceptions;

import java.net.URI;

/**
 * @author mirko.scotti
 * @version Jan 25, 2025
 */
public class BucketUriException
	extends RuntimeException
{

	private static final long serialVersionUID = 5540617888178858493L;

	public BucketUriException(URI uri)
	{
		super(uri == null ? "Missing URI." : "Malformed URI: %s.".formatted(uri));
	}
}
