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
class CaseTest<T>
{

	@Mock
	private T object;

	@Mock
	private Condition<T> condition;

	@Mock
	private Handler<T> handler;

	@Test
	void thenTest(@Mock Handler<T> otherwise) throws IOException
	{
		when(condition.isSatisfied(Mockito.any())).thenReturn(true);
		Case.of(object).when(condition).then(handler).otherwise(otherwise);
		verify(handler, Mockito.atLeastOnce()).handle(Mockito.any());
		verify(otherwise, never()).handle(Mockito.any());
	}

	@Test
	void otherwiseWhenNullTest(@Mock Handler<T> otherwise) throws IOException
	{
		Case.of(object).when(null).then(handler).otherwise(otherwise);
		verify(handler, never()).handle(Mockito.any());
		verify(otherwise, Mockito.atLeastOnce()).handle(Mockito.any());
	}

	@Test
	void otherwiseTest(@Mock Handler<T> otherwise) throws IOException
	{
		when(condition.isSatisfied(Mockito.any())).thenReturn(false);
		Case.of(object).when(condition).then(handler).otherwise(otherwise);
		verify(handler, never()).handle(Mockito.any());
		verify(otherwise, Mockito.atLeastOnce()).handle(Mockito.any());
	}

	@Test
	void errorOnConditionTest()
	{
		JunitHelper.tryCall(() -> when(condition.isSatisfied(Mockito.any())).thenThrow(IOException.class));
		var caseInstance = Case.of(object).when(condition);
		Assertions.assertThrows(IOException.class, () -> caseInstance.thenHandle(handler));
	}

	@Test
	void errorOnHandlerTest() throws IOException
	{
		when(condition.isSatisfied(Mockito.any())).thenReturn(true);
		doThrow(IOException.class).when(handler).handle(Mockito.any());
		var caseInstance = Case.of(object).when(condition);
		Assertions.assertThrows(IOException.class, () -> caseInstance.thenHandle(handler));
	}

	@Test
	void thenHandleTrueTest(@Mock Handler<T> otherwise) throws IOException
	{
		try (var mock = Mockito.mockStatic(Handler.class))
		{
			when(condition.isSatisfied(Mockito.any())).thenReturn(true);
			mock.when(Handler::doNothing).thenReturn(otherwise);
			Case.of(object).when(condition).thenHandle(handler);
			verify(handler, Mockito.atLeastOnce()).handle(Mockito.any());
			verify(otherwise, never()).handle(Mockito.any());
		}
	}

	@Test
	void thenHandleFalseTest(@Mock Handler<T> otherwise) throws IOException
	{
		try (var mock = Mockito.mockStatic(Handler.class))
		{
			when(condition.isSatisfied(Mockito.any())).thenReturn(false);
			mock.when(Handler::doNothing).thenReturn(otherwise);
			Case.of(object).when(condition).thenHandle(handler);
			verify(handler, never()).handle(Mockito.any());
			verify(otherwise, Mockito.atLeastOnce()).handle(Mockito.any());
		}
	}
}
