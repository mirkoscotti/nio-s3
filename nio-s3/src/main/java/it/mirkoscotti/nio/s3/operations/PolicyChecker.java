package it.mirkoscotti.nio.s3.operations;

import it.mirkoscotti.nio.s3.records.OperationRecord;

import java.util.Objects;

import software.amazon.awssdk.services.s3.S3AsyncClient;

/**
 * @author mirko.scotti
 * @version Jun 23, 2025
 */
public class PolicyChecker
{

	private final S3AsyncClient client;

	private final String bucket;

	private final String key;

	public PolicyChecker(OperationRecord operationRecord)
	{
		Objects.requireNonNull(operationRecord, () -> "Missing operation specifications.");
		client = Objects.requireNonNull(operationRecord.client(), () -> "Missing client.");
		bucket = Objects.requireNonNull(operationRecord.bucket(), () -> "Missing bucket.");
		key = Objects.requireNonNull(operationRecord.key(), () -> "Missing key.");
	}
}
