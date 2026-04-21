package io.github.mirkoscotti.nio.s3.records;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import jakarta.json.bind.annotation.JsonbProperty;

/**
 * @author mirko.scotti
 * @version Jun 15, 2025
 */
public record ObjectAttributes(@JsonbProperty("LastModified") String lastModified,
							   @JsonbProperty("ObjectSize") long objectSize,
							   @JsonbProperty("Checksum") Checksum checksum,
							   @JsonbProperty("ObjectParts") ObjectParts objectParts)
{

	public Instant lastModifiedInstant()
	{
		return DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss z", Locale.ENGLISH)
								.withZone(ZoneOffset.UTC)
								.parse(lastModified, Instant::from);
	}
}
