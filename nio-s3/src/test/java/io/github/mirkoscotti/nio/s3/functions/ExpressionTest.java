package io.github.mirkoscotti.nio.s3.functions;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.github.mirkoscotti.nio.s3.helpers.JunitHelper;

/**
 * @author mirko.scotti
 * @version Feb 26, 2026
 */
class ExpressionTest
{

	@Test
	void notTrueTest()
	{
		Expression expression = Expression.not(() -> true);
		Assertions.assertFalse(JunitHelper.tryCall(expression::evaluate));
	}

	@Test
	void notFalseTest()
	{
		Expression expression = Expression.not(() -> false);
		Assertions.assertTrue(JunitHelper.tryCall(expression::evaluate));
	}
}
