package it.mirkoscotti.nio.s3.functions;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

/**
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

	public static When when(Expression expression)
	{
		return new When(expression);
	}

	public When elseIf(Expression expression)
	{
		return new When(this, expression);
	}

	public void elseExecute(Action action) throws IOException
	{
		var reference = new AtomicReference<IOException>();
		blocks.entrySet()
			  .stream()
			  .filter(item -> matches(item.getKey(), reference))
			  .filter(item -> Objects.isNull(reference.get()))
			  .findFirst()
			  .ifPresentOrElse(item -> execute(item.getValue(), reference),
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

		public Evaluator then(Action action)
		{
			return evaluator.addBlock(expression, action);
		}

		public void thenExecute(Action action) throws IOException
		{
			then(action).run();
		}
	}
}
