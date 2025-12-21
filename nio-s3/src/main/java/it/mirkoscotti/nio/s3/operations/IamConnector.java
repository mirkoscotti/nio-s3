package it.mirkoscotti.nio.s3.operations;

import java.io.Closeable;
import java.io.IOException;
import java.util.Comparator;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

import it.mirkoscotti.nio.s3.enums.BucketAction;
import it.mirkoscotti.nio.s3.functions.Try;
import it.mirkoscotti.nio.s3.helpers.ExceptionsHelper;

import software.amazon.awssdk.services.iam.IamAsyncClient;
import software.amazon.awssdk.services.iam.IamAsyncClientBuilder;
import software.amazon.awssdk.services.iam.model.EvaluationResult;
import software.amazon.awssdk.services.iam.model.PolicyEvaluationDecisionType;
import software.amazon.awssdk.services.iam.model.SimulatePrincipalPolicyRequest.Builder;
import software.amazon.awssdk.services.iam.model.SimulatePrincipalPolicyResponse;
import software.amazon.awssdk.services.sts.model.StsException;

/**
 * @author mirko.scotti
 * @version Dec 14, 2025
 */
public final class IamConnector
	implements AwsConnector, Closeable
{

	private static final String RESOURCE_ARN = "arn:aws:s3:::%s/%s";

	private final IamAsyncClient client;

	IamConnector(IamAsyncClient client)
	{
		this.client = client;
	}

	@Override
	public void close() throws IOException
	{
		client.close();
	}

	@Override
	public int hashCode()
	{
		return client.hashCode();
	}

	@Override
	public boolean equals(Object obj)
	{
		return obj instanceof IamConnector other && Objects.equals(client, other.client);
	}

	public static IamConnectorBuilder create()
	{
		return new IamConnectorBuilder();
	}

	public String permissionOnFile(String arn, String bucket, String key)
	{
		var simulation = new SimulationRecord(arn, bucket, key);
		return Try.to(() -> simulate(simulation, BucketAction.S3_PUT_OBJECT))
				  .onCatch(item -> ExceptionsHelper.redirectException(item, StsException.class))
				  .get();
	}

	public String permissionOnDirectory(String arn, String bucket, String key)
	{
		var simulation = new SimulationRecord(arn, bucket, key);
		return Try.to(() -> simulate(simulation,
									 BucketAction.S3_PUT_OBJECT,
									 BucketAction.S3_DELETE_OBJECT))
				  .onCatch(item -> ExceptionsHelper.redirectException(item, StsException.class))
				  .get();
	}

	private String simulate(SimulationRecord simulation, BucketAction... bucketActions)
		throws TimeoutException,
			ExecutionException,
			InterruptedException
	{
		return client.simulatePrincipalPolicy(item -> simulate(simulation, item, bucketActions))
					 .thenApply(this::guessPermission)
					 .get(30, TimeUnit.SECONDS);
	}

	private void simulate(SimulationRecord simulation,
						  Builder builder,
						  BucketAction... bucketActions)
	{
		var resourceArns = RESOURCE_ARN.formatted(simulation.bucket(), simulation.key());
		var actions = Stream.of(bucketActions).map(BucketAction::tag).toArray(String[]::new);
		builder.policySourceArn(simulation.arn()).actionNames(actions).resourceArns(resourceArns);
	}

	private String guessPermission(SimulatePrincipalPolicyResponse response)
	{
		return response.evaluationResults()
					   .stream()
					   .map(EvaluationResult::evalDecision)
					   .min(Comparator.comparingInt(PolicyEvaluationDecisionType::ordinal))
					   .filter(Predicate.not(PolicyEvaluationDecisionType.ALLOWED::equals))
					   .map(PolicyEvaluationDecisionType::toString)
					   .orElse(null);
	}

	public static final class IamConnectorBuilder
		extends
		ClientConnectorBuilder<IamConnectorBuilder, IamAsyncClientBuilder, IamConnector, IamAsyncClient>
	{

		IamConnectorBuilder()
		{
			super(IamAsyncClient.builder());
		}

		@Override
		protected Function<IamAsyncClient, IamConnector> connectorCreator()
		{
			return IamConnector::new;
		}

		@Override
		protected IamConnectorBuilder thisBuilder()
		{
			return this;
		}
	}

	private static record SimulationRecord(String arn, String bucket, String key)
	{

	}
}
