package it.mirkoscotti.nio.s3.enums;

import it.mirkoscotti.nio.s3.operations.S3Connector;

import java.nio.file.AccessMode;
import java.util.stream.Stream;

/**
 * @author mirko.scotti
 * @version Jun 23, 2025
 */
public enum ObjectAccess
{

	READ
	{

		@Override
		void checkAccess(S3Connector connector, String bucket, String key)
		{
			connector.isBucketReadOnly(bucket);
		}
	},
	WRITE
	{

		@Override
		void checkAccess(S3Connector connector, String bucket, String key)
		{
			connector.isBucketReadOnly(bucket);
		}
	},
	EXECUTE
	{

		@Override
		void checkAccess(S3Connector connector, String bucket, String key)
		{
			throw new UnsupportedOperationException(name().concat(" access not supported by AWS S3."));
		}
	};

	abstract void checkAccess(S3Connector connector, String bucket, String key);

	public static void check(S3Connector connector,
							 String bucket,
							 String key,
							 AccessMode... accessModes)
	{
		var list = Stream.ofNullable(accessModes)
						 .flatMap(Stream::of)
						 .map(AccessMode::name)
						 .toList();
		Stream.of(ObjectAccess.values())
			  .filter(item -> list.contains(item.name()))
			  .forEach(item -> item.checkAccess(connector, bucket, key));
	}
}
