package it.mirkoscotti.nio.s3.functions;

import java.io.IOException;

import org.junit.jupiter.api.Test;

/**
 * @author mirko.scotti
 * @version Feb 13, 2026
 */
class CaseTest
{

	@Test
	void thenHandleTest() throws IOException
	{
		var age = 18;
		Case.of(age).when(item -> item < 18).thenHandle(item -> System.out.println("Minorenne"));
	}

	@Test
	void otherwiseTest() throws IOException
	{
		var age = 18;
		Case.of(age)
			.when(item -> item < 18)
			.then(item -> System.out.println("Minorenne"))
			.otherwise(item -> System.out.println("Maggiorenne"));
	}
}
