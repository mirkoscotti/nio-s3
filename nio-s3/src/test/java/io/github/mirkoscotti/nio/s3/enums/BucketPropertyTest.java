package io.github.mirkoscotti.nio.s3.enums;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * @author mirko.scotti
 * @version Dec 25, 2024
 */
class BucketPropertyTest
{

	private static final String AWS_ENDPOINT = "aws.endpoint";

	private static final String AWS_REGION = "aws.region";

	private static final String AWS_ACCESS_KEY = "aws.access-key";

	private static final String AWS_SECRET_KEY = "aws.secret-key";

	private static final String AWS_CREDENTIALS = "aws.credentials";

	private static final String AWS_ACL = "aws.acl";

	private static final String AWS_FULL_CONTROL = "aws.full-control";

	private static final String AWS_READ = "aws.read";

	private static final String AWS_READ_ACP = "aws.read-acp";

	private static final String AWS_WRITE = "aws.write";

	private static final String AWS_WRITE_ACP = "aws.write-acp";

	@Test
	void toPropertyTest()
	{
		Assertions.assertEquals(AWS_ENDPOINT, BucketProperty.ENDPOINT.toProperty());
		Assertions.assertEquals(AWS_REGION, BucketProperty.REGION.toProperty());
		Assertions.assertEquals(AWS_ACCESS_KEY, BucketProperty.ACCESS_KEY.toProperty());
		Assertions.assertEquals(AWS_SECRET_KEY, BucketProperty.SECRET_KEY.toProperty());
		Assertions.assertEquals(AWS_CREDENTIALS, BucketProperty.CREDENTIALS.toProperty());
		Assertions.assertEquals(AWS_ACL, BucketProperty.ACL.toProperty());
		Assertions.assertEquals(AWS_FULL_CONTROL, BucketProperty.FULL_CONTROL.toProperty());
		Assertions.assertEquals(AWS_READ, BucketProperty.READ.toProperty());
		Assertions.assertEquals(AWS_READ_ACP, BucketProperty.READ_ACP.toProperty());
		Assertions.assertEquals(AWS_WRITE, BucketProperty.WRITE.toProperty());
		Assertions.assertEquals(AWS_WRITE_ACP, BucketProperty.WRITE_ACP.toProperty());
	}

	@Test
	void defaultValueTest()
	{
		Stream.of(BucketProperty.ENDPOINT, BucketProperty.REGION)
			  .map(BucketProperty::defaultValue)
			  .forEach(Assertions::assertNotNull);
		Stream.of(BucketProperty.values())
			  .filter(Predicate.not(List.of(BucketProperty.ENDPOINT,
											BucketProperty.REGION)::contains))
			  .map(BucketProperty::defaultValue)
			  .forEach(Assertions::assertNull);
	}

	@Test
	void ofTest()
	{
		Assertions.assertEquals(Optional.of(BucketProperty.ENDPOINT),
								BucketProperty.of(AWS_ENDPOINT));
		Assertions.assertEquals(Optional.of(BucketProperty.REGION), BucketProperty.of(AWS_REGION));
		Assertions.assertEquals(Optional.of(BucketProperty.ACCESS_KEY),
								BucketProperty.of(AWS_ACCESS_KEY));
		Assertions.assertEquals(Optional.of(BucketProperty.SECRET_KEY),
								BucketProperty.of(AWS_SECRET_KEY));
		Assertions.assertEquals(Optional.of(BucketProperty.CREDENTIALS),
								BucketProperty.of(AWS_CREDENTIALS));
		Assertions.assertEquals(Optional.of(BucketProperty.ACL), BucketProperty.of(AWS_ACL));
		Assertions.assertEquals(Optional.of(BucketProperty.FULL_CONTROL),
								BucketProperty.of(AWS_FULL_CONTROL));
		Assertions.assertEquals(Optional.of(BucketProperty.READ), BucketProperty.of(AWS_READ));
		Assertions.assertEquals(Optional.of(BucketProperty.READ_ACP),
								BucketProperty.of(AWS_READ_ACP));
		Assertions.assertEquals(Optional.of(BucketProperty.WRITE), BucketProperty.of(AWS_WRITE));
		Assertions.assertEquals(Optional.of(BucketProperty.WRITE_ACP),
								BucketProperty.of(AWS_WRITE_ACP));
		Assertions.assertTrue(BucketProperty.of("AAA").isEmpty());
	}
}
