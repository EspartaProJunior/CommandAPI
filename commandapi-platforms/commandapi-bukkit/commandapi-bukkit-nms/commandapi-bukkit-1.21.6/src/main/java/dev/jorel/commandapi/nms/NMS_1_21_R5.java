/*******************************************************************************
 * Copyright 2024 Jorel Ali (Skepter) - MIT License
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *******************************************************************************/
package dev.jorel.commandapi.nms;

import com.google.common.collect.ImmutableList;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.Message;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.datafixers.util.Pair;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.CommandAPIHandler;
import dev.jorel.commandapi.CommandRegistrationStrategy;
import dev.jorel.commandapi.PaperCommandRegistration;
import dev.jorel.commandapi.SafeVarHandle;
import dev.jorel.commandapi.SpigotCommandRegistration;
import dev.jorel.commandapi.arguments.ArgumentSubType;
import dev.jorel.commandapi.arguments.SuggestionProviders;
import dev.jorel.commandapi.commandsenders.AbstractCommandSender;
import dev.jorel.commandapi.commandsenders.BukkitCommandSender;
import dev.jorel.commandapi.commandsenders.BukkitNativeProxyCommandSender;
import dev.jorel.commandapi.preprocessor.Differs;
import dev.jorel.commandapi.preprocessor.NMSMeta;
import dev.jorel.commandapi.preprocessor.RequireField;
import dev.jorel.commandapi.wrappers.ComplexRecipeImpl;
import dev.jorel.commandapi.wrappers.FloatRange;
import dev.jorel.commandapi.wrappers.FunctionWrapper;
import dev.jorel.commandapi.wrappers.IntegerRange;
import dev.jorel.commandapi.wrappers.Location2D;
import dev.jorel.commandapi.wrappers.NativeProxyCommandSender;
import dev.jorel.commandapi.wrappers.ParticleData;
import dev.jorel.commandapi.wrappers.Rotation;
import dev.jorel.commandapi.wrappers.ScoreboardSlot;
import dev.jorel.commandapi.wrappers.SimpleFunctionWrapper;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.chat.ComponentSerializer;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.critereon.MinMaxBounds;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandResultCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.FunctionInstantiationException;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.ColorArgument;
import net.minecraft.commands.arguments.ComponentArgument;
import net.minecraft.commands.arguments.DimensionArgument;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.MessageArgument;
import net.minecraft.commands.arguments.ObjectiveArgument;
import net.minecraft.commands.arguments.ParticleArgument;
import net.minecraft.commands.arguments.RangeArgument;
import net.minecraft.commands.arguments.ResourceArgument;
import net.minecraft.commands.arguments.ResourceKeyArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.commands.arguments.ScoreHolderArgument;
import net.minecraft.commands.arguments.ScoreboardSlotArgument;
import net.minecraft.commands.arguments.TeamArgument;
import net.minecraft.commands.arguments.blocks.BlockStateArgument;
import net.minecraft.commands.arguments.coordinates.RotationArgument;
import net.minecraft.commands.arguments.coordinates.Vec2Argument;
import net.minecraft.commands.arguments.item.FunctionArgument;
import net.minecraft.commands.arguments.item.ItemArgument;
import net.minecraft.commands.arguments.item.ItemInput;
import net.minecraft.commands.arguments.item.ItemPredicateArgument;
import net.minecraft.commands.arguments.selector.EntitySelector;
import net.minecraft.commands.execution.ExecutionContext;
import net.minecraft.commands.functions.CommandFunction;
import net.minecraft.commands.functions.InstantiatedFunction;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.core.particles.DustColorTransitionOptions;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.SculkChargeParticleOptions;
import net.minecraft.core.particles.ShriekParticleOption;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.particles.TrailParticleOption;
import net.minecraft.core.particles.VibrationParticleOption;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.MinecraftServer.ReloadableResources;
import net.minecraft.server.ServerFunctionLibrary;
import net.minecraft.server.ServerFunctionManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.server.packs.resources.MultiPackResourceManager;
import net.minecraft.server.packs.resources.SimpleReloadInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Unit;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.DataPackConfig;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.block.entity.FuelValues;
import net.minecraft.world.level.gameevent.BlockPositionSource;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.ScoreHolder;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Particle.DustOptions;
import org.bukkit.Particle.DustTransition;
import org.bukkit.Particle.Trail;
import org.bukkit.Registry;
import org.bukkit.Vibration;
import org.bukkit.Vibration.Destination;
import org.bukkit.Vibration.Destination.BlockDestination;
import org.bukkit.World;
import org.bukkit.advancement.Advancement;
import org.bukkit.block.Biome;
import org.bukkit.block.data.BlockData;
import org.bukkit.command.CommandSender;
import org.bukkit.command.SimpleCommandMap;
import org.bukkit.craftbukkit.v1_21_R5.CraftLootTable;
import org.bukkit.craftbukkit.v1_21_R5.CraftParticle;
import org.bukkit.craftbukkit.v1_21_R5.CraftServer;
import org.bukkit.craftbukkit.v1_21_R5.CraftSound;
import org.bukkit.craftbukkit.v1_21_R5.CraftWorld;
import org.bukkit.craftbukkit.v1_21_R5.block.data.CraftBlockData;
import org.bukkit.craftbukkit.v1_21_R5.command.BukkitCommandWrapper;
import org.bukkit.craftbukkit.v1_21_R5.command.VanillaCommandWrapper;
import org.bukkit.craftbukkit.v1_21_R5.entity.CraftEntity;
import org.bukkit.craftbukkit.v1_21_R5.help.CustomHelpTopic;
import org.bukkit.craftbukkit.v1_21_R5.help.SimpleHelpMap;
import org.bukkit.craftbukkit.v1_21_R5.inventory.CraftItemStack;
import org.bukkit.craftbukkit.v1_21_R5.potion.CraftPotionEffectType;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.help.HelpTopic;
import org.bukkit.inventory.Recipe;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Team;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import java.util.function.ToIntFunction;

// Mojang-Mapped reflection

/**
 * NMS implementation for Minecraft 1.21.6
 */
@NMSMeta(compatibleWith = {"1.21.6", "1.21.7", "1.21.8"})
@RequireField(in = SimpleHelpMap.class, name = "helpTopics", ofType = Map.class)
@RequireField(in = EntitySelector.class, name = "usesSelector", ofType = boolean.class)
// @RequireField(in = ItemInput.class, name = "tag", ofType = CompoundTag.class)
@RequireField(in = ServerFunctionLibrary.class, name = "dispatcher", ofType = CommandDispatcher.class)
@RequireField(in = MinecraftServer.class, name = "fuelValues", ofType = FuelValues.class)
public class NMS_1_21_R5 extends NMS_Common {

