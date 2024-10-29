/*
 * (C) Copyright 2019 - 2024 - Add Value S.R.L - All rights reserved.
 */

package it.mirkoscotti.nio.s3.extensions.jdknio;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * @author mirko.scotti
 * @version Oct 29, 2024
 */
class S3FileStoreAttributeViewTest
{

	@Test
	void nameTest()
	{
		var fileStoreAttributeView = new S3FileStoreAttributeView();
		Assertions.assertEquals(S3FileStoreAttributeView.class.getSimpleName(),
								fileStoreAttributeView.name());
	}
}
