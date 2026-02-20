package it.mirkoscotti.nio.s3.records;

import software.amazon.awssdk.services.s3.S3AsyncClient;

/**
 * @author mirko.scotti
 * @version Jun 24, 2025
 * @deprecated Use single fields as method parameters
 */
@Deprecated(forRemoval = true)
public record OperationRecord(S3AsyncClient client, String bucket, String key)
{

}
