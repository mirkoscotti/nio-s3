/*
 * (C) Copyright 2019 - 2025 - Add Value S.R.L - All rights reserved.
 */

package it.mirkoscotti.nio.s3.extensions.jdk.jsr203;

import java.io.IOException;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import it.mirkoscotti.nio.s3.operations.S3Connector;

/**
 * @author mirko.scotti
 * @version Nov 29, 2025
 */
@ExtendWith(MockitoExtension.class)
class BucketWritableByteChannelTest
{

	@Mock
	private S3Connector connector;

	@Mock
	private BucketPath path;

	@Mock
	private BucketFileSystem fileSystem;

	@BeforeEach
	void beforeEach()
	{
		Mockito.when(path.getFileSystem()).thenReturn(fileSystem);
		Mockito.when(fileSystem.bucketName()).thenReturn("test-bucket");
	}

	@Test
	void isOpenTest()
	{
		var writableByteChannel = new BucketWritableByteChannel(connector, path);
		try (var channel = writableByteChannel)
		{
			Assertions.assertTrue(channel.isOpen());
		}
		catch (IOException x)
		{
			Assertions.fail(x);
		}
		Assertions.assertFalse(writableByteChannel.isOpen());
	}
}
