package io.github.mirkoscotti.nio.s3.extensions.jdk.collections;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Consumer;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedConstruction.Context;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.jupiter.MockitoExtension;

import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.paginators.ListObjectsV2Publisher;

/**
 * @author mirko.scotti
 * @version Dec 28, 2025
 */
@SuppressWarnings("unchecked")
@ExtendWith(MockitoExtension.class)
class DirectoryIteratorTest
{

	private static final String BUCKET = "bucket";

	private static final String PREFIX = "prefix";

	@Mock
	private S3AsyncClient client;

	@Mock
	private ListObjectsV2Publisher publisher;

	@BeforeEach
	void beforeEach()
	{
		when(client.listObjectsV2Paginator(Mockito.any(Consumer.class))).thenReturn(publisher);
	}

	@Test
	void cannotPutElementTest()
	{
		when(publisher.subscribe(Mockito.any(Consumer.class))).thenAnswer(this::mockResponse);
		try (var mock = Mockito.mockConstruction(LinkedBlockingQueue.class, this::configurePutFail))
		{
			Assertions.assertThrows(IllegalStateException.class,
									() -> new DirectoryIterator(client, BUCKET, PREFIX));
		}
	}

	@Test
	void cannotTakeElementTest()
	{
		try (var mock = Mockito.mockConstruction(LinkedBlockingQueue.class,
												 this::configureTakeFail))
		{
			Assertions.assertThrows(IllegalStateException.class,
									() -> new DirectoryIterator(client, BUCKET, PREFIX));
		}
	}

	private void configurePutFail(LinkedBlockingQueue<String> mock, Context context)
	{
		try
		{
			doThrow(new InterruptedException("Test interrupt")).when(mock).put(Mockito.anyString());
		}
		catch (InterruptedException x)
		{
			Assertions.fail(x);
		}
	}

	private void configureTakeFail(LinkedBlockingQueue<String> mock, Context context)
	{
		try
		{
			when(mock.take()).thenThrow(new InterruptedException("Test interrupt"));
		}
		catch (InterruptedException x)
		{
			Assertions.fail(x);
		}
	}

	private Void mockResponse(InvocationOnMock invocation)
	{
		var response = mock(ListObjectsV2Response.class);
		Consumer<ListObjectsV2Response> consumer = invocation.getArgument(0);
		consumer.accept(response);
		return null;
	}
}
