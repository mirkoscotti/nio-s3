package it.mirkoscotti.nio.s3.records;

import java.util.Optional;

/**
 * @author mirko.scotti
 * @version Jan 25, 2025
 */
public record BucketRecord(Optional<String> endpoint, String bucketName)
{

}
