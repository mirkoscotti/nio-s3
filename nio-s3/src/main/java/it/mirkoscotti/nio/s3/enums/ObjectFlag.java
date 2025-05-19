package it.mirkoscotti.nio.s3.enums;

import it.mirkoscotti.nio.s3.extensions.jdk.jsr203.BucketPath;

import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.StandardOpenOption;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * @author mirko.scotti
 * @version May 17, 2025
 */
public enum ObjectFlag
{

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
	IS_APPENDABLE(item -> item.contains(StandardOpenOption.APPEND)),
	IS_TRUNCATABLE(item -> item.contains(StandardOpenOption.TRUNCATE_EXISTING)),
	IS_WRITABLE(item -> item.contains(StandardOpenOption.WRITE))
	{

		@Override
		public boolean matches(Set<? extends OpenOption> options)
		{
			return IS_APPENDABLE.matches(options) || super.matches(options);
		}
	},
	IS_CREATABLE(item -> item.contains(StandardOpenOption.CREATE_NEW)),
	IS_CREATABLE_IF_NOT_EXISTS(item -> item.contains(StandardOpenOption.CREATE));

	private final Predicate<Set<? extends OpenOption>> predicate;

	/**
	 * @param predicate
	 */
	private ObjectFlag(Predicate<Set<? extends OpenOption>> predicate)
	{
		this.predicate = predicate;
	}

	public boolean matches(Set<? extends OpenOption> options)
	{
		var set = Optional.ofNullable(options).orElseGet(Set::of);
		return predicate.test(set);
	}

	public static <T extends OpenOption> Set<T> readWriteCheck(Set<T> options)
	{
		return Optional.of(options)
					   .filter(Predicate.not(item -> IS_READABLE.matches(item)
						   && IS_WRITABLE.matches(item)))
					   .orElseThrow(() -> new IllegalArgumentException("An S3 object cannot be accessed for both read and write operations."));
	}

	public static <T extends OpenOption> Set<T> appendTruncateCheck(Set<T> options)
	{
		return Optional.of(options)
					   .filter(Predicate.not(item -> IS_APPENDABLE.matches(item)
						   && IS_TRUNCATABLE.matches(item)))
					   .orElseThrow(() -> new IllegalArgumentException("An S3 object cannot be accessed for both append and truncate operations."));
	}

	public static <T extends OpenOption> Set<T> truncateCheck(BucketPath path, Set<T> options)
	{
		Predicate<Set<? extends OpenOption>> pathExists = item -> Files.exists(path);
		return Optional.of(options)
					   .filter(Predicate.not(IS_TRUNCATABLE::matches)
										.or(IS_TRUNCATABLE.predicate.and(pathExists.and(Predicate.not(IS_CREATABLE::matches)))
																	.or(Predicate.not(pathExists)
																				 .and(IS_CREATABLE.predicate.or(IS_CREATABLE_IF_NOT_EXISTS::matches)))))
					   .orElseThrow(() -> new IllegalArgumentException("An existing truncatable S3 object cannot be creatable: %s".formatted(path)));
	}

	public static <T extends OpenOption> Set<T> createCheck(BucketPath path, Set<T> options)
		throws IOException
	{
		Predicate<Set<? extends OpenOption>> pathExists = item -> Files.exists(path);
		return Optional.of(options)
					   .filter(Predicate.not(IS_CREATABLE::matches)
										.or(Predicate.not(pathExists).and(IS_CREATABLE::matches)))
					   .orElseThrow(() -> new FileAlreadyExistsException("Cannot create an already existing S3 object: %s.".formatted(path)));
	}
}
