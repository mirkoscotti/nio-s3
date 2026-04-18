package it.mirkoscotti.nio.s3.records;

import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import io.github.mirkoscotti.nio.s3.enums.BucketAction;
import io.github.mirkoscotti.nio.s3.enums.BucketEffect;

/**
 * @author mirko.scotti
 * @version Oct 27, 2024
 */
@ExtendWith(MockitoExtension.class)
class PolicyRecordTest
{

	private static final String VERSION = "version";

	private static final String PRINCIPAL = "principal";

	@Test
	void emptyStatementTest(@Mock List<StatementRecord> statements)
	{
		var policyRecord = new PolicyRecord(VERSION, statements);
		Assertions.assertFalse(policyRecord.isReadOnly());
	}

	@Test
	void readOnlyActionsAllowedTest()
	{
		var effect = BucketEffect.ALLOW.toString();
		var actions = BucketAction.readOnlyActions().map(BucketAction::tag).toList();
		var statement = new StatementRecord(effect, PRINCIPAL, actions, List.of());
		var policyRecord = new PolicyRecord(VERSION, List.of(statement));
		Assertions.assertTrue(policyRecord.isReadOnly());
	}

	@Test
	void allWriteActionsDeniedTest()
	{
		var effect = BucketEffect.DENY.toString();
		var actions = BucketAction.writeActions().map(BucketAction::tag).toList();
		var statement = new StatementRecord(effect, PRINCIPAL, actions, List.of());
		var policyRecord = new PolicyRecord(VERSION, List.of(statement));
		Assertions.assertTrue(policyRecord.isReadOnly());
	}

	@Test
	void oneWriteActionAllowedTest()
	{
		var effect = BucketEffect.ALLOW.toString();
		var action = BucketAction.writeActions().iterator().next().tag();
		var statement = new StatementRecord(effect, PRINCIPAL, List.of(action), List.of());
		var policyRecord = new PolicyRecord(VERSION, List.of(statement));
		Assertions.assertFalse(policyRecord.isReadOnly());
	}

	@Test
	void wildcardTest()
	{
		var effect = BucketEffect.ALLOW.toString();
		var statement = new StatementRecord(effect, PRINCIPAL, List.of("s3:*"), List.of());
		var policyRecord = new PolicyRecord(VERSION, List.of(statement));
		Assertions.assertFalse(policyRecord.isReadOnly());
		statement = new StatementRecord(effect, PRINCIPAL, List.of("s3:Get*"), List.of());
		policyRecord = new PolicyRecord(VERSION, List.of(statement));
		Assertions.assertTrue(policyRecord.isReadOnly());
	}
}
