package it.mirkoscotti.nio.s3.operations;

import java.nio.file.attribute.BasicFileAttributes;
import java.time.Instant;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import it.mirkoscotti.nio.s3.configuration.BucketDescriptor;
import it.mirkoscotti.nio.s3.functions.LazyReference;
import it.mirkoscotti.nio.s3.operations.IamConnector.IamConnectorBuilder;
import it.mirkoscotti.nio.s3.operations.S3Connector.S3ConnectorBuilder;
import it.mirkoscotti.nio.s3.operations.StsConnector.StsConnectorBuilder;
import it.mirkoscotti.nio.s3.records.AwsRecord;
import it.mirkoscotti.nio.s3.records.CredentialsRecord;

/**
 * @author mirko.scotti
 * @version Dec 12, 2025
 */
@ExtendWith(MockitoExtension.class)
class AwsFacadeTest
{

	private static final String ARN = "arn";

	private static final String TEST_BUCKET = "test-bucket";

	private static final String TEST_KEY = "test-key";

	private static final String RESULT = "result";

	@Mock
	private AwsRecord awsRecord;

	@Mock
	private CredentialsRecord credentials;

	@Test
	void instantiationTest()
	{
		Assertions.assertThrows(NullPointerException.class, () -> AwsFacade.create(null));
		Assertions.assertDoesNotThrow(() -> AwsFacade.create(awsRecord));
	}

	@Test
	void createBucketTest(@Mock BucketDescriptor bucketDescriptor,
						  @Mock S3Connector connector,
						  @Mock S3ConnectorBuilder builder)
	{
		Mockito.when(awsRecord.credentials()).thenReturn(credentials);
		Mockito.when(credentials.accessKey()).thenReturn(TEST_KEY);
		Mockito.when(credentials.secretKey()).thenReturn(TEST_KEY);
		Mockito.when(builder.withCredentials(Mockito.anyString(), Mockito.anyString()))
			   .thenReturn(builder);
		Mockito.when(builder.build()).thenReturn(connector);
		try (var referenceMock = Mockito.mockStatic(LazyReference.class);
			 var s3Mock = Mockito.mockStatic(S3Connector.class))
		{
			referenceMock.when(() -> LazyReference.of(Mockito.any())).thenCallRealMethod();
			s3Mock.when(S3Connector::create).thenReturn(builder);
			AwsFacade.create(awsRecord).createBucket(bucketDescriptor);
			Mockito.verify(connector, Mockito.atLeastOnce()).createBucket(bucketDescriptor);
		}
	}

	@Test
	void bucketAclTest(@Mock LazyReference<S3Connector> s3, @Mock S3Connector connector)
	{
		Mockito.when(s3.get()).thenReturn(connector);
		Mockito.when(connector.bucketAcl(Mockito.anyString())).thenReturn(RESULT);
		try (var mock = Mockito.mockStatic(LazyReference.class))
		{
			mock.when(() -> LazyReference.of(Mockito.any())).thenReturn(s3);
			var awsFacade = AwsFacade.create(awsRecord);
			Assertions.assertEquals(RESULT, awsFacade.bucketAcl(TEST_BUCKET));
		}
	}

	@Test
	void isBucketReadOnlyTest(@Mock LazyReference<S3Connector> s3, @Mock S3Connector connector)
	{
		Mockito.when(s3.get()).thenReturn(connector);
		Mockito.when(connector.isBucketReadOnly(Mockito.anyString())).thenReturn(true);
		try (var mock = Mockito.mockStatic(LazyReference.class))
		{
			mock.when(() -> LazyReference.of(Mockito.any())).thenReturn(s3);
			var awsFacade = AwsFacade.create(awsRecord);
			Assertions.assertTrue(awsFacade.isBucketReadOnly(TEST_BUCKET));
		}
	}

