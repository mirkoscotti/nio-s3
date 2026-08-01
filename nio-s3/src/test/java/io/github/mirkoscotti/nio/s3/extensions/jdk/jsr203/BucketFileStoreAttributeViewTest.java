package io.github.mirkoscotti.nio.s3.extensions.jdk.jsr203;

import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import io.github.mirkoscotti.nio.s3.enums.BucketProperty;
import io.github.mirkoscotti.nio.s3.operations.AwsFacade;

/**
 * @author mirko.scotti
 * @version Dec 25, 2024
 */
@ExtendWith(MockitoExtension.class)
class BucketFileStoreAttributeViewTest
{

	private static final String BUCKET_NAME = "test-bucket";

	@Mock
	private AwsFacade awsFacade;

	@Test
	void nameTest()
	{
		var bucketFileStoreAttributeView = new BucketFileStoreAttributeView(awsFacade, BUCKET_NAME);
		Assertions.assertEquals(BucketFileStoreAttributeView.class.getSimpleName(),
								bucketFileStoreAttributeView.name());
	}

	@Test
	void getGenericPropertyTest(@Mock BucketProperty bucketProperty)
	{
		var bucketFileStoreAttributeView = new BucketFileStoreAttributeView(awsFacade, BUCKET_NAME);
		Assertions.assertNull(bucketFileStoreAttributeView.get(bucketProperty));
		verify(awsFacade, never()).bucketAcl(Mockito.anyString());
	}

	@Test
	void getAclPropertyTest()
	{
		when(awsFacade.bucketAcl(Mockito.anyString())).thenReturn("value");
		var bucketFileStoreAttributeView = new BucketFileStoreAttributeView(awsFacade, BUCKET_NAME);
		Assertions.assertNotNull(bucketFileStoreAttributeView.get(BucketProperty.ACL));
		verify(awsFacade, atLeastOnce()).bucketAcl(Mockito.anyString());
	}
}
