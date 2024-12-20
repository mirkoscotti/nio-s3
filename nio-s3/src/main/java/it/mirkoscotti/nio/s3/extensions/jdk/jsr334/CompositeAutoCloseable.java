/*
 * (C) Copyright 2019 - 2024 - Add Value S.R.L - All rights reserved.
 */

package it.mirkoscotti.nio.s3.extensions.jdk.jsr334;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author mirko.scotti
 * @version Oct 30, 2024
 */
public class CompositeAutoCloseable
	implements AutoCloseable
{

	private final Map<String, AutoCloseable> resources = new LinkedHashMap<>();

	@Override
	public void close() throws Exception
	{
		resources.values().forEach(this::close);
	}

	private Void close(AutoCloseable autoCloseable)
	{
		try
		{
			autoCloseable.close();
		}
		catch (Exception x)
		{
			// Nothing to do
		}
		return null;
	}
}
