package io.github.mirkoscotti.nio.s3.extensions.jdk.jsr203;

import java.io.IOException;
import java.nio.file.DirectoryIteratorException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.NotDirectoryException;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * A <code>DirectoryStream</code> implementation that provides iteration over a set of objects
 * having the same prefix represented by the configured {@link BucketPath}, eventually filtered by
 * the <code>DirectoryStream.Filter</code> supplied at the instantiation time.
 *
 * @author mirko.scotti
 * @version Dec 27, 2025
 * @see BucketPath
 */
class BucketDirectoryStream
	implements DirectoryStream<Path>
{

	private final BucketPath directory;

	private final Filter<? super Path> filter;

	private boolean isClosed = false;

	private boolean streamOver = false;

	BucketDirectoryStream(BucketPath directory, Filter<? super Path> filter) throws IOException
	{
		this.directory = Optional.of(directory)
								 .filter(Files::isDirectory)
								 .orElseThrow(() -> new NotDirectoryException(directory.toString()));
		this.filter = filter;
	}

	@Override
	public void close() throws IOException
	{
		isClosed = true;
		directory.getFileSystem().unregisterResource(this);
	}

	@Override
	public Iterator<Path> iterator()
	{
		var result = Optional.of(this)
							 .filter(Predicate.not(item -> item.isClosed || streamOver))
							 .map(item -> new DirectoryStreamIterator(directory, filter))
							 .orElseThrow(() -> new IllegalStateException("Stream closed or already consumed."));
		streamOver = true;
		return result;
	}

	private static final class DirectoryStreamIterator
		implements Iterator<Path>
	{

		private final Filter<? super Path> filter;

		private final BucketFileSystem fileSystem;

		private final Iterator<String> iterator;

		private Optional<BucketPath> next;

		public DirectoryStreamIterator(BucketPath directory, Filter<? super Path> filter)
		{
			this.filter = filter;
			fileSystem = directory.getFileSystem();
			var bucket = fileSystem.getFileStores().iterator().next().name();
			var prefix = directory.toString();
			iterator = directory.getFileSystem().awsFacade().scanDirectory(bucket, prefix);
			next = findNext();
		}

		@Override
		public boolean hasNext()
		{
			return next.isPresent();
		}

		@Override
		public Path next()
		{
			var result = next.orElseThrow(NoSuchElementException::new);
			next = findNext();
			return result;
		}

		private Optional<BucketPath> findNext()
		{
			return Stream.generate(() -> iterator.hasNext() ? iterator.next() : null)
						 .takeWhile(Objects::nonNull)
						 .map(item -> new BucketPath(fileSystem, item))
						 .filter(this::matches)
						 .findFirst();
		}

		private boolean matches(BucketPath bucketPath)
		{
			try
			{
				return filter.accept(bucketPath);
			}
			catch (IOException x)
			{
				throw new DirectoryIteratorException(x);
			}
		}
	}
}
