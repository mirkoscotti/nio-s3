package io.github.mirkoscotti.nio.s3.configuration;

import java.net.URI;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import io.github.mirkoscotti.nio.s3.enums.BucketProperty;
import io.github.mirkoscotti.nio.s3.exceptions.BucketUriException;
import io.github.mirkoscotti.nio.s3.records.CredentialsRecord;

/**
 * A simple S3 URI parser. The URI must be compliant with the AWS
 * <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/VirtualHosting.html">styles</a>,
 * but differently from what is specified in the documentation, the scheme must be <code>s3</code>,
 * instead of <code>http</code> or <code>https</code>.
 * <p>
 * The following are valid examples:
 * <p>
 * <table border="1">
 * <caption style="text-align:left; margin-bottom:10px"><b>Compact Virtual Hosted
 * Style</b></caption>
 * <tr>
 * <th style="text-align:left">Example</th>
 * <td><code>s3://bucket-name</code></td>
 * </tr>
 * <tr>
 * <th style="text-align:left">Description</th>
 * <td>It is the compact representation of a bucket in the default region of an AWS account when
 * used with any of the Java NIO.2 APIs specified below. If it is required to refer to a custom
 * region or to access LocalStack instead of the real AWS platform, they can be configured via
 * properties.</td>
 * </tr>
 * <tr>
 * <th style="text-align:left">Java NIO APIs</th>
 * <td><code>FileSystems.newFileSystem(uri, properties)</code></td>
 * </tr>
 * </table>
 * <p>
 * <table border="1">
 * <caption style="text-align:left; margin-bottom:10px"><b>Extended Virtual Hosted
 * Style</b></caption>
 * <tr>
 * <th style="text-align:left">Example</th>
 * <td><code>s3://bucket-name.endpoint</code><br>
 * <code>s3://bucket-name.region.endpoint}</td>
 * </tr>
 * <tr>
 * <th style="text-align:left">Description</th>
 * <td>It is the extended representation of a bucket in the default or specified region at the given
 * endpoint, when used with any of the Java NIO.2 APIs specified below. If it is required to refer
 * to a custom region or to access LocalStack instead of the real AWS platform, they can be
 * configured via properties.</td>
 * </tr>
 * <tr>
 * <th style="text-align:left">Java NIO APIs</th>
 * <td><code>FileSystems.newFileSystem(uri, properties)</code></td>
 * </tr>
 * </table>
 * <p>
 * <table border="1">
 * <caption style="text-align:left; margin-bottom:10px"><b>Authenticated Virtual Hosted
 * Style</b></caption>
 * <tr>
 * <th style="text-align:left">Example</th>
 * <td><code>s3://access-key:secret-key@bucket-name.endpoint</code><br>
 * <code>s3://access-key:secret-key@bucket-name.region.endpoint</code></td>
 * </tr>
 * <tr>
 * <th style="text-align:left">Description</th>
 * <td>It is the complete representation of a bucket in the default or specified region at the given
 * endpoint, when used with any of the Java NIO.2 APIs specified above. If it is required to refer
 * to a custom region or to access LocalStack instead of the real AWS platform, they can be
 * configured via properties.</td>
 * </tr>
 * <tr>
 * <th style="text-align:left">Java NIO APIs</th>
 * <td><code>FileSystems.newFileSystem(uri, properties)</code><br>
 * <code>Paths.get(uri)}</td>
 * </tr>
 * </table>
 * <p>
 * <table border="1">
 * <caption style="text-align:left; margin-bottom:10px"><b>Path Style</b></caption>
 * <tr>
 * <th style="text-align:left">Example</th>
 * <td><code>s3://endpoint/bucket-name</code><br>
 * <code>s3://region.endpoint/bucket-name</code></td>
 * </tr>
 * <tr>
 * <th style="text-align:left">Java NIO APIs</th>
 * <td><code>FileSystems.newFileSystem(uri, properties)</code></td>
 * </tr>
 * <tr>
 * <th style="text-align:left">Description</th>
 * <td>It is the path-style representation of a bucket at the given endpoint, when used with the
 * Java NIO.2 APIs listed below. If it is required to refer to a custom region or to access
 * LocalStack instead of the real AWS platform, they can be configured via properties.</td>
 * </tr>
 * </table>
 * <p>
 * <table border="1">
 * <caption style="text-align:left; margin-bottom:10px"><b>Authenticated Path Style</b></caption>
 * <tr>
 * <th style="text-align:left">Example</th>
 * <td><code>s3://access-key:secret-key@endpoint/bucket-name</code><br>
 * <code>s3://access-key:secret-key@region.endpoint/bucket-name</code></td>
 * </tr>
 * <tr>
 * <th style="text-align:left">Java NIO APIs</th>
 * <td><code>FileSystems.newFileSystem(uri, properties)</code><br>
 * <code>Paths.get(uri)</code></td>
 * </tr>
 * <tr>
 * <th style="text-align:left">Description</th>
 * <td>It is the complete path-style representation of a bucket at the given endpoint, when used
 * with the Java NIO.2 APIs listed below. If it is required to refer to a custom region or to access
 * LocalStack instead of the real AWS platform, they can be configured via properties.</td>
 * </tr>
 * </table>
 *
 * @author mirko.scotti
 * @version Mar 03, 2025
 */
public class UriDescriptor
{

	private final String bucketName;

	private final Optional<String> endpoint;

	private final Optional<CredentialsRecord> credentials;

	UriDescriptor(URI uri)
	{
		var checkedUri = checkedUri(uri);
		var host = checkedUri.getHost();
		bucketName = virtualHostBucketName(host).orElseGet(() -> pathBucketName(uri));
		endpoint = host.equals(bucketName)
			? Optional.empty()
			: endpoint(host, checkedUri.getPort());
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
					   .map(item -> List.of(item).indexOf("s3"))
					   .filter(item -> item >= 0)
					   .map(item -> IntStream.range(0, item)
											 .mapToObj(i -> array[i])
											 .collect(Collectors.joining(".")));
	}

	private String pathBucketName(URI uri)
	{
		return Optional.ofNullable(uri.getPath())
					   .map(item -> item.split(BucketDescriptor.PATH_SEPARATOR))
					   .filter(item -> item.length > 1)
					   .map(item -> item[1])
					   .orElseGet(uri::getHost);
	}

	private Optional<String> endpoint(String host, int port)
	{
		var uri = URI.create(host);
		var protocol = Optional.ofNullable(uri.getScheme()).orElse("https");
		var realHost = host.startsWith(bucketName) ? host.substring(bucketName.length() + 1) : host;
		var result = "%s://%s".formatted(protocol, realHost);
		return Optional.of(port < 0 ? result : String.join(":", result, Integer.toString(port)));
	}
}
