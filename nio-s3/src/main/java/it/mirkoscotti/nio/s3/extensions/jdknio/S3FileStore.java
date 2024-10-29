/*
 * (C) Copyright 2019 - 2024 - Add Value S.R.L - All rights reserved.
 */

package it.mirkoscotti.nio.s3.extensions.jdknio;

import it.mirkoscotti.nio.s3.operations.AwsConnector;

import java.io.IOException;
import java.nio.file.FileStore;
import java.nio.file.attribute.FileAttributeView;
import java.nio.file.attribute.FileStoreAttributeView;
import java.util.Objects;

/**
 * @author mirko.scotti
 * @version Oct 22, 2024
 */
public class S3FileStore
	extends FileStore
{

	private final AwsConnector connector;

	private final String bucketName;

	/**
	 * @param bucketName
	 */
	public S3FileStore(AwsConnector connector, String bucketName)
	{
		this.connector = Objects.requireNonNull(connector, () -> "Missing AWS connector.");
		this.bucketName = Objects.requireNonNull(bucketName, () -> "Missing bucket name.");
	}

	@Override
	public String name()
	{
		return bucketName;
	}

	@Override
	public String type()
	{
		return "AWS S3 Bucket";
	}

	@Override
	public boolean isReadOnly()
	{
		return connector.isBucketReadOnly();
	}

	@Override
	public long getTotalSpace() throws IOException
	{
		return Long.MAX_VALUE;
	}

	@Override
	public long getUsableSpace() throws IOException
	{
		return Long.MAX_VALUE;
	}

	@Override
	public long getUnallocatedSpace() throws IOException
	{
		return Long.MAX_VALUE;
	}

	@Override
	public boolean supportsFileAttributeView(Class<? extends FileAttributeView> type)
	{
		return type == S3BasicFileAttributeView.class;
	}

	@Override
	public boolean supportsFileAttributeView(String name)
	{
		var supportedName = new S3BasicFileAttributeView().name();
		return supportedName.equals(name);
	}

	@Override
	public <V extends FileStoreAttributeView> V getFileStoreAttributeView(Class<V> type)
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object getAttribute(String attribute) throws IOException
	{
		// TODO Auto-generated method stub
		return null;
	}
}
