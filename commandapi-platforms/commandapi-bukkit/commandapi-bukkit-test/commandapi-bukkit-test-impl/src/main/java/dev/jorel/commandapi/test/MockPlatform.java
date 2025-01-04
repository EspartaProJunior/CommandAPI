package dev.jorel.commandapi.test;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.context.ParsedArgument;
import dev.jorel.commandapi.*;
import dev.jorel.commandapi.commandsenders.*;
import dev.jorel.commandapi.nms.NMS;
import dev.jorel.commandapi.wrappers.NativeProxyCommandSender;
import org.bukkit.*;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.help.HelpTopic;
import org.bukkit.inventory.ItemFactory;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.mockito.Mockito;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.security.CodeSource;
import java.util.*;
import java.util.jar.JarFile;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * An implementation of {@link CommandAPIBukkit} for the test environment.
 * Not made specifically for Spigot or Paper.
 */
public abstract class MockPlatform<CLW> extends CommandAPIBukkit<CLW> implements Enums {
	// TODO: Does this work here, or is it actually version-specific?
	static {
		CodeSource src = PotionEffectType.class.getProtectionDomain().getCodeSource();
		if (src != null) {
			System.err.println("Loading PotionEffectType sources from " + src.getLocation());
		}
	}

	/*****************
	 * Instantiation *
	 *****************/

	private static MockPlatform<?> instance = null;

	@SuppressWarnings("unchecked")
	public static <CLW> MockPlatform<CLW> getInstance() {
		return (MockPlatform<CLW>) instance;
	}

	protected MockPlatform() {
		if (MockPlatform.instance == null) {
			MockPlatform.instance = this;
		} else {
			// wtf why was this called twice?
			throw new IllegalStateException("MockPlatform loaded twice? I don't think this should happen!");
		}

		setInstance(this);
	}
	
	public static void unload() {
		MockPlatform.instance = null;
	}

	/***********
	 * NMS Spy *
	 ***********/

	protected void createNMSSpy(NMS<CLW> baseNMS) {
		// Set up a Mockito spy
		//  We want to forward most methods to the original implementation so we
		//  can test that code, but we need to override some methods to help them
		//  play nice with MockBukkit.
		NMS<CLW> spy = Mockito.spy(baseNMS);

		// Version-specific setup
		setupNMSSpy(spy);

		// General setup
		// Ignore, nothing to do here
		Mockito.doNothing().when(spy).reloadDataPacks();

		// Stub in our getMinecraftServer implementation
		Mockito.doAnswer(i -> getMinecraftServer()).when(spy).getMinecraftServer();
		// Stub in our getSimpleCommandMap implementation
		//  nms throws a class cast exception  (`CraftServer` vs `CommandAPIServerMock`)
		Mockito.doAnswer(i -> getSimpleCommandMap()).when(spy).getSimpleCommandMap();
		// Stub in our getHelpMap implementation
		//  nms throws a class cast exception (`SimpleHelpMap` vs `HelpMapMock`)
		Mockito.doAnswer(i -> getHelpMap()).when(spy).getHelpMap();

		this.nms = spy;
	}

	protected abstract void setupNMSSpy(NMS<CLW> spy);

	public abstract <T> T getMinecraftServer();

	public abstract SimpleCommandMap getSimpleCommandMap();

	public abstract Map<String, HelpTopic> getHelpMap();

	/************************************
	 * CommandAPIBukkit implementations *
	 ************************************/

	private final CommandDispatcher<CLW> brigadierDispatcher = new CommandDispatcher<>();
	private final CommandDispatcher<CLW> resourcesDispatcher = new CommandDispatcher<>();

	public CommandDispatcher<CLW> getMockBrigadierDispatcher() {
		return brigadierDispatcher;
	}

	public CommandDispatcher<CLW> getMockResourcesDispatcher() {
		return resourcesDispatcher;
	}

	private static void setInternalConfig(InternalBukkitConfig config) {
		CommandAPIBukkit.config = config;
	}

	@Override
	public void onLoad(CommandAPIConfig<?> config) {
		if (config instanceof MockCommandAPIBukkitConfig mockConfig) {
			setInternalConfig(new MockInternalBukkitConfig(mockConfig));
		} else {
			CommandAPI.logError("CommandAPIBukkit was loaded with non-Bukkit config!");
			CommandAPI.logError("Attempts to access Bukkit-specific config variables will fail!");
		}
		super.onLoad();
	}

