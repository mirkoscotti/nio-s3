package it.mirkoscotti.nio.s3.extensions.jdk.jsr203;

import java.io.IOException;
import java.nio.file.DirectoryIteratorException;
import java.nio.file.DirectoryStream.Filter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import it.mirkoscotti.nio.s3.operations.AwsFacade;

/**
 * @author mirko.scotti
 * @version Dec 29, 2025
 */
@ExtendWith(MockitoExtension.class)
class BucketDirectoryStreamTest
{

	private static final String BUCKET = "bucket";

	private static final String FILE = "file";

	@Mock
	private BucketFileSystem fileSystem;

	@Mock
	private BucketFileStore fileStore;

	@Mock
	private AwsFacade awsFacade;

	@Mock
	private BucketPath path;

	@Mock
	private Filter<? super Path> filter;

	@Test
	void filterFailureTest()
	{
		Mockito.when(path.getFileSystem()).thenReturn(fileSystem);
		Mockito.when(fileSystem.getFileStores()).thenReturn(List.of(fileStore));
		Mockito.when(fileSystem.awsFacade()).thenReturn(awsFacade);
		Mockito.when(fileStore.name()).thenReturn(BUCKET);
		Mockito.when(awsFacade.scanDirectory(Mockito.anyString(), Mockito.anyString()))
			   .thenReturn(List.of(FILE).iterator());
		try (var mock = Mockito.mockStatic(Files.class))
		{
			Mockito.when(filter.accept(Mockito.any(Path.class))).thenThrow(IOException.class);
			mock.when(() -> Files.isDirectory(Mockito.any(Path.class))).thenReturn(true);
			try (var stream = new BucketDirectoryStream(path, filter))
			{
				Assertions.assertThrows(DirectoryIteratorException.class,
										() -> stream.forEach(this::doNothing));
			}
		}
		catch (IOException x)
		{
			Assertions.fail(x);
		}
	}

	private void doNothing(Object object)
	{
		// Nothing to do
	}
}