	private static final SafeVarHandle<SimpleHelpMap, Map<String, HelpTopic>> helpMapTopics;
	private static final Field entitySelectorUsesSelector;
	// private static final SafeVarHandle<ItemInput, CompoundTag> itemInput;
	private static final Field serverFunctionLibraryDispatcher;
	private static final MethodHandle minecraftServerSetSelected;
	private static final boolean vanillaCommandDispatcherFieldExists;
	private static final SafeVarHandle<MinecraftServer, FuelValues> minecraftServerFuelValues;

	// Derived from net.minecraft.commands.Commands;
	private static final CommandBuildContext COMMAND_BUILD_CONTEXT;

	// Compute all var handles all in one go so we don't do this during main server
	// runtime
	static {
		if (Bukkit.getServer() instanceof CraftServer server) {
			COMMAND_BUILD_CONTEXT = CommandBuildContext.simple(server.getServer().registryAccess(),
				server.getServer().getWorldData().getDataConfiguration().enabledFeatures());
		} else {
			COMMAND_BUILD_CONTEXT = null;
		}

		helpMapTopics = SafeVarHandle.ofOrNull(SimpleHelpMap.class, "helpTopics", "helpTopics", Map.class);
		// For some reason, MethodHandles fails for this field, but Field works okay
		entitySelectorUsesSelector = CommandAPIHandler.getField(EntitySelector.class, "p", "usesSelector");
		// itemInput = SafeVarHandle.ofOrNull(ItemInput.class, "c", "tag", CompoundTag.class);
		// For some reason, MethodHandles fails for this field, but Field works okay
		serverFunctionLibraryDispatcher = CommandAPIHandler.getField(ServerFunctionLibrary.class, "h", "dispatcher");

		MethodHandles.Lookup lookup = MethodHandles.lookup();
		MethodHandle setSelected;
		try {
			setSelected = lookup.findVirtual(PackRepository.class, "setSelected", MethodType.methodType(void.class, Collection.class, boolean.class));
		} catch (NoSuchMethodException | IllegalAccessException e) {
			// We're on Spigot or Paper 1.21.4 build 62 or earlier
			setSelected = null;
		}
		minecraftServerSetSelected = setSelected;

		boolean fieldExists;
		try {
			MinecraftServer.class.getDeclaredField("vanillaCommandDispatcher");
			fieldExists = true;
		} catch (NoSuchFieldException | SecurityException e) {
			// Expected on Paper-1.20.6-65 or later due to https://github.com/PaperMC/Paper/pull/8235
			fieldExists = false;
		}
		vanillaCommandDispatcherFieldExists = fieldExists;

		minecraftServerFuelValues = SafeVarHandle.ofOrNull(MinecraftServer.class, "aE", "fuelValues", FuelValues.class);
	}

	// Implementation taken from io.papermc.paper.adventure.WrapperAwareSerializer#deserialize(Component)
	private String toJson(net.minecraft.network.chat.Component component) {
		MinecraftServer server = this.getMinecraftServer();
		RegistryAccess.Frozen access = server.registryAccess();
		RegistryOps<JsonElement> ops = access.createSerializationContext(JsonOps.INSTANCE);
		JsonElement element = ComponentSerialization.CODEC.encodeStart(ops, component)
			.getOrThrow(s -> new RuntimeException("Failed to encode Minecraft Component: " + component + "; " + s));
		return new Gson().toJson(element);
	}

	private net.minecraft.network.chat.Component fromJson(String json) {
		Pair<net.minecraft.network.chat.Component, JsonElement> result = ComponentSerialization.CODEC.decode(JsonOps.INSTANCE, JsonParser.parseString(json))
			.getOrThrow(s -> new RuntimeException("Failed to decode Component: " + json + "; " + s));
		return result.getFirst();
	}

	@Override
	protected CommandBuildContext getCommandBuildContext() {
		return COMMAND_BUILD_CONTEXT;
	}

	@Override
	public ArgumentType<?> _ArgumentAdvancement() {
		return ResourceKeyArgument.key(Registries.ADVANCEMENT);
	}

	@Override
	public ArgumentType<?> _ArgumentChatComponent() {
		return ComponentArgument.textComponent(COMMAND_BUILD_CONTEXT);
	}

	@Override
	public final ArgumentType<?> _ArgumentEnchantment() {
		return ResourceArgument.resource(COMMAND_BUILD_CONTEXT, Registries.ENCHANTMENT);
	}

	@Override
	public ArgumentType<?> _ArgumentRecipe() {
		return ResourceKeyArgument.key(Registries.RECIPE);
	}

	@Override
	public final ArgumentType<?> _ArgumentSyntheticBiome() {
		return ResourceArgument.resource(COMMAND_BUILD_CONTEXT, Registries.BIOME);
	}

	@Override
	public final Map<String, HelpTopic> getHelpMap() {
		return helpMapTopics.get((SimpleHelpMap) Bukkit.getHelpMap());
	}

	@Override
	public String[] compatibleVersions() {
		return new String[]{"1.21.6", "1.21.7", "1.21.8"};
	}

	private static String serializeNMSItemStack(ItemStack is) {
		return new ItemInput(is.getItemHolder(), is.getComponentsPatch()).serialize(COMMAND_BUILD_CONTEXT);
	}

	@Override
	public final String convert(org.bukkit.inventory.ItemStack is) {
		return serializeNMSItemStack(CraftItemStack.asNMSCopy(is));
	}

	@SuppressWarnings({"rawtypes", "unchecked"})
	@Override
	public final String convert(ParticleData<?> particle) {
		final ParticleOptions particleOptions = CraftParticle.createParticleParam(particle.particle(), particle.data());
		final ResourceLocation particleKey = BuiltInRegistries.PARTICLE_TYPE.getKey(particleOptions.getType());

		// /particle dust{scale:2,color:[1,2,2]}
		// Use the particle option's codec to convert the data into NBT. If we have any tags, add them
		// to the end, otherwise leave it as it is (e.g. `/particle crit` as opposed to `/particle crit{}`)
		final Codec codec = particleOptions.getType().codec().codec();
		final DataResult<CompoundTag> result = codec.encodeStart(NbtOps.INSTANCE, particleOptions);
		final CompoundTag particleOptionsTag = result.result().get();
		final String dataString = particleOptionsTag.keySet().isEmpty() ? "" : particleOptionsTag.toString(); // TODO: Check if this is correct
		return particleKey.toString() + dataString;
	}

