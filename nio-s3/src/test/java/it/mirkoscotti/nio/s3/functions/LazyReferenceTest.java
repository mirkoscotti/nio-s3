package it.mirkoscotti.nio.s3.functions;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author mirko.scotti
 * @version Dec 21, 2025
 */
@ExtendWith(MockitoExtension.class)
class LazyReferenceTest
{

	@Test
	void nullTest()
	{
		Assertions.assertThrows(NullPointerException.class, () -> LazyReference.of(null));
	}

	@Test
	<T> void getAndPresentTest(@Mock T object)
	{
		var reference = LazyReference.of(() -> object);
		Assertions.assertFalse(reference.isPresent());
		Assertions.assertEquals(object, reference.get());
		Assertions.assertTrue(reference.isPresent());
	}
}
