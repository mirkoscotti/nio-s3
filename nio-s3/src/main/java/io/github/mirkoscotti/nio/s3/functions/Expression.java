package io.github.mirkoscotti.nio.s3.functions;

import java.io.IOException;

/**
 * @author mirko.scotti
 * @version Feb 13, 2026
 */
@FunctionalInterface
public interface Expression
{

	Expression FALSE = () -> false;

	boolean evaluate() throws IOException;

	public static Expression not(Expression expression)
	{
		return () -> !expression.evaluate();
	}
}
