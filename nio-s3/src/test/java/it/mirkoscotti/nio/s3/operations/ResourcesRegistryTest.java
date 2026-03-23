package it.mirkoscotti.nio.s3.operations;

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

import it.mirkoscotti.nio.s3.helpers.JunitHelper;

/**
 * @author mirko.scotti
 * @version Mar 10, 2026
 */
@SuppressWarnings("unchecked")
@ExtendWith(MockitoExtension.class)
class ResourcesRegistryTest
{

	@Mock
	private Closeable closeable;

	@Test
	void closeTest()
	{
		var registry = new ResourcesRegistry();
		try
		{
			var list = JunitHelper.findFieldValueByType(registry, List.class);
			list.add(new AtomicReference<>(closeable));
			registry.close();
			Mockito.verify(closeable, Mockito.atLeastOnce()).close();
		}
		catch (Exception x)
		{
			Assertions.fail(x);
		}
	}

	@Test
	void closeFailedTest(@Mock Closeable otherCloseable)
	{
		var registry = new ResourcesRegistry();
		try
		{
			Mockito.doThrow(IOException.class).when(otherCloseable).close();
			var list = JunitHelper.findFieldValueByType(registry, List.class);
			list.add(new AtomicReference<>(otherCloseable));
			list.add(new AtomicReference<>(closeable));
			Assertions.assertThrows(IOException.class, registry::close);
			Mockito.verify(closeable, Mockito.never()).close();
		}
		catch (Exception x)
		{
			Assertions.fail(x);
		}
	}
}
