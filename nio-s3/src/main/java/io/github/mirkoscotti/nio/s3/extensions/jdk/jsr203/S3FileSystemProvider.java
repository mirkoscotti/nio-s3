package io.github.mirkoscotti.nio.s3.extensions.jdk.jsr203;

import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URI;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.AccessMode;
import java.nio.file.ClosedFileSystemException;
import java.nio.file.CopyOption;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.DirectoryStream;
import java.nio.file.DirectoryStream.Filter;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.FileStore;
import java.nio.file.FileSystem;
import java.nio.file.FileSystemAlreadyExistsException;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.ProviderMismatchException;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.FileAttributeView;
import java.nio.file.spi.FileSystemProvider;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.github.mirkoscotti.nio.s3.configuration.BucketDescriptor;
import io.github.mirkoscotti.nio.s3.enums.BucketProperty;
import io.github.mirkoscotti.nio.s3.enums.CopyFlag;
import io.github.mirkoscotti.nio.s3.enums.ObjectAccess;
import io.github.mirkoscotti.nio.s3.functions.Case;
import io.github.mirkoscotti.nio.s3.functions.Condition;
import io.github.mirkoscotti.nio.s3.functions.Evaluator;
import io.github.mirkoscotti.nio.s3.functions.Expression;
import io.github.mirkoscotti.nio.s3.functions.Try;
import io.github.mirkoscotti.nio.s3.helpers.ExceptionHelper;
import io.github.mirkoscotti.nio.s3.operations.AwsFacade;
import io.github.mirkoscotti.nio.s3.operations.FileTransfer;
import io.github.mirkoscotti.nio.s3.records.AwsRecord;
import io.github.mirkoscotti.nio.s3.records.BucketRecord;

