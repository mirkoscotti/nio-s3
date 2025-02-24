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
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Stream;

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

	private final S3Connector connector;

	BucketFileSystem(BucketFileSystemProvider fileSystemProvider, BucketDescriptor bucketDescriptor)
	{
		this.fileSystemProvider = fileSystemProvider;
		var credentials = bucketDescriptor.credentials();
		var builder = S3Connector.create()
								 .withCredentials(credentials.accessKey(), credentials.secretKey());
		bucketDescriptor.bucketKey().endpoint().map(URI::create).ifPresent(builder::withEndpoint);
		connector = builder.build();
		bucketDescriptor.configuration().forEach(this::addProperty);
	}

	@Override
	public FileSystemProvider provider()
	{
		// TODO Auto-generated method stub
		return null;
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
		// TODO Auto-generated method stub
		return null;
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
		// TODO Auto-generated method stub
		return null;
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

	private void addProperty(String key, Object object)
	{
		BucketProperty.of(key).ifPresent(item -> addProperty(item, object));
	}

	private void addProperty(BucketProperty property, Object object)
	{
		Stream.<Supplier<?>>of(() -> object,
							   () -> System.getProperty(property.toProperty()),
							   () -> System.getenv(property.toProperty()),
							   property::defaultValue)
			  .map(Supplier::get)
			  .filter(Objects::nonNull)
			  .findFirst()
			  .map(Object::toString)
			  .ifPresent(item -> configuration.put(property, item));
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