	/**
	 * An implementation of {@link ServerFunctionManager#execute(CommandFunction, CommandSourceStack)} with a specified
	 * command result callback instead of {@link CommandResultCallback.EMPTY}
	 *
	 * @param commandFunction the command function to run
	 * @param css             the command source stack to execute this command
	 * @return the result of our function. This is either 0 is the command failed, or greater than 0 if the command succeeded
	 */
	private final int runCommandFunction(CommandFunction<CommandSourceStack> commandFunction, CommandSourceStack css) {
		// Profile the function. We want to simulate the execution sequence exactly
		ProfilerFiller profiler = Profiler.get();
		profiler.push(() -> "function " + commandFunction.id());

		// Store our function result
		AtomicInteger result = new AtomicInteger();
		CommandResultCallback onCommandResult = (succeeded, resultValue) -> result.set(resultValue);

		try {
			final InstantiatedFunction<CommandSourceStack> instantiatedFunction = commandFunction.instantiate((CompoundTag) null, this.getBrigadierDispatcher());
			net.minecraft.commands.Commands.executeCommandInContext(css, (executioncontext) -> {
				ExecutionContext.queueInitialFunctionCall(executioncontext, instantiatedFunction, css, onCommandResult);
			});
		} catch (FunctionInstantiationException functionInstantiationException) {
			// We don't care if the function failed to instantiate
			assert true;
		} catch (Exception exception) {
			LogUtils.getLogger().warn("Failed to execute function {}", commandFunction.id(), exception);
		} finally {
			profiler.pop();
		}

		return result.get();
	}

	// Converts NMS function to SimpleFunctionWrapper
	private final SimpleFunctionWrapper convertFunction(CommandFunction<CommandSourceStack> commandFunction) {
		ToIntFunction<CommandSourceStack> appliedObj = (CommandSourceStack css) -> runCommandFunction(commandFunction, css);

		// Unpack the commands by instantiating the function with no CSS, then retrieving its entries
		String[] commands = new String[0];
		try {
			final InstantiatedFunction<CommandSourceStack> instantiatedFunction = commandFunction.instantiate((CompoundTag) null, this.getBrigadierDispatcher());

			List<?> cArr = instantiatedFunction.entries();
			commands = new String[cArr.size()];
			for (int i = 0, size = cArr.size(); i < size; i++) {
				commands[i] = cArr.get(i).toString();
			}
		} catch (FunctionInstantiationException functionInstantiationException) {
			// We don't care if the function failed to instantiate
			assert true;
		}
		return new SimpleFunctionWrapper(fromResourceLocation(commandFunction.id()), appliedObj, commands);
	}

	@Override
	public final HelpTopic generateHelpTopic(String commandName, String shortDescription, String fullDescription,
											 String permission) {
		return new CustomHelpTopic(commandName, shortDescription, fullDescription, permission);
	}

	@Override
	public Advancement getAdvancement(CommandContext<CommandSourceStack> cmdCtx, String key)
		throws CommandSyntaxException {
		return ResourceKeyArgument.getAdvancement(cmdCtx, key).toBukkit();
	}

	@Differs(from = "1.21.5", by = "#toJson is now implemented in this class")
	@Override
	public Component getAdventureChat(CommandContext<CommandSourceStack> cmdCtx, String key) throws CommandSyntaxException {
		return GsonComponentSerializer.gson().deserialize(this.toJson(MessageArgument.getMessage(cmdCtx, key)));
	}

	@Override
	public NamedTextColor getAdventureChatColor(CommandContext<CommandSourceStack> cmdCtx, String key) {
		final Integer color = ColorArgument.getColor(cmdCtx, key).getColor();
		return color == null ? NamedTextColor.WHITE : NamedTextColor.namedColor(color);
	}

	@Differs(from = "1.21.5", by = "#toJson is now implemented in this class")
	@Override
	public final Component getAdventureChatComponent(CommandContext<CommandSourceStack> cmdCtx, String key) throws CommandSyntaxException {
		return GsonComponentSerializer.gson()
			.deserialize(this.toJson(ComponentArgument.getResolvedComponent(cmdCtx, key)));
	}

	@Override
	public final Object getBiome(CommandContext<CommandSourceStack> cmdCtx, String key, ArgumentSubType subType)
		throws CommandSyntaxException {
		final ResourceLocation resourceLocation = ResourceArgument.getResource(cmdCtx, key, Registries.BIOME).key()
			.location();
		return switch (subType) {
			case BIOME_BIOME -> {
				Biome biome = null;
				try {
					biome = Biome.valueOf(resourceLocation.getPath().toUpperCase());
				} catch (IllegalArgumentException biomeNotFound) {
					biome = null;
				}
				yield biome;
			}
			case BIOME_NAMESPACEDKEY -> (NamespacedKey) fromResourceLocation(resourceLocation);
			default -> null;
		};
	}

	@Override
	public final BlockData getBlockState(CommandContext<CommandSourceStack> cmdCtx, String key) {
		return CraftBlockData.fromData(BlockStateArgument.getBlock(cmdCtx, key).getState());
	}

	@Override
	public CommandSourceStack getBrigadierSourceFromCommandSender(
		AbstractCommandSender<? extends CommandSender> sender) {
		return VanillaCommandWrapper.getListener(sender.getSource());
	}

	@Differs(from = "1.21.5", by = "#toJson is now implemented in this class")
	@Override
	public final BaseComponent[] getChat(CommandContext<CommandSourceStack> cmdCtx, String key) throws CommandSyntaxException {
		return ComponentSerializer.parse(this.toJson(MessageArgument.getMessage(cmdCtx, key)));
	}

	@Differs(from = "1.21.5", by = "#toJson is now implemented in this class")
	@Override
	public final BaseComponent[] getChatComponent(CommandContext<CommandSourceStack> cmdCtx, String key) throws CommandSyntaxException {
		return ComponentSerializer.parse(this.toJson(ComponentArgument.getResolvedComponent(cmdCtx, key)));
	}

	@Override
	public final World getDimension(CommandContext<CommandSourceStack> cmdCtx, String key)
		throws CommandSyntaxException {
		return DimensionArgument.getDimension(cmdCtx, key).getWorld();
	}

	@Override
	public final Enchantment getEnchantment(CommandContext<CommandSourceStack> cmdCtx, String key)
		throws CommandSyntaxException {
		final net.minecraft.world.item.enchantment.Enchantment enchantment = ResourceArgument.getEnchantment(cmdCtx, key).value();
		final ResourceLocation resource = this.<MinecraftServer>getMinecraftServer().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getKey(enchantment);
		return Registry.ENCHANTMENT.get(fromResourceLocation(resource));
	}

