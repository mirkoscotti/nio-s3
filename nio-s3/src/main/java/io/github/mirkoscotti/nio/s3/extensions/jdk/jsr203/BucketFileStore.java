package io.github.mirkoscotti.nio.s3.extensions.jdk.jsr203;

import java.io.IOException;
import java.nio.file.FileStore;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.FileAttributeView;
import java.nio.file.attribute.FileStoreAttributeView;
import java.util.Objects;
import java.util.Optional;

import io.github.mirkoscotti.nio.s3.enums.BucketProperty;
import io.github.mirkoscotti.nio.s3.functions.Try;
import io.github.mirkoscotti.nio.s3.operations.AwsFacade;

/**
 * According to the {@link FileStore} specification, this one represents a single bucket in the same
 * account file system.
 *
 * @author mirko.scotti
 * @version Oct 22, 2024
 */
public class BucketFileStore
	extends FileStore
{

	private final AwsFacade awsFacade;

	private final String bucketName;

	/**
	 * @param bucketName
	 */
	BucketFileStore(AwsFacade awsFacade, String bucketName)
	{
		this.awsFacade = Objects.requireNonNull(awsFacade, () -> "Missing AWS connector.");
		this.bucketName = Objects.requireNonNull(bucketName, () -> "Missing bucket name.");
	}

	@Override
	public String name()
	{
		return bucketName;
	}

	@Override
	public String type()
	{
		return "AWS S3 Bucket";
	}

	@Override
	public boolean isReadOnly()
	{
		return awsFacade.isBucketReadOnly(bucketName);
	}

	@Override
	public long getTotalSpace() throws IOException
	{
		return Long.MAX_VALUE;
	}

	@Override
	public long getUsableSpace() throws IOException
	{
		return Long.MAX_VALUE;
	}

	@Override
	public long getUnallocatedSpace() throws IOException
	{
		return Long.MAX_VALUE;
	}

	@Override
	public boolean supportsFileAttributeView(Class<? extends FileAttributeView> type)
	{
		return type == BasicFileAttributeView.class || type == ObjectBasicFileAttributeView.class;
	}

	@Override
	public boolean supportsFileAttributeView(String name)
	{
		return ObjectBasicFileAttributeView.BASIC_FILE_ATTRIBUTE_VIEW.equals(name);
	}

	@Override
	public <V extends FileStoreAttributeView> V getFileStoreAttributeView(Class<V> type)
	{
		return Optional.ofNullable(type)
					   .filter(BucketFileStoreAttributeView.class::isAssignableFrom)
					   .map(item -> Try.to(() -> item.getConstructor(AwsFacade.class, String.class)
													 .newInstance(awsFacade, bucketName))
									   .get())
					   .orElse(null);
	}

	@Override
	public Object getAttribute(String attribute) throws IOException
	{
		return BucketProperty.of(attribute)
							 .map(getFileStoreAttributeView(BucketFileStoreAttributeView.class)::get)
							 .orElse(null);
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(awsFacade, bucketName);
	}

	@Override
	public boolean equals(Object obj)
	{
		return obj instanceof BucketFileStore other
			&& Objects.equals(awsFacade, other.awsFacade)
			&& Objects.equals(bucketName, other.bucketName);
	}
}
