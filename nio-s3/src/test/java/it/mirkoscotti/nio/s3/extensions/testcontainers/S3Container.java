package it.mirkoscotti.nio.s3.extensions.testcontainers;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.SequencedMap;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jakarta.json.bind.JsonbBuilder;

import org.junit.jupiter.api.Assertions;
import org.testcontainers.images.builder.Transferable;
import org.testcontainers.localstack.LocalStackContainer;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

import it.mirkoscotti.nio.s3.records.ObjectAttributes;
import it.mirkoscotti.nio.s3.records.ObjectParts;

/**
 * @author mirko.scotti
 * @version Oct 24, 2024
 */
public class S3Container
	extends LocalStackContainer
{

	private static final String LOCALSTACK = "localstack";

	private static final String IMAGE_NAME = "%1$s/%1$s:4.13.0".formatted(LOCALSTACK);

	private static final String INITIALIZATION_FILE = "/etc/localstack/init/ready.d/init-s3.sh";

	private static final String AWS_CREDENTIALS_FILE = "/root/.aws/credentials";

	private static final String PROFILE_TEMPLATE = """
		[%s]
		aws_access_key_id = %s
		aws_secret_access_key = %s

		""";

	private static final String IAM_POLICY_FILE = "/tmp/iam-policy.json";

	private static final String BUCKET_POLICY_FILE = "/tmp/bucket-policy.json";

	private static final String READ_ONLY_POLICY = """
		{
		    "Version": "2012-10-17",
		    "Statement": [
		        {
		            "Effect": "Allow",
		            "Principal": "*",
		            "Action": [
		                "s3:GetObject",
		                "s3:ListBucket",
		                "s3:GetObjectVersion",
		                "s3:GetBucketLocation",
		                "s3:GetObjectTagging",
		                "s3:GetBucketPolicy",
		                "s3:GetBucketAcl",
		                "s3:GetObjectAcl"
		            ],
		            "Resource": [
		                "arn:aws:s3:::%1$s",
		                "arn:aws:s3:::%1$s/*"
		            ]
		        }
		    ]
		}
		""";

	private static final String WRITE_POLICY = """
		{
		    "Version": "2012-10-17",
		    "Statement": [
		        {
		            "Effect": "Allow",
		            "Action": [
		                "s3:PutObject",
		                "s3:DeleteObject",
		                "s3:GetObject",
		                "s3:ListBucket"
		            ],
		            "Resource": [
		                "arn:aws:s3:::*",
		                "arn:aws:s3:::*/*"
		            ]
		        }
		    ]
		}
		""";

	private static final String GENERIC_COMMAND = """
		#!/bin/bash
		%s
		""";

	private static final String INSTALL_JQ_COMMAND = "apt-get update && apt-get install -y jq";

	private static final String CREATE_USER_COMMAND = "awslocal iam create-user --user-name %s";

	private static final String PUT_USER_POLICY_COMMAND = "awslocal iam put-user-policy --user-name %%s --policy-name %%s --policy-document file://%s".formatted(IAM_POLICY_FILE);

	private static final String LIST_USERS_COMMAND = "awslocal iam list-users | jq -r '.Users[].UserName'";

	private static final String GET_CALLER_IDENTITY_COMMAND = "awslocal sts get-caller-identity --profile %s";

	private static final String CREATE_ACCESS_KEY_COMMAND = "awslocal iam create-access-key --user-name %s | jq -r '.AccessKey | \"\\(.AccessKeyId) \\(.SecretAccessKey)\"'";

	private static final String CREATE_BUCKET_COMMAND = "awslocal s3api create-bucket --bucket %s";

	private static final String HEAD_BUCKET_COMMAND = "awslocal s3api head-bucket --bucket %s";

	private static final String DELETE_BUCKET_COMMAND = "awslocal s3api delete-bucket --bucket %s";

	private static final String PUT_BUCKET_POLICY_COMMAND = "awslocal s3api put-bucket-policy --bucket %%s --policy file://%s".formatted(BUCKET_POLICY_FILE);

	private static final String GET_BUCKET_POLICY_COMMAND = "awslocal s3api get-bucket-policy --bucket %s";

	private static final String DELETE_BUCKET_POLICY_COMMAND = "awslocal s3api delete-bucket-policy --bucket %s";

	private static final String DELETE_POLICY_FILE_COMMAND = "rm -f %s".formatted(BUCKET_POLICY_FILE);

	private static final String PUT_BUCKET_ACL_COMMAND = "awslocal s3api put-bucket-acl --bucket %s --acl %s";

	private static final String PUT_OBJECT_COMMAND = "awslocal s3api put-object --bucket %s --key %s";

	private static final String PUT_OBJECT_WITH_TEXT_COMMAND = "awslocal s3api put-object --bucket %s --key %s --body %s";

	private static final String HEAD_OBJECT_COMMAND = "awslocal s3api head-object --bucket %s --key %s";

	private static final String GET_OBJECT_COMMAND = "awslocal s3api get-object --bucket %s --key %s %s";

	private static final String GET_OBJECT_ATTRIBUTES_COMMAND = "awslocal s3api get-object-attributes --bucket %s --key %s --object-attributes Checksum ObjectSize ObjectParts";

	private static final String LIST_OBJECTS_COMMAND = "awslocal s3api list-objects-v2 --bucket %s --query Contents[*].Key --output text";

	private static final String DELETE_OBJECT_COMMAND = "awslocal s3api delete-object --bucket %s --key %s";

	private static final String OUTPUT_PROPERTY_COMMAND = " | jq -r '.%s'";

	private static final String CHECKSUM_OPTION = " --checksum-algorithm SHA256";

	private final List<String> commands = new ArrayList<>();

	private final Map<String, Credentials> credentials = new HashMap<>();

	public S3Container()
	{
		super(DockerImageName.parse(IMAGE_NAME));
		withServices("s3", "iam", "sts");
		withEnv("DEBUG", "1");
		withEnv("LS_LOG", "trace");
		commands.add(INSTALL_JQ_COMMAND);
	}

	@Override
	public void start()
	{
		var command = commands.stream().collect(Collectors.joining("\n"));
		var script = GENERIC_COMMAND.formatted(command);
		var content = script.getBytes(StandardCharsets.UTF_8);
		withCopyToContainer(Transferable.of(content, 755), INITIALIZATION_FILE);
		super.start();
		createCredentials();
		createProfiles();
	}

	public S3Container withUser(String user)
	{
		Objects.requireNonNull(user, () -> "Missing user.");
		commands.add(CREATE_USER_COMMAND.formatted(user));
		var policy = WRITE_POLICY.getBytes(StandardCharsets.UTF_8);
		withCopyToContainer(Transferable.of(policy), IAM_POLICY_FILE);
		commands.add(PUT_USER_POLICY_COMMAND.formatted(user, "bucket-write-policy"));
		return this;
	}

	public S3Container withReadOnlyUser(String user)
	{
		Objects.requireNonNull(user, () -> "Missing user.");
		commands.add(CREATE_USER_COMMAND.formatted(user));
		return this;
	}

	public S3Container withBucket(String bucketName)
	{
		Objects.requireNonNull(bucketName, () -> "Missing bucket name.");
		commands.add(CREATE_BUCKET_COMMAND.formatted(bucketName));
		return this;
	}

	public String getAccessKey(String user)
	{
		return credentials.get(user).accessKey();
	}

	public String getSecretKey(String user)
	{
		return credentials.get(user).secretKey();
	}

	public String arn(String user)
	{
		String result = null;
		var command = GET_CALLER_IDENTITY_COMMAND.concat(OUTPUT_PROPERTY_COMMAND)
												 .formatted(user, "Arn");
		try
		{
			var output = execInContainer("sh", "-c", command);
			var message = "Failed to retrieve arn of user %s: %s.";
			Assertions.assertEquals(0, output.getExitCode(),
									message.formatted(user, output.getStderr()));
			result = output.getStdout().trim();
		}
		catch (InterruptedException x)
		{
			Thread.currentThread().interrupt();
			Assertions.fail(x);
		}
		catch (IOException x)
		{
			Assertions.fail(x);
		}
		return result;
	}

	public void createBucket(String bucketName)
	{
		var command = CREATE_BUCKET_COMMAND.formatted(bucketName);
		try
		{
			var output = execInContainer(command.split(" "));
			var message = "Failed to create bucket %s.";
			Assertions.assertEquals(0, output.getExitCode(), message.formatted(bucketName));
		}
		catch (InterruptedException x)
		{
			Thread.currentThread().interrupt();
			Assertions.fail(x);
		}
		catch (IOException x)
		{
			Assertions.fail(x);
		}
	}

	public boolean bucketExists(String bucketName)
	{
		var result = false;
		var command = HEAD_BUCKET_COMMAND.formatted(bucketName);
		try
		{
			var output = execInContainer(command.split(" "));
			result = output.getExitCode() == 0;
		}
		catch (InterruptedException x)
		{
			Thread.currentThread().interrupt();
			Assertions.fail(x);
		}
		catch (IOException x)
		{
			Assertions.fail(x);
		}
		return result;
	}

	public void deleteBucket(String bucketName)
	{
		var command = DELETE_BUCKET_COMMAND.formatted(bucketName);
		try
		{
			var output = execInContainer(command.split(" "));
			var message = "Failed to delete bucket %s";
			Assertions.assertEquals(0, output.getExitCode(), message.formatted(bucketName));
		}
		catch (InterruptedException x)
		{
			Thread.currentThread().interrupt();
			Assertions.fail(x);
		}
		catch (IOException x)
		{
			Assertions.fail(x);
		}
	}

	public void createBucketPolicy(String bucketName)
	{
		try
		{
			var policy = READ_ONLY_POLICY.getBytes(StandardCharsets.UTF_8);
			copyFileToContainer(Transferable.of(policy), BUCKET_POLICY_FILE);
			var command = PUT_BUCKET_POLICY_COMMAND.formatted(bucketName);
			var output = execInContainer(command.split(" "));
			var message = "Failed to create policy for bucket %s";
			Assertions.assertEquals(0, output.getExitCode(), message.formatted(bucketName));
		}
		catch (InterruptedException x)
		{
			Thread.currentThread().interrupt();
			Assertions.fail(x);
		}
		catch (IOException x)
		{
			Assertions.fail(x);
		}
		finally
		{
			deletePolicyFile();
		}
	}

	public Optional<String> bucketPolicy(String bucketName)
	{
		var result = Optional.<String>empty();
		var command = GET_BUCKET_POLICY_COMMAND.formatted(bucketName);
		try
		{
			var output = Optional.of(execInContainer(command.split(" ")));
			result = output.filter(item -> item.getExitCode() == 0
				|| item.getStderr().contains("NoSuchBucketPolicy"))
						   .map(ExecResult::getStdout)
						   .filter(Predicate.not(String::isEmpty));
		}
		catch (InterruptedException x)
		{
			Thread.currentThread().interrupt();
			result = Assertions.fail(x);
		}
		catch (IOException x)
		{
			result = Assertions.fail(x);
		}
		return result;
	}

	public void deleteBucketPolicy(String bucketName)
	{
		var command = DELETE_BUCKET_POLICY_COMMAND.formatted(bucketName);
		try
		{
			var output = execInContainer(command.split(" "));
			var message = "Failed to delete policy for bucket %s.";
			Assertions.assertEquals(0, output.getExitCode(), message.formatted(bucketName));
		}
		catch (InterruptedException x)
		{
			Thread.currentThread().interrupt();
			Assertions.fail(x);
		}
		catch (IOException x)
		{
			Assertions.fail(x);
		}
	}

	public void createBucketAcl(String bucketName, String acl)
	{
		try
		{
			var command = PUT_BUCKET_ACL_COMMAND.formatted(bucketName, acl);
			var output = execInContainer(command.split(" "));
			var message = "Failed to create ACL for bucket %s.";
			Assertions.assertEquals(0, output.getExitCode(), message.formatted(bucketName));
		}
		catch (InterruptedException x)
		{
			Thread.currentThread().interrupt();
			Assertions.fail(x);
		}
		catch (IOException x)
		{
			Assertions.fail(x);
		}
		finally
		{
			deletePolicyFile();
		}
	}

	public void createObject(String bucketName, String key)
	{
		var command = PUT_OBJECT_COMMAND.formatted(bucketName, key);
		try
		{
			var output = execInContainer(command.split(" "));
			var message = "Failed to create object %s in bucket %s.";
			Assertions.assertEquals(0, output.getExitCode(), message.formatted(key, bucketName));
		}
		catch (InterruptedException x)
		{
			Thread.currentThread().interrupt();
			Assertions.fail(x);
		}
		catch (IOException x)
		{
			Assertions.fail(x);
		}
	}

	public void createObject(String bucketName, String key, Path file)
	{
		var path = "/tmp/input.txt";
		copyFileToContainer(MountableFile.forHostPath(file), path);
		var command = PUT_OBJECT_WITH_TEXT_COMMAND.formatted(bucketName, key, path);
		try
		{
			var output = execInContainer(command.split(" "));
			var message = "Failed to create object %s in bucket %s.";
			Assertions.assertEquals(0, output.getExitCode(), message.formatted(key, bucketName));
		}
		catch (InterruptedException x)
		{
			Thread.currentThread().interrupt();
			Assertions.fail(x);
		}
		catch (IOException x)
		{
			Assertions.fail(x);
		}
	}

	public void createObjectWithChecksum(String bucketName, String key)
	{
		var command = PUT_OBJECT_COMMAND.concat(CHECKSUM_OPTION).formatted(bucketName, key);
		try
		{
			var output = execInContainer(command.split(" "));
			var message = "Failed to create object %s with checksum in bucket %s.";
			Assertions.assertEquals(0, output.getExitCode(), message.formatted(key, bucketName));
		}
		catch (InterruptedException x)
		{
			Thread.currentThread().interrupt();
			Assertions.fail(x);
		}
		catch (IOException x)
		{
			Assertions.fail(x);
		}
	}

	public void createObjectWithChecksum(String bucketName, String key, Path file)
	{
		var path = "/tmp/input.txt";
		copyFileToContainer(MountableFile.forHostPath(file), path);
		var command = PUT_OBJECT_WITH_TEXT_COMMAND.concat(CHECKSUM_OPTION)
												  .formatted(bucketName, key, path);
		try
		{
			var output = execInContainer(command.split(" "));
			var message = "Failed to create object %s with checksum in bucket %s";
			Assertions.assertEquals(0, output.getExitCode(), message.formatted(key, bucketName));
		}
		catch (InterruptedException x)
		{
			Thread.currentThread().interrupt();
			Assertions.fail(x);
		}
		catch (IOException x)
		{
			Assertions.fail(x);
		}
	}

	public void readObject(String bucketName, String key, Path file)
	{
		var path = "/tmp/output.txt";
		var command = GET_OBJECT_COMMAND.formatted(bucketName, key, path);
		try
		{
			var output = execInContainer(command.split(" "));
			var message = "Failed to read object %s in bucket %s";
			Assertions.assertEquals(0, output.getExitCode(), message.formatted(key, bucketName));
			copyFileFromContainer(path, item -> Files.copy(item, file,
														   StandardCopyOption.REPLACE_EXISTING));
		}
		catch (InterruptedException x)
		{
			Thread.currentThread().interrupt();
			Assertions.fail(x);
		}
		catch (IOException x)
		{
			Assertions.fail(x);
		}
	}

	public boolean objectExists(String bucketName, String key)
	{
		boolean result;
		var command = HEAD_OBJECT_COMMAND.formatted(bucketName, key);
		try
		{
			var output = execInContainer(command.split(" "));
			result = output.getExitCode() == 0;
		}
		catch (InterruptedException x)
		{
			Thread.currentThread().interrupt();
			result = Assertions.fail(x);
		}
		catch (IOException x)
		{
			result = Assertions.fail(x);
		}
		return result;
	}

	public Instant lastModified(String bucketName, String key)
	{
		Instant result;
		var command = HEAD_OBJECT_COMMAND.concat(OUTPUT_PROPERTY_COMMAND)
										 .formatted(bucketName, key, "LastModified");
		try
		{
			var output = execInContainer("sh", "-c", command);
			var message = "Failed to retrieve the 'LastModified' property from object %s in bucket %s";
			Assertions.assertEquals(0, output.getExitCode(), message.formatted(key, bucketName));
			result = DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss z", Locale.ENGLISH)
									  .withZone(ZoneOffset.UTC)
									  .parse(output.getStdout().trim(), Instant::from);
		}
		catch (InterruptedException x)
		{
			Thread.currentThread().interrupt();
			result = Assertions.fail(x);
		}
		catch (IOException x)
		{
			result = Assertions.fail(x);
		}
		return result;
	}

	public SequencedMap<String, Long> checksum(String bucketName, String key)
	{
		var result = new LinkedHashMap<String, Long>();
		var command = GET_OBJECT_ATTRIBUTES_COMMAND.formatted(bucketName, key);
		try (var jsonb = JsonbBuilder.create())
		{
			var output = execInContainer("sh", "-c", command);
			var message = "Failed to retrieve the checksum of object %s in bucket %s";
			Assertions.assertEquals(0, output.getExitCode(), message.formatted(key, bucketName));
			var attributes = jsonb.fromJson(output.getStdout(), ObjectAttributes.class);
			Optional.ofNullable(attributes.objectParts())
					.map(ObjectParts::parts)
					.stream()
					.flatMap(List::stream)
					.forEach(item -> result.put(item.checksumSha256(), item.size()));
			result.put(attributes.checksum().checksumSha256(), attributes.objectSize());
		}
		catch (InterruptedException x)
		{
			Thread.currentThread().interrupt();
			Assertions.fail(x);
		}
		catch (Exception x)
		{
			Assertions.fail(x);
		}
		return result;
	}

	public String[] listObjects(String bucketName)
	{
		String[] result;
		var command = LIST_OBJECTS_COMMAND.formatted(bucketName);
		try
		{
			var output = execInContainer(command.split(" "));
			var message = "Failed to list objects from bucket %s";
			Assertions.assertEquals(0, output.getExitCode(), message.formatted(bucketName));
			result = output.getStdout().replace("\n", "").split("\t");
		}
		catch (InterruptedException x)
		{
			Thread.currentThread().interrupt();
			result = Assertions.fail(x);
		}
		catch (IOException x)
		{
			result = Assertions.fail(x);
		}
		return result;
	}

	public void deleteObject(String bucketName, String key)
	{
		var command = DELETE_OBJECT_COMMAND.formatted(bucketName, key);
		try
		{
			var output = execInContainer(command.split(" "));
			var message = "Failed to delete object %s from bucket %s";
			Assertions.assertEquals(0, output.getExitCode(), message.formatted(key, bucketName));
		}
		catch (InterruptedException x)
		{
			Thread.currentThread().interrupt();
			Assertions.fail(x);
		}
		catch (IOException x)
		{
			Assertions.fail(x);
		}
	}

	private void createCredentials()
	{
		try
		{
			var output = execInContainer("sh", "-c", LIST_USERS_COMMAND);
			var message = "Failed to list users.";
			Assertions.assertEquals(0, output.getExitCode(), message);
			Optional.of(output.getStdout())
					.filter(Predicate.not(String::isBlank))
					.stream()
					.flatMap(item -> Stream.of(item.split("\n")))
					.forEach(this::createCredentials);
		}
		catch (InterruptedException x)
		{
			Thread.currentThread().interrupt();
			Assertions.fail(x);
		}
		catch (IOException x)
		{
			Assertions.fail(x);
		}
	}

	private void createCredentials(String user)
	{
		var command = CREATE_ACCESS_KEY_COMMAND.formatted(user);
		try
		{
			var output = execInContainer("sh", "-c", command);
			var message = "Failed to create credentials for user %s";
			Assertions.assertEquals(0, output.getExitCode(), message.formatted(user));
			var result = output.getStdout().split(" ");
			credentials.put(user, new Credentials(result[0], result[1]));
		}
		catch (InterruptedException x)
		{
			Thread.currentThread().interrupt();
			Assertions.fail(x);
		}
		catch (IOException x)
		{
			Assertions.fail(x);
		}
	}

	private void createProfiles()
	{
		var content = credentials.entrySet()
								 .stream()
								 .map(item -> createProfile(item.getKey(), item.getValue()))
								 .collect(Collectors.joining("\n"));
		copyFileToContainer(Transferable.of(content.getBytes(StandardCharsets.UTF_8)),
							AWS_CREDENTIALS_FILE);
	}

	private String createProfile(String user, Credentials credentials)
	{
		return PROFILE_TEMPLATE.formatted(user, credentials.accessKey(), credentials.secretKey());
	}

	private void deletePolicyFile()
	{
		try
		{
			var output = execInContainer(DELETE_POLICY_FILE_COMMAND.split(" "));
			var message = "Failed to delete temporary policy file.";
			Assertions.assertEquals(0, output.getExitCode(), message);
		}
		catch (InterruptedException x)
		{
			Thread.currentThread().interrupt();
			Assertions.fail(x);
		}
		catch (IOException x)
		{
			Assertions.fail(x);
		}
	}

	private static record Credentials(String accessKey, String secretKey)
	{

	}
}
