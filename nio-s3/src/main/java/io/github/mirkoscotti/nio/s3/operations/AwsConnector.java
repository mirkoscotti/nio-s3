package io.github.mirkoscotti.nio.s3.operations;

/**
 * Marker interface representing a connector to one of the AWS services required to manage the
 * access to S3 according to the Java NIO.2 specifications.
 *
 * @author mirko.scotti
 * @since Dec 13, 2025
 * @see IamConnector
 * @see S3Connector
 * @see StsConnector
 */
public sealed interface AwsConnector
	permits IamConnector, S3Connector, StsConnector
{

}
