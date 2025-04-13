package dev.jorel.commandapi.test;

import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;

public abstract class RecipeManagerAccess extends RecipeManager {
	// This method is usually protected access, but we can use it here since we extend RecipeManager
	//  This way is extra convenient since this code gets remapped properly
	public static RecipeHolder<?> publicFromJson(ResourceLocation minecraftkey, JsonObject jsonobject) {
		return RecipeManager.fromJson(minecraftkey, jsonobject);
	}
}
