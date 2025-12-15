package it.mirkoscotti.nio.s3.records;

import java.util.Optional;

import software.amazon.awssdk.regions.Region;

/**
 * @author mirko.scotti
 * @version Apr 28, 2025
 */
public record FactoryRecord(Optional<String> endpoint,
							  Optional<Region> region,
							  CredentialsRecord credentials)
{

}
