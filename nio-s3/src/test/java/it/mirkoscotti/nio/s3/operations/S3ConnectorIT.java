package it.mirkoscotti.nio.s3.operations;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Optional;
import java.util.stream.IntStream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.CleanupMode;
import org.junit.jupiter.api.io.TempDir;
import org.junit.platform.commons.function.Try;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import io.github.mirkoscotti.nio.s3.configuration.BucketDescriptor;

import it.mirkoscotti.nio.s3.exceptions.TransportException;
import it.mirkoscotti.nio.s3.extensions.testcontainers.S3Container;
import it.mirkoscotti.nio.s3.helpers.ContainerHelper;
import it.mirkoscotti.nio.s3.helpers.IoHelper;
import it.mirkoscotti.nio.s3.helpers.JunitHelper;
import it.mirkoscotti.nio.s3.records.BucketRecord;

import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.model.BucketAlreadyExistsException;
import software.amazon.awssdk.services.s3.model.BucketAlreadyOwnedByYouException;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;
import software.amazon.awssdk.services.s3.model.S3Exception;

/**
 * @author mirko.scotti
 * @version Oct 24, 2024
 */
@Testcontainers
@ExtendWith(MockitoExtension.class)
class S3ConnectorIT
{

	private static final String ACCOUNT_1 = "000000000001";

	private static final String ACCOUNT_2 = "000000000002";

	private static final String USER_1 = "user-1";

	private static final String USER_2 = "user-2";

	private static final String USER_3 = "user-3";

	private static final String BUCKET_NAME = "test-bucket";

	private static final String TEST_OBJECT = "test.txt";

	private static final String PUBLIC_READ = "public-read";

	private static final String KEY = "..";

	private static final String PREFIX = "directory";

	private static final String DIRECTORY = PREFIX.concat("/");

	private static final String FILE = "file.txt";

	private static final String OBJECT = DIRECTORY.concat(FILE);

	@Container
	@SuppressWarnings("resource")
	private static final S3Container CONTAINER = new S3Container().withUser(USER_1, ACCOUNT_1)
																  .withUser(USER_2, ACCOUNT_2)
																  .withUser(USER_3, ACCOUNT_1);

	@TempDir(cleanup = CleanupMode.ALWAYS)
	private static Path baseDirectory;

	private static Path file;

	@BeforeAll
	static void beforeAll()
	{
		file = baseDirectory.resolve(TEST_OBJECT);
		JunitHelper.tryCall(() -> IoHelper.createNotEmptyFile(file));
	}

	@AfterEach
	void afterEach()
	{
		Optional.of(BUCKET_NAME)
				.filter(CONTAINER::bucketExists)
				.ifPresent(item -> ContainerHelper.deleteBucket(CONTAINER, item));
	}

	@Test
	void createBucketAlreadyOwnedByUserTest(@Mock BucketDescriptor bucketDescriptor,
											@Mock BucketRecord bucketKey)
	{
		Mockito.when(bucketDescriptor.bucketKey()).thenReturn(bucketKey);
		Mockito.when(bucketKey.bucketName()).thenReturn(BUCKET_NAME);
		Assertions.assertFalse(CONTAINER.bucketExists(BUCKET_NAME));
		createConnector(USER_1, Region.US_WEST_1.toString()).createBucket(bucketDescriptor);
		Assertions.assertTrue(CONTAINER.bucketExists(BUCKET_NAME));
		var connector = createConnector(USER_3, Region.US_WEST_2.toString());
		var exception = Assertions.assertThrows(TransportException.class,
												() -> connector.createBucket(bucketDescriptor));
		Assertions.assertInstanceOf(BucketAlreadyOwnedByYouException.class, exception.getCause());
	}

