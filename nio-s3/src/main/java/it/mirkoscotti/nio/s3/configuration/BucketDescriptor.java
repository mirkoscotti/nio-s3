/*
 * (C) Copyright 2019 - 2025 - Add Value S.R.L - All rights reserved.
 */

package it.mirkoscotti.nio.s3.configuration;

import it.mirkoscotti.nio.s3.enums.BucketProperty;
import it.mirkoscotti.nio.s3.exceptions.BucketNameException;
import it.mirkoscotti.nio.s3.exceptions.BucketUriException;
import it.mirkoscotti.nio.s3.exceptions.CredentialsException;
import it.mirkoscotti.nio.s3.records.BucketRecord;
import it.mirkoscotti.nio.s3.records.CredentialsRecord;

import java.net.URI;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author mirko.scotti
 * @version Jan 25, 2025
 */
public class BucketDescriptor
{

	public static final String PATH_SEPARATOR = "/";

	private static final String BUCKET_NAME_NOT_ADJACENT_PERIODS = "(?!.*\\.{2})";

	private static final String BUCKET_NAME_NOT_IP = "(?!\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}$)";

	private static final String BUCKET_NAME_DOES_NOT_START_WITH_DASH = "(?!-)";

	private static final String BUCKET_NAME_DOES_NOT_START_WITH_XN_DOUBLE_DASH = "((?!xn--).*)";

	private static final String BUCKET_NAME_DOES_NOT_START_WITH_S3 = "(^(?!sthree-))";

	private static final String BUCKET_NAME_DOES_NOT_END_WITH_S3ALIAS = "(?!.*-s3alias$)";

	private static final String BUCKET_NAME_DOES_NOT_END_WITH_OL_S3 = "(?!.*--ol-s3$)";

	private static final String BUCKET_LENGTH_BETWEEN_3_AND_63 = "[a-z0-9][a-z0-9.-]{1,61}[a-z0-9]";

	private static final String BUCKET_PATTERN = Stream.of(BUCKET_NAME_NOT_ADJACENT_PERIODS,
														   BUCKET_NAME_NOT_IP,
														   BUCKET_NAME_DOES_NOT_START_WITH_DASH,
														   BUCKET_NAME_DOES_NOT_START_WITH_XN_DOUBLE_DASH,
														   BUCKET_NAME_DOES_NOT_START_WITH_S3,
														   BUCKET_NAME_DOES_NOT_END_WITH_S3ALIAS,
														   BUCKET_NAME_DOES_NOT_END_WITH_OL_S3,
														   BUCKET_LENGTH_BETWEEN_3_AND_63)
													   .collect(Collectors.joining("", "^", "$"));

	private final Map<BucketProperty, String> configuration = new EnumMap<>(BucketProperty.class);

	private final BucketRecord bucketKey;

	private final CredentialsRecord credentials;

	public BucketDescriptor(URI uri)
	{
		this(uri, null);
	}

	public BucketDescriptor(URI uri, Map<String, ?> configuration)
	{
		checkUri(uri);
		Optional.ofNullable(configuration).orElseGet(Map::of).forEach(this::addProperty);
		var path = Optional.ofNullable(uri.getPath())
						   .filter(Predicate.not(PATH_SEPARATOR::equals))
						   .map(item -> item.endsWith(PATH_SEPARATOR)
							   ? item.substring(1, item.length() - 1)
							   : item.substring(1));
		var bucketName = Stream.of(path,
								   Optional.ofNullable(uri.getHost()),
								   Optional.ofNullable(uri.getAuthority()))
							   .filter(Optional::isPresent)
							   .findFirst()
							   .flatMap(Function.identity())
							   .filter(item -> Pattern.matches(BUCKET_PATTERN, item))
							   .orElseThrow(() -> new BucketNameException(uri));
		var endpoint = path.isPresent()
			? uri.getHost()
			: this.configuration.remove(BucketProperty.ENDPOINT);
		bucketKey = new BucketRecord(Optional.ofNullable(endpoint), bucketName);
		credentials = Optional.ofNullable(uri.getUserInfo())
							  .map(this::createCredentialsRecord)
							  .orElseGet(() -> new CredentialsRecord(this.configuration.remove(BucketProperty.ACCESS_KEY),
																	 this.configuration.remove(BucketProperty.SECRET_KEY)));
	}

	/**
	 * The wrapper method of the {@link #configuration} property.
	 *
	 * @return the value of the property
	 */
	public Map<BucketProperty, String> configuration()
	{
		return Collections.unmodifiableMap(configuration);
	}

	/**
	 * The wrapper method of the {@link #bucketKey} property.
	 *
	 * @return the value of the property
	 */
	public BucketRecord bucketKey()
	{
		return bucketKey;
	}

	/**
	 * The wrapper method of the {@link #credentials} property.
	 *
	 * @return the value of the property
	 */
	public CredentialsRecord credentials()
	{
		return credentials;
	}

	public Optional<String> region()
	{
		return Optional.ofNullable(configuration.get(BucketProperty.REGION));
	}

	private URI checkUri(URI uri)
	{
		Objects.requireNonNull(uri, () -> "Missing URI.");
		return Optional.ofNullable(uri)
					   .filter(item -> BucketProperty.SCHEME.equals(item.getScheme()))
					   .orElseThrow(() -> new BucketUriException(uri));
	}

	private void addProperty(String key, Object object)
	{
		BucketProperty.of(key).ifPresent(item -> addProperty(item, object));
	}

	private void addProperty(BucketProperty property, Object object)
	{
		Stream.<Supplier<?>>of(() -> object,
							   () -> System.getProperty(property.toProperty()),
							   () -> System.getenv(property.toProperty()),
							   property::defaultValue)
			  .map(Supplier::get)
			  .filter(Objects::nonNull)
			  .findFirst()
			  .map(Object::toString)
			  .ifPresent(item -> configuration.put(property, item));
	}

	private CredentialsRecord createCredentialsRecord(String userInfo)
	{
		var message = "Malformed credentials in URI: they must be in one of the forms 'access-key' or 'access-key:secret-key'";
		var tokens = Optional.of(userInfo.split(":")).filter(item -> item.length <= 2);
		var accessKey = tokens.orElseThrow(() -> new CredentialsException(message))[0];
		var secretKey = tokens.filter(item -> item.length == 2)
							  .map(item -> item[1])
							  .orElseThrow(() -> new CredentialsException(message));
		return new CredentialsRecord(accessKey, secretKey);
	}
}
