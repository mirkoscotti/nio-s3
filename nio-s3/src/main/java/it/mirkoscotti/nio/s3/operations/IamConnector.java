package it.mirkoscotti.nio.s3.operations;

import java.io.Closeable;
import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;

import it.mirkoscotti.nio.s3.enums.BucketAction;
import it.mirkoscotti.nio.s3.functions.Try;
import it.mirkoscotti.nio.s3.helpers.ExceptionsHelper;

import software.amazon.awssdk.services.iam.IamAsyncClient;
import software.amazon.awssdk.services.iam.IamAsyncClientBuilder;
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

	private static final String PUT_OBJECT = BucketAction.S3_PUT_OBJECT.tag();

	private static final String DELETE_OBJECT = BucketAction.S3_DELETE_OBJECT.tag();

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

	public boolean canWriteFile(String arn, String bucket, String key)
	{
		var simulation = new SimulationRecord(arn, bucket, key);
		return Try.to(() -> simulateFile(simulation))
				  .onCatch(item -> ExceptionsHelper.redirectException(item, StsException.class))
				  .get();
	}

	public boolean canWriteDirectory(String arn, String bucket, String key)
	{
		var simulation = new SimulationRecord(arn, bucket, key);
		return Try.to(() -> simulateDirectory(simulation))
				  .onCatch(item -> ExceptionsHelper.redirectException(item, StsException.class))
				  .get();
	}

	private boolean simulateFile(SimulationRecord simulation)
		throws TimeoutException,
			ExecutionException,
			InterruptedException
	{
		return client.simulatePrincipalPolicy(item -> simulateFile(simulation, item))
					 .thenApply(this::guessCanWrite)
					 .get(30, TimeUnit.SECONDS);
	}

	private void simulateFile(SimulationRecord simulation, Builder builder)
	{
		var resourceArns = RESOURCE_ARN.formatted(simulation.bucket(), simulation.key());
		builder.policySourceArn(simulation.arn())
			   .actionNames(PUT_OBJECT)
			   .resourceArns(resourceArns);
	}

	private boolean simulateDirectory(SimulationRecord simulation)
		throws TimeoutException,
			ExecutionException,
			InterruptedException
	{
		return client.simulatePrincipalPolicy(item -> simulateDirectory(simulation, item))
					 .thenApply(this::guessCanWrite)
					 .get(30, TimeUnit.SECONDS);
	}

	private void simulateDirectory(SimulationRecord simulation, Builder builder)
	{
		var resourceArns = RESOURCE_ARN.concat("/*")
									   .formatted(simulation.bucket(), simulation.key());
		builder.policySourceArn(simulation.arn())
			   .actionNames(PUT_OBJECT, DELETE_OBJECT)
			   .resourceArns(resourceArns);
	}

	private boolean guessCanWrite(SimulatePrincipalPolicyResponse response)
	{
		return response.evaluationResults()
					   .stream()
					   .anyMatch(result -> PUT_OBJECT.equals(result.evalActionName())
						   && PolicyEvaluationDecisionType.ALLOWED.equals(result.evalDecision()));
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
