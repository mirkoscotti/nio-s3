package io.github.mirkoscotti.nio.s3.functions;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

/**
 * The if/else pattern with functional programming.
 *
 * @author mirko.scotti
 * @version Feb 13, 2026
 */
public class Evaluator
{

	private final Map<Expression, Action> blocks = new LinkedHashMap<>();

	private Evaluator()
	{
		// Nothing to do
	}

	/**
	 * Starts a conditional evaluation with the given expression.
	 *
	 * @param expression
	 *            the expression to be evaluated
	 * @return the <code>when</code> instance of the evaluation
	 */
	public static When when(Expression expression)
	{
		return new When(expression);
	}

	/**
	 * Registers an additional condition to be evaluated if no previous condition was satisfied.
	 *
	 * @param expression
	 *            the expression to be evaluated
	 * @return the <code>when</code> instance of the evaluation
	 */
	public When elseWhen(Expression expression)
	{
		return new When(this, expression);
	}

	/**
	 * Performs the if/else pattern processing the first matching <code>when</code> expression,
	 * falling back to the given action if none is satisfied.
	 *
	 * @param action
	 *            the fall back action
	 * @throws IOException
	 *             when an I/O error occurs
	 */
	public void elseExecute(Action action) throws IOException
	{
		var reference = new AtomicReference<IOException>();
		evaluate(reference).ifPresentOrElse(item -> execute(item, reference),
											() -> tryExecute(action, reference));
		var exception = reference.get();
		if (exception != null)
		{
			throw exception;
		}
	}

	private Evaluator addBlock(Expression when, Action then)
	{
		blocks.put(when, then);
		return this;
	}

	private void run() throws IOException
	{
		elseExecute(Action.DO_NOTHING);
	}

	private Optional<Action> evaluate(AtomicReference<IOException> reference)
	{
		return blocks.entrySet()
					 .stream()
					 .filter(item -> matches(item.getKey(), reference))
					 .filter(item -> Objects.isNull(reference.get()))
					 .findFirst()
					 .map(Entry::getValue);
	}

	private boolean matches(Expression expression, AtomicReference<IOException> reference)
	{
		boolean result = false;
		try
		{
			result = expression.evaluate();
		}
		catch (IOException x)
		{
			reference.set(x);
		}
		return result;
	}

	private void tryExecute(Action action, AtomicReference<IOException> reference)
	{
		Optional.of(reference)
				.filter(item -> Objects.isNull(item.get()))
				.ifPresent(item -> execute(action, reference));
	}

	private void execute(Action action, AtomicReference<IOException> exception)
	{
		try
		{
			action.execute();
		}
		catch (IOException x)
		{
			exception.set(x);
		}
	}

	/**
	 * A <code>when</code> expression representation.
	 *
	 * @author mirko.scotti
	 * @version Jun 26, 2026
	 */
	public static class When
	{

		private final Evaluator evaluator;

		private final Expression expression;

		private When(Expression expression)
		{
			this(new Evaluator(), expression);
		}

		private When(Evaluator evaluator, Expression expression)
		{
			this.evaluator = evaluator;
			this.expression = Optional.ofNullable(expression).orElse(Expression.FALSE);
		}

		/**
		 * The action to perform when the internal expression is satisfied.
		 *
		 * @param action
		 *            the action to be executed
		 * @return the evaluator managing this <code>when</code> expression
		 */
		public Evaluator then(Action action)
		{
			return evaluator.addBlock(expression, action);
		}

		/**
		 * Creates an evaluator and performs the action directly if the <code>when</code> expression
		 * is satisfied.
		 *
		 * @param action
		 *            the action to be executed
		 * @throws IOException
		 *             when an I/O error occurs
		 */
		public void thenExecute(Action action) throws IOException
		{
			then(action).run();
		}

		/**
		 * Creates an evaluator throwing an exception if the <code>when</code> expression is
		 * satisfied and directly performs it.
		 *
		 * @param supplier
		 *            the supplier of the exception to be thrown
		 * @throws IOException
		 *             when an I/O error occurs
		 */
		public void thenThrow(Supplier<? extends Exception> supplier) throws IOException
		{
			then(Action.throwing(supplier.get())).run();
		}
	}
}
