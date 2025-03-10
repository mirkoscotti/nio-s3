/*
 * (C) Copyright 2019 - 2025 - Add Value S.R.L - All rights reserved.
 */

package it.mirkoscotti.nio.s3.extensions.jdk.jsr203;

import it.mirkoscotti.nio.s3.configuration.BucketDescriptor;
import it.mirkoscotti.nio.s3.enums.BucketProperty;
import it.mirkoscotti.nio.s3.exceptions.BucketNameException;
import it.mirkoscotti.nio.s3.exceptions.CredentialsException;
import it.mirkoscotti.nio.s3.operations.S3Connector;

import java.io.IOException;
import java.net.URI;
import java.nio.file.FileStore;
import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.WatchService;
import java.nio.file.attribute.UserPrincipalLookupService;
import java.nio.file.spi.FileSystemProvider;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import software.amazon.awssdk.services.s3.model.BucketAlreadyExistsException;
import software.amazon.awssdk.services.s3.model.BucketAlreadyOwnedByYouException;

/**
 * @author Add Value S.R.L by mirko.scotti
 * @version Jan 24, 2025
 */
public class BucketFileSystem
	extends FileSystem
{

	private final Map<BucketProperty, String> configuration = new EnumMap<>(BucketProperty.class);

	private final BucketFileSystemProvider fileSystemProvider;

	private final BucketFileStore fileStore;

	BucketFileSystem(BucketDescriptor bucketDescriptor, BucketFileSystemProvider fileSystemProvider)
	{
		var bucketKey = bucketDescriptor.bucketKey();
		var credentials = bucketDescriptor.credentials();
		var builder = S3Connector.create()
								 .withEndpoint(URI.create(bucketKey.endpoint()))
								 .withCredentials(credentials.accessKey(), credentials.secretKey());
		var connector = builder.build();
		ensureBucketExists(bucketDescriptor, connector);
		this.fileSystemProvider = fileSystemProvider;
		fileStore = new BucketFileStore(connector, bucketKey.bucketName());
	}

	@Override
	public FileSystemProvider provider()
	{
		return fileSystemProvider;
	}

	@Override
	public void close() throws IOException
	{
		// TODO Auto-generated method stub

	}

	@Override
	public boolean isOpen()
	{
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isReadOnly()
	{
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public String getSeparator()
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Iterable<Path> getRootDirectories()
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Iterable<FileStore> getFileStores()
	{
		return List.of(fileStore);
	}

	@Override
	public Set<String> supportedFileAttributeViews()
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Path getPath(String first, String... more)
	{
		return new BucketPath(this, first, more);
	}

	@Override
	public PathMatcher getPathMatcher(String syntaxAndPattern)
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public UserPrincipalLookupService getUserPrincipalLookupService()
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public WatchService newWatchService() throws IOException
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(configuration, fileStore, fileSystemProvider);
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj)
		{
			return true;
		}
		if (obj == null)
		{
			return false;
		}
		if (getClass() != obj.getClass())
		{
			return false;
		}
		BucketFileSystem other = (BucketFileSystem) obj;
		return Objects.equals(configuration, other.configuration)
			&& Objects.equals(fileStore, other.fileStore)
			&& Objects.equals(fileSystemProvider, other.fileSystemProvider);
	}

	private void ensureBucketExists(BucketDescriptor bucketDescriptor, S3Connector connector)
	{
		try
		{
			connector.createBucket(bucketDescriptor);
		}
		catch (BucketAlreadyOwnedByYouException x)
		{
			// Bucket already exists and it is granted to access, so there is nothing to do
		}
		catch (Exception x)
		{
			var bucketRecord = bucketDescriptor.bucketKey();
			var message = switch (x)
			{
				case BucketNameException exception -> "Illegal bucket name: %s.".formatted(bucketRecord.bucketName());
				case CredentialsException exception -> "Missing or wrong credentials.";
				case BucketAlreadyExistsException exception -> "You do not have the permission to access the bucket %s".formatted(bucketRecord.bucketName());
				default -> "Unpredicted issue. Possible a bug or a not supported feature?";
			};
			throw new IllegalArgumentException(message, x);
		}
	}
}
