/*
 * (C) Copyright 2019 - 2025 - Add Value S.R.L - All rights reserved.
 */

package it.mirkoscotti.nio.s3.extensions.testcontainers;

import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * @author Add Value S.R.L by Mirko Scotti
 * @version Dec 09, 2025
 */
@Testcontainers
class OrganizationsIT
{

	@Container
	private static final OrganizationsContainer CONTAINER = new OrganizationsContainer();

	@Test
	void test()
	{
		CONTAINER.checkAccount();
	}
}
