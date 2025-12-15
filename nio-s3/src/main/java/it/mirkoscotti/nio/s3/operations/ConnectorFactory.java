package it.mirkoscotti.nio.s3.operations;

import java.net.URI;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

import it.mirkoscotti.nio.s3.records.FactoryRecord;

import software.amazon.awssdk.regions.Region;

/**
 * @author mirko.scotti
 * @version Dec 12, 2025
 */
public class ConnectorFactory
{

	private final FactoryRecord factoryRecord;

	private IamConnector iamConnector = null;

	private S3Connector s3Connector = null;

	private StsConnector stsConnector = null;

	private ConnectorFactory(FactoryRecord factoryRecord)
	{
		this.factoryRecord = factoryRecord;
	}

	public static ConnectorFactory create(FactoryRecord factoryRecord)
	{
		Objects.requireNonNull(factoryRecord, () -> "Missing endpoint, region and credentials.");
		return new ConnectorFactory(factoryRecord);
	}

	public IamConnector iamConnector()
	{
		iamConnector = Optional.ofNullable(iamConnector)
							   .orElseGet(() -> createConnector(IamConnector::create));
		return iamConnector;
	}

	public S3Connector s3Connector()
	{
		s3Connector = Optional.ofNullable(s3Connector)
							  .orElseGet(() -> createConnector(S3Connector::create));
		return s3Connector;
	}

	public StsConnector stsConnector()
	{
		stsConnector = Optional.ofNullable(stsConnector)
							   .orElseGet(() -> createConnector(StsConnector::create));
		return stsConnector;
	}

	private <T extends AwsConnectorBuilder<T, ?, C, ?>,
			 C extends AwsConnector> C createConnector(Supplier<T> builder)
	{
		var credentials = factoryRecord.credentials();
		var connectorBuilder = builder.get()
									  .withCredentials(credentials.accessKey(),
													   credentials.secretKey());
		factoryRecord.endpoint().map(URI::create).ifPresent(connectorBuilder::withEndpoint);
		factoryRecord.region().map(Region::toString).ifPresent(connectorBuilder::withRegion);
		return connectorBuilder.build();
	}
}
