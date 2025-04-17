/*******************************************************************************
 * Copyright 2018, 2021 Jorel Ali (Skepter) - MIT License
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

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.jorel.commandapi.CommandAPIBukkit;
import dev.jorel.commandapi.CommandAPIHandler;
import dev.jorel.commandapi.arguments.ArgumentSubType;
import dev.jorel.commandapi.commandsenders.BukkitCommandSender;
import dev.jorel.commandapi.wrappers.MathOperation;
import dev.jorel.commandapi.wrappers.Rotation;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.AngleArgument;
import net.minecraft.commands.arguments.ColorArgument;
import net.minecraft.commands.arguments.CompoundTagArgument;
import net.minecraft.commands.arguments.DimensionArgument;
import net.minecraft.commands.arguments.GameProfileArgument;
import net.minecraft.commands.arguments.MessageArgument;
import net.minecraft.commands.arguments.ObjectiveArgument;
import net.minecraft.commands.arguments.ObjectiveCriteriaArgument;
import net.minecraft.commands.arguments.OperationArgument;
import net.minecraft.commands.arguments.RangeArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.commands.arguments.ScoreHolderArgument;
import net.minecraft.commands.arguments.ScoreboardSlotArgument;
import net.minecraft.commands.arguments.TeamArgument;
import net.minecraft.commands.arguments.TimeArgument;
import net.minecraft.commands.arguments.UuidArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.commands.arguments.coordinates.ColumnPosArgument;
import net.minecraft.commands.arguments.coordinates.RotationArgument;
import net.minecraft.commands.arguments.coordinates.SwizzleArgument;
import net.minecraft.commands.arguments.coordinates.Vec2Argument;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.commands.arguments.item.FunctionArgument;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec2;
import org.bukkit.Axis;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.NamespacedKey;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Team;

import java.util.EnumSet;
import java.util.Locale;
import java.util.UUID;
import java.util.function.Function;

/**
 * Common NMS code To ensure that this code actually works across all versions
 * of Minecraft that this is supposed to support (1.20+), you should be
 * compiling this code against all of the declared Maven profiles specified in
 * this submodule's pom.xml file, by running the following command:
 * ./compileNMSCommon
 * <p>
 * In case that fails, the method that causes the failure should be implemented in all
 * NMS classes to maintain an error-free NMS_Common module that is applicable to all
 * versions.
 */
public abstract class NMS_Common extends CommandAPIBukkit<CommandSourceStack> {
	private static NamespacedKey fromResourceLocation(ResourceLocation key) {
		return NamespacedKey.fromString(key.getNamespace() + ":" + key.getPath());
	}

	@Override
	public final ArgumentType<?> _ArgumentAngle() {
		return AngleArgument.angle();
	}

	@Override
	public final ArgumentType<?> _ArgumentAxis() {
		return SwizzleArgument.swizzle();
	}

	@Override
	public final ArgumentType<?> _ArgumentChat() {
		return MessageArgument.message();
	}

	@Override
	public final ArgumentType<?> _ArgumentChatFormat() {
		return ColorArgument.color();
	}

	@Override
	public ArgumentType<?> _ArgumentDimension() {
		return DimensionArgument.dimension();
	}

	@Override
	public final ArgumentType<?> _ArgumentFloatRange() {
		return RangeArgument.floatRange();
	}

	@Override
	public final ArgumentType<?> _ArgumentIntRange() {
		return RangeArgument.intRange();
	}

	@Override
	public final ArgumentType<?> _ArgumentMathOperation() {
		return OperationArgument.operation();
	}

	@Override
	public final ArgumentType<?> _ArgumentMinecraftKeyRegistered() {
		return ResourceLocationArgument.id();
	}

	@Override
	public final ArgumentType<?> _ArgumentNBTCompound() {
		return CompoundTagArgument.compoundTag();
	}

	@Override
	public final ArgumentType<?> _ArgumentPosition() {
		return BlockPosArgument.blockPos();
	}

	@Override
	public final ArgumentType<?> _ArgumentPosition2D() {
		return ColumnPosArgument.columnPos();
	}

	@Override
	public final ArgumentType<?> _ArgumentProfile() {
		return GameProfileArgument.gameProfile();
	}

	@Override
	public final ArgumentType<?> _ArgumentRotation() {
		return RotationArgument.rotation();
	}

	@Override
	public final ArgumentType<?> _ArgumentScoreboardCriteria() {
		return ObjectiveCriteriaArgument.criteria();
	}

	@Override
	public final ArgumentType<?> _ArgumentScoreboardObjective() {
		return ObjectiveArgument.objective();
	}

	@Override
	public final ArgumentType<?> _ArgumentScoreboardSlot() {
		return ScoreboardSlotArgument.displaySlot();
	}

	@Override
	public final ArgumentType<?> _ArgumentScoreboardTeam() {
		return TeamArgument.team();
	}

	@Override
	public final ArgumentType<?> _ArgumentScoreholder(ArgumentSubType subType) {
		return switch(subType) {
			case SCOREHOLDER_SINGLE -> ScoreHolderArgument.scoreHolder();
			case SCOREHOLDER_MULTIPLE -> ScoreHolderArgument.scoreHolders();
			default -> throw new IllegalArgumentException("Unexpected value: " + subType);
		};
	}

