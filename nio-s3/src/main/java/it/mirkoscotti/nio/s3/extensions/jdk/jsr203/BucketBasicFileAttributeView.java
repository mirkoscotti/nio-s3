/*
 * (C) Copyright 2019 - 2024 - Add Value S.R.L - All rights reserved.
 */

package it.mirkoscotti.nio.s3.extensions.jdk.jsr203;

import java.io.IOException;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;

/**
 * @author mirko.scotti
 * @version Oct 29, 2024
 */
public class BucketBasicFileAttributeView
	implements BasicFileAttributeView
{

	private static final String BASIC_FILE_ATTRIBUTE_VIEW = "basic";

	@Override
	public String name()
	{
		return BASIC_FILE_ATTRIBUTE_VIEW;
	}

	@Override
	public BasicFileAttributes readAttributes() throws IOException
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setTimes(FileTime lastModifiedTime, FileTime lastAccessTime, FileTime createTime)
		throws IOException
	{
		// TODO Auto-generated method stub

	}
}
