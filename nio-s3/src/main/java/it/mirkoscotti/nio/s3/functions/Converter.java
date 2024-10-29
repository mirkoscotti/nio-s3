/*
 * (C) Copyright 2019 - 2024 - Add Value S.R.L - All rights reserved.
 */

package it.mirkoscotti.nio.s3.functions;

/**
 * @author mirko.scotti
 * @version Oct 24, 2024
 */
@FunctionalInterface
public interface Converter<I, O>
{

	O apply(I input) throws Exception;
}
