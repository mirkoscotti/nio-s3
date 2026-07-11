package io.github.mirkoscotti.nio.s3.records;

import java.util.Optional;

import software.amazon.awssdk.regions.Region;

/**
 * The generic AWS or Localstack access key.
 *
 * @author mirko.scotti
 * @version Apr 28, 2025
 */
public record AwsRecord(Optional<String> endpoint,
						Optional<Region> region,
						CredentialsRecord credentials)
{

}