	@Test
	void createAlreadyExistingBucketTest(@Mock BucketDescriptor bucketDescriptor,
										 @Mock BucketRecord bucketKey)
	{
		Mockito.when(bucketDescriptor.bucketKey()).thenReturn(bucketKey);
		Mockito.when(bucketKey.bucketName()).thenReturn(BUCKET_NAME);
		Assertions.assertFalse(CONTAINER.bucketExists(BUCKET_NAME));
		createConnector(USER_1, Region.US_WEST_1.toString()).createBucket(bucketDescriptor);
		Assertions.assertTrue(CONTAINER.bucketExists(BUCKET_NAME));
		var connector = createConnector(USER_2, Region.US_WEST_2.toString());
		var exception = Assertions.assertThrows(TransportException.class,
												() -> connector.createBucket(bucketDescriptor));
		Assertions.assertInstanceOf(BucketAlreadyExistsException.class, exception.getCause());
	}

	@Test
	void createBucketTest(@Mock BucketDescriptor bucketDescriptor, @Mock BucketRecord bucketKey)
	{
		Mockito.when(bucketDescriptor.bucketKey()).thenReturn(bucketKey);
		Mockito.when(bucketKey.bucketName()).thenReturn(BUCKET_NAME);
		Assertions.assertFalse(CONTAINER.bucketExists(BUCKET_NAME));
		createConnector(USER_1).createBucket(bucketDescriptor);
		Assertions.assertTrue(CONTAINER.bucketExists(BUCKET_NAME));
	}

	@Test
	@Disabled("TODO: access policies to be managed considering that Enforcement IAM is a Localstack Pro feature")
	void createBucketWithReadOnlyUserTest(@Mock BucketDescriptor bucketDescriptor,
										  @Mock BucketRecord bucketKey)
	{
		Mockito.when(bucketDescriptor.bucketKey()).thenReturn(bucketKey);
		Mockito.when(bucketKey.bucketName()).thenReturn(BUCKET_NAME);
		var connector = createConnector(USER_1);
		Assertions.assertThrows(S3Exception.class, () -> connector.createBucket(bucketDescriptor));
	}

	@Test
	@Disabled("TODO: access policies to be managed considering that Enforcement IAM is a Localstack Pro feature")
	void isBucketReadOnlyForUserTest()
	{
		CONTAINER.createBucket(BUCKET_NAME);
		var connector = createConnector(USER_1);
		connector.createBucket(null);
		var content = "test".getBytes(StandardCharsets.UTF_8);
		Assertions.assertThrows(S3Exception.class,
								() -> connector.writeObject(BUCKET_NAME, TEST_OBJECT, content));
	}

	@Test
	void isBucketReadOnlyWithoutPolicyTest()
	{
		CONTAINER.createBucket(BUCKET_NAME);
		Assertions.assertFalse(createConnector().isBucketReadOnly(BUCKET_NAME));
	}

	@Test
	void isBucketReadOnlyWhenReadOnlyPolicyTest()
	{
		CONTAINER.createBucket(BUCKET_NAME);
		CONTAINER.createBucketPolicy(BUCKET_NAME);
		Assertions.assertTrue(createConnector().isBucketReadOnly(BUCKET_NAME));
	}

	@Test
	void isBucketReadOnlyWhenReadOnlyAclTest()
	{
		CONTAINER.createBucket(BUCKET_NAME);
		CONTAINER.createBucketAcl(BUCKET_NAME, PUBLIC_READ);
		Assertions.assertTrue(createConnector().isBucketReadOnly(BUCKET_NAME));
	}

	@Test
	void bucketAclTest()
	{
		CONTAINER.createBucket(BUCKET_NAME);
		CONTAINER.createBucketAcl(BUCKET_NAME, PUBLIC_READ);
		Assertions.assertTrue(createConnector().bucketAcl(BUCKET_NAME).contains("READ"));
	}

	@Test
	void bucketAclWhenBucketDoesNotExistTest()
	{
		var connector = createConnector();
		var exception = Assertions.assertThrows(TransportException.class,
												() -> connector.bucketAcl(BUCKET_NAME));
		Assertions.assertInstanceOf(NoSuchBucketException.class, exception.getCause());
	}