	@Override
	public final Object getEntitySelector(CommandContext<CommandSourceStack> cmdCtx, String str,
										  ArgumentSubType subType, boolean allowEmpty) throws CommandSyntaxException {

		// We override the rule whereby players need "minecraft.command.selector" and
		// have to have level 2 permissions in order to use entity selectors. We're
		// trying to allow entity selectors to be used by anyone that registers a
		// command via the CommandAPI.
		EntitySelector argument = cmdCtx.getArgument(str, EntitySelector.class);
		try {
			entitySelectorUsesSelector.set(argument, false);
		} catch (IllegalAccessException e) {
			// Shouldn't happen, CommandAPIHandler#getField makes it accessible
		}

		return switch (subType) {
			case ENTITYSELECTOR_MANY_ENTITIES:
				try {
					List<org.bukkit.entity.Entity> result = new ArrayList<>();
					for (Entity entity : argument.findEntities(cmdCtx.getSource())) {
						result.add(entity.getBukkitEntity());
					}
					if (result.isEmpty() && !allowEmpty) {
						throw EntityArgument.NO_ENTITIES_FOUND.create();
					} else {
						yield result;
					}
				} catch (CommandSyntaxException e) {
					if (allowEmpty) {
						yield new ArrayList<org.bukkit.entity.Entity>();
					} else {
						throw e;
					}
				}
			case ENTITYSELECTOR_MANY_PLAYERS:
				try {
					List<Player> result = new ArrayList<>();
					for (ServerPlayer player : argument.findPlayers(cmdCtx.getSource())) {
						result.add(player.getBukkitEntity());
					}
					if (result.isEmpty() && !allowEmpty) {
						throw EntityArgument.NO_PLAYERS_FOUND.create();
					} else {
						yield result;
					}
				} catch (CommandSyntaxException e) {
					if (allowEmpty) {
						yield new ArrayList<Player>();
					} else {
						throw e;
					}
				}
			case ENTITYSELECTOR_ONE_ENTITY:
				yield argument.findSingleEntity(cmdCtx.getSource()).getBukkitEntity();
			case ENTITYSELECTOR_ONE_PLAYER:
				yield argument.findSinglePlayer(cmdCtx.getSource()).getBukkitEntity();
			default:
				throw new IllegalArgumentException("Unexpected value: " + subType);
		};
	}

	@Override
	public FloatRange getFloatRange(CommandContext<CommandSourceStack> cmdCtx, String key) {
		MinMaxBounds.Doubles range = RangeArgument.Floats.getRange(cmdCtx, key);
		final Double lowBoxed = range.min().orElse(null);
		final Double highBoxed = range.max().orElse(null);
		final double low = lowBoxed == null ? -Float.MAX_VALUE : lowBoxed;
		final double high = highBoxed == null ? Float.MAX_VALUE : highBoxed;
		return new FloatRange((float) low, (float) high);
	}

	@Override
	public final FunctionWrapper[] getFunction(CommandContext<CommandSourceStack> cmdCtx, String key)
		throws CommandSyntaxException {
		List<FunctionWrapper> result = new ArrayList<>();
		CommandSourceStack css = cmdCtx.getSource().withSuppressedOutput().withMaximumPermission(2);

		for (CommandFunction<CommandSourceStack> commandFunction : FunctionArgument.getFunctions(cmdCtx, key)) {
			result.add(FunctionWrapper.fromSimpleFunctionWrapper(convertFunction(commandFunction), css,
				entity -> cmdCtx.getSource().withEntity(((CraftEntity) entity).getHandle())));
		}
		return result.toArray(new FunctionWrapper[0]);
	}

	@Override
	public SimpleFunctionWrapper getFunction(NamespacedKey key) {
		final ResourceLocation resourceLocation = ResourceLocation.fromNamespaceAndPath(key.getNamespace(), key.getKey());
		Optional<CommandFunction<CommandSourceStack>> commandFunctionOptional = this
			.<MinecraftServer>getMinecraftServer().getFunctions().get(resourceLocation);
		if (commandFunctionOptional.isPresent()) {
			return convertFunction(commandFunctionOptional.get());
		} else {
			throw new IllegalStateException("Failed to get defined function " + key
				+ "! This should never happen - please report this to the CommandAPI"
				+ "developers, we'd love to know how you got this error message!");
		}
	}

	@Override
	public Set<NamespacedKey> getFunctions() {
		Set<NamespacedKey> result = new HashSet<>();
		for (ResourceLocation resourceLocation : this.<MinecraftServer>getMinecraftServer().getFunctions()
			.getFunctionNames()) {
			result.add(fromResourceLocation(resourceLocation));
		}
		return result;
	}

	@Override
	public IntegerRange getIntRange(CommandContext<CommandSourceStack> cmdCtx, String key) {
		MinMaxBounds.Ints range = RangeArgument.Ints.getRange(cmdCtx, key);
		final Integer lowBoxed = range.min().orElse(null);
		final Integer highBoxed = range.max().orElse(null);
		final int low = lowBoxed == null ? Integer.MIN_VALUE : lowBoxed;
		final int high = highBoxed == null ? Integer.MAX_VALUE : highBoxed;
		return new IntegerRange(low, high);
	}

	@Override
	public final org.bukkit.inventory.ItemStack getItemStack(CommandContext<CommandSourceStack> cmdCtx, String key)
		throws CommandSyntaxException {
		ItemInput input = ItemArgument.getItem(cmdCtx, key);

		// Create the basic ItemStack with an amount of 1
		net.minecraft.world.item.ItemStack item = input.createItemStack(1, false);
		return CraftItemStack.asBukkitCopy(item);
	}

	@Override
	public final Predicate<org.bukkit.inventory.ItemStack> getItemStackPredicate(
		CommandContext<CommandSourceStack> cmdCtx, String key) throws CommandSyntaxException {
		// Not inside the lambda because getItemPredicate throws CommandSyntaxException
		Predicate<ItemStack> predicate = ItemPredicateArgument.getItemPredicate(cmdCtx, key);
		return item -> predicate.test(CraftItemStack.asNMSCopy(item));
	}

	@Override
	public final Location2D getLocation2DPrecise(CommandContext<CommandSourceStack> cmdCtx, String key) throws CommandSyntaxException {
		Vec2 vecPos = Vec2Argument.getVec2(cmdCtx, key);
		return new Location2D(getWorldForCSS(cmdCtx.getSource()), vecPos.x, vecPos.y);
	}

