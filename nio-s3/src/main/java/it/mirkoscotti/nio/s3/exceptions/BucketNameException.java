package it.mirkoscotti.nio.s3.exceptions;

import java.util.Optional;

import it.mirkoscotti.nio.s3.configuration.UriDescriptor;

/**
 * @author mirko.scotti
 * @version Jan 25, 2025
 */
public class BucketNameException
	extends IllegalArgumentException
{

	private static final long serialVersionUID = -1719565605695094042L;

	private static final String BUCKET_NAME_MESSAGE = """
		Illegal bucket name: %s. Bucket name must match the following rules:
		1. it cannot have two adjacent periods.
		2. it cannot match an IPv4
		3. it cannot start with dash
		4. it cannot start with 'xn--'
		5. it cannot start with 'sthree-'
		6. it cannot end with '-s3alias'
		7. it cannot end with '--ol-s3'
		8. its length must be between 3 and 63 characters
		9. it can contain only lowercase and uppercase letters, digits, periods, and dashes without violating one of the previous rules
		""";

	public BucketNameException(UriDescriptor uriDescriptor)
	{
		super(buildMessage(uriDescriptor));
	}

	private static final String buildMessage(UriDescriptor uriDescriptor)
	{
		var bucketName = Optional.ofNullable(uriDescriptor)
								 .map(UriDescriptor::bucketName)
								 .orElse("null");
		return BUCKET_NAME_MESSAGE.formatted(bucketName);
	}
}
