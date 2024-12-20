/*
 * (C) Copyright 2019 - 2024 - Add Value S.R.L - All rights reserved.
 */

package it.mirkoscotti.nio.s3.extensions.jdk.jsr203;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * @author mirko.scotti
 * @version Oct 29, 2024
 */
class S3BasicFileSAttributeViewTest
{

	@Test
	void nameTest()
	{
		var view = new BucketBasicFileAttributeView();
		Assertions.assertEquals("basic", view.name());
	}
}
