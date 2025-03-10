/*
 * (C) Copyright 2019 - 2025 - Add Value S.R.L - All rights reserved.
 */

package it.mirkoscotti.nio.s3.configuration;

import it.mirkoscotti.nio.s3.enums.BucketProperty;
import it.mirkoscotti.nio.s3.exceptions.BucketUriException;
import it.mirkoscotti.nio.s3.records.CredentialsRecord;

import java.net.URI;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * @author mirko.scotti
 * @version Mar 03, 2025
 */
public class UriDescriptor
{

	private static final String PATH_SEPARATOR = "/";

	private final String bucketName;

	private final Optional<String> endpoint;

	private final Optional<CredentialsRecord> credentials;

	public UriDescriptor(URI uri)
	{
		var host = checkedUri(uri).getHost();
		bucketName = virtualHostBucketName(host).orElseGet(() -> pathBucketName(uri));
		endpoint = host.equals(bucketName) ? Optional.empty() : endpoint(host);
		credentials = Optional.ofNullable(uri.getUserInfo())
							  .map(item -> item.split(":"))
							  .filter(item -> item.length == 2)
							  .map(item -> new CredentialsRecord(item[0], item[1]));
	}

	/**
	 * The wrapper method of the {@link #bucketName} property.
	 *
	 * @return the value of the property
	 */
	public String bucketName()
	{
		return bucketName;
	}

	/**
	 * The wrapper method of the {@link #endpoint} property.
	 *
	 * @return the value of the property
	 */
	public Optional<String> endpoint()
	{
		return endpoint;
	}

	/**
	 * The wrapper method of the {@link #credentials} property.
	 *
	 * @return the value of the property
	 */
	public Optional<CredentialsRecord> credentials()
	{
		return credentials;
	}

	private URI checkedUri(URI uri)
	{
		Objects.requireNonNull(uri, () -> "Missing URI.");
		return Optional.of(uri)
					   .filter(item -> BucketProperty.SCHEME.equals(item.getScheme()))
					   .orElseThrow(() -> new BucketUriException(uri));
	}

	private Optional<String> virtualHostBucketName(String hostName)
	{
		var array = hostName.split("\\.");
		return Optional.of(array)
					   .filter(item -> item.length > 1)
					   .map(item -> Arrays.binarySearch(item, "s3"))
					   .filter(item -> item >= 0)
					   .map(item -> IntStream.range(0, item)
											 .mapToObj(i -> array[i])
											 .collect(Collectors.joining(".")));
	}

	private String pathBucketName(URI uri)
	{
		return Optional.ofNullable(uri.getPath())
					   .map(item -> item.split(PATH_SEPARATOR))
					   .filter(item -> item.length > 1)
					   .map(item -> item[1])
					   .orElseGet(uri::getHost);
	}

	private Optional<String> endpoint(String host)
	{
		return Optional.of(host.startsWith(bucketName)
			? host.substring(bucketName.length() + 1)
			: host);
	}
}
