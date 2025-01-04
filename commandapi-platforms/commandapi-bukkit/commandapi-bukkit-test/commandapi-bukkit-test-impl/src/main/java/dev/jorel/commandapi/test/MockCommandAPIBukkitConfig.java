package dev.jorel.commandapi.test;

import dev.jorel.commandapi.CommandAPIBukkitConfig;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * An implementation of {@link CommandAPIBukkitConfig} for the test environment.
 * Not made specifically for Spigot or Paper.
 */
public class MockCommandAPIBukkitConfig extends CommandAPIBukkitConfig<MockCommandAPIBukkitConfig> {
	public MockCommandAPIBukkitConfig(JavaPlugin plugin) {
		super(plugin);
	}

	@Override
	public MockCommandAPIBukkitConfig instance() {
		return this;
	}
}
