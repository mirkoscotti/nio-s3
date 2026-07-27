package io.github.mirkoscotti.nio.s3.records;

import java.util.Objects;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentials;

/**
 * The generic credentials key.
 *
 * @param accessKey
 *            the key pair's access key
 * @param secretKey
 *            key the key pair's secret key
 * @author mirko.scotti
 * @version Oct 27, 2024
 */
public record CredentialsRecord(String accessKey, String secretKey)
{

	/**
	 * Creates an instance with well defined credentials.
	 *
	 * @param accessKey
	 *            the access key
	 * @param secretKey
	 *            the secret key
	 */
	public CredentialsRecord
	{
		Objects.requireNonNull(accessKey, () -> "Missing access key.");
		Objects.requireNonNull(secretKey, () -> "Missing secret key.");
	}

	/**
	 * Converts the embedded credentials to a native AWS credentials object.
	 *
	 * @return the AWS credentials object
	 */
	public AwsCredentials awsCredentials()
	{
		return AwsBasicCredentials.create(accessKey, secretKey);
	}
}
