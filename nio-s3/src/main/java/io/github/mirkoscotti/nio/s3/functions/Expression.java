package io.github.mirkoscotti.nio.s3.functions;

import java.io.IOException;

/**
 * The equivalent of a boolean supplier managing I/O exceptions.
 *
 * @author mirko.scotti
 * @version Feb 13, 2026
 */
@FunctionalInterface
public interface Expression
{

	/**
	 * The trivial expression never satisfied.
	 */
	Expression FALSE = () -> false;

	/**
	 * Evaluates the boolean expression.
	 *
	 * @return the outcome of the expression
	 * @throws IOException
	 *             when an I/O error occurs
	 */
	boolean evaluate() throws IOException;

	/**
	 * Negates the expression
	 *
	 * @param expression
	 *            the expression to be negated
	 * @return true if the expression returns false, false otherwise
	 */
	public static Expression not(Expression expression)
	{
		return () -> !expression.evaluate();
	}
}