	@Override
	public final ArgumentType<?> _ArgumentTag() {
		return FunctionArgument.functions();
	}

	@Override
	public final ArgumentType<?> _ArgumentTime() {
		return TimeArgument.time();
	}

	@Override
	public final ArgumentType<?> _ArgumentUUID() {
		return UuidArgument.uuid();
	}

	@Override
	public final ArgumentType<?> _ArgumentVec2(boolean centerPosition) {
		return Vec2Argument.vec2(centerPosition);
	}

	@Override
	public final ArgumentType<?> _ArgumentVec3(boolean centerPosition) {
		return Vec3Argument.vec3(centerPosition);
	}

	@Override
	public final String convert(PotionEffectType potion) {
		return potion.getName().toLowerCase(Locale.ENGLISH);
	}

	@Override
	public final String convert(Sound sound) {
		return sound.getKey().toString();
	}

	@Override
	public final float getAngle(CommandContext<CommandSourceStack> cmdCtx, String key) {
		return AngleArgument.getAngle(cmdCtx, key);
	}

	@Override
	public final EnumSet<Axis> getAxis(CommandContext<CommandSourceStack> cmdCtx, String key) {
		EnumSet<Axis> set = EnumSet.noneOf(Axis.class);
		EnumSet<Direction.Axis> parsedEnumSet = SwizzleArgument.getSwizzle(cmdCtx, key);
		for (Direction.Axis element : parsedEnumSet) {
			set.add(switch (element) {
				case X -> Axis.X;
				case Y -> Axis.Y;
				case Z -> Axis.Z;
			});
		}
		return set;
	}

	@Override
	public final ChatColor getChatColor(CommandContext<CommandSourceStack> cmdCtx, String key) {
		return ChatColor.getByChar(ColorArgument.getColor(cmdCtx, key).getChar());
	}

	@Override
	public final BukkitCommandSender<? extends CommandSender> getCommandSenderFromCommandSource(CommandSourceStack css) {
		try {
			return wrapCommandSender(css.getBukkitSender());
		} catch (UnsupportedOperationException e) {
			return null;
		}
	}

	@Override
	public final MathOperation getMathOperation(CommandContext<CommandSourceStack> cmdCtx, String key) throws CommandSyntaxException {
		// We run this to ensure the argument exists/parses properly
		OperationArgument.getOperation(cmdCtx, key);
		return MathOperation.fromString(CommandAPIHandler.getRawArgumentInput(cmdCtx, key));
	}

	@Override
	public final <NBTContainer> Object getNBTCompound(CommandContext<CommandSourceStack> cmdCtx, String key,
		Function<Object, NBTContainer> nbtContainerConstructor) {
		return nbtContainerConstructor.apply(CompoundTagArgument.getCompoundTag(cmdCtx, key));
	}

	@Override
	public Objective getObjective(CommandContext<CommandSourceStack> cmdCtx, String key)
		throws CommandSyntaxException {
		String objectiveName = ObjectiveArgument.getObjective(cmdCtx, key).getName();
		return Bukkit.getScoreboardManager().getMainScoreboard().getObjective(objectiveName);
	}

	@Override
	public final String getObjectiveCriteria(CommandContext<CommandSourceStack> cmdCtx, String key) {
		return ObjectiveCriteriaArgument.getCriteria(cmdCtx, key).getName();
	}

	@Override
	public final OfflinePlayer getOfflinePlayer(CommandContext<CommandSourceStack> cmdCtx, String key) throws CommandSyntaxException {
		return Bukkit.getOfflinePlayer(GameProfileArgument.getGameProfiles(cmdCtx, key).iterator().next().getId());
	}

	@Override
	public final Player getPlayer(CommandContext<CommandSourceStack> cmdCtx, String key) throws CommandSyntaxException {
		Player target = Bukkit.getPlayer(GameProfileArgument.getGameProfiles(cmdCtx, key).iterator().next().getId());
		if (target == null) {
			throw GameProfileArgument.ERROR_UNKNOWN_PLAYER.create();
		} else {
			return target;
		}
	}

	@Override
	public final Rotation getRotation(CommandContext<CommandSourceStack> cmdCtx, String key) {
		Vec2 rotation = RotationArgument.getRotation(cmdCtx, key).getRotation(cmdCtx.getSource());
		return new Rotation(rotation.y, rotation.x);
	}

	@Override
	public Team getTeam(CommandContext<CommandSourceStack> cmdCtx, String key) throws CommandSyntaxException {
		String teamName = TeamArgument.getTeam(cmdCtx, key).getName();
		return Bukkit.getScoreboardManager().getMainScoreboard().getTeam(teamName);
	}

	@Override
	public final int getTime(CommandContext<CommandSourceStack> cmdCtx, String key) {
		return cmdCtx.getArgument(key, Integer.class);
	}

	@Override
	public final UUID getUUID(CommandContext<CommandSourceStack> cmdCtx, String key) {
		return UuidArgument.getUuid(cmdCtx, key);
	}
}
