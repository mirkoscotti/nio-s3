package it.mirkoscotti.nio.s3.operations;

/**
 * @author mirko.scotti
 * @version Dec 13, 2025
 */
public sealed interface AwsConnector
	permits IamConnector, S3Connector, StsConnector
{

}
