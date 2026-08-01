package io.github.mirkoscotti.nio.s3.extensions.jdk.jsr203;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.file.ClosedWatchServiceException;
import java.nio.file.FileStore;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.platform.commons.function.Try;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedConstruction.Context;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.jupiter.MockitoExtension;

import io.github.mirkoscotti.nio.s3.operations.AwsFacade;

/**
 * @author mirko.scotti
 * @version Apr 22, 2025
 */
@ExtendWith(MockitoExtension.class)
class DirectoryWatchServiceTest
{

	@Mock
	private AwsFacade awsFacade;

	@Mock
	private ScheduledExecutorService scheduler;

	@Test
	void nullTest()
	{
		Assertions.assertThrows(NullPointerException.class, () -> new DirectoryWatchService(null));
		try (var mock = Mockito.mockStatic(Executors.class))
		{
			mock.when(() -> Executors.newSingleThreadScheduledExecutor(Mockito.any(ThreadFactory.class)))
				.thenReturn(scheduler);
			Assertions.assertDoesNotThrow(() -> new DirectoryWatchService(awsFacade));
		}
	}

	@Test
	void ioExceptionDuringCloseTest()
	{
		try (var mock = Mockito.mockStatic(Executors.class))
		{
			Try.call(() -> when(scheduler.awaitTermination(Mockito.anyLong(),
														   Mockito.any(TimeUnit.class))).thenReturn(false));
			mock.when(() -> Executors.newSingleThreadScheduledExecutor(Mockito.any(ThreadFactory.class)))
				.thenReturn(scheduler);
			var watchService = new DirectoryWatchService(awsFacade);
			Assertions.assertThrows(IOException.class, watchService::close);
		}
	}

	@Test
	void interruptedExceptionDuringCloseTest()
	{
		try (var mock = Mockito.mockStatic(Executors.class))
		{
			Try.call(() -> doThrow(InterruptedException.class).when(scheduler)
															  .awaitTermination(Mockito.anyLong(),
																				Mockito.any(TimeUnit.class)));
			mock.when(() -> Executors.newSingleThreadScheduledExecutor(Mockito.any(ThreadFactory.class)))
				.thenReturn(scheduler);
			var watchService = new DirectoryWatchService(awsFacade);
			Assertions.assertThrows(IOException.class, watchService::close);
		}
	}

	@Test
	void closeTest() throws InterruptedException
	{
		try (var mock = mockStatic(Executors.class))
		{
			Try.call(() -> when(scheduler.awaitTermination(Mockito.anyLong(),
														   Mockito.any(TimeUnit.class))).thenReturn(true));
			mock.when(() -> Executors.newSingleThreadScheduledExecutor(Mockito.any(ThreadFactory.class)))
				.thenReturn(scheduler);
			var watchService = new DirectoryWatchService(awsFacade);
			Assertions.assertDoesNotThrow(watchService::close);
			verify(scheduler, Mockito.atLeastOnce()).awaitTermination(Mockito.anyLong(),
																	  Mockito.any(TimeUnit.class));
			when(scheduler.isTerminated()).thenReturn(true);
			Assertions.assertThrows(ClosedWatchServiceException.class, watchService::poll);
		}
	}

	@Test
	void pollTest() throws IOException
	{
		try (var queueMock = Mockito.mockConstruction(LinkedBlockingQueue.class);
			 var schedulerMock = Mockito.mockStatic(Executors.class))
		{
			Try.call(() -> when(scheduler.awaitTermination(Mockito.anyLong(),
														   Mockito.any(TimeUnit.class))).thenReturn(true));
			schedulerMock.when(() -> Executors.newSingleThreadScheduledExecutor(Mockito.any(ThreadFactory.class)))
						 .thenReturn(scheduler);
			try (var watchService = new DirectoryWatchService(awsFacade))
			{
				watchService.poll();
				var queue = queueMock.constructed().get(0);
				verify(queue, Mockito.atLeastOnce()).poll();
			}
		}
	}

	@Test
	void customPollTest() throws IOException, InterruptedException
	{
		try (var queueMock = Mockito.mockConstruction(LinkedBlockingQueue.class);
			 var schedulerMock = Mockito.mockStatic(Executors.class))
		{
			Try.call(() -> when(scheduler.awaitTermination(Mockito.anyLong(),
														   Mockito.any(TimeUnit.class))).thenReturn(true));
			schedulerMock.when(() -> Executors.newSingleThreadScheduledExecutor(Mockito.any(ThreadFactory.class)))
						 .thenReturn(scheduler);
			try (var watchService = new DirectoryWatchService(awsFacade))
			{
				watchService.poll(1, TimeUnit.SECONDS);
				var queue = queueMock.constructed().get(0);
				verify(queue, Mockito.atLeastOnce()).poll(Mockito.anyLong(),
														  Mockito.any(TimeUnit.class));
			}
		}
	}

