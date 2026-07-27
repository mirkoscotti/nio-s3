package io.github.mirkoscotti.nio.s3.enums;

import java.nio.file.WatchEvent.Modifier;

import io.github.mirkoscotti.nio.s3.extensions.jdk.jsr203.DirectoryWatchService;

/**
 * Custom modifier configurable in a monitor.
 *
 * @author mirko.scotti
 * @version Mar 29, 2025
 * @see DirectoryWatchService
 */
public enum BucketModifier
	implements Modifier
{

	/**
	 * The specific event modifier to watch directories recursively.
	 */
	RECURSIVE;
}
