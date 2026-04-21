package io.github.mirkoscotti.nio.s3.records;

import java.util.List;

import jakarta.json.bind.annotation.JsonbProperty;

/**
 * @author mirko.scotti
 * @version Jun 15, 2025
 */
public record ObjectParts(@JsonbProperty("TotalPartsCount") int totalPartsCount,
						  @JsonbProperty("PartNumberMarker") int partNumberMarker,
						  @JsonbProperty("NextPartNumberMarker") int nextartNumberMarker,
						  @JsonbProperty("MaxParts") int maxParts,
						  @JsonbProperty("IsTruncated") boolean isTruncated,
						  @JsonbProperty("Parts") List<Part> parts)
{

}
