package io.github.mirkoscotti.nio.s3.operations;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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

import io.github.mirkoscotti.nio.s3.configuration.BucketDescriptor;
import io.github.mirkoscotti.nio.s3.functions.LazyReference;
import io.github.mirkoscotti.nio.s3.operations.IamConnector.IamConnectorBuilder;
import io.github.mirkoscotti.nio.s3.operations.S3Connector.S3ConnectorBuilder;
import io.github.mirkoscotti.nio.s3.operations.StsConnector.StsConnectorBuilder;
import io.github.mirkoscotti.nio.s3.records.AwsRecord;
import io.github.mirkoscotti.nio.s3.records.CredentialsRecord;

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
		when(awsRecord.credentials()).thenReturn(credentials);
		when(credentials.accessKey()).thenReturn(TEST_KEY);
		when(credentials.secretKey()).thenReturn(TEST_KEY);
		when(builder.withCredentials(Mockito.anyString(), Mockito.anyString())).thenReturn(builder);
		when(builder.build()).thenReturn(connector);
		try (var referenceMock = Mockito.mockStatic(LazyReference.class);
			 var s3Mock = Mockito.mockStatic(S3Connector.class))
		{
			referenceMock.when(() -> LazyReference.of(Mockito.any())).thenCallRealMethod();
			s3Mock.when(S3Connector::create).thenReturn(builder);
			AwsFacade.create(awsRecord).createBucket(bucketDescriptor);
			verify(connector, Mockito.atLeastOnce()).createBucket(bucketDescriptor);
		}
	}

	@Test
	void bucketAclTest(@Mock LazyReference<S3Connector> s3, @Mock S3Connector connector)
	{
		when(s3.get()).thenReturn(connector);
		when(connector.bucketAcl(Mockito.anyString())).thenReturn(RESULT);
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
		when(s3.get()).thenReturn(connector);
		when(connector.isBucketReadOnly(Mockito.anyString())).thenReturn(true);
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
		when(awsRecord.credentials()).thenReturn(credentials);
		when(awsRecord.endpoint()).thenReturn(Optional.of("scheme://host"));
		when(credentials.accessKey()).thenReturn(TEST_KEY);
		when(credentials.secretKey()).thenReturn(TEST_KEY);
		when(iamBuilder.withCredentials(Mockito.anyString(),
										Mockito.anyString())).thenReturn(iamBuilder);
		when(iamBuilder.withCredentials(Mockito.anyString(),
										Mockito.anyString())).thenReturn(iamBuilder);
		when(iamBuilder.build()).thenReturn(iamConnector);
		when(stsBuilder.withCredentials(Mockito.anyString(),
										Mockito.anyString())).thenReturn(stsBuilder);
		when(stsBuilder.build()).thenReturn(stsConnector);
		when(iamConnector.filePermission(Mockito.anyString(),
										 Mockito.anyString(),
										 Mockito.anyString())).thenReturn(RESULT);
		when(stsConnector.arn()).thenReturn(ARN);
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
		when(iam.get()).thenReturn(iamConnector);
		when(sts.get()).thenReturn(stsConnector);
		when(iamConnector.directoryPermission(Mockito.anyString(),
											  Mockito.anyString(),
											  Mockito.anyString())).thenReturn(RESULT);
		when(stsConnector.arn()).thenReturn(ARN);
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
		when(s3.get()).thenReturn(connector);
		when(connector.objectMetadata(Mockito.anyString(),
									  Mockito.anyString())).thenReturn(attributes);
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
		when(s3.get()).thenReturn(connector);
		when(connector.listObjects(Mockito.anyString(),
								   Mockito.anyString())).thenReturn(Map.of(RESULT, instant));
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
		when(s3.get()).thenReturn(connector);
		when(connector.listObjects(Mockito.anyString(),
								   Mockito.anyString(),
								   Mockito.anyInt())).thenReturn(Map.of(RESULT, instant));
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
		when(s3.get()).thenReturn(connector);
		when(connector.scanDirectory(Mockito.anyString(),
									 Mockito.anyString())).thenReturn(iterator);
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
		when(s3.get()).thenReturn(connector);
		when(connector.readObject(Mockito.anyString(), Mockito.anyString())).thenReturn(result);
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
		when(s3.get()).thenReturn(connector);
		when(connector.readObject(Mockito.anyString(),
								  Mockito.anyString(),
								  Mockito.anyLong(),
								  Mockito.anyLong())).thenReturn(result);
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
		when(s3.get()).thenReturn(connector);
		try (var mock = Mockito.mockStatic(LazyReference.class))
		{
			mock.when(() -> LazyReference.of(Mockito.any())).thenReturn(s3);
			AwsFacade.create(awsRecord).writeObject(TEST_BUCKET, TEST_KEY, RESULT.getBytes());
			verify(connector, Mockito.atLeastOnce()).writeObject(TEST_BUCKET,
																 TEST_KEY,
																 RESULT.getBytes());
		}
	}

	@Test
	void startMultipartUploadTest(@Mock LazyReference<S3Connector> s3, @Mock S3Connector connector)
	{
		when(s3.get()).thenReturn(connector);
		try (var mock = Mockito.mockStatic(LazyReference.class))
		{
			mock.when(() -> LazyReference.of(Mockito.any())).thenReturn(s3);
			AwsFacade.create(awsRecord).startMultipartUpload(TEST_BUCKET, TEST_KEY);
			verify(connector, Mockito.atLeastOnce()).startMultipartUpload(TEST_BUCKET, TEST_KEY);
		}
	}
}