	// Below is copied from Spigot
	@Override
	public void onEnable() {
		JavaPlugin plugin = getConfiguration().getPlugin();

		Bukkit.getScheduler().runTaskLater(plugin, () -> {
			// Sort out permissions after the server has finished registering them all
			getCommandRegistrationStrategy().runTasksAfterServerStart();
			if (!getConfiguration().skipReloadDatapacks()) {
				reloadDataPacks();
			}
			updateHelpForCommands(CommandAPI.getRegisteredCommands());
		}, 0L);

		super.stopCommandRegistrations();
	}

	@Override
	public CommandMap getCommandMap() {
		return getSimpleCommandMap();
	}

	@Override
	public Platform activePlatform() {
		return Platform.SPIGOT;
	}

	@Override
	public BukkitCommandSender<? extends CommandSender> wrapCommandSender(CommandSender sender) {
		if (sender instanceof BlockCommandSender block) {
			return new BukkitBlockCommandSender(block);
		}
		if (sender instanceof ConsoleCommandSender console) {
			return new BukkitConsoleCommandSender(console);
		}
		if (sender instanceof Player player) {
			return new BukkitPlayer(player);
		}
		if (sender instanceof org.bukkit.entity.Entity entity) {
			return new BukkitEntity(entity);
		}
		if (sender instanceof NativeProxyCommandSender nativeProxy) {
			return new BukkitNativeProxyCommandSender(nativeProxy);
		}
		if (sender instanceof ProxiedCommandSender proxy) {
			return new BukkitProxiedCommandSender(proxy);
		}
		if (sender instanceof RemoteConsoleCommandSender remote) {
			return new BukkitRemoteConsoleCommandSender(remote);
		}
		throw new RuntimeException("Failed to wrap CommandSender " + sender + " to a CommandAPI-compatible BukkitCommandSender");
	}

	/******************
	 * Helper methods *
	 ******************/

	public static Object getField(Class<?> className, String fieldName, Object instance) {
		return getField(className, fieldName, fieldName, instance);
	}

	public static Object getField(Class<?> className, String fieldName, String mojangMappedName, Object instance) {
		try {
			Field field = className.getDeclaredField(SafeVarHandle.USING_MOJANG_MAPPINGS ? mojangMappedName : fieldName);
			field.setAccessible(true);
			return field.get(instance);
		} catch (ReflectiveOperationException e) {
			throw new IllegalStateException("Reflection failed :(", e);
		}
	}

	public static void setField(Class<?> className, String fieldName, Object instance, Object value) {
		setField(className, fieldName, fieldName, instance, value);
	}

	public static void setField(Class<?> className, String fieldName, String mojangMappedName, Object instance, Object value) {
		try {
			Field field = className.getDeclaredField(SafeVarHandle.USING_MOJANG_MAPPINGS ? mojangMappedName : fieldName);
			field.setAccessible(true);
			field.set(instance, value);
		} catch (ReflectiveOperationException e) {
			throw new IllegalStateException("Reflection failed :(", e);
		}
	}

	public static <T> T getFieldAs(Class<?> className, String fieldName, Object instance, Class<T> asType) {
		return getFieldAs(className, fieldName, fieldName, instance, asType);
	}

