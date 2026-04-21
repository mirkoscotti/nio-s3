package io.github.mirkoscotti.nio.s3.records;

import jakarta.json.bind.annotation.JsonbProperty;

/**
 * @author mirko.scotti
 * @version Jun 21, 2025
 */
public record Part(@JsonbProperty("PartNumber") int partNumber,
				   @JsonbProperty("Size") long size,
				   @JsonbProperty("ChecksumSHA256") String checksumSha256)
{

}
