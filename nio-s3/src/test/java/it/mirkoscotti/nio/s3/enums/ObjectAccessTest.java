package it.mirkoscotti.nio.s3.enums;

import java.nio.file.AccessDeniedException;
import java.nio.file.AccessMode;
import java.nio.file.NoSuchFileException;
import java.nio.file.attribute.BasicFileAttributes;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import it.mirkoscotti.nio.s3.operations.S3Connector;

import software.amazon.awssdk.awscore.exception.AwsErrorDetails;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.S3Exception;

/**
 * @author mirko.scotti
 * @version Dec 04, 2025
 */
@ExtendWith(MockitoExtension.class)
class ObjectAccessTest
{

	private static final String TEST_BUCKET = "test-bucket";

	private static final String TEST_OBJECT = "test-object";

	@Mock
	private S3Connector connector;

	@Mock
	private BasicFileAttributes basicFileAttributes;

	@Test
	void checkNotExistingFileTest()
	{
		Mockito.when(connector.objectMetadata(Mockito.anyString(), Mockito.anyString()))
			   .thenThrow(NoSuchKeyException.class);
		Assertions.assertThrows(NoSuchFileException.class,
								() -> ObjectAccess.check(connector, TEST_BUCKET, TEST_OBJECT));
	}

	@Test
	void checkWithoutAccessModeTest()
	{
		Mockito.when(connector.objectMetadata(Mockito.anyString(), Mockito.anyString()))
			   .thenReturn(basicFileAttributes);
		Assertions.assertDoesNotThrow(() -> ObjectAccess.check(connector,
															   TEST_BUCKET,
															   TEST_OBJECT));
	}

	@Test
	void executeDeniedForFileTest()
	{
		Mockito.when(connector.objectMetadata(Mockito.anyString(), Mockito.anyString()))
			   .thenReturn(basicFileAttributes);
		Mockito.when(basicFileAttributes.fileKey()).thenReturn(TEST_OBJECT);
		Mockito.when(basicFileAttributes.isDirectory()).thenReturn(false);
		Assertions.assertThrows(AccessDeniedException.class,
								() -> ObjectAccess.check(connector,
														 TEST_BUCKET,
														 TEST_OBJECT,
														 AccessMode.EXECUTE));
	}

	@Test
	void unexpectedExceptionWhileCheckingForExecuteTest(@Mock S3Exception exception,
														@Mock AwsErrorDetails errorDetails)
	{
		Mockito.when(connector.objectMetadata(Mockito.anyString(), Mockito.anyString()))
			   .thenReturn(basicFileAttributes);
		Mockito.when(connector.listObjects(Mockito.anyString(), Mockito.anyString(), Mockito.eq(0)))
			   .thenThrow(exception);
		Mockito.when(basicFileAttributes.fileKey()).thenReturn(TEST_OBJECT);
		Mockito.when(basicFileAttributes.isDirectory()).thenReturn(true);
		Mockito.when(exception.awsErrorDetails()).thenReturn(errorDetails);
		Assertions.assertThrows(S3Exception.class,
								() -> ObjectAccess.check(connector,
														 TEST_BUCKET,
														 TEST_OBJECT,
														 AccessMode.EXECUTE));
	}

	@Test
	void executeDeniedForDirectoryTest(@Mock S3Exception exception,
									   @Mock AwsErrorDetails errorDetails)
	{
		Mockito.when(connector.objectMetadata(Mockito.anyString(), Mockito.anyString()))
			   .thenReturn(basicFileAttributes);
		Mockito.when(connector.listObjects(Mockito.anyString(), Mockito.anyString(), Mockito.eq(0)))
			   .thenThrow(exception);
		Mockito.when(basicFileAttributes.fileKey()).thenReturn(TEST_OBJECT);
		Mockito.when(basicFileAttributes.isDirectory()).thenReturn(true);
		Mockito.when(exception.awsErrorDetails()).thenReturn(errorDetails);
		Mockito.when(errorDetails.errorCode()).thenReturn("AccessDenied");
		Assertions.assertThrows(AccessDeniedException.class,
								() -> ObjectAccess.check(connector,
														 TEST_BUCKET,
														 TEST_OBJECT,
														 AccessMode.EXECUTE));
	}

	@Test
	void executeAllowedForDirectoryTest()
	{
		Mockito.when(connector.objectMetadata(Mockito.anyString(), Mockito.anyString()))
			   .thenReturn(basicFileAttributes);
		Mockito.when(basicFileAttributes.fileKey()).thenReturn(TEST_OBJECT);
		Mockito.when(basicFileAttributes.isDirectory()).thenReturn(true);
		Assertions.assertDoesNotThrow(() -> ObjectAccess.check(connector,
															   TEST_BUCKET,
															   TEST_OBJECT,
															   AccessMode.EXECUTE));
	}
}
