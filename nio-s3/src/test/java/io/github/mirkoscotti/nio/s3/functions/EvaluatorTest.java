package io.github.mirkoscotti.nio.s3.functions;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import io.github.mirkoscotti.nio.s3.helpers.JunitHelper;

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
	void thenTest(@Mock Action elseAction) throws IOException
	{
		when(expression.evaluate()).thenReturn(true);
		Evaluator.when(expression).then(action).elseExecute(elseAction);
		verify(action, Mockito.atLeastOnce()).execute();
		verify(elseAction, Mockito.never()).execute();
	}

	@Test
	void elseTest(@Mock Action elseAction) throws IOException
	{
		when(expression.evaluate()).thenReturn(false);
		Evaluator.when(expression).then(action).elseExecute(elseAction);
		verify(action, never()).execute();
		verify(elseAction, Mockito.atLeastOnce()).execute();
	}

	@Test
	void elseWhenTrueTest(@Mock Expression elseWhen,
						  @Mock Action elseWhenAction,
						  @Mock Action elseAction)
		throws IOException
	{
		when(expression.evaluate()).thenReturn(false);
		when(elseWhen.evaluate()).thenReturn(true);
		Evaluator.when(expression)
				 .then(action)
				 .elseWhen(elseWhen)
				 .then(elseWhenAction)
				 .elseExecute(elseAction);
		verify(action, never()).execute();
		verify(elseWhenAction, Mockito.atLeastOnce()).execute();
		verify(elseAction, never()).execute();
	}

	@Test
	void elseWhenFalseTest(@Mock Expression elseWhen,
						   @Mock Action elseWhenAction,
						   @Mock Action elseAction)
		throws IOException
	{
		when(expression.evaluate()).thenReturn(false);
		when(elseWhen.evaluate()).thenReturn(false);
		Evaluator.when(expression)
				 .then(action)
				 .elseWhen(elseWhen)
				 .then(elseWhenAction)
				 .elseExecute(elseAction);
		verify(action, never()).execute();
		verify(elseWhenAction, never()).execute();
		verify(elseAction, Mockito.atLeastOnce()).execute();
	}

	@Test
	void errorOnEvaluatorTest()
	{
		JunitHelper.tryCall(() -> when(expression.evaluate()).thenThrow(IOException.class));
		var evaluator = Evaluator.when(expression);
		Assertions.assertThrows(IOException.class, () -> evaluator.thenExecute(action));
	}

	@Test
	void errorOnHandlerTest() throws IOException
	{
		when(expression.evaluate()).thenReturn(true);
		doThrow(IOException.class).when(action).execute();
		var evaluator = Evaluator.when(expression);
		Assertions.assertThrows(IOException.class, () -> evaluator.thenExecute(action));
	}

	@Test
	void thenExecuteTrueTest() throws IOException
	{
		when(expression.evaluate()).thenReturn(true);
		Evaluator.when(expression).thenExecute(action);
		verify(action, Mockito.atLeastOnce()).execute();
	}

	@Test
	void thenExecuteFalseTest() throws IOException
	{
		when(expression.evaluate()).thenReturn(false);
		Evaluator.when(expression).thenExecute(action);
		verify(action, never()).execute();
	}

	@Test
	void thenThrowTest(@Mock Exception exception) throws IOException
	{
		when(expression.evaluate()).thenReturn(true);
		var evaluator = Evaluator.when(expression);
		var result = Assertions.assertThrows(IOException.class,
											 () -> evaluator.thenThrow(() -> exception));
		Assertions.assertEquals(exception, result.getCause());
	}

	@Test
	void elseWhenExecuteTrueTest(@Mock Expression elseWhen, @Mock Action elseIfAction)
		throws IOException
	{
		when(expression.evaluate()).thenReturn(false);
		when(elseWhen.evaluate()).thenReturn(true);
		Evaluator.when(expression).then(action).elseWhen(elseWhen).thenExecute(elseIfAction);
		verify(action, Mockito.never()).execute();
		verify(elseIfAction, Mockito.atLeastOnce()).execute();
	}

	@Test
	void elseWhenExecuteFalseTest(@Mock Expression elseWhen, @Mock Action elseWhenAction)
		throws IOException
	{
		when(expression.evaluate()).thenReturn(false);
		when(elseWhen.evaluate()).thenReturn(false);
		Evaluator.when(expression).then(action).elseWhen(elseWhen).thenExecute(elseWhenAction);
		verify(action, never()).execute();
		verify(elseWhenAction, never()).execute();
	}
}
