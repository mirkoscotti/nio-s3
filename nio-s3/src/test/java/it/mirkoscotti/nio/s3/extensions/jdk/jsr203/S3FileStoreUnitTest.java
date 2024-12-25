/*
 * (C) Copyright 2019 - 2024 - Add Value S.R.L - All rights reserved.
 */

package it.mirkoscotti.nio.s3.extensions.jdk.jsr203;

import it.mirkoscotti.nio.s3.enums.BucketProperty;
import it.mirkoscotti.nio.s3.functions.Try;
import it.mirkoscotti.nio.s3.helpers.JunitHelper;
import it.mirkoscotti.nio.s3.operations.S3Connector;

import java.nio.file.attribute.FileAttributeView;
import java.nio.file.attribute.FileStoreAttributeView;
import java.util.Optional;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedConstruction.Context;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author mirko.scotti
 * @version Oct 22, 2024
 */
@ExtendWith(MockitoExtension.class)
class S3FileStoreUnitTest
{

	private static final String BUCKET_NAME = "test-bucket";

	private static final String VALUE = "value";

	@Mock
	private S3Connector connector;

	@Test
	void nullTest()
	{
		Assertions.assertThrows(NullPointerException.class, () -> new BucketFileStore(null, null));
		Assertions.assertThrows(NullPointerException.class,
								() -> new BucketFileStore(connector, null));
	}

	@Test
	void nameTest()
	{
		var fileStore = new BucketFileStore(connector, BUCKET_NAME);
		Assertions.assertEquals(BUCKET_NAME, fileStore.name());
	}

	@Test
	void typeTest()
	{
		var fileStore = new BucketFileStore(connector, BUCKET_NAME);
		Assertions.assertEquals("AWS S3 Bucket", fileStore.type());
	}

	@Test
	void isReadOnlyTest()
	{
		Mockito.when(connector.isBucketReadOnly(BUCKET_NAME)).thenReturn(true);
		var fileStore = new BucketFileStore(connector, BUCKET_NAME);
		Assertions.assertTrue(fileStore.isReadOnly());
	}

	@Test
	void getTotalSpaceTest()
	{
		var fileStore = new BucketFileStore(connector, BUCKET_NAME);
		var totalSpace = Try.to(fileStore::getTotalSpace).onCatch(Assertions::fail).get();
		Assertions.assertEquals(Long.MAX_VALUE, totalSpace);
	}

	@Test
	void getUsableSpaceTest()
	{
		var fileStore = new BucketFileStore(connector, BUCKET_NAME);
		var usableSpace = Try.to(fileStore::getUsableSpace).onCatch(Assertions::fail).get();
		Assertions.assertEquals(Long.MAX_VALUE, usableSpace);
	}

	@Test
	void getUnallocatedSpaceTest()
	{
		var fileStore = new BucketFileStore(connector, BUCKET_NAME);
		var unallocatedSpace = Try.to(fileStore::getUnallocatedSpace)
								  .onCatch(Assertions::fail)
								  .get();
		Assertions.assertEquals(Long.MAX_VALUE, unallocatedSpace);
	}

	@Test
	void supportsFileAttributesViewByClassTest()
	{
		var fileStore = new BucketFileStore(connector, BUCKET_NAME);
		Assertions.assertTrue(fileStore.supportsFileAttributeView(BucketBasicFileAttributeView.class));
		Assertions.assertFalse(fileStore.supportsFileAttributeView(FileAttributeView.class));
	}

	@Test
	void supportsFileAttributesViewByNameTest()
	{
		var fileStore = new BucketFileStore(connector, BUCKET_NAME);
		Assertions.assertTrue(fileStore.supportsFileAttributeView("basic"));
		Assertions.assertFalse(fileStore.supportsFileAttributeView("other"));
	}

	@Test
	void getFileStoreAttributeViewTest()
	{
		var fileStore = new BucketFileStore(connector, BUCKET_NAME);
		Assertions.assertNull(fileStore.getFileStoreAttributeView(FileStoreAttributeView.class));
		Assertions.assertNotNull(fileStore.getFileStoreAttributeView(BucketFileStoreAttributeView.class));
	}

	@Test
	void getAttributeTest(@Mock BucketProperty bucketProperty)
	{
		try (var propertyMock = Mockito.mockStatic(BucketProperty.class);
			 var viewMock = Mockito.mockConstruction(BucketFileStoreAttributeView.class,
													 this::initializeBucketFileStoreAttributeView))
		{
			propertyMock.when(() -> BucketProperty.of(Mockito.anyString()))
						.thenReturn(Optional.of(bucketProperty));
			var fileStore = new BucketFileStore(connector, BUCKET_NAME);
			Assertions.assertEquals(VALUE,
									JunitHelper.tryCall(() -> fileStore.getAttribute("attribute")));
		}
	}

	private void initializeBucketFileStoreAttributeView(BucketFileStoreAttributeView view,
														Context context)
	{
		Mockito.when(view.get(Mockito.any(BucketProperty.class))).thenReturn(VALUE);
	}
}
