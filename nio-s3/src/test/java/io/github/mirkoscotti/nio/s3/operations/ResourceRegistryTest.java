package io.github.mirkoscotti.nio.s3.operations;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.io.Closeable;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import io.github.mirkoscotti.nio.s3.helpers.JunitHelper;

/**
 * @author mirko.scotti
 * @version Mar 10, 2026
 */
@SuppressWarnings("unchecked")
@ExtendWith(MockitoExtension.class)
class ResourceRegistryTest
{

	@Mock
	private Closeable closeable;

	@Test
	void closeTest() throws Exception
	{
		var registry = new ResourceRegistry();
		var list = JunitHelper.findFieldValueByType(registry, List.class);
		list.add(new AtomicReference<>(closeable));
		registry.close();
		verify(closeable, Mockito.atLeastOnce()).close();
	}

	@Test
	void closeFailedTest(@Mock Closeable otherCloseable) throws Exception
	{
		var registry = new ResourceRegistry();
		doThrow(IOException.class).when(otherCloseable).close();
		var list = JunitHelper.findFieldValueByType(registry, List.class);
		list.add(new AtomicReference<>(otherCloseable));
		list.add(new AtomicReference<>(closeable));
		Assertions.assertThrows(IOException.class, registry::close);
		verify(closeable, never()).close();
	}
}