	public static <T> T getFieldAs(Class<?> className, String fieldName, String mojangMappedName, Object instance, Class<T> asType) {
		try {
			Field field = className.getDeclaredField(SafeVarHandle.USING_MOJANG_MAPPINGS ? mojangMappedName : fieldName);
			field.setAccessible(true);
			return asType.cast(field.get(instance));
		} catch (ReflectiveOperationException e) {
			throw new IllegalStateException("Reflection failed :(", e);
		}
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static <T> T forceGetArgument(CommandContext cmdCtx, String key) {
		Map<String, ParsedArgument> result = getFieldAs(CommandContext.class, "arguments", cmdCtx, Map.class);
		return result == null ? null : (T) result.get(key).getResult();
	}

	/***************
	 * Other stuff *
	 ***************/

	public abstract ItemFactory getItemFactory();

	public abstract org.bukkit.advancement.Advancement addAdvancement(NamespacedKey key);

	public abstract void addFunction(NamespacedKey key, List<String> commands);
	public abstract void addTag(NamespacedKey key, List<List<String>> commands);

	public abstract Player setupMockedCraftPlayer(String name);

	/**
	 * Converts 1.16.5 and below potion effect names to NamespacedKey names. For
	 * example, converts "effect.minecraft.speed" into "minecraft:speed"
	 *
	 * @param potionEffectType the potion effect to get the namespaced key for
	 * @return a Minecraft namespaced key name for a potion effect
	 */
	public abstract String getBukkitPotionEffectTypeName(PotionEffectType potionEffectType);

	public abstract String getNMSParticleNameFromBukkit(Particle particle);
	
	// Overrideable
	public int popFunctionCallbackResult() {
		throw new IllegalStateException("Pop function callback result hasn't been overridden");
	}
	
	static record Pair<A, B>(A first, B second) {}
	
	/**
	 * Gets recipes from {@code data/minecraft/recipes/<file>.json}. Parses them and
	 * returns a list of {@code {name, json}}, where {@code name} is the name of the
	 * file without the {@code .json} extension, and {@code json} is the parsed JSON
	 * result from the file
	 * 
	 * @param minecraftServerClass an instance of MinecraftServer.class
	 * @return A list of pairs of resource locations (with no namespace) and JSON objects
	 */
	public final List<Pair<String, JsonObject>> getRecipes(Class<?> minecraftServerClass) {
		List<Pair<String, JsonObject>> list = new ArrayList<>();
		// Get the spigot-x.x.x-Rx.x-SNAPSHOT.jar file
		try(JarFile jar = new JarFile(minecraftServerClass.getProtectionDomain().getCodeSource().getLocation().getPath())) {
			// Iterate over everything in the jar 
			jar.entries().asIterator().forEachRemaining(entry -> {
				if(entry.getName().startsWith("data/minecraft/recipes/") && entry.getName().endsWith(".json")) {
					// If it's what we want, read everything
					InputStream is = minecraftServerClass.getClassLoader().getResourceAsStream(entry.getName());
					String jsonStr = new BufferedReader(new InputStreamReader(is))
						.lines()
						.map(line -> {
							// We can't load tags in the testing environment. If we have any recipes that
							// use tags as ingredients (e.g. wooden_axe or charcoal), we'll get an illegal
							// state exception from TagUtil complaining that a tag has been used before it
							// was bound. To mitigate this, we simply remove all tags and put in a dummy
							// item (in this case, stick)
							if(line.contains("\"tag\": ")) {
								return "\"item\": \"minecraft:stick\"";
							}
							return line;
						})
						.collect(Collectors.joining("\n"));
					// Get the resource location (file name, no extension, no path) and parse the JSON.
					// Using deprecated method as the alternative doesn't exist in 1.17
					@SuppressWarnings("deprecation")
					JsonObject parsedJson = new JsonParser().parse(jsonStr).getAsJsonObject();
					list.add(new Pair<>(entry.getName().substring("data/minecraft/recipes/".length(), entry.getName().lastIndexOf(".")), parsedJson));
				}
			});
		} catch (IOException e) {
			throw new IllegalStateException("Failed to load any recipes for testing!", e);
		}
		
		return list;
	}

	/**
	 * @return A list of all item names, sorted in alphabetical order. Each item
	 * is prefixed with {@code minecraft:}
	 */
	public abstract List<String> getAllItemNames();
	
	public abstract List<NamespacedKey> getAllRecipes();
	
	/**********
	 * Runtime object registries (enchantments, potions etc.)
	 ********/
	
	public Map<Class<?>, Map<NamespacedKey, Object>> registry = null;
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public <T extends Keyed> void addToRegistry(Class<T> className, NamespacedKey key, T object) {
		if (registry == null) {
			registry = new HashMap<>();
		}
		
		if (registry.containsKey(className)) {
			registry.get(className).put(key, object);
		} else {
			Map<NamespacedKey, T> registryMap = new HashMap<>();
			registryMap.put(key, object);
			registry.put(className, (Map) registryMap);
		}
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public <T extends Keyed> Registry<T> getRegistry(Class<T> className) {
		return new Registry() {
			@Nullable
			public T get(@NotNull NamespacedKey key) {
				return (T) registry.get(className).get(key);
			}
			
			@NotNull
			public Stream<T> stream() {
				return (Stream) registry.get(className).values().stream();
			}

			public Iterator<T> iterator() {
				return (Iterator) registry.get(className).values().iterator();
			}
		};
	}
}