	@Test
	void filePermissionTest(@Mock IamConnector iamConnector,
							@Mock IamConnectorBuilder iamBuilder,
							@Mock StsConnector stsConnector,
							@Mock StsConnectorBuilder stsBuilder)
	{
		Mockito.when(awsRecord.credentials()).thenReturn(credentials);
		Mockito.when(awsRecord.endpoint()).thenReturn(Optional.of("scheme://host"));
		Mockito.when(credentials.accessKey()).thenReturn(TEST_KEY);
		Mockito.when(credentials.secretKey()).thenReturn(TEST_KEY);
		Mockito.when(iamBuilder.withCredentials(Mockito.anyString(), Mockito.anyString()))
			   .thenReturn(iamBuilder);
		Mockito.when(iamBuilder.withCredentials(Mockito.anyString(), Mockito.anyString()))
			   .thenReturn(iamBuilder);
		Mockito.when(iamBuilder.build()).thenReturn(iamConnector);
		Mockito.when(stsBuilder.withCredentials(Mockito.anyString(), Mockito.anyString()))
			   .thenReturn(stsBuilder);
		Mockito.when(stsBuilder.build()).thenReturn(stsConnector);
		Mockito.when(iamConnector.filePermission(Mockito.anyString(), Mockito.anyString(),
												 Mockito.anyString()))
			   .thenReturn(RESULT);
		Mockito.when(stsConnector.arn()).thenReturn(ARN);
		try (var referenceMock = Mockito.mockStatic(LazyReference.class);
			 var iamMock = Mockito.mockStatic(IamConnector.class);
			 var stsMock = Mockito.mockStatic(StsConnector.class))
		{
			referenceMock.when(() -> LazyReference.of(Mockito.any())).thenCallRealMethod();
			iamMock.when(IamConnector::create).thenReturn(iamBuilder);
			iamMock.when(StsConnector::create).thenReturn(stsBuilder);
			var awsFacade = AwsFacade.create(awsRecord);
			Assertions.assertEquals(RESULT, awsFacade.filePermission(TEST_BUCKET, TEST_KEY));
		}
	}

	@Test
	void directoryPermissionTest(@Mock LazyReference<IamConnector> iam,
								 @Mock LazyReference<StsConnector> sts,
								 @Mock IamConnector iamConnector,
								 @Mock StsConnector stsConnector)
	{
		Mockito.when(iam.get()).thenReturn(iamConnector);
		Mockito.when(sts.get()).thenReturn(stsConnector);
		Mockito.when(iamConnector.directoryPermission(Mockito.anyString(), Mockito.anyString(),
													  Mockito.anyString()))
			   .thenReturn(RESULT);
		Mockito.when(stsConnector.arn()).thenReturn(ARN);
		try (var mock = Mockito.mockStatic(LazyReference.class))
		{
			mock.when(() -> LazyReference.of(Mockito.any())).thenReturn(iam).thenReturn(sts);
			var awsFacade = AwsFacade.create(awsRecord);
			Assertions.assertEquals(RESULT, awsFacade.directoryPermission(TEST_BUCKET, TEST_KEY));
		}
	}

	@Test
	void objectMetadataTest(@Mock LazyReference<S3Connector> s3,
							@Mock S3Connector connector,
							@Mock BasicFileAttributes attributes)
	{
		Mockito.when(s3.get()).thenReturn(connector);
		Mockito.when(connector.objectMetadata(Mockito.anyString(), Mockito.anyString()))
			   .thenReturn(attributes);
		try (var mock = Mockito.mockStatic(LazyReference.class))
		{
			mock.when(() -> LazyReference.of(Mockito.any())).thenReturn(s3);
			var awsFacade = AwsFacade.create(awsRecord);
			Assertions.assertEquals(attributes, awsFacade.objectMetadata(TEST_BUCKET, TEST_KEY));
		}
	}

	@Test
	void listObjectsTest(@Mock LazyReference<S3Connector> s3,
						 @Mock S3Connector connector,
						 @Mock Instant instant)
	{
		Mockito.when(s3.get()).thenReturn(connector);
		Mockito.when(connector.listObjects(Mockito.anyString(), Mockito.anyString()))
			   .thenReturn(Map.of(RESULT, instant));
		try (var mock = Mockito.mockStatic(LazyReference.class))
		{
			mock.when(() -> LazyReference.of(Mockito.any())).thenReturn(s3);
			var awsFacade = AwsFacade.create(awsRecord);
			var result = awsFacade.listObjects(TEST_BUCKET, TEST_KEY);
			Assertions.assertEquals(1, result.size());
			Assertions.assertTrue(result.containsKey(RESULT));
			Assertions.assertEquals(instant, result.get(RESULT));
		}
	}