	@Override
	public final org.bukkit.loot.LootTable getLootTable(CommandContext<CommandSourceStack> cmdCtx, String key) {
		return CraftLootTable.minecraftToBukkit(ResourceLocationArgument.getId(cmdCtx, key));
	}

	@Override
	public NamespacedKey getMinecraftKey(CommandContext<CommandSourceStack> cmdCtx, String key) {
		return fromResourceLocation(ResourceLocationArgument.getId(cmdCtx, key));
	}

	@Override
	public final Objective getObjective(CommandContext<CommandSourceStack> cmdCtx, String key) throws CommandSyntaxException {
		String objectiveName = ObjectiveArgument.getObjective(cmdCtx, key).getName();
		return Bukkit.getScoreboardManager().getMainScoreboard().getObjective(objectiveName);
	}

	@Override
	public final ParticleData<?> getParticle(CommandContext<CommandSourceStack> cmdCtx, String key) {
		final ParticleOptions particleOptions = ParticleArgument.getParticle(cmdCtx, key);

		// In our test suite, we can't parse particles via CraftParticle.minecraftToBukkit
		// on 1.20.3+ because initializing CraftParticle's static constructor requires
		// implementing a registry. We don't care about a registry for the sake of testing,
		// all we're actually interested in is testing that the particle data is being parsed
		// and converted to Bukkit properly, because that's what actually matters. If the
		// Bukkit#getServer is a CraftServer, that means we're running on a normal (Bukkit/Spigot/Paper)
		// server. If it isn't, that means we're running in our test environment (i.e. a mocked
		// server instance), or some weird flavour of Bukkit that we're not supposed to use.
		final Particle particle;
		if (Bukkit.getServer() instanceof CraftServer server) {
			particle = CraftParticle.minecraftToBukkit(particleOptions.getType());
		} else {
			particle = null;
		}

		if (particleOptions instanceof SimpleParticleType) {
			return new ParticleData<Void>(particle, null);
		} else if (particleOptions instanceof BlockParticleOption options) {
			return new ParticleData<BlockData>(particle, CraftBlockData.fromData(options.getState()));
		} else if (particleOptions instanceof DustColorTransitionOptions options) {
			return getParticleDataAsDustColorTransitionOption(particle, options);
		} else if (particleOptions instanceof DustParticleOptions options) {
			final Color color = Color.fromRGB((int) (options.getColor().x() * 255.0F),
				(int) (options.getColor().y() * 255.0F), (int) (options.getColor().z() * 255.0F));
			return new ParticleData<DustOptions>(particle, new DustOptions(color, options.getScale()));
		} else if (particleOptions instanceof ItemParticleOption options) {
			return new ParticleData<org.bukkit.inventory.ItemStack>(particle,
				CraftItemStack.asBukkitCopy(options.getItem()));
		} else if (particleOptions instanceof VibrationParticleOption options) {
			return getParticleDataAsVibrationParticleOption(cmdCtx, particle, options);
		} else if (particleOptions instanceof ShriekParticleOption options) {
			// CraftBukkit implements shriek particles as a (boxed) Integer object
			return new ParticleData<Integer>(particle, Integer.valueOf(options.getDelay()));
		} else if (particleOptions instanceof SculkChargeParticleOptions options) {
			// CraftBukkit implements sculk charge particles as a (boxed) Float object
			return new ParticleData<Float>(particle, Float.valueOf(options.roll()));
		} else if (particleOptions instanceof ColorParticleOption options) {
			return getParticleDataAsColorParticleOption(particle, options);
		} else if (particleOptions instanceof TrailParticleOption options) {
			return getParticleDataAsTrailParticleOption(cmdCtx, particle, options);
		} else {
			CommandAPI.getLogger().warning("Invalid particle data type for " + particle.getDataType().toString());
			return new ParticleData<Void>(particle, null);
		}
	}

	private ParticleData<Trail> getParticleDataAsTrailParticleOption(CommandContext<CommandSourceStack> cmdCtx,
																	 Particle particle, TrailParticleOption options) {
		final Level level = cmdCtx.getSource().getLevel();
		final Vec3 target = options.target();
		final Location targetLocation = new Location(level.getWorld(), target.x, target.y, target.z);
		final Color color = Color.fromARGB(options.color());
		return new ParticleData<Trail>(particle, new Trail(targetLocation, color, options.duration()));
	}

	private ParticleData<Color> getParticleDataAsColorParticleOption(Particle particle,
																	 ColorParticleOption options) {
		final Color color = Color.fromARGB(
			(int) (options.getAlpha() * 255.0F),
			(int) (options.getRed() * 255.0F),
			(int) (options.getGreen() * 255.0F),
			(int) (options.getBlue() * 255.0F)
		);
		return new ParticleData<Color>(particle, color);
	}

	private ParticleData<DustTransition> getParticleDataAsDustColorTransitionOption(Particle particle,
																					DustColorTransitionOptions options) {
		final Color color = Color.fromRGB((int) (options.getFromColor().x() * 255.0F),
			(int) (options.getFromColor().y() * 255.0F), (int) (options.getFromColor().z() * 255.0F));
		final Color toColor = Color.fromRGB((int) (options.getToColor().x() * 255.0F),
			(int) (options.getToColor().y() * 255.0F), (int) (options.getToColor().z() * 255.0F));
		return new ParticleData<DustTransition>(particle, new DustTransition(color, toColor, options.getScale()));
	}

	private ParticleData<?> getParticleDataAsVibrationParticleOption(CommandContext<CommandSourceStack> cmdCtx,
																	 Particle particle, VibrationParticleOption options) {
		// The "from" part of the Vibration object in Bukkit is completely ignored now,
		// so we just populate it with some "feasible" information
		final Vec3 origin = cmdCtx.getSource().getPosition();
		Level level = cmdCtx.getSource().getLevel();
		Location from = new Location(level.getWorld(), origin.x, origin.y, origin.z);
		final Destination destination;

		if (options.getDestination() instanceof BlockPositionSource positionSource) {
			Vec3 to = positionSource.getPosition(level).get();
			destination = new BlockDestination(new Location(level.getWorld(), to.x(), to.y(), to.z()));
		} else {
			CommandAPI.getLogger().warning("Unknown or unsupported vibration destination " + options.getDestination());
			return new ParticleData<Void>(particle, null);
		}
		return new ParticleData<Vibration>(particle, new Vibration(from, destination, options.getArrivalInTicks()));
	}

