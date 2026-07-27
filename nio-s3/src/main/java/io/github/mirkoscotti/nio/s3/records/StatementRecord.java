package io.github.mirkoscotti.nio.s3.records;

import java.util.List;

import jakarta.json.bind.annotation.JsonbProperty;

/**
 * The generic statement descriptor.
 *
 * @param effect
 *            the mapper of <code>Effect</code> tag in the AWS policy's statement JSON format
 * @param principal
 *            the mapper of <code>Principal</code> tag in the AWS policy's statement JSON format
 * @param actions
 *            the mapper of <code>Action</code> tag in the AWS policy's statement JSON format
 * @param resources
 *            the mapper of <code>Resource</code> tag in the AWS policy's statement JSON format
 * @author mirko.scotti
 * @version Oct 25, 2024
 */
public record StatementRecord(@JsonbProperty("Effect") String effect,
							  @JsonbProperty("Principal") String principal,
							  @JsonbProperty("Action") List<String> actions,
							  @JsonbProperty("Resource") List<String> resources)
{

}
