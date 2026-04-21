package io.github.mirkoscotti.nio.s3.enums;

import java.lang.reflect.Field;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.platform.commons.support.HierarchyTraversalMode;
import org.junit.platform.commons.support.ReflectionSupport;
import org.junit.platform.commons.util.ReflectionUtils;

import io.github.mirkoscotti.nio.s3.helpers.JunitHelper;

/**
 * @author mirko.scotti
 * @version Dec 25, 2024
 */
class BucketActionTest
{

	private static final String PATTERN = "s3:*";

	@Test
	void tagTest()
	{
		Stream.of(BucketAction.values()).forEach(this::assertTag);
	}

	@Test
	void isReadOnlyTest()
	{
		Stream.of(BucketAction.values()).forEach(this::assertIsReadOnly);
	}

	@Test
	void matchesWithNullTest()
	{
		Stream.of(BucketAction.values())
			  .forEach(item -> Assertions.assertFalse(item.matches(null)));
	}

	@Test
	void matchesWithWildcardTest()
	{
		Stream.of(BucketAction.values())
			  .forEach(item -> Assertions.assertTrue(item.matches(PATTERN)));
	}

	@Test
	void matchesTest()
	{
		Stream.of(BucketAction.values())
			  .forEach(item -> Assertions.assertTrue(item.matches(item.tag())));
	}

	@Test
	void doesNotMatchTest()
	{
		Stream.of(BucketAction.values())
			  .forEach(item -> Assertions.assertFalse(item.matches("doesNotMatch*")));
	}

	@Test
	void readOnlyActionsTest()
	{
		var optional = BucketAction.readOnlyActions()
								   .filter(Predicate.not(BucketAction::isReadOnly))
								   .findAny();
		Assertions.assertTrue(optional.isEmpty());
	}

	@Test
	void writeActionsTest()
	{
		var optional = BucketAction.writeActions().filter(BucketAction::isReadOnly).findAny();
		Assertions.assertTrue(optional.isEmpty());
	}

	@Test
	void matchingActionsTest()
	{
		var optional = BucketAction.matchingActions(PATTERN)
								   .filter(Predicate.not(item -> item.matches(PATTERN)))
								   .findAny();
		Assertions.assertTrue(optional.isEmpty());
	}

	private void assertTag(BucketAction bucketAction)
	{
		var field = expectedField("tag");
		Assertions.assertEquals(JunitHelper.tryCall(() -> field.get(bucketAction)),
								bucketAction.tag());
	}

	private void assertIsReadOnly(BucketAction bucketAction)
	{
		var field = expectedField("isReadOnly");
		Assertions.assertEquals(JunitHelper.tryCall(() -> field.get(bucketAction)),
								bucketAction.isReadOnly());
	}

	private Field expectedField(String fieldName)
	{
		return ReflectionSupport.streamFields(BucketAction.class,
											  item -> item.getName().equals(fieldName),
											  HierarchyTraversalMode.TOP_DOWN)
								.map(ReflectionUtils::makeAccessible)
								.findAny()
								.orElseGet(Assertions::fail);
	}
}
