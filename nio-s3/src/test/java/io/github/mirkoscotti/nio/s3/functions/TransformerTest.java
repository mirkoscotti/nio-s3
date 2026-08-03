package io.github.mirkoscotti.nio.s3.functions;

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
	void thenTest(@Mock Mapper<I, O> orMapper) throws IOException
	{
		when(condition.isSatisfied(Mockito.any())).thenReturn(true);
		when(mapper.map(input)).thenReturn(output);
		var result = Transformer.<I, O>of(input).when(condition).then(mapper).orReturn(orMapper);
		verify(mapper, Mockito.atLeastOnce()).map(input);
		verify(orMapper, never()).map(Mockito.any());
		Assertions.assertEquals(output, result);
	}

	@Test
	void orReturnWhenNullTest(@Mock Mapper<I, O> orMapper) throws IOException
	{
		when(orMapper.map(input)).thenReturn(output);
		var result = Transformer.<I, O>of(input).when(null).then(mapper).orReturn(orMapper);
		verify(mapper, never()).map(Mockito.any());
		verify(orMapper, Mockito.atLeastOnce()).map(Mockito.any());
		Assertions.assertEquals(output, result);
	}

	@Test
	void orReturnTest(@Mock Mapper<I, O> orMapper, @Mock O fallback) throws IOException
	{
		when(condition.isSatisfied(Mockito.any())).thenReturn(false);
		when(orMapper.map(input)).thenReturn(fallback);
		var result = Transformer.<I, O>of(input).when(condition).then(mapper).orReturn(orMapper);
		verify(mapper, never()).map(input);
		verify(orMapper, Mockito.atLeastOnce()).map(Mockito.any());
		Assertions.assertEquals(fallback, result);
	}

	@Test
	void errorOnConditionTest()
	{
		JunitHelper.tryCall(() -> when(condition.isSatisfied(Mockito.any())).thenThrow(IOException.class));
		var transformer = Transformer.<I, O>of(input).when(condition);
		Assertions.assertThrows(IOException.class, () -> transformer.thenReturn(mapper));
	}

	@Test
	void errorOnMapperTest() throws IOException
	{
		when(condition.isSatisfied(Mockito.any())).thenReturn(true);
		when(mapper.map(input)).thenThrow(IOException.class);
		var transformer = Transformer.<I, O>of(input).when(condition);
		Assertions.assertThrows(IOException.class, () -> transformer.thenReturn(mapper));
	}

	@Test
	void thenReturnTrueTest() throws IOException
	{
		when(condition.isSatisfied(Mockito.any())).thenReturn(true);
		when(mapper.map(input)).thenReturn(output);
		var result = Transformer.<I, O>of(input).when(condition).thenReturn(mapper);
		verify(mapper, Mockito.atLeastOnce()).map(Mockito.any());
		Assertions.assertEquals(output, result);
	}

	@Test
	void thenReturnFalseTest() throws IOException
	{
		when(condition.isSatisfied(Mockito.any())).thenReturn(false);
		var result = Transformer.<I, O>of(input).when(condition).thenReturn(mapper);
		verify(mapper, never()).map(Mockito.any());
		Assertions.assertNull(result);
	}

	@Test
	void whenNotNullTest() throws IOException
	{
		when(mapper.map(input)).thenReturn(output);
		var result = Transformer.<I, O>of(input).whenNotNull().thenReturn(mapper);
		Assertions.assertEquals(output, result);
	}

	@Test
	void orThrowTest(@Mock Exception exception) throws IOException
	{
		when(condition.isSatisfied(Mockito.any())).thenReturn(false);
		var transformer = Transformer.<I, O>of(input).when(condition).then(mapper);
		Assertions.assertThrows(Exception.class, () -> transformer.orThrow(() -> exception));
	}
}
