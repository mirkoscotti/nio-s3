package it.mirkoscotti.nio.s3.extensions.jdk.jsr203;

import it.mirkoscotti.nio.s3.enums.BucketProperty;
import it.mirkoscotti.nio.s3.operations.S3Connector;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author mirko.scotti
 * @version Dec 25, 2024
 */
@ExtendWith(MockitoExtension.class)
class BucketFileStoreAttributeViewTest
{

	private static final String BUCKET_NAME = "test-bucket";

	@Mock
	private S3Connector connector;

	@Test
	void nameTest()
	{
		var bucketFileStoreAttributeView = new BucketFileStoreAttributeView(connector, BUCKET_NAME);
		Assertions.assertEquals(BucketFileStoreAttributeView.class.getSimpleName(),
								bucketFileStoreAttributeView.name());
	}

	@Test
	void getGenericPropertyTest(@Mock BucketProperty bucketProperty)
	{
		var bucketFileStoreAttributeView = new BucketFileStoreAttributeView(connector, BUCKET_NAME);
		Assertions.assertNull(bucketFileStoreAttributeView.get(bucketProperty));
		Mockito.verify(connector, Mockito.never()).bucketAcl(Mockito.anyString());
	}

	@Test
	void getAclPropertyTest()
	{
		Mockito.when(connector.bucketAcl(Mockito.anyString())).thenReturn("value");
		var bucketFileStoreAttributeView = new BucketFileStoreAttributeView(connector, BUCKET_NAME);
		Assertions.assertNotNull(bucketFileStoreAttributeView.get(BucketProperty.ACL));
		Mockito.verify(connector, Mockito.atLeastOnce()).bucketAcl(Mockito.anyString());
	}
}
