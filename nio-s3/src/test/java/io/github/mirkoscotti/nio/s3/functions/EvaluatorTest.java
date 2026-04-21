package io.github.mirkoscotti.nio.s3.functions;

import java.io.IOException;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import it.mirkoscotti.nio.s3.helpers.JunitHelper;

/**
 * @author mirko.scotti
 * @version Feb 13, 2026
 */
@ExtendWith(MockitoExtension.class)
class EvaluatorTest
{

	@Mock
	private Expression expression;

	@Mock
	private Action action;

	@Test
	void thenTest(@Mock Action elseAction)
	{
		try
		{
			Mockito.when(expression.evaluate()).thenReturn(true);
			Evaluator.when(expression).then(action).elseExecute(elseAction);
			Mockito.verify(action, Mockito.atLeastOnce()).execute();
			Mockito.verify(elseAction, Mockito.never()).execute();
		}
		catch (IOException x)
		{
			Assertions.fail(x);
		}
	}

	@Test
	void elseTest(@Mock Action elseAction)
	{
		try
		{
			Mockito.when(expression.evaluate()).thenReturn(false);
			Evaluator.when(expression).then(action).elseExecute(elseAction);
			Mockito.verify(action, Mockito.never()).execute();
			Mockito.verify(elseAction, Mockito.atLeastOnce()).execute();
		}
		catch (IOException x)
		{
			Assertions.fail(x);
		}
	}

	@Test
	void elseWhenTrueTest(@Mock Expression elseWhen,
						  @Mock Action elseWhenAction,
						  @Mock Action elseAction)
	{
		try
		{
			Mockito.when(expression.evaluate()).thenReturn(false);
			Mockito.when(elseWhen.evaluate()).thenReturn(true);
			Evaluator.when(expression)
					 .then(action)
					 .elseWhen(elseWhen)
					 .then(elseWhenAction)
					 .elseExecute(elseAction);
			Mockito.verify(action, Mockito.never()).execute();
			Mockito.verify(elseWhenAction, Mockito.atLeastOnce()).execute();
			Mockito.verify(elseAction, Mockito.never()).execute();
		}
		catch (IOException x)
		{
			Assertions.fail(x);
		}
	}

	@Test
	void elseWhenFalseTest(@Mock Expression elseWhen,
						   @Mock Action elseWhenAction,
						   @Mock Action elseAction)
	{
		try
		{
			Mockito.when(expression.evaluate()).thenReturn(false);
			Mockito.when(elseWhen.evaluate()).thenReturn(false);
			Evaluator.when(expression)
					 .then(action)
					 .elseWhen(elseWhen)
					 .then(elseWhenAction)
					 .elseExecute(elseAction);
			Mockito.verify(action, Mockito.never()).execute();
			Mockito.verify(elseWhenAction, Mockito.never()).execute();
			Mockito.verify(elseAction, Mockito.atLeastOnce()).execute();
		}
		catch (IOException x)
		{
			Assertions.fail(x);
		}
	}

	@Test
	void errorOnEvaluatorTest()
	{
		JunitHelper.tryCall(() -> Mockito.when(expression.evaluate()).thenThrow(IOException.class));
		var evaluator = Evaluator.when(expression);
		Assertions.assertThrows(IOException.class, () -> evaluator.thenExecute(action));
	}

	@Test
	void errorOnHandlerTest()
	{
		try
		{
			Mockito.when(expression.evaluate()).thenReturn(true);
			Mockito.doThrow(IOException.class).when(action).execute();
			var evaluator = Evaluator.when(expression);
			Assertions.assertThrows(IOException.class, () -> evaluator.thenExecute(action));
		}
		catch (IOException x)
		{
			Assertions.fail(x);
		}
	}

	@Test
	void thenExecuteTrueTest()
	{
		try
		{
			Mockito.when(expression.evaluate()).thenReturn(true);
			Evaluator.when(expression).thenExecute(action);
			Mockito.verify(action, Mockito.atLeastOnce()).execute();
		}
		catch (IOException x)
		{
			Assertions.fail(x);
		}
	}

	@Test
	void thenExecuteFalseTest()
	{
		try
		{
			Mockito.when(expression.evaluate()).thenReturn(false);
			Evaluator.when(expression).thenExecute(action);
			Mockito.verify(action, Mockito.never()).execute();
		}
		catch (IOException x)
		{
			Assertions.fail(x);
		}
	}

	@Test
	void thenThrowTest(@Mock Exception exception)
	{
		try
		{
			Mockito.when(expression.evaluate()).thenReturn(true);
			var evaluator = Evaluator.when(expression);
			var result = Assertions.assertThrows(IOException.class,
												 () -> evaluator.thenThrow(() -> exception));
			Assertions.assertEquals(exception, result.getCause());
		}
		catch (IOException x)
		{
			Assertions.fail(x);
		}
	}

	@Test
	void elseWhenExecuteTrueTest(@Mock Expression elseWhen, @Mock Action elseIfAction)
	{
		try
		{
			Mockito.when(expression.evaluate()).thenReturn(false);
			Mockito.when(elseWhen.evaluate()).thenReturn(true);
			Evaluator.when(expression).then(action).elseWhen(elseWhen).thenExecute(elseIfAction);
			Mockito.verify(action, Mockito.never()).execute();
			Mockito.verify(elseIfAction, Mockito.atLeastOnce()).execute();
		}
		catch (IOException x)
		{
			Assertions.fail(x);
		}
	}

	@Test
	void elseWhenExecuteFalseTest(@Mock Expression elseWhen, @Mock Action elseWhenAction)
	{
		try
		{
			Mockito.when(expression.evaluate()).thenReturn(false);
			Mockito.when(elseWhen.evaluate()).thenReturn(false);
			Evaluator.when(expression).then(action).elseWhen(elseWhen).thenExecute(elseWhenAction);
			Mockito.verify(action, Mockito.never()).execute();
			Mockito.verify(elseWhenAction, Mockito.never()).execute();
		}
		catch (IOException x)
		{
			Assertions.fail(x);
		}
	}
}
