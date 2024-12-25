/*
 * (C) Copyright 2019 - 2024 - Add Value S.R.L - All rights reserved.
 */

package it.mirkoscotti.nio.s3.extensions.testcontainers;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;

import org.junit.jupiter.api.Assertions;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.images.builder.Transferable;
import org.testcontainers.utility.DockerImageName;

/**
 * @author mirko.scotti
 * @version Oct 24, 2024
 */
public class S3Container
	extends LocalStackContainer
{

	private static final String LOCALSTACK = "localstack";

	private static final String IMAGE_NAME = "%1$s/%1$s".formatted(LOCALSTACK);

	private static final String INITIALIZATION_FILE = "/etc/localstack/init/ready.d/init-s3.sh";

	private static final String POLICY_FILE = "/tmp/bucket-policy.json";

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

	private static final String GENERIC_COMMAND = """
		#!/bin/bash
		%s
		""";

	private static final String CREATE_BUCKET_COMMAND = "awslocal s3api create-bucket --bucket %s";

	private static final String HEAD_BUCKET_COMMAND = "awslocal s3api head-bucket --bucket %s";

	private static final String DELETE_BUCKET_COMMAND = "awslocal s3api delete-bucket --bucket %s";

	private static final String PUT_BUCKET_POLICY_COMMAND = "awslocal s3api put-bucket-policy --bucket %%s --policy file://%s".formatted(POLICY_FILE);

	private static final String GET_BUCKET_POLICY_COMMAND = "awslocal s3api get-bucket-policy --bucket %s";

	private static final String DELETE_BUCKET_POLICY_COMMAND = "awslocal s3api delete-bucket-policy --bucket %s";

	private static final String DELETE_POLICY_FILE_COMMAND = "rm -f %s".formatted(POLICY_FILE);

	private static final String PUT_BUCKET_ACL_COMMAND = "awslocal s3api put-bucket-acl --bucket %s --acl %s";

	public S3Container()
	{
		super(DockerImageName.parse(IMAGE_NAME));
		withServices(Service.S3);
	}

	public S3Container withBucket(String bucketName)
	{
		Objects.requireNonNull(bucketName, () -> "Missing bucket name.");
		var command = CREATE_BUCKET_COMMAND.formatted(bucketName);
		var script = GENERIC_COMMAND.formatted(command);
		withCopyToContainer(Transferable.of(script.getBytes(StandardCharsets.UTF_8), 755),
							INITIALIZATION_FILE);
		return this;
	}

	public void createBucket(String bucketName)
	{
		var command = CREATE_BUCKET_COMMAND.formatted(bucketName);
		try
		{
			var output = execInContainer(command.split(" "));
			Assertions.assertEquals(0,
									output.getExitCode(),
									"Failed to create bucket %s".formatted(bucketName));
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
			Assertions.assertEquals(0,
									output.getExitCode(),
									"Failed to delete bucket %s".formatted(bucketName));
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
			copyFileToContainer(Transferable.of(policy), POLICY_FILE);
			var command = PUT_BUCKET_POLICY_COMMAND.formatted(bucketName);
			var output = execInContainer(command.split(" "));
			Assertions.assertEquals(0,
									output.getExitCode(),
									"Failed to create policy for bucket %s".formatted(bucketName));
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
			Assertions.assertEquals(0,
									output.getExitCode(),
									"Failed to delete policy for bucket %s".formatted(bucketName));
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
			Assertions.assertEquals(0,
									output.getExitCode(),
									"Failed to create ACL for bucket %s".formatted(bucketName));
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

	private void deletePolicyFile()
	{
		try
		{
			var output = execInContainer(DELETE_POLICY_FILE_COMMAND.split(" "));
			Assertions.assertEquals(0,
									output.getExitCode(),
									"Failed to delete temporary policy file.");
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
}
