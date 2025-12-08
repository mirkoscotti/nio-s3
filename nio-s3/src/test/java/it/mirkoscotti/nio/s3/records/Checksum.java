package it.mirkoscotti.nio.s3.records;

import jakarta.json.bind.annotation.JsonbProperty;

/**
 * @author mirko.scotti
 * @version Jun 15, 2025
 */
public record Checksum(@JsonbProperty("ChecksumSHA256") String checksumSha256,
					   @JsonbProperty("ChecksumType") String checksumType)
{

}
