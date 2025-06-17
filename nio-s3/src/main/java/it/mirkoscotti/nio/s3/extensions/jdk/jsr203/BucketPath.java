package it.mirkoscotti.nio.s3.extensions.jdk.jsr203;

import it.mirkoscotti.nio.s3.configuration.BucketDescriptor;
import it.mirkoscotti.nio.s3.enums.BucketModifier;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.LinkOption;
import java.nio.file.NotDirectoryException;
import java.nio.file.Path;
import java.nio.file.ProviderMismatchException;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent.Kind;
import java.nio.file.WatchEvent.Modifier;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * Defines an S3 bucket object with a name compliant with the rules described in
 * <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/object-keys.html">AWS
 * documentation</a>. All warnings found in the documentation correspond to restrictions applied to
 * the name wrapped by an instance of this class.
 * <p>
 * Although the S3 naming convention does not allow an object name to start with "/", a name with
 * this characteristic is still accepted for correct handling of absolute and relative paths. A path
 * is considered absolute if it starts with "/" and relative otherwise. With this assumption, the
 * root path of an S3 bucket will be "/" and empty path will be its relative representation.
 * <p>
 * According to the naming convention, an object with key <code>directory/file.txt</code> is
 * considered a file and its logical directory can be an empty object named <code>directory/</code>.
 * Therefore, any path ending with "/" will be considered a directory, meaning
 * <code>Files.isDirectory(path)</code> will return true. In particular, the root path "/", will be
 * considered a directory even if it cannot correspond to a real object in a bucket. This directory
 * will contain all the objects whose keys does not contain any "/". Since we can always save an
 * object on the root of an S3 bucket, the API <code>Files.exists(path)</code>, where path is "/",
 * will always return true.
 * <p>
 * POSIX relative paths like "." and ".." are treated as their meaning. Thus, even if it is possible
 * to save an object with key <code>abc/xyz/../file.txt</code> using the AWS console or AWSCLI, the
 * corresponding path will be considered relative and equivalent to <code>abc/./file.txt</code> and
 * finally to <code>abc/file.txt</code>. S3 does not allow keys where the number of ".." parts
 * exceeds the number of preceding literal parts, like for instance <code>abc/../../file.txt</code>.
 * According to the POSIX rules, even if such a key is forbidden, a path representing this key will
 * be normalized to <code>file.txt</code>.
 *
 * @author mirko.scotti
 * @version Jul 14, 2024
 */
