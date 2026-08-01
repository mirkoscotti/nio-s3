package io.github.mirkoscotti.nio.s3.records;

import java.util.Optional;

import software.amazon.awssdk.regions.Region;

/**
 * The generic AWS or Localstack access key.
 *
 * @param endpoint
 *            the LocalStack endpoint, or empty if AWS is targeted
 * @param region
 *            the AWS region, or empty if US_EAST_1 is preferred
 * @param credentials
 *            the access credentials
 * @author mirko.scotti
 * @version Apr 28, 2025
 */
public record AwsRecord(Optional<String> endpoint,
						Optional<Region> region,
						CredentialsRecord credentials)
{

}
