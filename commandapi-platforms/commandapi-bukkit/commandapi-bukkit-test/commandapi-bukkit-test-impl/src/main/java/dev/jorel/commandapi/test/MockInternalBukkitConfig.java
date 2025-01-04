package dev.jorel.commandapi.test;

import dev.jorel.commandapi.InternalBukkitConfig;

/**
 * An implementation of {@link InternalBukkitConfig} for the test environment.
 * Not made specifically for Spigot or Paper.
 */
public class MockInternalBukkitConfig extends InternalBukkitConfig {
	public MockInternalBukkitConfig(MockCommandAPIBukkitConfig config) {
		super(config);
	}
}