	@Override
	public Object getPotionEffect(CommandContext<CommandSourceStack> cmdCtx, String key, ArgumentSubType subType) throws CommandSyntaxException {
		return switch (subType) {
			case POTION_EFFECT_POTION_EFFECT ->
				CraftPotionEffectType.minecraftToBukkit(ResourceArgument.getMobEffect(cmdCtx, key).value());
			case POTION_EFFECT_NAMESPACEDKEY -> fromResourceLocation(ResourceLocationArgument.getId(cmdCtx, key));
			default -> throw new IllegalArgumentException("Unexpected value: " + subType);
		};
	}

	@Override
	public final Recipe getRecipe(CommandContext<CommandSourceStack> cmdCtx, String key) throws CommandSyntaxException {
		RecipeHolder<?> recipe = ResourceKeyArgument.getRecipe(cmdCtx, key);
		return new ComplexRecipeImpl(fromResourceLocation(recipe.id().registry()), recipe.toBukkitRecipe());
	}

	@Override
	public final Rotation getRotation(CommandContext<CommandSourceStack> cmdCtx, String key) {
		Vec2 rotation = RotationArgument.getRotation(cmdCtx, key).getRotation(cmdCtx.getSource());
		return new Rotation(rotation.y, rotation.x);
	}

	@Override
	public ScoreboardSlot getScoreboardSlot(CommandContext<CommandSourceStack> cmdCtx, String key) {
		return ScoreboardSlot.ofMinecraft(ScoreboardSlotArgument.getDisplaySlot(cmdCtx, key).id());
	}

	@Override
	public Collection<String> getScoreHolderMultiple(CommandContext<CommandSourceStack> cmdCtx, String key) throws CommandSyntaxException {
		final Collection<ScoreHolder> scoreHolders = ScoreHolderArgument.getNames(cmdCtx, key);
		Set<String> scoreHolderNames = new HashSet<>();
		for (ScoreHolder scoreHolder : scoreHolders) {
			scoreHolderNames.add(scoreHolder.getScoreboardName());
		}
		return scoreHolderNames;
	}

	@Override
	public String getScoreHolderSingle(CommandContext<CommandSourceStack> cmdCtx, String key) throws CommandSyntaxException {
		return ScoreHolderArgument.getName(cmdCtx, key).getScoreboardName();
	}

	@Override
	public BukkitCommandSender<? extends CommandSender> getSenderForCommand(CommandContext<CommandSourceStack> cmdCtx, boolean isNative) {
		CommandSourceStack css = cmdCtx.getSource();

		CommandSender sender = css.getBukkitSender();
		if (sender == null) {
			// Sender CANNOT be null. This can occur when using a remote console
			// sender. You can access it directly using
			// this.<MinecraftServer>getMinecraftServer().remoteConsole
			// however this may also be null, so delegate to the next most-meaningful
			// sender.
			sender = Bukkit.getConsoleSender();
		}

		Entity proxyEntity = css.getEntity();
		CommandSender proxy = proxyEntity == null ? null : proxyEntity.getBukkitEntity();
		if (isNative || (proxy != null && !sender.equals(proxy))) {
			if (proxy == null) {
				proxy = sender;
			}

			return new BukkitNativeProxyCommandSender(new NativeProxyCommandSender_1_21_R5(css, sender, proxy));
		} else {
			return wrapCommandSender(sender);
		}
	}

	@Override
	public NativeProxyCommandSender createNativeProxyCommandSender(CommandSender caller, CommandSender callee, Location location, World world) {
		if (callee == null) callee = caller;

		// Most parameters default to what is defined by the caller
		CommandSourceStack css = getBrigadierSourceFromCommandSender(wrapCommandSender(caller));

		// Position and rotation may be overridden by the Location
		if (location != null) {
			css = css
				.withPosition(new Vec3(location.getX(), location.getY(), location.getZ()))
				.withRotation(new Vec2(location.getPitch(), location.getYaw()));
		}

		// ServerLevel may be overridden by the World
		if (world == null && location != null) {
			world = location.getWorld();
		}
		if (world != null) {
			css = css.withLevel(((CraftWorld) world).getHandle());
		}

		// The proxied sender can only be an Entity in the CommandSourceStack
		if (callee instanceof org.bukkit.entity.Entity e) {
			css = css.withEntity(((CraftEntity) e).getHandle());
		}

		return new NativeProxyCommandSender_1_21_R5(css, caller, callee);
	}

	@Override
	public final SimpleCommandMap getSimpleCommandMap() {
		return ((CraftServer) Bukkit.getServer()).getCommandMap();
	}

	@Override
	public final Object getSound(CommandContext<CommandSourceStack> cmdCtx, String key, ArgumentSubType subType) {
		final ResourceLocation soundResource = ResourceLocationArgument.getId(cmdCtx, key);
		return switch (subType) {
			case SOUND_SOUND -> {
				final Optional<Holder.Reference<SoundEvent>> soundEvent = BuiltInRegistries.SOUND_EVENT.get(soundResource);
				if (soundEvent.isEmpty()) {
					yield null;
				} else {
					yield CraftSound.minecraftToBukkit(soundEvent.get().value());
				}
			}
			case SOUND_NAMESPACEDKEY -> {
				yield NamespacedKey.fromString(soundResource.getNamespace() + ":" + soundResource.getPath());
			}
			default -> throw new IllegalArgumentException("Unexpected value: " + subType);
		};
	}

