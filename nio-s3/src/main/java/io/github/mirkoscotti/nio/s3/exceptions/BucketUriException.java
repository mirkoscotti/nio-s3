package io.github.mirkoscotti.nio.s3.exceptions;

import java.net.URI;

/**
 * The exception for bucket URIs that are not compliant to the AWS specification.
 *
 * @author mirko.scotti
 * @version Jan 25, 2025
 */
public class BucketUriException
	extends RuntimeException
{

	private static final long serialVersionUID = 5540617888178858493L;

	/**
	 * Creates an exception based on a specific bucket URI.
	 *
	 * @param uri
	 *            the bucket URI
	 */
	public BucketUriException(URI uri)
	{
		super(uri == null ? "Missing URI." : "Malformed URI: %s.".formatted(uri));
	}
}
