/*
 * (C) Copyright 2019 - 2024 - Add Value S.R.L - All rights reserved.
 */

package it.mirkoscotti.nio.s3.extensions.jdk.jsr203;

import it.mirkoscotti.nio.s3.configuration.BucketDescriptor;

import java.io.IOException;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.InvalidPathException;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.ProviderMismatchException;
import java.nio.file.WatchEvent.Kind;
import java.nio.file.WatchEvent.Modifier;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
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
 * root path of an S3 bucket will be "/".
 * <p>
 * According to the naming convention, an object with key <code>directory/file.txt</code> is
 * considered a file and its logical directory can be an empty object named <code>directory/</code>.
 * Therefore, any path ending with "/" will be considered a directory, meaning
 * <code>Files.isDirectory(path)</code> will return true. In particular, the root path "/", will be
 * considered a directory even if it cannot correspond to a real object in a bucket. This directory
 * will contain all the objects whose keys does not contains any "/". Since we can always save an
 * object on the root of an S3 bucket, the API <code>Files.exists(path)</code>, where path is "/",
 * will always return true.
 * <p>
 * POSIX relative paths like "." and ".." are treated as their meaning. Thus, even if it is possible
 * to save an object with key <code>abc/xyz/../file.txt</code>, the corresponding path will be
 * considered relative and equivalent to <code>abc/./file.txt</code> and finally to
 * <code>abc/file.txt</code>. S3 does not allow keys where the number of ".." parts exceeds the
 * number of preceding literal parts, like for instance <code>abc/../../file.txt</code>. According
 * to the POSIX rules, even if such a key is forbidden, a path representing this key will be
 * normalized to <code>file.txt</code>.
 *
 * @author mirko.scotti
 * @version Jul 14, 2024
 */
class BucketPath
	implements Path
{

	private static final String NOT_SUPPORTED = "Not supported yet.";

	private static final String NOT_CONSECUTIVE_SLASHES = "^(?!.*\\/\\/)(\\/)?[^/]*(\\/[^/]*)?$";

	private static final String NOT_ENDING_WITH_DOT = ".*(?<!\\.)$";

	private static final String FORBIDDEN_CHARACTERS = "^[^\\\\{^}%`\\]\">\\[~<#|\\x00-\\x1f\\x7f-\\xff]*$";

	private static final String SPECIAL_CHARACTERS = "[&$@=;:+,?\\s]";

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
	 * @param path
	 */
	public BucketPath(BucketFileSystem fileSystem, String path)
	{
		this.fileSystem = Objects.requireNonNull(fileSystem, () -> "Missing file system.");
		root = Objects.requireNonNull(path, () -> "Missing path.")
					  .startsWith(BucketDescriptor.PATH_SEPARATOR)
						  ? new BucketPath(fileSystem)
						  : null;
		objectKey = validatedPath(path);
	}

	private BucketPath(BucketFileSystem fileSystem)
	{
		this.fileSystem = fileSystem;
		root = this;
		objectKey = null;
	}

	@Override
	public FileSystem getFileSystem()
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
		throw new UnsupportedOperationException(NOT_SUPPORTED);
	}

	@Override
	public Path getParent()
	{
		throw new UnsupportedOperationException(NOT_SUPPORTED);
	}

	@Override
	public int getNameCount()
	{
		throw new UnsupportedOperationException(NOT_SUPPORTED);
	}

	@Override
	public Path getName(int index)
	{
		throw new UnsupportedOperationException(NOT_SUPPORTED);
	}

	@Override
	public Path subpath(int beginIndex, int endIndex)
	{
		throw new UnsupportedOperationException(NOT_SUPPORTED);
	}

	@Override
	public boolean startsWith(Path other)
	{
		throw new UnsupportedOperationException(NOT_SUPPORTED);
	}

	@Override
	public boolean endsWith(Path other)
	{
		throw new UnsupportedOperationException(NOT_SUPPORTED);
	}

	@Override
	public Path normalize()
	{
		throw new UnsupportedOperationException(NOT_SUPPORTED);
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
		var path = Optional.of(validatePath(other))
						   .filter(item -> fileSystem.equals(item.getFileSystem()))
						   .orElseThrow(() -> new IllegalArgumentException("File system mismatch. Given path belongs to a different S3 bucket."));
		return switch (path)
		{
			case BucketPath s3Path when s3Path.objectKey.isEmpty() -> this;
			case BucketPath s3Path when s3Path.isAbsolute() -> s3Path;
			case BucketPath s3Path when objectKey.endsWith(BucketDescriptor.PATH_SEPARATOR) -> new BucketPath(fileSystem,
																											  objectKey.concat(path.objectKey));
			default -> objectKey.endsWith(BucketDescriptor.PATH_SEPARATOR)
				? new BucketPath(fileSystem, objectKey.concat(path.objectKey))
				: new BucketPath(fileSystem,
								 Stream.of(objectKey, path.objectKey)
									   .collect(Collectors.joining(BucketDescriptor.PATH_SEPARATOR)));
		};
	}

	@Override
	public Path relativize(Path other)
	{
		throw new UnsupportedOperationException(NOT_SUPPORTED);
	}

	@Override
	public URI toUri()
	{
		throw new UnsupportedOperationException(NOT_SUPPORTED);
	}

	/**
	 * Creates a new path attaching the object key to the root path of the file system.
	 */
	@Override
	public Path toAbsolutePath()
	{
		return isAbsolute() ? this : new BucketPath(fileSystem).resolve(objectKey);
	}

	@Override
	public Path toRealPath(LinkOption... options) throws IOException
	{
		throw new UnsupportedOperationException(NOT_SUPPORTED);
	}

	@Override
	public WatchKey register(WatchService watcher, Kind<?>[] events, Modifier... modifiers)
		throws IOException
	{
		throw new UnsupportedOperationException(NOT_SUPPORTED);
	}

	@Override
	public int compareTo(Path other)
	{
		throw new UnsupportedOperationException(NOT_SUPPORTED);
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
		return obj instanceof BucketPath s3Path
			&& Objects.equals(fileSystem, s3Path.fileSystem)
			&& Objects.equals(objectKey, s3Path.objectKey)
			&& isAbsolute() == s3Path.isAbsolute();
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
		return objectKey;
	}

	static BucketPath validatePath(Path path)
	{
		Objects.requireNonNull(path, () -> "Missing path.");
		if (path instanceof BucketPath result)
		{
			return result;
		}
		throw new ProviderMismatchException("Path not compliant with AWS S3 buckets.");
	}

	/*
	 * POSIX normalization is not applied here. The aim is only to validate the path against the S3
	 * rules. Thus paths like "abc/../file.txt" will be returned unchanged.
	 */
	private String validatedPath(String path)
	{
		// Path not empty
		var current = Optional.of(path)
							  .filter(Predicate.not(String::isEmpty))
							  .orElseThrow(() -> new InvalidPathException("Empty path", path));
		// Path length less or equals to 1KB
		current = Optional.of(current)
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
}
