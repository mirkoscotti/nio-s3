package it.mirkoscotti.nio.s3.helpers;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

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
}
