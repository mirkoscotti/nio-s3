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

import it.mirkoscotti.nio.s3.operations.AwsFacade;

import software.amazon.awssdk.awscore.exception.AwsErrorDetails;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.services.iam.model.IamException;
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
	private AwsFacade awsFacade;

	@Mock
	private BasicFileAttributes basicFileAttributes;

	@Test
	void checkNotExistingFileTest()
	{
		Mockito.when(awsFacade.objectMetadata(Mockito.anyString(), Mockito.anyString()))
			   .thenThrow(NoSuchKeyException.class);
		Assertions.assertThrows(NoSuchFileException.class,
								() -> ObjectAccess.check(awsFacade, TEST_BUCKET, TEST_OBJECT));
	}

	@Test
	void checkWithoutAccessModeTest()
	{
		Mockito.when(awsFacade.objectMetadata(Mockito.anyString(), Mockito.anyString()))
			   .thenReturn(basicFileAttributes);
		Assertions.assertDoesNotThrow(() -> ObjectAccess.check(awsFacade,
															   TEST_BUCKET,
															   TEST_OBJECT));
	}

	@Test
	void readDeniedForDirectoryTest(@Mock S3Exception exception, @Mock AwsErrorDetails errorDetails)
	{
		actionDeniedForDirectory(exception, errorDetails, AccessMode.READ);
	}

	@Test
	void unexpectedExceptionWhileCheckingForReadTest(@Mock S3Exception exception,
													 @Mock AwsErrorDetails errorDetails)
	{
		unexpectedExceptionWhileCheckingDirectory(exception, errorDetails, AccessMode.READ);
	}

	@Test
	void readAllowedForDirectoryTest()
	{
		actionAllowedForDirectory(AccessMode.READ);
	}

	@Test
	void readAllowedForFileTest()
	{
		Mockito.when(awsFacade.objectMetadata(Mockito.anyString(), Mockito.anyString()))
			   .thenReturn(basicFileAttributes);
		Mockito.when(basicFileAttributes.fileKey()).thenReturn(TEST_OBJECT);
		Mockito.when(basicFileAttributes.isDirectory()).thenReturn(false);
		Assertions.assertDoesNotThrow(() -> ObjectAccess.check(awsFacade,
															   TEST_BUCKET,
															   TEST_OBJECT,
															   AccessMode.READ));
	}

	@Test
	void writeDeniedForDirectoryTest(@Mock IamException exception,
									 @Mock AwsErrorDetails errorDetails)
	{
		Mockito.when(awsFacade.objectMetadata(Mockito.anyString(), Mockito.anyString()))
			   .thenReturn(basicFileAttributes);
		Mockito.when(awsFacade.directoryPermission(Mockito.anyString(), Mockito.anyString()))
			   .thenThrow(exception);
		Mockito.when(basicFileAttributes.fileKey()).thenReturn(TEST_OBJECT);
		Mockito.when(basicFileAttributes.isDirectory()).thenReturn(true);
		Mockito.when(exception.awsErrorDetails()).thenReturn(errorDetails);
		Mockito.when(errorDetails.errorCode()).thenReturn("AccessDenied");
		Assertions.assertThrows(AccessDeniedException.class,
								() -> ObjectAccess.check(awsFacade,
														 TEST_BUCKET,
														 TEST_OBJECT,
														 AccessMode.WRITE));
	}

	@Test
	void unexpectedExceptionWhileCheckingForWriteTest(@Mock IamException exception,
													  @Mock AwsErrorDetails errorDetails)
	{
		Mockito.when(awsFacade.objectMetadata(Mockito.anyString(), Mockito.anyString()))
			   .thenReturn(basicFileAttributes);
		Mockito.when(awsFacade.directoryPermission(Mockito.anyString(), Mockito.anyString()))
			   .thenThrow(exception);
		Mockito.when(basicFileAttributes.fileKey()).thenReturn(TEST_OBJECT);
		Mockito.when(basicFileAttributes.isDirectory()).thenReturn(true);
		Mockito.when(exception.awsErrorDetails()).thenReturn(errorDetails);
		Assertions.assertThrows(IamException.class,
								() -> ObjectAccess.check(awsFacade,
														 TEST_BUCKET,
														 TEST_OBJECT,
														 AccessMode.WRITE));
	}

	@Test
	void writeDirectoryDeniedWithoutErrorsTest()
	{
		Mockito.when(awsFacade.objectMetadata(Mockito.anyString(), Mockito.anyString()))
			   .thenReturn(basicFileAttributes);
		Mockito.when(awsFacade.directoryPermission(Mockito.anyString(), Mockito.anyString()))
			   .thenReturn("implicitDeny");
		Mockito.when(basicFileAttributes.fileKey()).thenReturn(TEST_OBJECT);
		Mockito.when(basicFileAttributes.isDirectory()).thenReturn(true);
		Assertions.assertThrows(AccessDeniedException.class,
								() -> ObjectAccess.check(awsFacade,
														 TEST_BUCKET,
														 TEST_OBJECT,
														 AccessMode.WRITE));
	}

	@Test
	void writeFileDeniedWithoutErrorsTest()
	{
		Mockito.when(awsFacade.objectMetadata(Mockito.anyString(), Mockito.anyString()))
			   .thenReturn(basicFileAttributes);
		Mockito.when(awsFacade.filePermission(Mockito.anyString(), Mockito.anyString()))
			   .thenReturn("implicitDeny");
		Mockito.when(basicFileAttributes.fileKey()).thenReturn(TEST_OBJECT);
		Mockito.when(basicFileAttributes.isDirectory()).thenReturn(false);
		Assertions.assertThrows(AccessDeniedException.class,
								() -> ObjectAccess.check(awsFacade,
														 TEST_BUCKET,
														 TEST_OBJECT,
														 AccessMode.WRITE));
	}

	@Test
	void writeDirectoryAllowedTest()
	{
		Mockito.when(awsFacade.objectMetadata(Mockito.anyString(), Mockito.anyString()))
			   .thenReturn(basicFileAttributes);
		Mockito.when(awsFacade.directoryPermission(Mockito.anyString(), Mockito.anyString()))
			   .thenReturn("allowed");
		Mockito.when(basicFileAttributes.fileKey()).thenReturn(TEST_OBJECT);
		Mockito.when(basicFileAttributes.isDirectory()).thenReturn(true);
		Assertions.assertDoesNotThrow(() -> ObjectAccess.check(awsFacade,
															   TEST_BUCKET,
															   TEST_OBJECT,
															   AccessMode.WRITE));
	}

	@Test
	void writeFileAllowedTest()
	{
		Mockito.when(awsFacade.objectMetadata(Mockito.anyString(), Mockito.anyString()))
			   .thenReturn(basicFileAttributes);
		Mockito.when(awsFacade.filePermission(Mockito.anyString(), Mockito.anyString()))
			   .thenReturn("allowed");
		Mockito.when(basicFileAttributes.fileKey()).thenReturn(TEST_OBJECT);
		Mockito.when(basicFileAttributes.isDirectory()).thenReturn(false);
		Assertions.assertDoesNotThrow(() -> ObjectAccess.check(awsFacade,
															   TEST_BUCKET,
															   TEST_OBJECT,
															   AccessMode.WRITE));
	}

	@Test
	void executeDeniedForDirectoryTest(@Mock S3Exception exception,
									   @Mock AwsErrorDetails errorDetails)
	{
		actionDeniedForDirectory(exception, errorDetails, AccessMode.EXECUTE);
	}

	@Test
	void unexpectedExceptionWhileCheckingForExecuteTest(@Mock S3Exception exception,
														@Mock AwsErrorDetails errorDetails)
	{
		unexpectedExceptionWhileCheckingDirectory(exception, errorDetails, AccessMode.EXECUTE);
	}

	@Test
	void executeAllowedForDirectoryTest()
	{
		actionAllowedForDirectory(AccessMode.EXECUTE);
	}

	@Test
	void executeDeniedForFileTest()
	{
		Mockito.when(awsFacade.objectMetadata(Mockito.anyString(), Mockito.anyString()))
			   .thenReturn(basicFileAttributes);
		Mockito.when(basicFileAttributes.fileKey()).thenReturn(TEST_OBJECT);
		Mockito.when(basicFileAttributes.isDirectory()).thenReturn(false);
		Assertions.assertThrows(AccessDeniedException.class,
								() -> ObjectAccess.check(awsFacade,
														 TEST_BUCKET,
														 TEST_OBJECT,
														 AccessMode.EXECUTE));
	}

	private void actionDeniedForDirectory(S3Exception exception,
										  AwsErrorDetails errorDetails,
										  AccessMode accessMode)
	{
		Mockito.when(awsFacade.objectMetadata(Mockito.anyString(), Mockito.anyString()))
			   .thenReturn(basicFileAttributes);
		Mockito.when(awsFacade.listObjects(Mockito.anyString(), Mockito.anyString(), Mockito.eq(0)))
			   .thenThrow(exception);
		Mockito.when(basicFileAttributes.fileKey()).thenReturn(TEST_OBJECT);
		Mockito.when(basicFileAttributes.isDirectory()).thenReturn(true);
		Mockito.when(exception.awsErrorDetails()).thenReturn(errorDetails);
		Mockito.when(errorDetails.errorCode()).thenReturn("AccessDenied");
		Assertions.assertThrows(AccessDeniedException.class,
								() -> ObjectAccess.check(awsFacade,
														 TEST_BUCKET,
														 TEST_OBJECT,
														 accessMode));
	}

	private void unexpectedExceptionWhileCheckingDirectory(AwsServiceException exception,
														   AwsErrorDetails errorDetails,
														   AccessMode accessMode)
	{
		Mockito.when(awsFacade.objectMetadata(Mockito.anyString(), Mockito.anyString()))
			   .thenReturn(basicFileAttributes);
		Mockito.when(awsFacade.listObjects(Mockito.anyString(), Mockito.anyString(), Mockito.eq(0)))
			   .thenThrow(exception);
		Mockito.when(basicFileAttributes.fileKey()).thenReturn(TEST_OBJECT);
		Mockito.when(basicFileAttributes.isDirectory()).thenReturn(true);
		Mockito.when(exception.awsErrorDetails()).thenReturn(errorDetails);
		Assertions.assertThrows(S3Exception.class,
								() -> ObjectAccess.check(awsFacade,
														 TEST_BUCKET,
														 TEST_OBJECT,
														 accessMode));
	}

	private void actionAllowedForDirectory(AccessMode accessMode)
	{
		Mockito.when(awsFacade.objectMetadata(Mockito.anyString(), Mockito.anyString()))
			   .thenReturn(basicFileAttributes);
		Mockito.when(basicFileAttributes.fileKey()).thenReturn(TEST_OBJECT);
		Mockito.when(basicFileAttributes.isDirectory()).thenReturn(true);
		Assertions.assertDoesNotThrow(() -> ObjectAccess.check(awsFacade,
															   TEST_BUCKET,
															   TEST_OBJECT,
															   accessMode));
	}
}
