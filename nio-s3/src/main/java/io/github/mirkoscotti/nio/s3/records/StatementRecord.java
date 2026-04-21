package io.github.mirkoscotti.nio.s3.records;

import java.util.List;

import jakarta.json.bind.annotation.JsonbProperty;

/**
 * @author mirko.scotti
 * @version Oct 25, 2024
 */
public record StatementRecord(@JsonbProperty("Effect") String effect,
							  @JsonbProperty("Principal") String principal,
							  @JsonbProperty("Action") List<String> actions,
							  @JsonbProperty("Resource") List<String> resources)
{

}
