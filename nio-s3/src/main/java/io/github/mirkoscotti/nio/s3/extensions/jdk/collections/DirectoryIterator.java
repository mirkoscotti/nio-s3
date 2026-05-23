package io.github.mirkoscotti.nio.s3.extensions.jdk.collections;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Predicate;
import java.util.stream.Stream;

import io.github.mirkoscotti.nio.s3.configuration.BucketDescriptor;

import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.CommonPrefix;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request.Builder;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.S3Object;

/**
 * The AWS API <code>listObjectsV2Paginator</code> represented as a standard JDK iterator.
 *
 * @author mirko.scotti
 * @version Dec 26, 2025
 */
public class DirectoryIterator
	implements Iterator<String>
{

	private static final int PAGE_SIZE = 50;

	private static final String END_MARKER = "\0".repeat(1025);

	private final BlockingQueue<String> queue = new LinkedBlockingQueue<>(2 * PAGE_SIZE);

	private final String bucket;

	private final String prefix;

	private String next;

	public DirectoryIterator(S3AsyncClient client, String bucket, String prefix)
	{
		this.bucket = bucket;
		this.prefix = prefix;
		client.listObjectsV2Paginator(this::configurePagination).subscribe(this::processResponse);
		next = take();
	}

	@Override
	public boolean hasNext()
	{
		return !next.equals(END_MARKER);
	}

	@Override
	public String next()
	{
		var result = Optional.of(next)
							 .filter(Predicate.not(END_MARKER::equals))
							 .orElseThrow(NoSuchElementException::new);
		next = take();
		return result;
	}

	private void processResponse(ListObjectsV2Response response)
	{
		var directories = response.commonPrefixes().stream().map(CommonPrefix::prefix);
		var files = response.contents().stream().map(S3Object::key);
		Stream.concat(directories, files)
			  .map(Optional::ofNullable)
			  .map(item -> item.filter(Predicate.not(prefix::equals)))
			  .forEach(item -> item.ifPresent(this::put));
		Optional.of(response)
				.filter(Predicate.not(ListObjectsV2Response::isTruncated))
				.ifPresent(item -> put(END_MARKER));
	}

	private void configurePagination(Builder builder)
	{
		builder.bucket(bucket)
			   .prefix(prefix)
			   .delimiter(BucketDescriptor.PATH_SEPARATOR)
			   .maxKeys(PAGE_SIZE);
	}

	private String take()
	{
		try
		{
			return queue.take();
		}
		catch (InterruptedException x)
		{
			Thread.currentThread().interrupt();
			throw new IllegalStateException(x);
		}
	}

	private void put(String value)
	{
		try
		{
			queue.put(value);
		}
		catch (InterruptedException x)
		{
			Thread.currentThread().interrupt();
			throw new IllegalStateException(x);
		}
	}
}
