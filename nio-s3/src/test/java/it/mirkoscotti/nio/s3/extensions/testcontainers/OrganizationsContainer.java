package it.mirkoscotti.nio.s3.extensions.testcontainers;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.Assertions;
import org.testcontainers.images.builder.Transferable;
import org.testcontainers.localstack.LocalStackContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * @author mirko.scotti
 * @version Dec 09, 2025
 */
public class OrganizationsContainer
	extends LocalStackContainer
{

	private static final String LOCALSTACK = "localstack";

	private static final String IMAGE_NAME = "%1$s/%1$s:4.11.0".formatted(LOCALSTACK);

	private static final String INSTALL_JQ_COMMAND = "apt-get update && apt-get install -y jq";

	private static final String INITIALIZATION_FILE = "/etc/localstack/init/ready.d/init-s3.sh";

	private static final String GENERIC_COMMAND = """
		#!/bin/bash
		%s
		""";

	private static final String LIST_USERS_COMMAND = "awslocal iam list-users | jq -r '.Users[].UserName'";

	private static final String CREATE_ACCESS_KEY_COMMAND = "awslocal iam create-access-key --user-name %s | jq -r '.AccessKey | \"\\(.AccessKeyId) \\(.SecretAccessKey)\"'";

	private static final String DESCRIBE_ORGANIZATION_COMMAND = "awslocal organizations describe-organization";

	private final List<String> commands = new ArrayList<>();

	private final Map<String, Credentials> credentials = new HashMap<>();

	public OrganizationsContainer()
	{
		super(DockerImageName.parse(IMAGE_NAME));
		withServices("s3", "iam");
		commands.add(INSTALL_JQ_COMMAND);
	}

	@Override
	public void start()
	{
		var command = commands.stream().collect(Collectors.joining("\n"));
		var script = GENERIC_COMMAND.formatted(command);
		withCopyToContainer(Transferable.of(script.getBytes(StandardCharsets.UTF_8), 755),
							INITIALIZATION_FILE);
		super.start();
		createCredentials();
	}

	public void checkAccount()
	{
		describeOrganization();
	}

	private void describeOrganization()
	{
		try
		{
			var output = execInContainer(DESCRIBE_ORGANIZATION_COMMAND.split(" "));
			Assertions.assertEquals(0, output.getExitCode(), "Failed to describr organization");
		}
		catch (InterruptedException x)
		{
			Thread.currentThread().interrupt();
			Assertions.fail(x);
		}
		catch (IOException x)
		{
			Assertions.fail(x);
		}
	}

	private void createCredentials()
	{
		try
		{
			var output = execInContainer("sh", "-c", LIST_USERS_COMMAND);
			var message = "Failed to list users.";
			Assertions.assertEquals(0, output.getExitCode(), message);
			Optional.of(output.getStdout())
					.filter(Predicate.not(String::isBlank))
					.stream()
					.flatMap(item -> Stream.of(item.split("\n")))
					.forEach(this::createCredentials);
		}
		catch (InterruptedException x)
		{
			Thread.currentThread().interrupt();
			Assertions.fail(x);
		}
		catch (IOException x)
		{
			Assertions.fail(x);
		}
	}

	private void createCredentials(String user)
	{
		var command = CREATE_ACCESS_KEY_COMMAND.formatted(user);
		try
		{
			var output = execInContainer("sh", "-c", command);
			var message = "Failed to create credentials for user %s";
			Assertions.assertEquals(0, output.getExitCode(), message.formatted(user));
			var result = output.getStdout().split(" ");
			credentials.put(user, new Credentials(result[0], result[1]));
		}
		catch (InterruptedException x)
		{
			Thread.currentThread().interrupt();
			Assertions.fail(x);
		}
		catch (IOException x)
		{
			Assertions.fail(x);
		}
	}

	private static record Credentials(String accessKey, String secretKey)
	{

	}
}
