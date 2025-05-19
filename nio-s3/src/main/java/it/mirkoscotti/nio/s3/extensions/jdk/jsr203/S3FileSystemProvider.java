package it.mirkoscotti.nio.s3.extensions.jdk.jsr203;

import it.mirkoscotti.nio.s3.configuration.BucketDescriptor;
import it.mirkoscotti.nio.s3.enums.BucketProperty;
import it.mirkoscotti.nio.s3.operations.S3Connector;
import it.mirkoscotti.nio.s3.records.BucketRecord;
import it.mirkoscotti.nio.s3.records.ConnectorRecord;

import java.io.IOException;
import java.net.URI;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.AccessMode;
import java.nio.file.CopyOption;
import java.nio.file.DirectoryStream;
import java.nio.file.DirectoryStream.Filter;
import java.nio.file.FileStore;
import java.nio.file.FileSystem;
import java.nio.file.FileSystemAlreadyExistsException;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.FileSystems;
import java.nio.file.LinkOption;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.FileAttributeView;
import java.nio.file.spi.FileSystemProvider;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * * This provider manages one file system for each S3 bucket on an AWS account or its emulator
 * LocalStack. Each file system is created at most once during the JVM life and internally cached,
 * so that it cannot be created twice. It can be created from URI and credentials for accessing the
 * bucket. Region is mandatory but, if not specified, <code>us-east-1</code> is assumed. URIs must
 * be compliant with one of the following formats:
 * <ul>
 * <li><code>s3://bucket-name</code>
 * <p>
 * In this case, the {@link FileSystems#newFileSystem(URI, Map)} must be invoked and the credentials
 * for accessing the bucket must be provided in the environment map through the
 * {@link S3Property#ACCESS_KEY access-key} and {@link S3Property#SECRET_KEY secret-key} properties.
 * If the property {@link S3Property#ENDPOINT} is also specified in the same map, the bucket is
 * created, or it is required to be existing, on the specific LocalStack instance instead of an AWS
 * account.
 * <li><code>s3://access-key:secret-key@bucket-name/<code>
 * <p>
 * In this case, since credentials are directly provided within the URI, the file system can be
 * created either invoking {@link FileSystems#newFileSystem(URI, Map)} or {@link Paths#get(URI)}. If
 * this last API is used, necessarily it is not possible to access LocalStack. If the first one is
 * invoked, credentials eventually specified in the environment properties are ignored because the
 * ones specified in the URI are used.
 * <li><code>s3://access-key:secret-key@host-name/bucket-name/<code>
 * <p>
 * This is for accessing LocalStack from the {@link Paths#get(URI)} API. If
 * {@link FileSystems#newFileSystem(URI, Map)} is invoked and the {@link S3Property#ENDPOINT}
 * property is specified, it is ignored because the host name specified in the URI is used.
 * </ul>
 * The bucket name must match the
 * <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/bucketnamingrules.html">bucket
 * naming rules</a>. The following additional rules for managing the bucket are also applied:
 * <p>
 * <ul>
 * <li>if the bucket does not exist in AWS or LocalStack, a new one is attempted to be created in
 * the account the provided credentials belong to before creating the file system.
 * <li>if the bucket already exists in AWS and the provided credentials are granted for it, a new
 * file system is created if never created before in the current JVM.
 * <li>if the bucket already exists in AWS and the provided credentials are not granted for it, the
 * corresponding file system is not created and an exception is thrown.
 * </ul>
 *
 * @author mirko.scotti
 * @version Apr 28, 2025
 */
public class S3FileSystemProvider
	extends FileSystemProvider
{

	private static final Map<BucketRecord, BucketFileSystem> FILE_SYSTEMS_CACHE = new ConcurrentHashMap<>();

	private static final Map<ConnectorRecord, S3Connector> CONNECTORS_CACHE = new ConcurrentHashMap<>();

	@Override
	public String getScheme()
	{
		return BucketProperty.SCHEME;
	}

	/**
	 * Creates a new file system representing the bucket specified in the given URI as authority or
	 * first path part depending on whether the target platform is AWS or LocalStack. The key that
	 * uniquely identifies a file system is the tuple [<i>bucket name</i>, <i>access key</i>,
	 * <i>endpoint</i>]. In this key, bucket name and access key are mandatory, while the endpoint
	 * is optional, meaning that if not specified, AWS's default endpoint is assumed.
	 * <p>
	 * This means that if a file system is created from <code>s3://bucket-name/<code> with
	 * credentials <i>access-key</i> and <i>secret-key</i> passed in the environment map, a second
	 * invocation to this method passing <code>s3://access-key:secret-key@bucket-name/<code> will
	 * raise a {@link FileSystemAlreadyExistsException} even if the URI is formally different from
	 * the previous one. Same if also the LocalStack endpoint is configured.
	 * <p>
	 * Given that the platform (AWS or LocalStack) is part of the key, it is formally possible to
	 * create two file systems within the same JVM having the same bucket name and credentials, one
	 * on AWS and one on LocalStack. By the way it is strongly discouraged, because the use of
	 * LocalStak is intended only for testing purpose while AWS is production target.
	 *
	 * @throws NullPointerException
	 *             if the URI is not specified
	 * @throws IllegalArgumentException
	 *             if the bucket name does not match the AWS rules or if the credentials are not
	 *             provided or do not have the permissions to access the bucket
	 * @throws FileSystemAlreadyExistsException
	 *             if the file system corresponding to the given bucket name and platform has
	 *             already been created within this JVM. In this case, it is accessible through the
	 *             {@link #getFileSystem(URI)}
	 */
	@Override
	public FileSystem newFileSystem(URI uri, Map<String, ?> env) throws IOException
	{
		var bucketDescriptor = new BucketDescriptor(uri, env);
		var bucketKey = bucketDescriptor.bucketKey();
		if (FILE_SYSTEMS_CACHE.containsKey(bucketKey))
		{
			var message = "File system for bucket %s already existing.";
			throw new FileSystemAlreadyExistsException(message.formatted(bucketKey.bucketName()));
		}
		return createFileSystem(bucketDescriptor);
	}

	@Override
	public FileSystem getFileSystem(URI uri)
	{
		var bucketKey = new BucketDescriptor(uri).bucketKey();
		return Optional.of(bucketKey)
					   .map(FILE_SYSTEMS_CACHE::get)
					   .orElseThrow(() -> new FileSystemNotFoundException("File system for bucket %s not loaded yet.".formatted(bucketKey.bucketName())));
	}

	@Override
	public Path getPath(URI uri)
	{
		var bucketDescriptor = new BucketDescriptor(uri);
		return Optional.ofNullable(FILE_SYSTEMS_CACHE.get(bucketDescriptor.bucketKey()))
					   .orElseGet(() -> createFileSystem(bucketDescriptor))
					   .getPath(uri.getPath());
	}

	@Override
	public SeekableByteChannel newByteChannel(Path path,
											  Set<? extends OpenOption> options,
											  FileAttribute<?>... attrs)
		throws IOException
	{
		if (path instanceof BucketPath bucketPath)
		{
			var connector = bucketPath.getFileSystem().connector();
			return new BucketSeekableByteChannel(connector, bucketPath, options);
		}
		throw invalidPath(path);
	}

	@Override
	public DirectoryStream<Path> newDirectoryStream(Path dir, Filter<? super Path> filter)
		throws IOException
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void createDirectory(Path dir, FileAttribute<?>... attrs) throws IOException
	{
		// TODO Auto-generated method stub

	}

	@Override
	public void delete(Path path) throws IOException
	{
		// TODO Auto-generated method stub

	}

	@Override
	public void copy(Path source, Path target, CopyOption... options) throws IOException
	{
		// TODO Auto-generated method stub

	}

	@Override
	public void move(Path source, Path target, CopyOption... options) throws IOException
	{
		// TODO Auto-generated method stub

	}

	@Override
	public boolean isSameFile(Path path, Path path2) throws IOException
	{
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isHidden(Path path) throws IOException
	{
		return false;
	}

	@Override
	public FileStore getFileStore(Path path) throws IOException
	{
		if (path instanceof BucketPath bucketPath)
		{
			return bucketPath.getFileSystem().getFileStores().iterator().next();
		}
		throw invalidPath(path);
	}

	@Override
	public void checkAccess(Path path, AccessMode... modes) throws IOException
	{
		// TODO Auto-generated method stub

	}

	@Override
	public <V extends FileAttributeView> V getFileAttributeView(Path path,
																Class<V> type,
																LinkOption... options)
	{
		if (path instanceof BucketPath bucketPath)
		{
			var list = List.<FileAttributeViewFactory<?>>of(new FileAttributeViewFactory<>(ObjectBasicFileAttributeView.class,
																						   this::createObjectBasicFileAttributeView));
			@SuppressWarnings("unchecked")
			var result = (V) list.stream()
								 .filter(item -> item.fileAttributeViewType() == type)
								 .map(item -> item.factory().apply(bucketPath))
								 .findFirst()
								 .orElse(null);
			return result;
		}
		throw invalidPath(path);
	}

	@Override
	@SuppressWarnings("unchecked")
	public <A extends BasicFileAttributes> A readAttributes(Path path,
															Class<A> type,
															LinkOption... options)
		throws IOException
	{
		if (path instanceof BucketPath bucketPath)
		{
			var map = new HashMap<Class<? extends BasicFileAttributes>, Class<? extends BasicFileAttributeView>>();
			map.put(ObjectBasicFileAttributes.class, ObjectBasicFileAttributeView.class);
			var fileAttributeViewType = Optional.ofNullable(type)
												.filter(map::containsKey)
												.map(map::get)
												.orElseThrow(() -> unexpectedFileAttributes(type));
			var fileAttributeView = getFileAttributeView(bucketPath,
														 fileAttributeViewType,
														 options);
			return (A) fileAttributeView.readAttributes();
		}
		throw invalidPath(path);
	}

	@Override
	public Map<String, Object> readAttributes(Path path, String attributes, LinkOption... options)
		throws IOException
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setAttribute(Path path, String attribute, Object value, LinkOption... options)
		throws IOException
	{
		// TODO Auto-generated method stub

	}

	private BucketFileSystem createFileSystem(BucketDescriptor bucketDescriptor)
	{
		var connectorKey = bucketDescriptor.connectorKey();
		var connector = CONNECTORS_CACHE.computeIfAbsent(connectorKey, this::createConnector);
		var result = new BucketFileSystem(connector, bucketDescriptor, this);
		var bucketKey = bucketDescriptor.bucketKey();
		FILE_SYSTEMS_CACHE.put(bucketKey, result);
		return result;
	}

	private S3Connector createConnector(ConnectorRecord connectorKey)
	{
		var credentials = connectorKey.credentials();
		var connectorBuilder = S3Connector.create()
										  .withCredentials(credentials.accessKey(),
														   credentials.secretKey());
		connectorKey.endpoint().map(URI::create).ifPresent(connectorBuilder::withEndpoint);
		return connectorBuilder.build();
	}

	private ObjectBasicFileAttributeView createObjectBasicFileAttributeView(BucketPath bucketPath)
	{
		var fileSystem = bucketPath.getFileSystem();
		var connector = fileSystem.connector();
		var bucketName = fileSystem.getFileStores().iterator().next().name();
		var objectKey = bucketPath.toString();
		return new ObjectBasicFileAttributeView(connector, bucketName, objectKey);
	}

	private RuntimeException invalidPath(Path path)
	{
		return Optional.ofNullable(path)
					   .map(this::unsupportedPath)
					   .orElseGet(() -> new NullPointerException("Missing path"));
	}

	private RuntimeException unsupportedPath(Path path)
	{
		var pathType = path.getClass().getName();
		var pathTemplate = "Expected path of type %s. Found: %s.";
		var pathMessage = pathTemplate.formatted(BucketPath.class.getName(), pathType);
		return new UnsupportedOperationException(pathMessage);
	}

	private RuntimeException unexpectedFileAttributes(Class<? extends BasicFileAttributes> type)
	{
		var attributesTemplate = "Expected attributes of type %s. Found: %s.";
		var attributesMessage = attributesTemplate.formatted(ObjectBasicFileAttributes.class.getName(),
															 type.getName());
		return new UnsupportedOperationException(attributesMessage);
	}

	private static record FileAttributeViewFactory<F extends FileAttributeView>(Class<F> fileAttributeViewType,
																				Function<BucketPath, F> factory)
	{

	}
}
