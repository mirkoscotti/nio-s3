package it.mirkoscotti.nio.s3.helpers;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Assertions;

/**
 * @author mirko.scotti
 * @version Apr 25, 2025
 */
public final class IoHelper
{

	private IoHelper()
	{
		super();
	}

	public static Path createNotEmptyFile(Path file) throws IOException
	{
		try (var writer = Files.newBufferedWriter(file, StandardOpenOption.CREATE_NEW))
		{
			writer.write("test");
		}
		return file;
	}

	public static void createNotEmptyFile(Path file, long size) throws IOException
	{
		try (var writer = Files.newBufferedWriter(file, StandardOpenOption.CREATE_NEW))
		{
			var counter = new AtomicLong();
			IntStream.iterate(1, i -> updateCounter(counter, writer, i) < size, i -> i + 1).count();
		}
		catch (IOException x)
		{
			Assertions.fail(x);
		}
	}

	private static long updateCounter(AtomicLong counter, BufferedWriter writer, int rowIndex)
	{
		var bytesWritten = JunitHelper.tryCall(() -> write(writer, rowIndex));
		return counter.addAndGet(bytesWritten);
	}

	private static int write(BufferedWriter writer, int rowIndex) throws IOException
	{
		var row = "Row %d - %s".formatted(rowIndex, UUID.randomUUID().toString());
		writer.write(row);
		writer.newLine();
		return row.length();
	}
}