	@Differs(from = "1.21.5", by = "Suggestion providers are per command context, not globally defined")
	@Override
	public SuggestionProvider<CommandSourceStack> getSuggestionProvider(SuggestionProviders provider) {
		return switch (provider) {
			case FUNCTION -> (context, builder) -> {
				ServerFunctionManager functionData = this.<MinecraftServer>getMinecraftServer().getFunctions();
				SharedSuggestionProvider.suggestResource(functionData.getTagNames(), builder, "#");
				return SharedSuggestionProvider.suggestResource(functionData.getFunctionNames(), builder);
			};
			case RECIPES -> (cmdCtx, builder) -> SharedSuggestionProvider.suggestResource(this.<MinecraftServer>getMinecraftServer().getRecipeManager()
				.getRecipes().stream().map(holder -> holder.id().location()), builder);
			case SOUNDS ->
				(cmdCtx, builder) -> SharedSuggestionProvider.suggestResource(cmdCtx.getSource().getAvailableSounds(), builder);
			case ADVANCEMENTS -> (cmdCtx, builder) -> SharedSuggestionProvider.suggestResource(this.<MinecraftServer>getMinecraftServer().getAdvancements()
				.getAllAdvancements().stream().map(AdvancementHolder::id), builder);
			case LOOT_TABLES ->
				(cmdCtx, builder) -> {
					// TODO: Test this actually works
					final List<ResourceLocation> LootTableKeys = this.<MinecraftServer>getMinecraftServer().reloadableRegistries().lookup().lookupOrThrow(Registries.LOOT_TABLE).listElementIds().map(ResourceKey::location).toList();
					return SharedSuggestionProvider.suggestResource(LootTableKeys, builder);
				};
			case BIOMES -> _ArgumentSyntheticBiome()::listSuggestions;
			case ENTITIES -> (cmdCtx, builder) -> SharedSuggestionProvider.suggestResource(
				BuiltInRegistries.ENTITY_TYPE
					.stream()
					.filter(type -> type.isEnabled(cmdCtx.getSource().enabledFeatures()) && type.canSummon()),
				builder, EntityType::getKey, EntityType::getDescription
			);
			case POTION_EFFECTS ->
				(context, builder) -> SharedSuggestionProvider.suggestResource(BuiltInRegistries.MOB_EFFECT.keySet(), builder);
		};
	}

	@Override
	public final SimpleFunctionWrapper[] getTag(NamespacedKey key) {
		Collection<CommandFunction<CommandSourceStack>> customFunctions = this.<MinecraftServer>getMinecraftServer().getFunctions().getTag(ResourceLocation.fromNamespaceAndPath(key.getNamespace(), key.getKey()));
		SimpleFunctionWrapper[] convertedCustomFunctions = new SimpleFunctionWrapper[customFunctions.size()];
		int index = 0;
		for (CommandFunction<CommandSourceStack> customFunction : customFunctions) {
			convertedCustomFunctions[index++] = convertFunction(customFunction);
		}
		return convertedCustomFunctions;
	}

	@Override
	public Set<NamespacedKey> getTags() {
		Set<NamespacedKey> result = new HashSet<>();
		for (ResourceLocation resourceLocation : this.<MinecraftServer>getMinecraftServer().getFunctions().getTagNames()) {
			result.add(fromResourceLocation(resourceLocation));
		}
		return result;
	}

	@Override
	public final Team getTeam(CommandContext<CommandSourceStack> cmdCtx, String key) throws CommandSyntaxException {
		String teamName = TeamArgument.getTeam(cmdCtx, key).getName();
		return Bukkit.getScoreboardManager().getMainScoreboard().getTeam(teamName);
	}

	@Override
	public World getWorldForCSS(CommandSourceStack css) {
		return (css.getLevel() == null) ? null : css.getLevel().getWorld();
	}