public class BucketPath
	implements Path
{

	private static final String NOT_CONSECUTIVE_SLASHES = "^(?!.*\\/\\/).*$";

	private static final String NOT_ENDING_WITH_DOT = "^(?:.*(?<![.])|.*/(?:\\.|\\.\\.))$";

	private static final String FORBIDDEN_CHARACTERS = "^[^\\\\{^}%`\\]\">\\[~<#|\\x00-\\x1f\\x7f-\\xff]*$";

	private static final String SPECIAL_CHARACTERS = "[&$@=;:+,?\\s]";

	private static final Supplier<String> MISSING_PATH = () -> "Missing path.";

	private final BucketFileSystem fileSystem;

	private final Path root;

	private final String objectKey;

	/**
	 * Main rules for the object keys defining a path:
	 * <ul>
	 * <li>keys cannot be empty.
	 * <li>keys cannot be longer than 1kB
	 * <li>keys cannot contain consecutive slashes (<code>//</code>)
	 * <li>keys cannot end with dot
	 * <li>keys cannot contain any extended ASCII characters (from decimal 128 to 255)
	 * <li>keys cannot contain non-printable characters
	 * <li>the following characters are forbidden: <code>\{^}%`]">[~<#|</code>
	 * </ul>
	 *
	 * @param fileSystem
	 * @param first
	 * @param more
	 */
	BucketPath(BucketFileSystem fileSystem, String first, String... more)
	{
		this.fileSystem = Objects.requireNonNull(fileSystem, () -> "Missing file system.");
		root = Objects.requireNonNull(first, MISSING_PATH)
					  .startsWith(BucketDescriptor.PATH_SEPARATOR)
						  ? new BucketPath(fileSystem)
						  : null;
		var path = Stream.concat(Stream.of(first), Stream.ofNullable(more).flatMap(Stream::of))
						 .collect(Collectors.joining(BucketDescriptor.PATH_SEPARATOR));
		objectKey = validatedPath(path);
	}

	BucketPath(BucketFileSystem fileSystem)
	{
		this.fileSystem = fileSystem;
		root = this;
		objectKey = null;
	}

	@Override
	public BucketFileSystem getFileSystem()
	{
		return fileSystem;
	}

	@Override
	public boolean isAbsolute()
	{
		return root != null;
	}

	@Override
	public Path getRoot()
	{
		return root;
	}

	@Override
	public Path getFileName()
	{
		return Optional.ofNullable(objectKey)
					   .map(item -> item.split(BucketDescriptor.PATH_SEPARATOR))
					   .stream()
					   .flatMap(Stream::of)
					   .filter(Predicate.not(String::isEmpty))
					   .reduce((item1, item2) -> item2)
					   .map(fileSystem::getPath)
					   .orElse(null);
	}

	@Override
	public Path getParent()
	{
		return Optional.ofNullable(objectKey)
					   .map(item -> item.split(BucketDescriptor.PATH_SEPARATOR))
					   .map(item -> Stream.of(item)
										  .limit(item.length - 1l)
										  .filter(Predicate.not(String::isBlank))
										  .collect(Collectors.joining(BucketDescriptor.PATH_SEPARATOR,
																	  BucketDescriptor.PATH_SEPARATOR,
																	  "")))
					   .map(fileSystem::getPath)
					   .orElse(null);
	}

	@Override
	public int getNameCount()
	{
		return Optional.ofNullable(objectKey)
					   .map(item -> item.split(BucketDescriptor.PATH_SEPARATOR))
					   .map(item -> item.length)
					   .orElse(0);
	}

	@Override
	public Path getName(int index)
	{
		if (index < 0)
		{
			throw new IllegalArgumentException("Index must not be negative.");
		}
		return Optional.ofNullable(objectKey)
					   .map(item -> item.split(BucketDescriptor.PATH_SEPARATOR))
					   .filter(item -> item.length > index)
					   .map(item -> item[index])
					   .map(fileSystem::getPath)
					   .orElseThrow(() -> new IllegalArgumentException("Index greater than the number of elements."));
	}

	@Override
	public Path subpath(int beginIndex, int endIndex)
	{
		if (beginIndex < 0)
		{
			throw new IllegalArgumentException("Begin index must not be negative.");
		}
		if (endIndex <= beginIndex)
		{
			throw new IllegalArgumentException("Begin index must be lower than end index.");
		}
		var array = elements();
		if (beginIndex >= array.length)
		{
			throw new IllegalArgumentException("Begin index is greater than the number of elements.");
		}
		if (endIndex > array.length)
		{
			throw new IllegalArgumentException("End index is greater than the number of elements.");
		}
		return new BucketPath(fileSystem,
							  array[beginIndex],
							  Stream.of(array)
									.skip(beginIndex + 1l)
									.limit(endIndex - beginIndex - 1l)
									.toArray(String[]::new));
	}

	@Override
	public boolean startsWith(Path other)
	{
		boolean result = false;
		if (other != null)
		{
			var nameCount = other.getNameCount();
			result = other.getFileSystem().equals(fileSystem)
				&& nameCount <= getNameCount()
				&& subpath(0, nameCount).equals(other);
		}
		return result;
	}

	@Override
	public boolean endsWith(Path other)
	{
		boolean result = false;
		if (other != null)
		{
			var thisNameCount = getNameCount();
			var otherNameCount = other.getNameCount();
			result = other.getFileSystem().equals(fileSystem)
				&& otherNameCount <= thisNameCount
				&& subpath(thisNameCount - otherNameCount, thisNameCount).equals(other);
		}
		return result;
	}

	@Override
	public Path normalize()
	{
		var stack = new ArrayDeque<String>();
		Stream.of(elements())
			  .filter(Predicate.not(item -> item.equals(".")))
			  .forEach(item -> updateElements(stack, item));
		var prefix = isAbsolute() ? BucketDescriptor.PATH_SEPARATOR : "";
		var suffix = Optional.ofNullable(objectKey)
							 .orElse(BucketDescriptor.PATH_SEPARATOR)
							 .endsWith(BucketDescriptor.PATH_SEPARATOR)
								 ? BucketDescriptor.PATH_SEPARATOR
								 : "";
		var result = stack.stream()
						  .collect(Collectors.joining(BucketDescriptor.PATH_SEPARATOR,
													  prefix,
													  suffix));
		return fileSystem.getPath(result);
	}

	/**
	 * {@inheritDoc}
	 *
	 * @return the resolved path, that is the same independently on whether the object key of this
	 *         one ends with path separator or not
	 */
	@Override
	public Path resolve(Path other)
	{
		var path = compliantPath(other);
		return switch (path)
		{
			case BucketPath bucketPath when bucketPath.objectKey == null -> this;
			case BucketPath bucketPath when bucketPath.isAbsolute() -> bucketPath;
			default ->
			{
				var basePath = Optional.ofNullable(objectKey)
									   .orElse(BucketDescriptor.PATH_SEPARATOR);
				yield basePath.endsWith(BucketDescriptor.PATH_SEPARATOR)
					? new BucketPath(fileSystem, basePath.concat(path.objectKey))
					: new BucketPath(fileSystem,
									 Stream.of(basePath, path.objectKey)
										   .collect(Collectors.joining(BucketDescriptor.PATH_SEPARATOR)));
			}
		};
	}

	@Override
	public Path relativize(Path other)
	{
		var path = Optional.of(compliantPath(other))
						   .filter(item -> isAbsolute() == item.isAbsolute())
						   .orElseThrow(() -> bothAbsoluteOrRelativeMessage(other));
		var sourceList = List.of(elements());
		var otherList = List.of(path.elements());
		int size = (int) IntStream.range(0, Math.min(sourceList.size(), otherList.size()))
								  .takeWhile(i -> sourceList.get(i).equals(otherList.get(i)))
								  .count();
		long sourceSize = sourceList.size();
		var result = size == sourceSize ? new ArrayList<String>() : new ArrayList<>(sourceList);
		result.addAll(Stream.generate(() -> "..").limit(sourceSize - size).toList());
		var additionalElements = otherList.subList(size, otherList.size());
		result.addAll(additionalElements);
		var first = result.stream().collect(Collectors.joining(BucketDescriptor.PATH_SEPARATOR));
		return new BucketPath(fileSystem, first);
	}

	@Override
	public URI toUri()
	{
		var scheme = fileSystem.provider().getScheme();
		var bucketName = fileSystem.getFileStores().iterator().next().name();
		var path = toAbsolutePath();
		return URI.create("%s://%s/%s".formatted(scheme, bucketName, path));
	}

	/**
	 * Creates a new path attaching the object key to the root path of the file system.
	 */
	@Override
	public Path toAbsolutePath()
	{
		return isAbsolute()
			? this
			: new BucketPath(fileSystem, BucketDescriptor.PATH_SEPARATOR.concat(objectKey));
	}

	@Override
	public Path toRealPath(LinkOption... options) throws IOException
	{
		var path = toAbsolutePath().normalize();
		return Optional.of(path)
					   .filter(item -> fileSystem.provider().exists(item, options))
					   .orElseThrow(() -> fileNotFoundInBucket(path));
	}

	@Override
	public WatchKey register(WatchService watcher, Kind<?>[] events, Modifier... modifiers)
		throws IOException
	{
		if (!Files.isDirectory(this) || !Files.exists(this))
		{
			throw new NotDirectoryException("""
				Only existing directories can be watched.
				This path does not exist or it is a file: %s
				""".formatted(objectKey));
		}
		var exception = Stream.of(events)
							  .filter(Predicate.not(List.of(StandardWatchEventKinds.ENTRY_CREATE,
															StandardWatchEventKinds.ENTRY_DELETE,
															StandardWatchEventKinds.ENTRY_MODIFY)::contains))
							  .map("Invalid event: %s"::formatted)
							  .map(IllegalArgumentException::new)
							  .collect(() -> new UnsupportedOperationException("Unsupported events."),
									   Throwable::addSuppressed,
									   Throwable::addSuppressed);
		if (exception.getSuppressed().length > 0)
		{
			throw exception;
		}
		exception = Stream.ofNullable(modifiers)
						  .flatMap(Stream::of)
						  .filter(Predicate.not(List.of(BucketModifier.values())::contains))
						  .map("Invalid modifier: %s"::formatted)
						  .map(IllegalArgumentException::new)
						  .collect(() -> new UnsupportedOperationException("Only %s's items are supported.".formatted(BucketModifier.class.getName())),
								   Throwable::addSuppressed,
								   Throwable::addSuppressed);
		if (exception.getSuppressed().length > 0)
		{
			throw exception;
		}
		if (watcher instanceof DirectoryWatchService watchService)
		{
			return watchService.registerPath(this);
		}
		throw new ProviderMismatchException("Watcher missing or not working with S3 buckets.");
	}

	@Override
	public int compareTo(Path other)
	{
		var path = compliantPath(other);
		var path1 = isAbsolute() ? BucketDescriptor.PATH_SEPARATOR.concat(objectKey) : objectKey;
		var path2 = path.isAbsolute()
			? BucketDescriptor.PATH_SEPARATOR.concat(path.objectKey)
			: path.objectKey;
		return path1.compareTo(path2);
	}

	/**
	 * {@inheritDoc}
	 *
	 * @return true if the other object is a path of the same type, it belongs to the same file
	 *         system, it has the same key and it is absolute if this is absolute and relative if
	 *         this is relative
	 */
	@Override
	public boolean equals(Object obj)
	{
		return obj instanceof BucketPath bucketPath
			&& Objects.equals(fileSystem, bucketPath.getFileSystem())
			&& Objects.equals(objectKey, bucketPath.objectKey)
			&& isAbsolute() == bucketPath.isAbsolute();
	}

	/**
	 * Paths with the same file system, the same object key and they are both absolute o both
	 * relative, have the same hash code, resulting also equals to each other.
	 */
	@Override
	public int hashCode()
	{
		return Objects.hash(fileSystem, objectKey, isAbsolute());
	}

	@Override
	public String toString()
	{
		return Optional.ofNullable(objectKey).orElseGet(() -> BucketDescriptor.PATH_SEPARATOR);
	}

	/*
	 * POSIX normalization is not applied here. The aim is only to validate the path against the S3
	 * rules. Thus paths like "abc/../file.txt" will be returned unchanged.
	 */
	private String validatedPath(String path)
	{
		// Path length less or equals to 1KB
		var current = Optional.of(path)
							  .filter(item -> item.length() <= 1024)
							  .orElseThrow(() -> new InvalidPathException("Path too long", path));
		// Path does not contain consecutive slashes
		current = Optional.of(current)
						  .filter(item -> item.matches(NOT_CONSECUTIVE_SLASHES))
						  .orElseThrow(() -> new InvalidPathException("Path cannot have two consecutive slashes.",
																	  path));
		// Path not ending with dot
		current = Optional.of(current)
						  .filter(item -> item.matches(NOT_ENDING_WITH_DOT))
						  .orElseThrow(() -> new InvalidPathException("Path cannot end with dot.",
																	  path));
		// Path does not contain forbidden characters
		current = Optional.of(current)
						  .filter(item -> item.matches(FORBIDDEN_CHARACTERS))
						  .orElseThrow(() -> new InvalidPathException("Forbidden characters in path",
																	  path));
		// Initial slash removed
		var result = Optional.of(current)
							 .filter(item -> item.startsWith(BucketDescriptor.PATH_SEPARATOR))
							 .map(item -> item.substring(BucketDescriptor.PATH_SEPARATOR.length()))
							 .orElse(current);
		// Special characters escaped
		var matcher = Pattern.compile(SPECIAL_CHARACTERS).matcher(result);
		return IntStream.range(0, result.length())
						.mapToObj(i -> matcher.find(i) && matcher.start() == i
							? "%%%02X".formatted((int) result.charAt(i))
							: String.valueOf(result.charAt(i)))
						.collect(Collectors.joining());
	}

	private BucketPath compliantPath(Path path)
	{
		Objects.requireNonNull(path, MISSING_PATH);
		return Optional.of(path)
					   .filter(BucketPath.class::isInstance)
					   .map(BucketPath.class::cast)
					   .filter(item -> fileSystem.equals(item.getFileSystem()))
					   .orElseThrow(() -> new IllegalArgumentException("Path '%s' does not belonging to the same AWS S3 bucket.".formatted(path)));
	}

	private String[] elements()
	{
		return Optional.ofNullable(objectKey)
					   .map(item -> item.split(BucketDescriptor.PATH_SEPARATOR))
					   .orElseGet(() -> new String[0]);
	}

	private Deque<String> updateElements(Deque<String> elements, String element)
	{
		Optional.ofNullable(element)
				.filter(".."::equals)
				.ifPresentOrElse(item -> Optional.of(elements)
												 .filter(Predicate.not(Deque::isEmpty))
												 .ifPresent(Deque::pollLast),
								 () -> elements.addLast(element));
		return elements;
	}

	private IllegalArgumentException bothAbsoluteOrRelativeMessage(Path path)
	{
		var pattern = """
			Paths must be both absolute or both relative.
			- This path: %s -> %s
			- Other path: %s -> %s
			""";
		var thisIsAbsolute = isAbsolute()
			? BucketDescriptor.PATH_SEPARATOR.concat(toString())
			: this;
		var pathIsAbsolute = path.isAbsolute()
			? BucketDescriptor.PATH_SEPARATOR.concat(toString())
			: this;
		var thisFlag = isAbsolute() ? "absolute" : "relative";
		var pathFlag = path.isAbsolute() ? "absolute" : "relative";
		var message = pattern.formatted(thisIsAbsolute, thisFlag, pathIsAbsolute, pathFlag);
		return new IllegalArgumentException(message);
	}

	private FileNotFoundException fileNotFoundInBucket(Path path)
	{
		var bucketName = fileSystem.getFileStores().iterator().next().name();
		var message = "Object %s not found in bucket %s".formatted(path, bucketName);
		return new FileNotFoundException(message);
	}
}
