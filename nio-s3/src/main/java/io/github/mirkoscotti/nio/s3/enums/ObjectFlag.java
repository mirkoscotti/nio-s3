package io.github.mirkoscotti.nio.s3.enums;

import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.NoSuchFileException;
import java.nio.file.OpenOption;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Stream;

import io.github.mirkoscotti.nio.s3.extensions.jdk.jsr203.BucketPath;

/**
 * The set of access modes applicable to an S3 object, each corresponding to a specific
 * <code>OpenOption</code> declared by the NIO.2 API.
 * <p>
 * Each constant acts as a named predicate that determines whether a given set of open options
 * activates that particular mode. Constants can be queried individually or validated collectively
 * enforcing the mutual-exclusion rules imposed by the S3 protocol.
 * <p>
 * <strong>Mutual-exclusion rules:</strong>
 * <ul>
 * <li>Read and write are mutually exclusive.</li>
 * <li>Append and truncate are mutually exclusive.</li>
 * </ul>
 *
 * @author mirko.scotti
 * @version May 17, 2025
 */
public enum ObjectFlag
{

	/**
	 * The object is opened for reading. This is the implicit mode when neither write nor append is
	 * requested.
	 */
	IS_READABLE(item -> item.contains(StandardOpenOption.READ))
	{

		@Override
		public boolean matches(Set<? extends OpenOption> options)
		{
			return options == null
				|| Stream.of(StandardOpenOption.READ,
							 StandardOpenOption.WRITE,
							 StandardOpenOption.APPEND)
						 .noneMatch(options::contains)
				|| super.matches(options);
		}
	},
	/**
	 * The object is opened for appending to its existing content.
	 */
	IS_APPENDABLE(item -> item.contains(StandardOpenOption.APPEND)),
	/**
	 * The object's existing content is discarded before writing.
	 */
	IS_TRUNCATABLE(item -> item.contains(StandardOpenOption.TRUNCATE_EXISTING)),
	/**
	 * The object is opened for writing, including when opened for appending.
	 */
	IS_WRITABLE(item -> item.contains(StandardOpenOption.WRITE))
	{

		@Override
		public boolean matches(Set<? extends OpenOption> options)
		{
			return IS_APPENDABLE.matches(options) || super.matches(options);
		}
	},
	/**
	 * The object must be created before writing.
	 */
	IS_CREATABLE(item -> item.contains(StandardOpenOption.CREATE_NEW)),
	/**
	 * The object is created if missing.
	 */
	IS_CREATABLE_IF_NOT_EXISTS(item -> item.contains(StandardOpenOption.CREATE));

	private final Predicate<Set<? extends OpenOption>> predicate;

	private ObjectFlag(Predicate<Set<? extends OpenOption>> predicate)
	{
		this.predicate = predicate;
	}

	/**
	 * Checks whether this item is semantically compliant with the given set of options.
	 *
	 * @param options
	 *            the open options to inspect, empty set if null
	 * @return <code>true</code> if this item is compliant, <code>false</code> otherwise
	 */
	public boolean matches(Set<? extends OpenOption> options)
	{
		var set = Optional.ofNullable(options).orElseGet(Set::of);
		return predicate.test(set);
	}

	/**
	 * S3 does not support a channel that is open for both reading and writing at the same time.
	 * This is a validation against this restriction.
	 *
	 * @param <T>
	 *            the open-option type
	 * @param options
	 *            the options to validate
	 * @return the same options set, if valid
	 * @throws IllegalArgumentException
	 *             if both read and write modes match
	 */
	public static <T extends OpenOption> Set<T> readWriteCheck(Set<T> options)
	{
		return Optional.of(options)
					   .filter(Predicate.not(item -> IS_READABLE.matches(item)
						   && IS_WRITABLE.matches(item)))
					   .orElseThrow(() -> new IllegalArgumentException("An S3 object cannot be accessed for both read and write operations."));
	}

	/**
	 * Appending to the end of an object and truncating it to zero length are semantically
	 * contradictory operations and cannot be combined.
	 *
	 * @param options
	 *            the options to validate
	 * @return an <code>Optional</code> containing {@link #IS_APPENDABLE} or {@link #IS_TRUNCATABLE}
	 *         if exactly one of them is active, or an empty <code>Optional</code> if neither is
	 * @throws IllegalArgumentException
	 *             if both append and truncate modes are active
	 */
	public static Optional<ObjectFlag> appendTruncateCheck(Set<? extends OpenOption> options)
	{
		var list = Optional.of(Stream.of(IS_TRUNCATABLE, IS_APPENDABLE)
									 .filter(item -> item.matches(options))
									 .toList())
						   .filter(item -> item.size() < 2)
						   .orElseThrow(() -> new IllegalArgumentException("An S3 object cannot be accessed for both append and truncate operations."));
		return Optional.of(list).filter(Predicate.not(List::isEmpty)).map(item -> item.get(0));
	}

	/**
	 * Validates that the given options permit object creation, when a file does not yet exist. If
	 * neither {@link #IS_CREATABLE} nor {@link #IS_CREATABLE_IF_NOT_EXISTS} is active, the
	 * operation is considered an attempt to open a non-existent object and fails accordingly.
	 *
	 * @param <T>
	 *            the open-option type
	 * @param options
	 *            the options to validate
	 * @param path
	 *            the path of the missing object, used to populate the exception message
	 * @return the same options set, if creation is permitted
	 * @throws NoSuchFileException
	 *             if neither creation flag is present
	 */
	public static <T extends OpenOption> Set<T> creationWhenFileNotFoundCheck(Set<T> options,
																			  BucketPath path)
		throws IOException
	{
		return Optional.of(options)
					   .filter(item -> IS_CREATABLE.matches(item)
						   || IS_CREATABLE_IF_NOT_EXISTS.matches(item))
					   .orElseThrow(() -> new NoSuchFileException(path.toString()));
	}

	/**
	 * Validates that the given options do not require exclusive creation.
	 *
	 * @param <T>
	 *            the open-option type
	 * @param options
	 *            the options to validate; must not be <code>null</code>
	 * @param path
	 *            the path of the existing object
	 * @return the same options set, if the validation succeeds
	 * @throws FileAlreadyExistsException
	 *             if {@link #IS_CREATABLE} is active and the file exists
	 */
	public static <T extends OpenOption> Set<T> creationWhenFileExistingCheck(Set<T> options,
																			  BucketPath path)
		throws IOException
	{
		return Optional.of(options)
					   .filter(Predicate.not(IS_CREATABLE::matches))
					   .orElseThrow(() -> new FileAlreadyExistsException(path.toString()));
	}
}
