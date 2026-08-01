package io.github.mirkoscotti.nio.s3.records;

import java.util.Objects;
import java.util.Optional;

/**
 * The generic bucket key.
 *
 * @param endpoint
 *            the LocalStack endpoint, or empty if AWS is targeted
 * @param bucketName
 *            the name of the target bucket
 * @author mirko.scotti
 * @version Jan 25, 2025
 */
public record BucketRecord(Optional<String> endpoint, String bucketName)
{

	/**
	 * Create a DTO containing at least a well defined bucket name.
	 *
	 * @param endpoint
	 *            the optional endpoint in case of access to Localstack
	 * @param bucketName
	 *            the name of the bucket
	 */
	public BucketRecord
	{
		Objects.requireNonNull(bucketName, () -> "Missing bucket name.");
	}
}
