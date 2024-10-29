/*
 * (C) Copyright 2019 - 2024 - Add Value S.R.L - All rights reserved.
 */

package it.mirkoscotti.nio.s3.records;

import java.util.Objects;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentials;

/**
 * @author mirko.scotti
 * @version Oct 27, 2024
 */
public record CredentialsRecord(String accessKey, String secretKey)
{

	public CredentialsRecord
	{
		Objects.requireNonNull(accessKey, () -> "Missing access key.");
		Objects.requireNonNull(secretKey, () -> "Missing secret key.");
	}

	public AwsCredentials awsCredentials()
	{
		return AwsBasicCredentials.create(accessKey, secretKey);
	}
}
