package it.mirkoscotti.nio.s3.enums;

import java.util.Optional;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * @author mirko.scotti
 * @version Dec 25, 2024
 */
class BucketEffectUnitTest
{

	private static final String ALLOW = "Allow";

	private static final String DENY = "Deny";

	@Test
	void toStringTest()
	{
		Assertions.assertEquals(ALLOW, BucketEffect.ALLOW.toString());
		Assertions.assertEquals(DENY, BucketEffect.DENY.toString());
	}

	@Test
	void ofTest()
	{
		Assertions.assertEquals(Optional.of(BucketEffect.ALLOW), BucketEffect.of(ALLOW));
		Assertions.assertEquals(Optional.of(BucketEffect.DENY), BucketEffect.of(DENY));
		Assertions.assertTrue(BucketEffect.of("AAA").isEmpty());
	}
}
