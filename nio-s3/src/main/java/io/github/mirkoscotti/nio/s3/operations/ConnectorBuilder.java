package io.github.mirkoscotti.nio.s3.operations;

import java.net.URI;

/**
 * @author mirko.scotti
 * @version Dec 13, 2025
 */
interface ConnectorBuilder<B extends ConnectorBuilder<B, T>, T>
{

	B withEndpoint(URI endpoint);

	B withRegion(String region);

	B withCredentials(String accessKey, String secretKey);

	T build();
}
