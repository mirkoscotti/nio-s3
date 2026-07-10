package io.github.mirkoscotti.nio.s3.operations;

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

import io.github.mirkoscotti.nio.s3.enums.BucketAction;
import io.github.mirkoscotti.nio.s3.functions.Try;
import io.github.mirkoscotti.nio.s3.helpers.ExceptionHelper;

import software.amazon.awssdk.services.iam.IamAsyncClient;
import software.amazon.awssdk.services.iam.IamAsyncClientBuilder;
import software.amazon.awssdk.services.iam.model.EvaluationResult;
import software.amazon.awssdk.services.iam.model.PolicyEvaluationDecisionType;
import software.amazon.awssdk.services.iam.model.SimulatePrincipalPolicyRequest.Builder;
import software.amazon.awssdk.services.iam.model.SimulatePrincipalPolicyResponse;

/**
 * IAM connector used to evaluate S3 permissions for a given principal.
 *
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

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void close() throws IOException
	{
		client.close();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int hashCode()
	{
		return client.hashCode();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean equals(Object obj)
	{
		return obj instanceof IamConnector other && Objects.equals(client, other.client);
	}

	/**
	 * Creates a new connector builder.
	 *
	 * @return the builder instance
	 */
	public static IamConnectorBuilder create()
	{
		return new IamConnectorBuilder();
	}

	/**
	 * Evaluates whether the given principal is allowed to put an object at the given key in the
	 * specified bucket.
	 *
	 * @param arn
	 *            the ARN of the principal to simulate
	 * @param bucket
	 *            the target bucket
	 * @param key
	 *            the target object key
	 * @return the denying AWS <code>PolicyEvaluationDecisionType</code> name, or null if the action
	 *         is allowed
	 */
	public String filePermission(String arn, String bucket, String key)
	{
		var simulation = new SimulationRecord(arn, bucket, key);
		return Try.to(() -> simulate(simulation, BucketAction.S3_PUT_OBJECT))
				  .onCatch(ExceptionHelper::sneakyThrow)
				  .get();
	}

	/**
	 * Evaluates whether the given principal is allowed to put and delete objects at the given in
	 * the specified bucket, treating it as a directory.
	 *
	 * @param arn
	 *            the ARN of the principal to simulate
	 * @param bucket
	 *            the target bucket
	 * @param key
	 *            the target object key
	 * @return the denying AWS <code>PolicyEvaluationDecisionType</code> name, or null if all
	 *         actions are allowed
	 */
	public String directoryPermission(String arn, String bucket, String key)
	{
		var simulation = new SimulationRecord(arn, bucket, key);
		return Try.to(() -> simulate(simulation,
									 BucketAction.S3_PUT_OBJECT,
									 BucketAction.S3_DELETE_OBJECT))
				  .onCatch(ExceptionHelper::sneakyThrow)
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

	/**
	 * Builder for {@link IamConnector}.
	 */
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