	@Test
	void paginatedListObjectsTest(@Mock LazyReference<S3Connector> s3,
								  @Mock S3Connector connector,
								  @Mock Instant instant)
	{
		Mockito.when(s3.get()).thenReturn(connector);
		Mockito.when(connector.listObjects(Mockito.anyString(), Mockito.anyString(),
										   Mockito.anyInt()))
			   .thenReturn(Map.of(RESULT, instant));
		try (var mock = Mockito.mockStatic(LazyReference.class))
		{
			mock.when(() -> LazyReference.of(Mockito.any())).thenReturn(s3);
			var awsFacade = AwsFacade.create(awsRecord);
			var result = awsFacade.listObjects(TEST_BUCKET, TEST_KEY, 10);
			Assertions.assertEquals(1, result.size());
			Assertions.assertTrue(result.containsKey(RESULT));
			Assertions.assertEquals(instant, result.get(RESULT));
		}
	}

	@Test
	void scanDirectoryTest(@Mock LazyReference<S3Connector> s3,
						   @Mock S3Connector connector,
						   @Mock Iterator<String> iterator)
	{
		Mockito.when(s3.get()).thenReturn(connector);
		Mockito.when(connector.scanDirectory(Mockito.anyString(), Mockito.anyString()))
			   .thenReturn(iterator);
		try (var mock = Mockito.mockStatic(LazyReference.class))
		{
			mock.when(() -> LazyReference.of(Mockito.any())).thenReturn(s3);
			var awsFacade = AwsFacade.create(awsRecord);
			Assertions.assertEquals(iterator, awsFacade.scanDirectory(TEST_BUCKET, TEST_KEY));
		}
	}

	@Test
	void readObjectTest(@Mock LazyReference<S3Connector> s3, @Mock S3Connector connector)
	{
		var result = RESULT.getBytes();
		Mockito.when(s3.get()).thenReturn(connector);
		Mockito.when(connector.readObject(Mockito.anyString(), Mockito.anyString()))
			   .thenReturn(result);
		try (var mock = Mockito.mockStatic(LazyReference.class))
		{
			mock.when(() -> LazyReference.of(Mockito.any())).thenReturn(s3);
			var awsFacade = AwsFacade.create(awsRecord);
			Assertions.assertEquals(result, awsFacade.readObject(TEST_BUCKET, TEST_KEY));
		}
	}

	@Test
	void readObjectFragmentTest(@Mock LazyReference<S3Connector> s3, @Mock S3Connector connector)
	{
		var result = RESULT.getBytes();
		Mockito.when(s3.get()).thenReturn(connector);
		Mockito.when(connector.readObject(Mockito.anyString(), Mockito.anyString(),
										  Mockito.anyLong(), Mockito.anyLong()))
			   .thenReturn(result);
		try (var mock = Mockito.mockStatic(LazyReference.class))
		{
			mock.when(() -> LazyReference.of(Mockito.any())).thenReturn(s3);
			var awsFacade = AwsFacade.create(awsRecord);
			Assertions.assertEquals(result, awsFacade.readObject(TEST_BUCKET, TEST_KEY, 10, 100));
		}
	}

	@Test
	void writeObjectTest(@Mock LazyReference<S3Connector> s3, @Mock S3Connector connector)
	{
		Mockito.when(s3.get()).thenReturn(connector);
		try (var mock = Mockito.mockStatic(LazyReference.class))
		{
			mock.when(() -> LazyReference.of(Mockito.any())).thenReturn(s3);
			AwsFacade.create(awsRecord).writeObject(TEST_BUCKET, TEST_KEY, RESULT.getBytes());
			Mockito.verify(connector, Mockito.atLeastOnce())
				   .writeObject(TEST_BUCKET, TEST_KEY, RESULT.getBytes());
		}
	}

	@Test
	void startMultipartUploadTest(@Mock LazyReference<S3Connector> s3, @Mock S3Connector connector)
	{
		Mockito.when(s3.get()).thenReturn(connector);
		try (var mock = Mockito.mockStatic(LazyReference.class))
		{
			mock.when(() -> LazyReference.of(Mockito.any())).thenReturn(s3);
			AwsFacade.create(awsRecord).startMultipartUpload(TEST_BUCKET, TEST_KEY);
			Mockito.verify(connector, Mockito.atLeastOnce())
				   .startMultipartUpload(TEST_BUCKET, TEST_KEY);
		}
	}
}
