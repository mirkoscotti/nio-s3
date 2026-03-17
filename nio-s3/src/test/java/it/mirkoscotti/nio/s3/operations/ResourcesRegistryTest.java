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
@ExtendWith(MockitoExtension.class)
class ResourcesRegistryTest
{

	@Test
	@SuppressWarnings("unchecked")
	void closeFailedTest(@Mock Closeable closeable)
	{
		var registry = new ResourcesRegistry();
		try
		{
			Mockito.doThrow(IOException.class).when(closeable).close();
			var list = JunitHelper.findFieldValueByType(registry, List.class);
			list.add(new AtomicReference<>(closeable));
			Assertions.assertThrows(IOException.class, registry::close);
		}
		catch (Exception x)
		{
			Assertions.fail(x);
		}
	}
}
