/*
 * (C) Copyright 2019 - 2024 - Add Value S.R.L - All rights reserved.
 */

package it.mirkoscotti.nio.s3.extensions.jdknio;

import it.mirkoscotti.nio.s3.functions.Try;
import it.mirkoscotti.nio.s3.operations.AwsConnector;

import java.nio.file.attribute.FileAttributeView;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author mirko.scotti
 * @version Oct 22, 2024
 */
@ExtendWith(MockitoExtension.class)
class S3FileStoreTest
{

	private static final String BUCKET_NAME = "test-bucket";

	@Mock
	private AwsConnector connector;

	@Test
	void nullTest()
	{
		Assertions.assertThrows(NullPointerException.class, () -> new S3FileStore(null, null));
		Assertions.assertThrows(NullPointerException.class, () -> new S3FileStore(connector, null));
	}

	@Test
	void nameTest()
	{
		var fileStore = new S3FileStore(connector, BUCKET_NAME);
		Assertions.assertEquals(BUCKET_NAME, fileStore.name());
	}

	@Test
	void typeTest()
	{
		var fileStore = new S3FileStore(connector, BUCKET_NAME);
		Assertions.assertEquals("AWS S3 Bucket", fileStore.type());
	}

	@Test
	void isReadOnlyTest()
	{
		Mockito.when(connector.isBucketReadOnly()).thenReturn(true);
		var fileStore = new S3FileStore(connector, BUCKET_NAME);
		Assertions.assertTrue(fileStore.isReadOnly());
	}

	@Test
	void getTotalSpaceTest()
	{
		var fileStore = new S3FileStore(connector, BUCKET_NAME);
		var totalSpace = Try.to(() -> fileStore.getTotalSpace()).onCatch(Assertions::fail).get();
		Assertions.assertEquals(Long.MAX_VALUE, totalSpace);
	}

	@Test
	void getUsableSpaceTest()
	{
		var fileStore = new S3FileStore(connector, BUCKET_NAME);
		var usableSpace = Try.to(() -> fileStore.getUsableSpace()).onCatch(Assertions::fail).get();
		Assertions.assertEquals(Long.MAX_VALUE, usableSpace);
	}

	@Test
	void getUnallocatedSpaceTest()
	{
		var fileStore = new S3FileStore(connector, BUCKET_NAME);
		var unallocatedSpace = Try.to(() -> fileStore.getUnallocatedSpace())
								  .onCatch(Assertions::fail)
								  .get();
		Assertions.assertEquals(Long.MAX_VALUE, unallocatedSpace);
	}

	@Test
	void supportsFileAttributesViewByClassTest()
	{
		var fileStore = new S3FileStore(connector, BUCKET_NAME);
		Assertions.assertTrue(fileStore.supportsFileAttributeView(S3BasicFileAttributeView.class));
		Assertions.assertFalse(fileStore.supportsFileAttributeView(FileAttributeView.class));
	}

	@Test
	void supportsFileAttributesViewByNameTest()
	{
		var fileStore = new S3FileStore(connector, BUCKET_NAME);
		Assertions.assertTrue(fileStore.supportsFileAttributeView("basic"));
		Assertions.assertFalse(fileStore.supportsFileAttributeView("other"));
	}
}
