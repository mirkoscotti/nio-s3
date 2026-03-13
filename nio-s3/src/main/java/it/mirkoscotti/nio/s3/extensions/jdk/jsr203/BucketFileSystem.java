package it.mirkoscotti.nio.s3.extensions.jdk.jsr203;

import java.io.Closeable;
import java.io.IOException;
import java.nio.file.FileStore;
import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.WatchService;
import java.nio.file.attribute.UserPrincipalLookupService;
import java.nio.file.spi.FileSystemProvider;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import it.mirkoscotti.nio.s3.configuration.BucketDescriptor;
import it.mirkoscotti.nio.s3.enums.PathSyntax;
import it.mirkoscotti.nio.s3.exceptions.BucketNameException;
import it.mirkoscotti.nio.s3.exceptions.CredentialsException;
import it.mirkoscotti.nio.s3.operations.AwsFacade;
import it.mirkoscotti.nio.s3.operations.ResourcesRegistry;

import software.amazon.awssdk.services.s3.model.BucketAlreadyExistsException;
import software.amazon.awssdk.services.s3.model.BucketAlreadyOwnedByYouException;

/**
 * @author mirko.scotti
 * @version Jan 24, 2025
 */
class BucketFileSystem
	extends FileSystem
{

	private final AtomicBoolean isClosing = new AtomicBoolean(false);

	private final ResourcesRegistry resourcesRegistry = new ResourcesRegistry();

	private final AwsFacade awsFacade;

	private final S3FileSystemProvider fileSystemProvider;

	private final BucketFileStore fileStore;

	BucketFileSystem(AwsFacade awsFacade,
					 BucketDescriptor bucketDescriptor,
					 S3FileSystemProvider fileSystemProvider)
	{
		this.awsFacade = awsFacade;
		this.fileSystemProvider = fileSystemProvider;
		var bucketName = ensureBucketExists(bucketDescriptor);
		fileStore = new BucketFileStore(awsFacade, bucketName);
		resourcesRegistry.registerResource(awsFacade);
	}

	@Override
	public FileSystemProvider provider()
	{
		return fileSystemProvider;
	}

	@Override
	public void close() throws IOException
	{
		isClosing.compareAndSet(false, true);
		resourcesRegistry.close();
		fileSystemProvider.closeFileSystem(this);
		isClosing.compareAndSet(true, false);
	}

	@Override
	public boolean isOpen()
	{
		return fileSystemProvider.isFileSystemOpen(this);
	}

	@Override
	public boolean isReadOnly()
	{
		return fileStore.isReadOnly();
	}

	@Override
	public String getSeparator()
	{
		return BucketDescriptor.PATH_SEPARATOR;
	}

	@Override
	public Iterable<Path> getRootDirectories()
	{
		return List.<Path>of(new BucketPath(this));
	}

	@Override
	public Iterable<FileStore> getFileStores()
	{
		return List.of(fileStore);
	}

	@Override
	public Set<String> supportedFileAttributeViews()
	{
		return Set.of("basic");
	}

	@Override
	public BucketPath getPath(String first, String... more)
	{
		return new BucketPath(this, first, more);
	}

	@Override
	public PathMatcher getPathMatcher(String syntaxAndPattern)
	{
		Objects.requireNonNull(syntaxAndPattern, () -> "Missing Glob or regex.");
		var array = Optional.of(syntaxAndPattern)
							.map(item -> item.split(":"))
							.filter(item -> item.length == 2)
							.orElseThrow(() -> new IllegalArgumentException("Pattern must be in the form 'syntax:pattern'."));
		var pattern = PathSyntax.of(array[0]).pattern(array[1]);
		return item -> pattern.matcher(item.toString()).matches();
	}

	@Override
	public UserPrincipalLookupService getUserPrincipalLookupService()
	{
		throw new UnsupportedOperationException("S3 is not a POSIX file system. Thus user/group do not make sense.");
	}

	@Override
	public WatchService newWatchService() throws IOException
	{
		var result = new DirectoryWatchService(awsFacade);
		resourcesRegistry.registerResource(result);
		return result;
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(fileStore, fileSystemProvider);
	}

	@Override
	public boolean equals(Object obj)
	{
		return obj instanceof BucketFileSystem other
			&& Objects.equals(fileSystemProvider, other.fileSystemProvider)
			&& Objects.equals(fileStore, other.fileStore);
	}

	void registerResource(Closeable closeable)
	{
		resourcesRegistry.registerResource(closeable);
	}

	void unregisterResource(Closeable closeable)
	{
		resourcesRegistry.unregisterResource(closeable);
	}

	boolean isClosing()
	{
		return isClosing.get();
	}

	String bucketName()
	{
		return fileStore.name();
	}

	/**
	 * The wrapper method of the {@link #connector} property.
	 *
	 * @return the value of the property
	 */
	AwsFacade awsFacade()
	{
		return awsFacade;
	}

	private String ensureBucketExists(BucketDescriptor bucketDescriptor)
	{
		try
		{
			awsFacade.createBucket(bucketDescriptor);
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
		return bucketDescriptor.bucketKey().bucketName();
	}
}
