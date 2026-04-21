package io.github.mirkoscotti.nio.s3.functions;

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
class CaseTest<T>
{

	@Mock
	private T object;

	@Mock
	private Condition<T> condition;

	@Mock
	private Handler<T> handler;

	@Test
	void thenTest(@Mock Handler<T> otherwise)
	{
		try
		{
			Mockito.when(condition.isSatisfied(Mockito.any())).thenReturn(true);
			Case.of(object).when(condition).then(handler).otherwise(otherwise);
			Mockito.verify(handler, Mockito.atLeastOnce()).handle(Mockito.any());
			Mockito.verify(otherwise, Mockito.never()).handle(Mockito.any());
		}
		catch (IOException x)
		{
			Assertions.fail(x);
		}
	}

	@Test
	void otherwiseWhenNullTest(@Mock Handler<T> otherwise)
	{
		try
		{
			Case.of(object).when(null).then(handler).otherwise(otherwise);
			Mockito.verify(handler, Mockito.never()).handle(Mockito.any());
			Mockito.verify(otherwise, Mockito.atLeastOnce()).handle(Mockito.any());
		}
		catch (IOException x)
		{
			Assertions.fail(x);
		}
	}

	@Test
	void otherwiseTest(@Mock Handler<T> otherwise)
	{
		try
		{
			Mockito.when(condition.isSatisfied(Mockito.any())).thenReturn(false);
			Case.of(object).when(condition).then(handler).otherwise(otherwise);
			Mockito.verify(handler, Mockito.never()).handle(Mockito.any());
			Mockito.verify(otherwise, Mockito.atLeastOnce()).handle(Mockito.any());
		}
		catch (IOException x)
		{
			Assertions.fail(x);
		}
	}

	@Test
	void errorOnConditionTest()
	{
		JunitHelper.tryCall(() -> Mockito.when(condition.isSatisfied(Mockito.any()))
										 .thenThrow(IOException.class));
		var caseInstance = Case.of(object).when(condition);
		Assertions.assertThrows(IOException.class, () -> caseInstance.thenHandle(handler));
	}

	@Test
	void errorOnHandlerTest()
	{
		try
		{
			Mockito.when(condition.isSatisfied(Mockito.any())).thenReturn(true);
			Mockito.doThrow(IOException.class).when(handler).handle(Mockito.any());
			var caseInstance = Case.of(object).when(condition);
			Assertions.assertThrows(IOException.class, () -> caseInstance.thenHandle(handler));
		}
		catch (IOException x)
		{
			Assertions.fail(x);
		}
	}

	@Test
	void thenHandleTrueTest(@Mock Handler<T> otherwise)
	{
		try (var mock = Mockito.mockStatic(Handler.class))
		{
			Mockito.when(condition.isSatisfied(Mockito.any())).thenReturn(true);
			mock.when(Handler::doNothing).thenReturn(otherwise);
			Case.of(object).when(condition).thenHandle(handler);
			Mockito.verify(handler, Mockito.atLeastOnce()).handle(Mockito.any());
			Mockito.verify(otherwise, Mockito.never()).handle(Mockito.any());
		}
		catch (IOException x)
		{
			Assertions.fail(x);
		}
	}

	@Test
	void thenHandleFalseTest(@Mock Handler<T> otherwise)
	{
		try (var mock = Mockito.mockStatic(Handler.class))
		{
			Mockito.when(condition.isSatisfied(Mockito.any())).thenReturn(false);
			mock.when(Handler::doNothing).thenReturn(otherwise);
			Case.of(object).when(condition).thenHandle(handler);
			Mockito.verify(handler, Mockito.never()).handle(Mockito.any());
			Mockito.verify(otherwise, Mockito.atLeastOnce()).handle(Mockito.any());
		}
		catch (IOException x)
		{
			Assertions.fail(x);
		}
	}
}
