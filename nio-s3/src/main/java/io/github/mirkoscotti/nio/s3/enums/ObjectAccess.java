package io.github.mirkoscotti.nio.s3.enums;

import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.nio.file.AccessMode;
import java.nio.file.NoSuchFileException;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import it.mirkoscotti.nio.s3.operations.AwsFacade;

import software.amazon.awssdk.awscore.exception.AwsErrorDetails;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.services.iam.model.IamException;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.sts.model.StsException;

/**
 * @author mirko.scotti
 * @version Jun 23, 2025
 */
public enum ObjectAccess
{

	READ
	{

		@Override
		protected String checkAccess(AwsFacade awsFacade,
									 BasicFileAttributes basicFileAttributes,
									 String bucket)
		{
			var key = basicFileAttributes.fileKey().toString();
			return basicFileAttributes.isDirectory()
				// As well as for traditional file systems, let's consider a directory readable if
				// it has the permission to be traversed.
				? tryListObjects(awsFacade, bucket, key)
				// Files metadata are already available. If it is possible, it means that also the
				// content is accessible.
				: null;
		}
	},
	WRITE
	{

		@Override
		protected String checkAccess(AwsFacade awsFacade,
									 BasicFileAttributes basicFileAttributes,
									 String bucket)
		{
			String result;
			var key = basicFileAttributes.fileKey().toString();
			var isDirectory = basicFileAttributes.isDirectory();
			try
			{
				var permission = isDirectory
					? awsFacade.directoryPermission(bucket, key)
					: awsFacade.filePermission(bucket, key);
				result = permission.toUpperCase().contains(BucketEffect.ALLOW.name())
					? null
					: errorMessage(bucket, key, isDirectory, permission);
			}
			catch (IamException | StsException x)
			{
				result = awsErrorMessage(x);
			}
			return result;
		}

		private String errorMessage(String bucket,
									String key,
									boolean isDirectory,
									String permission)
		{
			var resourceType = isDirectory ? "Directory" : "File";
			return ERROR_TEMPLATE.formatted(bucket, resourceType, key, permission);
		}
	},
	EXECUTE
	{

		@Override
		protected String checkAccess(AwsFacade awsFacade,
									 BasicFileAttributes basicFileAttributes,
									 String bucket)
		{
			var key = basicFileAttributes.fileKey().toString();
			return basicFileAttributes.isDirectory()
				// Similarly to traditional file systems, let's consider a directory executable if
				// it has the permission to be traversed.
				? tryListObjects(awsFacade, bucket, key)
				// Files cannot be executed on S3 buckets
				: ERROR_TEMPLATE.formatted(bucket, "File", key, name());
		}
	};

	private static final String ERROR_TEMPLATE = "Bucket: %s, Object Type: %s, Key: %s, Permission: %s";

	protected abstract String checkAccess(AwsFacade awsFacade,
										  BasicFileAttributes basicFileAttributes,
										  String bucket);

	protected String tryListObjects(AwsFacade awsFacade, String bucket, String key)
	{
		String result = null;
		try
		{
			awsFacade.listObjects(bucket, key, 0);
		}
		catch (S3Exception x)
		{
			result = awsErrorMessage(x);
		}
		return result;
	}

	protected String awsErrorMessage(AwsServiceException exception)
	{
		return Optional.of(exception.awsErrorDetails())
					   .filter(item -> "AccessDenied".equals(item.errorCode()))
					   .map(AwsErrorDetails::toString)
					   .orElseThrow(() -> exception);
	}

	public static void check(AwsFacade awsFacade,
							 String bucket,
							 String key,
							 AccessMode... accessModes)
		throws IOException
	{
		var list = Stream.ofNullable(accessModes)
						 .flatMap(Stream::of)
						 .map(AccessMode::name)
						 .toList();
		try
		{
			var basicFileAttributes = awsFacade.objectMetadata(bucket, key);
			var result = Stream.of(ObjectAccess.values())
							   .filter(item -> list.contains(item.name()))
							   .map(item -> item.checkAccess(awsFacade,
															 basicFileAttributes,
															 bucket))
							   .filter(Objects::nonNull)
							   .toList();
			if (!result.isEmpty())
			{
				var message = result.stream().collect(Collectors.joining("\n", "\n", ""));
				throw new AccessDeniedException("Access denied.".concat(message));
			}
		}
		catch (NoSuchKeyException x)
		{
			var originalMessage = Optional.ofNullable(x.getMessage()).orElse("");
			var message = "File not found and not creatable: ".concat(originalMessage);
			throw new NoSuchFileException(key, null, message);
		}
	}
}
