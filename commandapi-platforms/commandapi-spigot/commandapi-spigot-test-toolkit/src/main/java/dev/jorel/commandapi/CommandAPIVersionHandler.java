package dev.jorel.commandapi;

public abstract class CommandAPIVersionHandler {
	// Allow loading a different platform implementation (most likely to implement something `MockCommandAPISpigot` doesn't)
	private static CommandAPIPlatform<?, ?, ?> alternativePlatform = null;

	/**
	 * Configures the test kit to use the given {@link CommandAPIPlatform} when the CommandAPI is loaded.
	 *
	 * @param platform The {@link CommandAPIPlatform} to use for the next test. This will likely be a custom
	 *                 implementation of {@link MockCommandAPISpigot} that overrides a method you need to run
	 *                 tests that doesn't have a proper implementation in {@link MockCommandAPISpigot}.
	 */
	public static void usePlatformImplementation(CommandAPIPlatform<?, ?, ?> platform) {
		alternativePlatform = platform;
	}

	static LoadContext getPlatform() {
		// Default to MockCommandAPIBukkit if not given
		CommandAPIPlatform<?, ?, ?> platform = alternativePlatform == null ? new MockCommandAPISpigot() : alternativePlatform;

		// Reset to avoid platform persisting between tests
		alternativePlatform = null;

		return new LoadContext(platform);
	}
}
