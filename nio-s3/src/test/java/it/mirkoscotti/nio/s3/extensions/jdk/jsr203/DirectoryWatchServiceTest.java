package it.mirkoscotti.nio.s3.extensions.jdk.jsr203;

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

import it.mirkoscotti.nio.s3.operations.AwsFacade;

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
			Try.call(() -> Mockito.when(scheduler.awaitTermination(Mockito.anyLong(),
																   Mockito.any(TimeUnit.class)))
								  .thenReturn(false));
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
			Try.call(() -> Mockito.doThrow(InterruptedException.class)
								  .when(scheduler)
								  .awaitTermination(Mockito.anyLong(),
													Mockito.any(TimeUnit.class)));
			mock.when(() -> Executors.newSingleThreadScheduledExecutor(Mockito.any(ThreadFactory.class)))
				.thenReturn(scheduler);
			var watchService = new DirectoryWatchService(awsFacade);
			Assertions.assertThrows(IOException.class, watchService::close);
		}
	}

	@Test
	void closeTest()
	{
		try (var mock = Mockito.mockStatic(Executors.class))
		{
			Try.call(() -> Mockito.when(scheduler.awaitTermination(Mockito.anyLong(),
																   Mockito.any(TimeUnit.class)))
								  .thenReturn(true));
			mock.when(() -> Executors.newSingleThreadScheduledExecutor(Mockito.any(ThreadFactory.class)))
				.thenReturn(scheduler);
			var watchService = new DirectoryWatchService(awsFacade);
			Assertions.assertDoesNotThrow(watchService::close);
			Mockito.verify(scheduler, Mockito.atLeastOnce())
				   .awaitTermination(Mockito.anyLong(), Mockito.any(TimeUnit.class));
			Mockito.when(scheduler.isTerminated()).thenReturn(true);
			Assertions.assertThrows(ClosedWatchServiceException.class, watchService::poll);
		}
		catch (Exception x)
		{
			Assertions.fail(x);
		}
	}

	@Test
	void pollTest()
	{
		try (var queueMock = Mockito.mockConstruction(LinkedBlockingQueue.class);
			 var schedulerMock = Mockito.mockStatic(Executors.class))
		{
			Try.call(() -> Mockito.when(scheduler.awaitTermination(Mockito.anyLong(),
																   Mockito.any(TimeUnit.class)))
								  .thenReturn(true));
			schedulerMock.when(() -> Executors.newSingleThreadScheduledExecutor(Mockito.any(ThreadFactory.class)))
						 .thenReturn(scheduler);
			try (var watchService = new DirectoryWatchService(awsFacade))
			{
				watchService.poll();
				var queue = queueMock.constructed().get(0);
				Mockito.verify(queue, Mockito.atLeastOnce()).poll();
			}
			catch (IOException x)
			{
				Assertions.fail(x);
			}
		}
	}

	@Test
	void customPollTest()
	{
		try (var queueMock = Mockito.mockConstruction(LinkedBlockingQueue.class);
			 var schedulerMock = Mockito.mockStatic(Executors.class))
		{
			Try.call(() -> Mockito.when(scheduler.awaitTermination(Mockito.anyLong(),
																   Mockito.any(TimeUnit.class)))
								  .thenReturn(true));
			schedulerMock.when(() -> Executors.newSingleThreadScheduledExecutor(Mockito.any(ThreadFactory.class)))
						 .thenReturn(scheduler);
			try (var watchService = new DirectoryWatchService(awsFacade))
			{
				watchService.poll(1, TimeUnit.SECONDS);
				var queue = queueMock.constructed().get(0);
				Mockito.verify(queue, Mockito.atLeastOnce())
					   .poll(Mockito.anyLong(), Mockito.any(TimeUnit.class));
			}
			catch (Exception x)
			{
				Assertions.fail(x);
			}
		}
	}

	@Test
	void takeTest()
	{
		try (var queueMock = Mockito.mockConstruction(LinkedBlockingQueue.class);
			 var schedulerMock = Mockito.mockStatic(Executors.class))
		{
			Try.call(() -> Mockito.when(scheduler.awaitTermination(Mockito.anyLong(),
																   Mockito.any(TimeUnit.class)))
								  .thenReturn(true));
			schedulerMock.when(() -> Executors.newSingleThreadScheduledExecutor(Mockito.any(ThreadFactory.class)))
						 .thenReturn(scheduler);
			try (var watchService = new DirectoryWatchService(awsFacade))
			{
				watchService.take();
				var queue = queueMock.constructed().get(0);
				Mockito.verify(queue, Mockito.atLeastOnce()).take();
			}
			catch (Exception x)
			{
				Assertions.fail(x);
			}
		}
	}

	@Test
	void registerUndefinedPathTest()
	{
		try (var mapMock = Mockito.mockConstruction(ConcurrentHashMap.class);
			 var schedulerMock = Mockito.mockStatic(Executors.class))
		{
			Try.call(() -> Mockito.when(scheduler.awaitTermination(Mockito.anyLong(),
																   Mockito.any(TimeUnit.class)))
								  .thenReturn(true));
			schedulerMock.when(() -> Executors.newSingleThreadScheduledExecutor(Mockito.any(ThreadFactory.class)))
						 .thenReturn(scheduler);
			try (var watchService = new DirectoryWatchService(awsFacade))
			{
				Assertions.assertThrows(NullPointerException.class,
										() -> watchService.registerPath(null));
			}
			catch (Exception x)
			{
				Assertions.fail(x);
			}
		}
	}

	@Test
	@SuppressWarnings("unchecked")
	void registerPathTest(@Mock BucketPath directory,
						  @Mock BucketFileSystem fileSystem,
						  @Mock BucketFileStore fileStore)
	{
		Mockito.when(directory.getFileSystem()).thenReturn(fileSystem);
		Mockito.when(fileSystem.getFileStores()).thenReturn(List.of(fileStore));
		try (var schedulerMock = Mockito.mockStatic(Executors.class);
			 var mapMock = Mockito.mockConstruction(ConcurrentHashMap.class,
													this::initializeRegistry))
		{
			var argumentCaptor = ArgumentCaptor.forClass(Runnable.class);
			Try.call(() -> Mockito.when(scheduler.awaitTermination(Mockito.anyLong(),
																   Mockito.any(TimeUnit.class)))
								  .thenReturn(true));
			schedulerMock.when(() -> Executors.newSingleThreadScheduledExecutor(Mockito.any(ThreadFactory.class)))
						 .thenReturn(scheduler);
			try (var watchService = new DirectoryWatchService(awsFacade))
			{
				watchService.registerPath(directory);
				if (mapMock.constructed().get(0) instanceof Map<?, ?> map)
				{
					Mockito.verify(map, Mockito.atLeastOnce())
						   .compute(Mockito.any(), Mockito.any(BiFunction.class));
					Mockito.verify(scheduler, Mockito.atLeastOnce())
						   .scheduleAtFixedRate(argumentCaptor.capture(), Mockito.anyLong(),
												Mockito.anyLong(), Mockito.any(TimeUnit.class));
					var runnable = argumentCaptor.getValue();
					Assertions.assertNotNull(runnable);
					runnable.run();
					Mockito.verify(map, Mockito.atLeastOnce())
						   .compute(Mockito.any(), Mockito.any(BiFunction.class));
					Mockito.verify(awsFacade, Mockito.atLeastOnce())
						   .listObjects(Mockito.anyString(), Mockito.anyString());
				}
				else
				{
					Assertions.fail("Should never occur.");
				}
			}
			catch (Exception x)
			{
				Assertions.fail(x);
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
		var fileStore = Mockito.mock(FileStore.class);
		Mockito.when(fileStore.name()).thenReturn("bucket-name");
		var fileSystem = Mockito.mock(BucketFileSystem.class);
		Mockito.when(fileSystem.getFileStores()).thenReturn(List.of(fileStore));
		var directory = Mockito.mock(BucketPath.class);
		Mockito.when(directory.getFileSystem()).thenReturn(fileSystem);
		var watchKey = Mockito.mock(DirectoryWatchKey.class);
		BiConsumer<BucketPath, DirectoryWatchKey> consumer = invocation.getArgument(0);
		consumer.accept(directory, watchKey);
		return null;
	}
}