	@Test
	void objectMetadataTest()
	{
		CONTAINER.createBucket(BUCKET_NAME);
		CONTAINER.createObject(BUCKET_NAME, KEY);
		var basicFileAttributes = createConnector().objectMetadata(BUCKET_NAME, KEY);
		var lastModified = basicFileAttributes.lastModifiedTime();
		Assertions.assertEquals(CONTAINER.lastModified(BUCKET_NAME, KEY), lastModified.toInstant());
		Assertions.assertEquals(0, basicFileAttributes.size());
		Assertions.assertEquals(KEY, basicFileAttributes.fileKey());
	}

	@Test
	void listObjectsWithSimplePrefixTest()
	{
		CONTAINER.createBucket(BUCKET_NAME);
		CONTAINER.createObject(BUCKET_NAME, PREFIX);
		var map = createConnector().listObjects(BUCKET_NAME, PREFIX);
		Assertions.assertTrue(map.isEmpty());
	}

	@Test
	void listObjectsWithDirectoryPrefixTest()
	{
		CONTAINER.createBucket(BUCKET_NAME);
		CONTAINER.createObject(BUCKET_NAME, DIRECTORY);
		CONTAINER.createObject(BUCKET_NAME, OBJECT, file);
		var map = createConnector().listObjects(BUCKET_NAME, DIRECTORY);
		Assertions.assertTrue(map.containsKey(OBJECT));
	}

	@Test
	void listObjectsWithPaginationTest()
	{
		CONTAINER.createBucket(BUCKET_NAME);
		CONTAINER.createObject(BUCKET_NAME, DIRECTORY);
		IntStream.range(0, 10)
				 .mapToObj("test-%d.txt"::formatted)
				 .map(baseDirectory::resolve)
				 .map(item -> Try.call(() -> IoHelper.createNotEmptyFile(item))
								 .getOrThrow(IllegalStateException::new))
				 .forEach(item -> CONTAINER.createObject(BUCKET_NAME,
														 DIRECTORY.concat(item.getFileName()
																			  .toString()),
														 item));
		var map = createConnector().listObjects(BUCKET_NAME, DIRECTORY, 5);
		IntStream.range(0, 10)
				 .mapToObj(DIRECTORY.concat("test-%d.txt")::formatted)
				 .map(map::containsKey)
				 .forEach(Assertions::assertTrue);
	}

	@Test
	void isEmptyDirectoryTest()
	{
		CONTAINER.createBucket(BUCKET_NAME);
		CONTAINER.createObject(BUCKET_NAME, DIRECTORY);
		var connector = createConnector();
		Assertions.assertFalse(connector.isNotEmptyDirectory(BUCKET_NAME, PREFIX));
	}

	@Test
	void isNotEmptyDirectoryTest()
	{
		CONTAINER.createBucket(BUCKET_NAME);
		CONTAINER.createObject(BUCKET_NAME, DIRECTORY.concat(TEST_OBJECT));
		CONTAINER.createObject(BUCKET_NAME, OBJECT, file);
		var connector = createConnector();
		Assertions.assertTrue(connector.isNotEmptyDirectory(BUCKET_NAME, DIRECTORY));
	}

	private S3Connector createConnector()
	{
		return createConnector(null);
	}

	private S3Connector createConnector(String user)
	{
		return createConnector(user, CONTAINER.getRegion());
	}

	private S3Connector createConnector(String user, String region)
	{
		var optional = Optional.ofNullable(user);
		var accessKey = optional.map(CONTAINER::getAccessKey).orElseGet(CONTAINER::getAccessKey);
		var secretKey = optional.map(CONTAINER::getSecretKey).orElseGet(CONTAINER::getSecretKey);
		return S3Connector.create()
						  .withEndpoint(CONTAINER.getEndpoint())
						  .withRegion(region)
						  .withCredentials(accessKey, secretKey)
						  .build();
	}
}
