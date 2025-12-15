package it.mirkoscotti.nio.s3.operations;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import it.mirkoscotti.nio.s3.operations.IamConnector.IamConnectorBuilder;
import it.mirkoscotti.nio.s3.operations.S3Connector.S3ConnectorBuilder;
import it.mirkoscotti.nio.s3.operations.StsConnector.StsConnectorBuilder;
import it.mirkoscotti.nio.s3.records.CredentialsRecord;
import it.mirkoscotti.nio.s3.records.FactoryRecord;

/**
 * @author mirko.scotti
 * @version Dec 12, 2025
 */
@ExtendWith(MockitoExtension.class)
class ConnectorFactoryTest
{

	@Mock
	private FactoryRecord factoryRecord;

	@Test
	void instantiationTest()
	{
		Assertions.assertThrows(NullPointerException.class, () -> ConnectorFactory.create(null));
		Assertions.assertDoesNotThrow(() -> ConnectorFactory.create(factoryRecord));
	}

	@Test
	void iamConnectorTest(@Mock CredentialsRecord credentialsRecord,
						  @Mock IamConnectorBuilder builder)
	{
		Mockito.when(factoryRecord.credentials()).thenReturn(credentialsRecord);
		Mockito.when(credentialsRecord.accessKey()).thenReturn("access-key");
		Mockito.when(credentialsRecord.secretKey()).thenReturn("secret-key");
		Mockito.when(builder.withCredentials(Mockito.anyString(), Mockito.anyString()))
			   .thenReturn(builder);
		try (var mock = Mockito.mockStatic(IamConnector.class))
		{
			mock.when(IamConnector::create).thenReturn(builder);
			var factory = ConnectorFactory.create(factoryRecord);
			factory.iamConnector();
			mock.verify(IamConnector::create, Mockito.atLeastOnce());
		}
	}

	@Test
	void s3ConnectorTest(@Mock CredentialsRecord credentialsRecord,
						 @Mock S3ConnectorBuilder builder)
	{
		Mockito.when(factoryRecord.credentials()).thenReturn(credentialsRecord);
		Mockito.when(credentialsRecord.accessKey()).thenReturn("access-key");
		Mockito.when(credentialsRecord.secretKey()).thenReturn("secret-key");
		Mockito.when(builder.withCredentials(Mockito.anyString(), Mockito.anyString()))
			   .thenReturn(builder);
		try (var mock = Mockito.mockStatic(S3Connector.class))
		{
			mock.when(S3Connector::create).thenReturn(builder);
			var factory = ConnectorFactory.create(factoryRecord);
			factory.s3Connector();
			mock.verify(S3Connector::create, Mockito.atLeastOnce());
		}
	}

	@Test
	void stsConnectorTest(@Mock CredentialsRecord credentialsRecord,
						  @Mock StsConnectorBuilder builder)
	{
		Mockito.when(factoryRecord.credentials()).thenReturn(credentialsRecord);
		Mockito.when(credentialsRecord.accessKey()).thenReturn("access-key");
		Mockito.when(credentialsRecord.secretKey()).thenReturn("secret-key");
		Mockito.when(builder.withCredentials(Mockito.anyString(), Mockito.anyString()))
			   .thenReturn(builder);
		try (var mock = Mockito.mockStatic(StsConnector.class))
		{
			mock.when(StsConnector::create).thenReturn(builder);
			var factory = ConnectorFactory.create(factoryRecord);
			factory.stsConnector();
			mock.verify(StsConnector::create, Mockito.atLeastOnce());
		}
	}
}
