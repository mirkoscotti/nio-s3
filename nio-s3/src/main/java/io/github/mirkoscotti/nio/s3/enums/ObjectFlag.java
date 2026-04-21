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
				|| Stream.of(StandardOpenOption.READ, StandardOpenOption.WRITE,
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

	public static Optional<ObjectFlag> appendTruncateCheck(Set<? extends OpenOption> options)
	{
		var list = Optional.of(Stream.of(IS_TRUNCATABLE, IS_APPENDABLE)
									 .filter(item -> item.matches(options))
									 .toList())
						   .filter(item -> item.size() < 2)
						   .orElseThrow(() -> new IllegalArgumentException("An S3 object cannot be accessed for both append and truncate operations."));
		return Optional.of(list).filter(Predicate.not(List::isEmpty)).map(item -> item.get(0));
	}

	public static <T extends OpenOption> Set<T> creationWhenFileNotFoundCheck(Set<T> options,
																			  BucketPath path)
		throws IOException
	{
		return Optional.of(options)
					   .filter(item -> IS_CREATABLE.matches(item)
						   || IS_CREATABLE_IF_NOT_EXISTS.matches(item))
					   .orElseThrow(() -> new NoSuchFileException(path.toString()));
	}

	public static <T extends OpenOption> Set<T> creationWhenFileExistingCheck(Set<T> options,
																			  BucketPath path)
		throws IOException
	{
		return Optional.of(options)
					   .filter(Predicate.not(IS_CREATABLE::matches))
					   .orElseThrow(() -> new FileAlreadyExistsException(path.toString()));
	}
}
