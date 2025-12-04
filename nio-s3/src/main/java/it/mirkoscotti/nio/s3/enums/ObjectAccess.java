package it.mirkoscotti.nio.s3.enums;

import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.nio.file.AccessMode;
import java.nio.file.NoSuchFileException;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import it.mirkoscotti.nio.s3.operations.S3Connector;

import software.amazon.awssdk.awscore.exception.AwsErrorDetails;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.S3Exception;

/**
 * @author mirko.scotti
 * @version Jun 23, 2025
 */
public enum ObjectAccess
{

	READ
	{

		@Override
		protected String checkAccess(S3Connector connector,
									 BasicFileAttributes basicFileAttributes,
									 String bucket)
		{
			return null;
		}
	},
	WRITE
	{

		@Override
		protected String checkAccess(S3Connector connector,
									 BasicFileAttributes basicFileAttributes,
									 String bucket)
		{
			return null;
		}
	},
	EXECUTE
	{

		@Override
		protected String checkAccess(S3Connector connector,
									 BasicFileAttributes basicFileAttributes,
									 String bucket)
		{
			var key = basicFileAttributes.fileKey().toString();
			return basicFileAttributes.isDirectory()
				// As well as for traditional file systems, directory is executable if it has the
				// permission to be traversed.
				? tryListObjects(connector, bucket, key)
				// Files cannot be executed on S3 buckets
				: "Bucket: %s, File: %s, Permission: %s".formatted(bucket, key, name());
		}

		private String tryListObjects(S3Connector connector, String bucket, String key)
		{
			String result = null;
			try
			{
				connector.listObjects(bucket, key, 0);
			}
			catch (S3Exception x)
			{
				result = Optional.of(x.awsErrorDetails())
								 .filter(item -> "AccessDenied".equals(item.errorCode()))
								 .map(AwsErrorDetails::toString)
								 .orElseThrow(() -> x);
			}
			return result;
		}
	};

	protected abstract String checkAccess(S3Connector connector,
										  BasicFileAttributes basicFileAttributes,
										  String bucket);

	public static void check(S3Connector connector,
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
			var basicFileAttributes = connector.objectMetadata(bucket, key);
			var result = Stream.of(ObjectAccess.values())
							   .filter(item -> list.contains(item.name()))
							   .map(item -> item.checkAccess(connector,
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