/**
 * This provider manages one file system for each S3 bucket on an AWS account or its emulator
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
 * <li><code>s3://access-key:secret-key@bucket-name<code>
 * <p>
 * In this case, since credentials are directly provided within the URI, the file system can be
 * created either invoking {@link FileSystems#newFileSystem(URI, Map)} or {@link Paths#get(URI)}. If
 * this last API is used, necessarily it is not possible to access LocalStack. If the first one is
 * invoked, credentials eventually specified in the environment properties are ignored because the
 * ones specified in the URI are used.
 * <li><code>s3://access-key:secret-key@host-name/bucket-name<code>
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

	private static final Map<AwsRecord, AwsFacade> FACADES_CACHE = new ConcurrentHashMap<>();

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

	/**
	 * Retrieves an existing file system previously created for the bucket identified by the given
	 * URI. The bucket is resolved from the URI authority or from its first path segment, depending
	 * on whether the target platform is AWS or LocalStack.
	 *
	 * @param uri
	 *            the URI whose authority or first path segment identifies the target bucket; must
	 *            not be {@code null}
	 * @return the {@link BucketFileSystem} associated with the given bucket
	 * @throws FileSystemNotFoundException
	 *             if no file system for the given bucket has been created yet within this JVM
	 */
	@Override
	public FileSystem getFileSystem(URI uri)
	{
		var bucketKey = new BucketDescriptor(uri).bucketKey();
		return Optional.of(bucketKey)
					   .map(FILE_SYSTEMS_CACHE::get)
					   .orElseThrow(() -> new FileSystemNotFoundException("File system for bucket %s not loaded yet.".formatted(bucketKey.bucketName())));
	}

	/**
	 * Creates a path from the given URI. If the file system this path belongs to still has not been
	 * created before in the current JVM, it is created too. In this case, the given URI must
	 * contain all the required information such as host name and credentials.
	 *
	 * @param uri
	 *            the URI identifying both the target bucket and the object path within it; must not
	 *            be {@code null}
	 * @return the {@link BucketPath} representing the S3 object identified by the given URI
	 * @throws IllegalArgumentException
	 *             if the URI does not contain the credentials required to access the bucket
	 */
	@Override
	public Path getPath(URI uri)
	{
		var bucketDescriptor = new BucketDescriptor(uri);
		return Optional.ofNullable(FILE_SYSTEMS_CACHE.get(bucketDescriptor.bucketKey()))
					   .orElseGet(() -> createFileSystem(bucketDescriptor))
					   .getPath(uri.getPath());
	}

	/**
	 * Opens a channel to access or create an S3 object corresponding to the given path. The channel
	 * is registered as an open resource on the owning file system so that it can be properly closed
	 * when the file system itself is closed.
	 *
	 * @param path
	 *            the path to the S3 object to open or create
	 * @param options
	 *            options specifying how the object is opened
	 * @param attrs
	 *            an optional list of file attributes to set atomically when creating the object;
	 *            currently ignored because S3 object metadata cannot be set after creation
	 * @return a new channel instance connected to the given S3 object
	 * @throws IOException
	 *             if an I/O error occurs opening or creating the object
	 * @throws ProviderMismatchException
	 *             if the given path is not a {@link BucketPath}
	 * @throws NullPointerException
	 *             if the path is not specified
	 */
	@Override
	public SeekableByteChannel newByteChannel(Path path,
											  Set<? extends OpenOption> options,
											  FileAttribute<?>... attrs)
		throws IOException
	{
		var bucketPath = validatePath(path);
		var result = new BucketSeekableByteChannel(bucketPath, options);
		bucketPath.getFileSystem().registerResource(result);
		return result;
	}

	/**
	 * Opens a stream on the given directory to iterate over the entries it contains. Each entry
	 * that passes the given filter is included in the stream. The stream is registered as an open
	 * resource on the owning file system so that it can be properly closed when the file system
	 * itself is closed.
	 *
	 * @param dir
	 *            the path to the directory to open
	 * @param filter
	 *            the filter to exclude some of the files in the directory
	 * @return a new stream instance for the given directory
	 * @throws IOException
	 *             if an I/O error occurs opening the directory
	 * @throws ProviderMismatchException
	 *             if the given path is not a {@link BucketPath}
	 * @throws NullPointerException
	 *             if the path is not specified
	 */
	@Override
	public DirectoryStream<Path> newDirectoryStream(Path dir, Filter<? super Path> filter)
		throws IOException
	{
		var bucketPath = validatePath(dir);
		var result = new BucketDirectoryStream(bucketPath, filter);
		bucketPath.getFileSystem().registerResource(result);
		return result;
	}

	/**
	 * Creates a new directory at the given path by writing an empty object whose key ends with the
	 * S3 path separator. The root directory of a bucket cannot be created because it is implicitly
	 * represented by the bucket itself. All parent directories in the path must exist before this
	 * method is called.
	 *
	 * @param dir
	 *            the path of the directory to create; must be a {@link BucketPath} and must not
	 *            refer to the root of the bucket
	 * @param attrs
	 *            an optional list of file attributes to set atomically when creating the directory;
	 *            currently ignored because S3 object metadata cannot be set after creation
	 * @throws IOException
	 *             if an I/O error occurs
	 * @throws FileAlreadyExistsException
	 *             if a directory or object already exists at the given path, or if the given path
	 *             is the root directory of the bucket
	 * @throws ProviderMismatchException
	 *             if the given path is not a {@link BucketPath}
	 */
	@Override
	public void createDirectory(Path dir, FileAttribute<?>... attrs) throws IOException
	{
		var bucketPath = validatePath(dir);
		var optional = Optional.of(bucketPath);
		var fileSystem = optional.filter(Predicate.not(BucketPath::isRootDirectory))
								 .orElseThrow(() -> new FileAlreadyExistsException(bucketPath.toString()))
								 .getFileSystem();
		var path = optional.map(BucketPath::toString)
						   .filter(Predicate.not(item -> item.endsWith(BucketDescriptor.PATH_SEPARATOR)))
						   .map(item -> item.concat(BucketDescriptor.PATH_SEPARATOR))
						   .map(fileSystem::getPath)
						   .map(BucketPath::toAbsolutePath)
						   .orElse(bucketPath);
		var reference = new AtomicReference<Exception>(new FileAlreadyExistsException(path.toString()));
		Try.to(() -> readAttributes(path, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS))
		   .onCatch(item -> reference.set(null))
		   .run();
		var exception = Optional.ofNullable(reference.get())
								.orElseGet(() -> checkIfPathExists(path));
		Case.of(exception)
			.when(Condition.not(Objects::isNull))
			.thenHandle(ExceptionHelper::throwIoException);
		fileSystem.awsFacade()
				  .writeObject(fileSystem.getFileStores().iterator().next().name(),
							   bucketPath.toString());
	}

	/**
	 * Deletes the S3 object or virtual directory identified by the given path. The root directory
	 * of a bucket cannot be deleted. If the path refers to a non-empty directory, the deletion is
	 * refused to avoid accidental data loss.
	 *
	 * @param path
	 *            the path of the object or directory to delete; must be a {@link BucketPath}
	 * @throws IOException
	 *             if an I/O error occurs during deletion
	 * @throws UnsupportedOperationException
	 *             if the path refers to the root directory of the bucket
	 * @throws DirectoryNotEmptyException
	 *             if the path refers to a non-empty virtual directory
	 * @throws ProviderMismatchException
	 *             if the given path is not a {@link BucketPath}
	 * @throws NullPointerException
	 *             if the path is not specified
	 */
	@Override
	public void delete(Path path) throws IOException
	{
		var realPath = Optional.of(validatePath(path))
							   .filter(Predicate.not(BucketPath::isRootDirectory))
							   .orElseThrow(() -> new UnsupportedOperationException("Root directory cannot be deleted."))
							   .toRealPath(LinkOption.NOFOLLOW_LINKS);
		var fileSystem = realPath.getFileSystem();
		var awsFacade = fileSystem.awsFacade();
		var bucketName = getFileStore(realPath).name();
		var key = realPath.toString();
		Case.of(readAttributes(realPath, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS))
			.when(item -> item.isDirectory() && awsFacade.isNotEmptyDirectory(bucketName, key))
			.thenThrow(() -> new DirectoryNotEmptyException(key));
		awsFacade.deleteObject(bucketName, key);
	}

	/**
	 * Copies an S3 object from the source path to the target path. If source and target refer to
	 * the same object , this method does nothing. By default, the copy fails if the target already
	 * exists unless <code>REPLACE_EXISTING</code> is configured to overwrite it.
	 *
	 * @param source
	 *            the path to the source object
	 * @param target
	 *            the path to the target object
	 * @param options
	 *            options specifying how the copy should be performed
	 * @throws IOException
	 *             if an I/O error occurs during the copy
	 * @throws FileAlreadyExistsException
	 *             if the target already exists and <code>REPLACE_EXISTING</code> option is not
	 *             specified
	 * @throws ProviderMismatchException
	 *             if either path is not a {@link BucketPath}
	 */
	@Override
	public void copy(Path source, Path target, CopyOption... options) throws IOException
	{
		Evaluator.when(Expression.not(() -> Files.isSameFile(source, target)))
				 .thenExecute(() -> executeCopy(source, target, options));
	}

	/**
	 * Moves an S3 object from the source path to the target path. This operation is implemented as
	 * a {@link #copy(Path, Path, CopyOption...) copy} followed by a {@link #delete(Path) delete} of
	 * the source. The same options and constraints that apply to {@link #copy} apply here as well.
	 *
	 * @param source
	 *            the path to the source object
	 * @param target
	 *            the path to the target object
	 * @param options
	 *            options specifying how the move should be performed
	 * @throws IOException
	 *             if an I/O error occurs during the copy or the delete
	 * @throws FileAlreadyExistsException
	 *             if the target already exists and <code>REPLACE_EXISTING</code> is not specified
	 * @throws ProviderMismatchException
	 *             if either path is not a {@link BucketPath}
	 */
	@Override
	public void move(Path source, Path target, CopyOption... options) throws IOException
	{
		copy(source, target, options);
		delete(source);
	}

	/**
	 * Tells whether two paths locate the same S3 object. Two paths are considered to refer to the
	 * same object if they are equal or if the absolute form of the first path is equal to the
	 * absolute form of the second path.
	 *
	 * @param path
	 *            the source path
	 * @param path2
	 *            the target path
	 * @return true if and only if both paths locate the same S3 object
	 * @throws IOException
	 *             if an I/O error occurs
	 * @throws ProviderMismatchException
	 *             if path is not a {@link BucketPath}
	 * @throws NullPointerException
	 *             if the target path is not specified
	 */
	@Override
	public boolean isSameFile(Path path, Path path2) throws IOException
	{
		var source = validatePath(path);
		var target = Objects.requireNonNull(path2, () -> "Missing target path.");
		return source.equals(path2)
			|| source.toRealPath(LinkOption.NOFOLLOW_LINKS).equals(target.toAbsolutePath());
	}

	/**
	 * S3 does not have a native concept of hidden.
	 *
	 * @param path
	 *            the path to the S3 object
	 * @return false
	 * @throws IOException
	 *             if an I/O error occurs
	 * @throws ProviderMismatchException
	 *             if the given path is not a {@link BucketPath}
	 */
	@Override
	public boolean isHidden(Path path) throws IOException
	{
		validatePath(path);
		return false;
	}

	/**
	 * Returns the unique file store of the file system the given path belongs to.
	 *
	 * @param path
	 *            the path whose containing bucket is to be returned
	 * @return the {@link FileStore} for the bucket that contains the given path
	 * @throws IOException
	 *             if an I/O error occurs
	 * @throws ProviderMismatchException
	 *             if the given path is not a {@link BucketPath}
	 */
	@Override
	public FileStore getFileStore(Path path) throws IOException
	{
		var bucketPath = validatePath(path);
		return bucketPath.getFileSystem().getFileStores().iterator().next();
	}

	/**
	 * Checks whether the current credentials have the permission corresponding to the given access
	 * modes on the S3 object identified by the given path.
	 *
	 * @param path
	 *            the path to the S3 object to check
	 * @param modes
	 *            the access modes to check; an empty array checks only for the existence of the
	 *            object
	 * @throws IOException
	 *             if an I/O error occurs or access is denied
	 * @throws ProviderMismatchException
	 *             if the given path is not a {@link BucketPath}
	 * @throws NullPointerException
	 *             if the path is not specified
	 */
	@Override
	public void checkAccess(Path path, AccessMode... modes) throws IOException
	{
		var bucketPath = validatePath(path);
		var fileSystem = bucketPath.getFileSystem();
		var bucketName = getFileStore(bucketPath).name();
		var objectKey = bucketPath.toString();
		var awsFacade = fileSystem.awsFacade();
		ObjectAccess.check(awsFacade, bucketName, objectKey, modes);
	}

	/**
	 * Returns a file attribute view of a given type for the S3 object identified by the given path.
	 *
	 * @param <V>
	 *            the type of the file attribute view
	 * @param path
	 *            the path to the S3 object
	 * @param type
	 *            the class of the desired attribute view
	 * @param options
	 *            options indicating how symbolic links are handled; currently unused because S3
	 *            does not support symbolic links
	 * @return a file attribute view of the specified type, or null if the view type is not
	 *         supported
	 * @throws ProviderMismatchException
	 *             if the given path is not a {@link BucketPath}
	 */
	@Override
	public <V extends FileAttributeView> V getFileAttributeView(Path path,
																Class<V> type,
																LinkOption... options)
	{
		var bucketPath = validatePath(path);
		var factory = new FileAttributeViewFactory<>(ObjectBasicFileAttributeView.class,
													 this::createBasicFileAttributeView);
		@SuppressWarnings("unchecked")
		var result = (V) List.<FileAttributeViewFactory<?>>of(factory)
							 .stream()
							 .filter(item -> item.fileAttributeViewType() == type)
							 .map(item -> item.factory().apply(bucketPath))
							 .findFirst()
							 .orElse(null);
		return result;
	}

	/**
	 * Reads the attributes of an S3 object as a bulk operation.
	 *
	 * @param <A>
	 *            the type of the file attributes
	 * @param path
	 *            the path to the S3 object
	 * @param type
	 *            the class of the desired attribute object
	 * @param options
	 *            currently unused
	 * @return the file attributes of the given S3 object
	 * @throws IOException
	 *             if an I/O error occurs reading the attributes
	 * @throws UnsupportedOperationException
	 *             if the requested attribute type is not supported
	 * @throws ProviderMismatchException
	 *             if the given path is not a {@link BucketPath}
	 */
	@Override
	@SuppressWarnings("unchecked")
	public <A extends BasicFileAttributes> A readAttributes(Path path,
															Class<A> type,
															LinkOption... options)
		throws IOException
	{
		var bucketPath = validatePath(path);
		var map = new HashMap<Class<? extends BasicFileAttributes>, Class<? extends BasicFileAttributeView>>();
		map.put(BasicFileAttributes.class, ObjectBasicFileAttributeView.class);
		map.put(ObjectBasicFileAttributes.class, ObjectBasicFileAttributeView.class);
		var fileAttributeViewType = Optional.ofNullable(type)
											.filter(map::containsKey)
											.map(map::get)
											.orElseThrow(() -> unexpectedFileAttributes(type));
		var fileAttributeView = getFileAttributeView(bucketPath, fileAttributeViewType, options);
		return (A) fileAttributeView.readAttributes();
	}

	/**
	 * Reads a set of file attributes of an S3 object as a bulk operation, returning them as a
	 * name-to-value map. The attributes string specifies the desired attribute names, optionally
	 * preceded by the view name separated by a colon (e.g. <code>"basic:size,isDirectory"<code> or
	 * simply <code>"size,isDirectory"<code>). The wildcard selects all available attributes. Only
	 * the basic view is currently supported.
	 *
	 * @param path
	 *            the path to the S3 object
	 * @param attributes
	 *            a comma-separated list of attribute names, optionally prefixed by the view name
	 *            followed by a colon; use "*" to request all attributes
	 * @param options
	 *            options indicating how symbolic links are handled; currently unused
	 * @return a map from attribute name to attribute value
	 * @throws IOException
	 *             if an I/O error occurs reading the attributes
	 * @throws IllegalArgumentException
	 *             if the attributes string is malformed (e.g. contains more than one colon)
	 * @throws UnsupportedOperationException
	 *             if the requested view is not supported
	 * @throws ProviderMismatchException
	 *             if the given path is not a {@link BucketPath}
	 */
	@Override
	public Map<String, Object> readAttributes(Path path, String attributes, LinkOption... options)
		throws IOException
	{
		var list = Optional.ofNullable(attributes)
						   .map(item -> item.split(":"))
						   .stream()
						   .flatMap(Stream::of)
						   .map(String::trim)
						   .filter(Predicate.not(String::isBlank))
						   .toList();
		var view = switch (list.size())
		{
			case 0 -> new View(ObjectBasicFileAttributeView.BASIC_FILE_ATTRIBUTE_VIEW, "");
			case 1 -> new View(ObjectBasicFileAttributeView.BASIC_FILE_ATTRIBUTE_VIEW, attributes);
			case 2 -> new View(list.get(0), list.get(1));
			default -> throw new IllegalArgumentException("Malformed attributes. Expected format: [view-name:]a1,a2,...");
		};
		var basicFileAttributes = switch (view.name())
		{
			case ObjectBasicFileAttributeView.BASIC_FILE_ATTRIBUTE_VIEW -> readAttributes(path,
																						  ObjectBasicFileAttributes.class,
																						  options);
			default -> throw new UnsupportedOperationException("Unsupported file attributes view: %s".formatted(view.name));
		};
		var excludedMethods = Stream.of(Object.class.getDeclaredMethods())
									.map(Method::getName)
									.collect(Collectors.toSet());
		var methods = Stream.of(basicFileAttributes.getClass().getDeclaredMethods())
							.filter(Predicate.<Method>not(item -> excludedMethods.contains(item.getName())))
							.filter(item -> Modifier.isPublic(item.getModifiers()))
							.filter(Predicate.not(Method::isBridge))
							.collect(Collectors.toSet());
		var includedMethods = new HashSet<>();
		includedMethods.addAll(view.attributes.equals("*")
			? methods.stream().map(Method::getName).toList()
			: Stream.of(view.attributes.split(","))
					.filter(Predicate.not(String::isEmpty))
					.toList());
		var reference = new AtomicReference<Exception>();
		var result = methods.stream()
							.filter(item -> includedMethods.contains(item.getName()))
							.collect(Collectors.toMap(Method::getName,
													  item -> Try.to(() -> item.invoke(basicFileAttributes))
																 .onCatch(reference::set)
																 .get()));
		Case.of(reference.get())
			.when(Objects::nonNull)
			.thenHandle(ExceptionHelper::throwIoException);
		return result;
	}

	/**
	 * S3 object metadata is immutable
	 *
	 * @throws UnsupportedOperationException
	 *             always
	 */
	@Override
	public void setAttribute(Path path, String attribute, Object value, LinkOption... options)
		throws IOException
	{
		throw new UnsupportedOperationException("Metadata of an S3 object cannot be modified once it has been created.");
	}

	/**
	 * Removes the given file system from the internal cache, effectively closing it. If the
	 * underlying {@link AwsFacade} is no longer referenced by any other open file system, it is
	 * also removed from the facades cache to release the associated AWS client resources.
	 * <p>
	 * This method is intended to be called only by {@link BucketFileSystem#close()} and must not be
	 * invoked directly by client code.
	 *
	 * @param fileSystem
	 *            the file system to close
	 * @throws IOException
	 *             if an I/O error occurs during the close operation
	 * @throws ClosedFileSystemException
	 *             if the file system has already been closed
	 */
	void closeFileSystem(BucketFileSystem fileSystem) throws IOException
	{
		var awsFacade = fileSystem.awsFacade();
		var awsRecord = awsFacade.awsRecord();
		var bucketRecord = new BucketRecord(awsRecord.endpoint(), fileSystem.bucketName());
		Case.of(FILE_SYSTEMS_CACHE.remove(bucketRecord))
			.when(Objects::isNull)
			.thenThrow(ClosedFileSystemException::new);
		FILE_SYSTEMS_CACHE.values()
						  .stream()
						  .map(BucketFileSystem::awsFacade)
						  .filter(awsFacade::equals)
						  .findAny()
						  .ifPresentOrElse(item ->
						  {
						  }, () -> FACADES_CACHE.remove(awsRecord));
	}

	/**
	 * Tells whether the given file system is still open, i.e. whether it is currently present in
	 * the internal cache. A file system is considered open from the moment it is successfully
	 * created until {@link #closeFileSystem(BucketFileSystem)} is called on it.
	 * <p>
	 * This method is intended to be called only by {@link BucketFileSystem#isOpen()} and must not
	 * be invoked directly by client code.
	 *
	 * @param fileSystem
	 *            the file system whose open state is to be checked
	 * @return true if the file system is open, false otherwise
	 */
	boolean isFileSystemOpen(BucketFileSystem fileSystem)
	{
		var awsRecord = fileSystem.awsFacade().awsRecord();
		var bucketRecord = new BucketRecord(awsRecord.endpoint(), fileSystem.bucketName());
		return FILE_SYSTEMS_CACHE.containsKey(bucketRecord);
	}

	private BucketFileSystem createFileSystem(BucketDescriptor bucketDescriptor)
	{
		var facadeKey = bucketDescriptor.connectorKey();
		var awsFacade = FACADES_CACHE.computeIfAbsent(facadeKey, AwsFacade::create);
		var result = new BucketFileSystem(awsFacade, bucketDescriptor, this);
		var bucketKey = bucketDescriptor.bucketKey();
		FILE_SYSTEMS_CACHE.put(bucketKey, result);
		return result;
	}

	private ObjectBasicFileAttributeView createBasicFileAttributeView(BucketPath bucketPath)
	{
		var fileSystem = bucketPath.getFileSystem();
		var awsFacade = fileSystem.awsFacade();
		var bucketName = Try.to(() -> getFileStore(bucketPath)).get().name();
		var objectKey = bucketPath.toString();
		return new ObjectBasicFileAttributeView(awsFacade, bucketName, objectKey);
	}

	private Exception checkIfPathExists(BucketPath path)
	{
		var reference = new AtomicReference<Exception>();
		Stream.iterate(path.getParent(),
					   Predicate.not(BucketPath::isRootDirectory)
								.and(item -> reference.get() == null),
					   BucketPath::getParent)
			  .forEach(item -> Try.to(() -> readAttributes(item,
														   BasicFileAttributes.class,
														   LinkOption.NOFOLLOW_LINKS))
								  .onCatch(reference::set)
								  .run());
		return reference.get();
	}

	private void executeCopy(Path source, Path target, CopyOption... options) throws IOException
	{
		var sourcePath = validatePath(source).toRealPath(LinkOption.NOFOLLOW_LINKS);
		var targetPath = validatePath(target);
		Evaluator.when(() -> CopyFlag.IS_REPLACEABLE.matches(Set.of(options)))
				 .then(() -> Files.deleteIfExists(targetPath))
				 .elseWhen(() -> Files.exists(targetPath, LinkOption.NOFOLLOW_LINKS))
				 .thenThrow(() -> new FileAlreadyExistsException(targetPath.toString()));
		CopyFile.from(sourcePath).to(targetPath);
	}

	private BucketPath validatePath(Path path)
	{
		if (path instanceof BucketPath bucketPath)
		{
			return bucketPath;
		}
		throw Optional.ofNullable(path)
					  .map(this::unsupportedPath)
					  .orElseGet(() -> new NullPointerException("Missing path."));
	}

	private RuntimeException unsupportedPath(Path path)
	{
		var pathType = path.getClass().getName();
		var pathTemplate = "Expected path of type %s. Found: %s.";
		var pathMessage = pathTemplate.formatted(BucketPath.class.getName(), pathType);
		return new ProviderMismatchException(pathMessage);
	}

	private RuntimeException unexpectedFileAttributes(Class<? extends BasicFileAttributes> type)
	{
		var attributesTemplate = "Expected attributes of type %s. Found: %s.";
		var attributesMessage = attributesTemplate.formatted(ObjectBasicFileAttributes.class.getName(),
															 type.getName());
		return new UnsupportedOperationException(attributesMessage);
	}

	private static final class CopyFile
	{

		private final FileTransfer fileTransfer;

		public CopyFile(FileTransfer fileTransfer)
		{
			this.fileTransfer = fileTransfer;
		}

		public static CopyFile from(BucketPath path)
		{
			var fileSystem = path.getFileSystem();
			var bucketName = fileSystem.bucketName();
			var key = path.toString();
			var fileTransfer = fileSystem.awsFacade().fileTransfer(bucketName, key);
			return new CopyFile(fileTransfer);
		}

		public void to(BucketPath path) throws IOException
		{
			var fileSystem = path.getFileSystem();
			var bucketName = fileSystem.bucketName();
			var key = path.toString();
			fileSystem.awsFacade().receiveFile(bucketName, key, fileTransfer);
		}
	}

	private static record FileAttributeViewFactory<F extends FileAttributeView>(Class<F> fileAttributeViewType,
																				Function<BucketPath, F> factory)
	{

	}

	private static record View(String name, String attributes)
	{

	}
}
