package it.mirkoscotti.nio.s3.extensions.jdk.jsr203;

import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import io.github.mirkoscotti.nio.s3.exceptions.UnsupportedIoOperationException;

import it.mirkoscotti.nio.s3.helpers.JunitHelper;
import it.mirkoscotti.nio.s3.operations.AwsFacade;

import software.amazon.awssdk.services.s3.model.S3Exception;

/**
 * @author mirko.scotti
 * @version Oct 29, 2024
 */
@ExtendWith(MockitoExtension.class)
class ObjectBasicFileSAttributeViewTest
{

	private static final String BUCKET_NAME = "bucketName";

	private static final String KEY = "key";

	@Mock
	private AwsFacade awsFacade;

	@Test
	void nullTest()
	{
		Assertions.assertThrows(NullPointerException.class,
								() -> new ObjectBasicFileAttributeView(null, null, null));
		Assertions.assertThrows(NullPointerException.class,
								() -> new ObjectBasicFileAttributeView(awsFacade, null, null));
		Assertions.assertThrows(NullPointerException.class,
								() -> new ObjectBasicFileAttributeView(awsFacade,
																	   BUCKET_NAME,
																	   null));
	}

	@Test
	void nameTest()
	{
		var view = new ObjectBasicFileAttributeView(awsFacade, BUCKET_NAME, KEY);
		Assertions.assertEquals("basic", view.name());
	}

	@Test
	void s3exceptionWhileReadAttributesTest()
	{
		Mockito.when(awsFacade.objectMetadata(Mockito.anyString(), Mockito.anyString()))
			   .thenThrow(S3Exception.class);
		var view = new ObjectBasicFileAttributeView(awsFacade, BUCKET_NAME, KEY);
		Assertions.assertThrows(S3Exception.class, view::readAttributes);
	}

	@Test
	void readAttributesTest(@Mock BasicFileAttributes basicFileAttributes)
	{
		Mockito.when(awsFacade.objectMetadata(Mockito.anyString(), Mockito.anyString()))
			   .thenReturn(basicFileAttributes);
		var view = new ObjectBasicFileAttributeView(awsFacade, BUCKET_NAME, KEY);
		var result = JunitHelper.tryCall(view::readAttributes);
		Assertions.assertEquals(basicFileAttributes, result);
	}

	@Test
	void setTimesTest(@Mock FileTime fileTime)
	{
		var view = new ObjectBasicFileAttributeView(awsFacade, BUCKET_NAME, KEY);
		Assertions.assertThrows(UnsupportedIoOperationException.class,
								() -> view.setTimes(fileTime, fileTime, fileTime));
	}
}
