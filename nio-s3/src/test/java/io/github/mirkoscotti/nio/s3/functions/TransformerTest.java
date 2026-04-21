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
 * @version Mar 17, 2026
 */
@ExtendWith(MockitoExtension.class)
class TransformerTest<I, O>
{

	@Mock
	private I input;

	@Mock
	private O output;

	@Mock
	private Condition<I> condition;

	@Mock
	private Mapper<I, O> mapper;

	@Test
	void thenTest(@Mock Mapper<I, O> orMapper)
	{
		try
		{
			Mockito.when(condition.isSatisfied(Mockito.any())).thenReturn(true);
			Mockito.when(mapper.map(input)).thenReturn(output);
			var result = Transformer.<I, O>of(input)
									.when(condition)
									.then(mapper)
									.orReturn(orMapper);
			Mockito.verify(mapper, Mockito.atLeastOnce()).map(input);
			Mockito.verify(orMapper, Mockito.never()).map(Mockito.any());
			Assertions.assertEquals(output, result);
		}
		catch (IOException x)
		{
			Assertions.fail(x);
		}
	}

	@Test
	void orReturnWhenNullTest(@Mock Mapper<I, O> orMapper)
	{
		try
		{
			Mockito.when(orMapper.map(input)).thenReturn(output);
			var result = Transformer.<I, O>of(input).when(null).then(mapper).orReturn(orMapper);
			Mockito.verify(mapper, Mockito.never()).map(Mockito.any());
			Mockito.verify(orMapper, Mockito.atLeastOnce()).map(Mockito.any());
			Assertions.assertEquals(output, result);
		}
		catch (IOException x)
		{
			Assertions.fail(x);
		}
	}

	@Test
	void orReturnTest(@Mock Mapper<I, O> orMapper, @Mock O fallback)
	{
		try
		{
			Mockito.when(condition.isSatisfied(Mockito.any())).thenReturn(false);
			Mockito.when(orMapper.map(input)).thenReturn(fallback);
			var result = Transformer.<I, O>of(input)
									.when(condition)
									.then(mapper)
									.orReturn(orMapper);
			Mockito.verify(mapper, Mockito.never()).map(input);
			Mockito.verify(orMapper, Mockito.atLeastOnce()).map(Mockito.any());
			Assertions.assertEquals(fallback, result);
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
		var transformer = Transformer.<I, O>of(input).when(condition);
		Assertions.assertThrows(IOException.class, () -> transformer.thenReturn(mapper));
	}

	@Test
	void errorOnMapperTest()
	{
		try
		{
			Mockito.when(condition.isSatisfied(Mockito.any())).thenReturn(true);
			Mockito.when(mapper.map(input)).thenThrow(IOException.class);
			var transformer = Transformer.<I, O>of(input).when(condition);
			Assertions.assertThrows(IOException.class, () -> transformer.thenReturn(mapper));
		}
		catch (IOException x)
		{
			Assertions.fail(x);
		}
	}

	@Test
	void thenReturnTrueTest()
	{
		try
		{
			Mockito.when(condition.isSatisfied(Mockito.any())).thenReturn(true);
			Mockito.when(mapper.map(input)).thenReturn(output);
			var result = Transformer.<I, O>of(input).when(condition).thenReturn(mapper);
			Mockito.verify(mapper, Mockito.atLeastOnce()).map(Mockito.any());
			Assertions.assertEquals(output, result);
		}
		catch (IOException x)
		{
			Assertions.fail(x);
		}
	}

	@Test
	void thenReturnFalseTest()
	{
		try
		{
			Mockito.when(condition.isSatisfied(Mockito.any())).thenReturn(false);
			var result = Transformer.<I, O>of(input).when(condition).thenReturn(mapper);
			Mockito.verify(mapper, Mockito.never()).map(Mockito.any());
			Assertions.assertNull(result);
		}
		catch (IOException x)
		{
			Assertions.fail(x);
		}
	}

	@Test
	void whenNotNullTest()
	{
		try
		{
			Mockito.when(mapper.map(input)).thenReturn(output);
			var result = Transformer.<I, O>of(input).whenNotNull().thenReturn(mapper);
			Assertions.assertEquals(output, result);
		}
		catch (IOException x)
		{
			Assertions.fail(x);
		}
	}

	@Test
	void orThrowTest(@Mock Exception exception)
	{
		try
		{
			Mockito.when(condition.isSatisfied(Mockito.any())).thenReturn(false);
			var transformer = Transformer.<I, O>of(input).when(condition).then(mapper);
			Assertions.assertThrows(Exception.class, () -> transformer.orThrow(() -> exception));
		}
		catch (IOException x)
		{
			Assertions.fail(x);
		}
	}
}