	@Test
	void takeTest() throws IOException, InterruptedException
	{
		try (var queueMock = Mockito.mockConstruction(LinkedBlockingQueue.class);
			 var schedulerMock = Mockito.mockStatic(Executors.class))
		{
			Try.call(() -> when(scheduler.awaitTermination(Mockito.anyLong(),
														   Mockito.any(TimeUnit.class))).thenReturn(true));
			schedulerMock.when(() -> Executors.newSingleThreadScheduledExecutor(Mockito.any(ThreadFactory.class)))
						 .thenReturn(scheduler);
			try (var watchService = new DirectoryWatchService(awsFacade))
			{
				watchService.take();
				var queue = queueMock.constructed().get(0);
				verify(queue, Mockito.atLeastOnce()).take();
			}
		}
	}

	@Test
	void registerUndefinedPathTest() throws IOException
	{
		try (var mapMock = Mockito.mockConstruction(ConcurrentHashMap.class);
			 var schedulerMock = Mockito.mockStatic(Executors.class))
		{
			Try.call(() -> when(scheduler.awaitTermination(Mockito.anyLong(),
														   Mockito.any(TimeUnit.class))).thenReturn(true));
			schedulerMock.when(() -> Executors.newSingleThreadScheduledExecutor(Mockito.any(ThreadFactory.class)))
						 .thenReturn(scheduler);
			try (var watchService = new DirectoryWatchService(awsFacade))
			{
				Assertions.assertThrows(NullPointerException.class,
										() -> watchService.registerPath(null));
			}
		}
	}

	@Test
	@SuppressWarnings("unchecked")
	void registerPathTest(@Mock BucketPath directory,
						  @Mock BucketFileSystem fileSystem,
						  @Mock BucketFileStore fileStore)
		throws IOException
	{
		when(directory.getFileSystem()).thenReturn(fileSystem);
		when(fileSystem.getFileStores()).thenReturn(List.of(fileStore));
		try (var schedulerMock = Mockito.mockStatic(Executors.class);
			 var mapMock = Mockito.mockConstruction(ConcurrentHashMap.class,
													this::initializeRegistry))
		{
			var argumentCaptor = ArgumentCaptor.forClass(Runnable.class);
			Try.call(() -> when(scheduler.awaitTermination(Mockito.anyLong(),
														   Mockito.any(TimeUnit.class))).thenReturn(true));
			schedulerMock.when(() -> Executors.newSingleThreadScheduledExecutor(Mockito.any(ThreadFactory.class)))
						 .thenReturn(scheduler);
			try (var watchService = new DirectoryWatchService(awsFacade))
			{
				watchService.registerPath(directory);
				if (mapMock.constructed().get(0) instanceof Map<?, ?> map)
				{
					verify(map, Mockito.atLeastOnce()).compute(Mockito.any(),
															   Mockito.any(BiFunction.class));
					verify(scheduler,
						   Mockito.atLeastOnce()).scheduleAtFixedRate(argumentCaptor.capture(),
																	  Mockito.anyLong(),
																	  Mockito.anyLong(),
																	  Mockito.any(TimeUnit.class));
					var runnable = argumentCaptor.getValue();
					Assertions.assertNotNull(runnable);
					runnable.run();
					verify(map, Mockito.atLeastOnce()).compute(Mockito.any(),
															   Mockito.any(BiFunction.class));
					verify(awsFacade, Mockito.atLeastOnce()).listObjects(Mockito.anyString(),
																		 Mockito.anyString());
				}
				else
				{
					Assertions.fail("Should never occur.");
				}
			}
		}
	}

	@SuppressWarnings("unchecked")
	private void initializeRegistry(ConcurrentHashMap<String, Instant> registry, Context context)
	{
		Mockito.doAnswer(this::registrationAnswer)
			   .when(registry)
			   .forEach(Mockito.any(BiConsumer.class));
	}

	private Void registrationAnswer(InvocationOnMock invocation)
	{
		var fileStore = mock(FileStore.class);
		when(fileStore.name()).thenReturn("bucket-name");
		var fileSystem = mock(BucketFileSystem.class);
		when(fileSystem.getFileStores()).thenReturn(List.of(fileStore));
		var directory = mock(BucketPath.class);
		when(directory.getFileSystem()).thenReturn(fileSystem);
		var watchKey = mock(DirectoryWatchKey.class);
		BiConsumer<BucketPath, DirectoryWatchKey> consumer = invocation.getArgument(0);
		consumer.accept(directory, watchKey);
		return null;
	}
}
