/*
 * (C) Copyright 2019 - 2024 - Add Value S.R.L - All rights reserved.
 */

package it.mirkoscotti.nio.s3.extensions.jdknio;

import java.nio.file.attribute.FileStoreAttributeView;

/**
 * @author mirko.scotti
 * @version Oct 29, 2024
 */
public class S3FileStoreAttributeView
	implements FileStoreAttributeView
{

	@Override
	public String name()
	{
		return S3FileStoreAttributeView.class.getSimpleName();
	}
}
