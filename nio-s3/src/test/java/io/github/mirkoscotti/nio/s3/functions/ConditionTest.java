package io.github.mirkoscotti.nio.s3.functions;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import it.mirkoscotti.nio.s3.helpers.JunitHelper;

/**
 * @author mirko.scotti
 * @version Feb 26, 2026
 */
@ExtendWith(MockitoExtension.class)
class ConditionTest
{

	@Test
	<T> void unsatisfiedTest(@Mock T object)
	{
		var condition = Condition.<T>unsatisfied();
		Assertions.assertFalse(JunitHelper.tryCall(() -> condition.isSatisfied(object)));
	}
}