	@Override
	public final void reloadDataPacks() {
		CommandAPI.logNormal("Reloading datapacks...");

		// Get previously declared recipes to be re-registered later
		Iterator<Recipe> recipes = Bukkit.recipeIterator();

		// Update the commandDispatcher with the current server's commandDispatcher
		ReloadableResources serverResources = this.<MinecraftServer>getMinecraftServer().resources;
		serverResources.managers().commands = this.<MinecraftServer>getMinecraftServer().getCommands();

		// Update the ServerFunctionLibrary's command dispatcher with the new one
		try {
			serverFunctionLibraryDispatcher.set(serverResources.managers().getFunctionLibrary(),
				getBrigadierDispatcher());
		} catch (IllegalAccessException ignored) {
			// Shouldn't happen, CommandAPIHandler#getField makes it accessible
		}

		// From this.<MinecraftServer>getMinecraftServer().reloadResources //
		// Discover new packs
		Collection<String> collection;
		{
			List<String> packIDs = new ArrayList<>(
				this.<MinecraftServer>getMinecraftServer().getPackRepository().getSelectedIds());
			List<String> disabledPacks = this.<MinecraftServer>getMinecraftServer().getWorldData()
				.getDataConfiguration().dataPacks().getDisabled();

			for (String availablePack : this.<MinecraftServer>getMinecraftServer().getPackRepository()
				.getAvailableIds()) {
				// Add every other available pack that is not disabled
				// and is not already in the list of existing packs
				if (!disabledPacks.contains(availablePack) && !packIDs.contains(availablePack)) {
					packIDs.add(availablePack);
				}
			}
			collection = packIDs;
		}

		// Step 1: Construct an async supplier of a list of all resource packs to
		// be loaded in the reload phase
		CompletableFuture<List<PackResources>> first = CompletableFuture.supplyAsync(() -> {
			PackRepository serverPackRepository = this.<MinecraftServer>getMinecraftServer().getPackRepository();

			List<PackResources> packResources = new ArrayList<>();
			for (String packID : collection) {
				Pack pack = serverPackRepository.getPack(packID);
				if (pack != null) {
					packResources.add(pack.open());
				}
			}
			return packResources;
		}).exceptionally(exception -> {
			CommandAPI.logException("Something went wrong while trying to collect resource packs!", exception);
			// Return all currently selected packs
			return this.<MinecraftServer>getMinecraftServer().getPackRepository().openAllSelected();
		});

		// Step 2: Convert all of the resource packs into ReloadableResources which
		// are replaced by our custom server resources with defined commands
		CompletableFuture<ReloadableResources> second = first.thenCompose(packResources -> {
			MultiPackResourceManager resourceManager = new MultiPackResourceManager(PackType.SERVER_DATA,
				packResources);

			// TODO: I'm not sure if this is sufficient anymore - Do we not want to load tags for existing
			// registries here as well?
			// List<PendingTags<?>> TagList = TagLoader.loadTagsForExistingRegistries(resourceManager, this.<MinecraftServer>getMinecraftServer().registries().compositeAccess());

			// Not using packResources, because we really really want this to work
			CompletableFuture<?> simpleReloadInstance = SimpleReloadInstance.create(resourceManager,
				serverResources.managers().listeners(), this.<MinecraftServer>getMinecraftServer().executor,
				this.<MinecraftServer>getMinecraftServer(), CompletableFuture
					.completedFuture(Unit.INSTANCE) /* ReloadableServerResources.DATA_RELOAD_INITIAL_TASK */,
				LogUtils.getLogger().isDebugEnabled()).done();

			return simpleReloadInstance.thenApply(x -> serverResources);
		}).exceptionally(exception -> {
			CommandAPI.logException("Something went wrong while trying to convert resource packs into ReloadableResources", exception);
			// Return existing resources
			return this.<MinecraftServer>getMinecraftServer().resources;
		});

		// Step 3: Actually load all of the resources
		CompletableFuture<Void> third = second.thenAcceptAsync(resources -> {
			this.<MinecraftServer>getMinecraftServer().resources.close();
			this.<MinecraftServer>getMinecraftServer().resources = serverResources;
			this.<MinecraftServer>getMinecraftServer().server.syncCommands();
			if (minecraftServerSetSelected == null) {
				this.<MinecraftServer>getMinecraftServer().getPackRepository().setSelected(collection);
			} else {
				try {
					minecraftServerSetSelected.invoke(this.<MinecraftServer>getMinecraftServer().getPackRepository(), collection, true);
				} catch (Throwable e) {
					CommandAPI.logException("Something went wrong while trying to invoke PackRepository#setSelected(Collection, boolean)", e);
				}
			}

			final FeatureFlagSet enabledFeatures = this.<MinecraftServer>getMinecraftServer().getWorldData().getDataConfiguration().enabledFeatures();

			// this.<MinecraftServer>getMinecraftServer().getSelectedPacks
			Collection<String> selectedIDs = this.<MinecraftServer>getMinecraftServer().getPackRepository()
				.getSelectedIds();
			List<String> enabledIDs = ImmutableList.copyOf(selectedIDs);
			List<String> disabledIDs = new ArrayList<>(
				this.<MinecraftServer>getMinecraftServer().getPackRepository().getAvailableIds());

			disabledIDs.removeIf(enabledIDs::contains);

			this.<MinecraftServer>getMinecraftServer().getWorldData()
				.setDataConfiguration(new WorldDataConfiguration(new DataPackConfig(enabledIDs, disabledIDs), enabledFeatures));
			// this.<MinecraftServer>getMinecraftServer().resources.managers().updateRegistryTags(registryAccess);
			this.<MinecraftServer>getMinecraftServer().resources.managers().updateStaticRegistryTags(); // TODO: Review this
			this.<MinecraftServer>getMinecraftServer().resources.managers().getRecipeManager().finalizeRecipeLoading(enabledFeatures);

			// May need to be commented out, may not. Comment it out just in case.
			// For some reason, calling getPlayerList().saveAll() may just hang
			// the server indefinitely. Not sure why!
			// this.<MinecraftServer>getMinecraftServer().getPlayerList().saveAll();
			// this.<MinecraftServer>getMinecraftServer().getPlayerList().reloadResources();
			// this.<MinecraftServer>getMinecraftServer().getFunctions().replaceLibrary(this.<MinecraftServer>getMinecraftServer().resources.managers().getFunctionLibrary());
			this.<MinecraftServer>getMinecraftServer().getStructureManager()
				.onResourceManagerReload(this.<MinecraftServer>getMinecraftServer().resources.resourceManager());

			// Set fuel values with the new loaded fuel values from the list of enabled features
			minecraftServerFuelValues.set(this.<MinecraftServer>getMinecraftServer(),
				FuelValues.vanillaBurnTimes(this.<MinecraftServer>getMinecraftServer().registries().compositeAccess(),
					enabledFeatures
				)
			);
		}).exceptionally(exception -> {
			CommandAPI.logException("Something went wrong while trying to load resources.", exception);
			return null;
		});

		// Step 4: Block the thread until everything's done
		if (this.<MinecraftServer>getMinecraftServer().isSameThread()) {
			this.<MinecraftServer>getMinecraftServer().managedBlock(third::isDone);
		}

		// Run the completableFuture (and bind tags?)
		try {

			// Register recipes again because reloading datapacks
			// removes all non-vanilla recipes
			registerBukkitRecipesSafely(recipes);

			CommandAPI.logNormal("Finished reloading datapacks");
		} catch (Exception e) {
			StringWriter stringWriter = new StringWriter();
			PrintWriter printWriter = new PrintWriter(stringWriter);
			e.printStackTrace(printWriter);

			CommandAPI.logError(
				"Failed to load datapacks, can't proceed with normal server load procedure. Try fixing your datapacks?\n"
					+ stringWriter.toString());
		}
	}

	@Differs(from = "1.21.5", by = "#toJson is now implemented in this class")
	@Override
	public Message generateMessageFromJson(String json) {
		return this.fromJson(json);
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T getMinecraftServer() {
		if (Bukkit.getServer() instanceof CraftServer server) {
			return (T) server.getServer();
		} else {
			return null;
		}
	}

	@Override
	public ArgumentType<?> _ArgumentMobEffect() {
		return ResourceArgument.resource(COMMAND_BUILD_CONTEXT, Registries.MOB_EFFECT);
	}

	@Override
	public ArgumentType<?> _ArgumentEntitySummon() {
		return ResourceArgument.resource(COMMAND_BUILD_CONTEXT, Registries.ENTITY_TYPE);
	}

	@Override
	public CommandRegistrationStrategy<CommandSourceStack> createCommandRegistrationStrategy() {
		if (vanillaCommandDispatcherFieldExists) {
			return new SpigotCommandRegistration<>(
				this.<MinecraftServer>getMinecraftServer().vanillaCommandDispatcher.getDispatcher(),
				(SimpleCommandMap) getPaper().getCommandMap(),
				() -> this.<MinecraftServer>getMinecraftServer().getCommands().getDispatcher(),
				command -> command instanceof VanillaCommandWrapper,
				node -> new VanillaCommandWrapper(this.<MinecraftServer>getMinecraftServer().vanillaCommandDispatcher, node),
				node -> node.getCommand() instanceof BukkitCommandWrapper
			);
		} else {
			// This class is Paper-server specific, so we need to use paper's userdev plugin to
			//  access it directly. That might need gradle, but there might also be a maven version?
			//  https://discord.com/channels/289587909051416579/1121227200277004398/1246910745761812480
			Class<?> bukkitCommandNode_bukkitBrigCommand;
			try {
				bukkitCommandNode_bukkitBrigCommand = Class.forName("io.papermc.paper.command.brigadier.bukkit.BukkitCommandNode$BukkitBrigCommand");
			} catch (ClassNotFoundException e) {
				throw new IllegalStateException("Expected to find class", e);
			}
			return new PaperCommandRegistration<>(
				() -> this.<MinecraftServer>getMinecraftServer().getCommands().getDispatcher(),
				() -> {
					SimpleHelpMap helpMap = (SimpleHelpMap) Bukkit.getServer().getHelpMap();
					helpMap.clear();
					helpMap.initializeGeneralTopics();
					helpMap.initializeCommands();
				},
				node -> bukkitCommandNode_bukkitBrigCommand.isInstance(node.getCommand())
			);
		}
	}
}
