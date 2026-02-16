package it.mirkoscotti.nio.s3.functions;

import java.io.IOException;

import org.junit.jupiter.api.Test;

/**
 * @author mirko.scotti
 * @version Feb 13, 2026
 */
class EvaluatorTest
{

	@Test
	void thenExecuteTest() throws IOException
	{
		var age = 18;
		Evaluator.when(() -> age < 18).then(() -> System.out.println("Minorenne"));
	}

	@Test
	void otherwiseTest() throws IOException
	{
		var age = 18;
		Evaluator.when(null)
				 .then(() -> System.out.println("Minorenne"))
				 .elseIf(() -> age < 60)
				 .then(() -> System.out.println("Maggiorenne"))
				 .elseExecute(() -> System.out.println("Vecchio"));
	}
}
