package com.nobodiiiii.createbiotech.compat.jei;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.IRecipeSlotBuilder;
import mezz.jei.api.gui.ingredient.ICraftingGridHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.category.extensions.vanilla.crafting.ICraftingCategoryExtension;
import net.minecraft.client.Minecraft;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.ShapedRecipe;

public class CapturedEntityBoxCraftingCategoryExtension implements ICraftingCategoryExtension {

	private final CraftingRecipe recipe;

	public CapturedEntityBoxCraftingCategoryExtension(CraftingRecipe recipe) {
		this.recipe = recipe;
	}

	@Override
	public void setRecipe(IRecipeLayoutBuilder builder, ICraftingGridHelper craftingGridHelper, IFocusGroup focuses) {
		List<List<ItemStack>> inputs = new ArrayList<>();
		for (Ingredient ingredient : recipe.getIngredients())
			inputs.add(List.of(ingredient.getItems()));

		int width = getWidth();
		int height = getHeight();
		craftingGridHelper.createAndSetOutputs(builder, List.of(getResultItem(recipe)));
		List<IRecipeSlotBuilder> inputSlots = craftingGridHelper.createAndSetInputs(builder, inputs, width, height);
		CapturedEntityBoxJeiSupport.applyCraftingRenderers(inputSlots, recipe.getIngredients(), width, height);
	}

	@Nullable
	@Override
	public ResourceLocation getRegistryName() {
		return recipe.getId();
	}

	@Override
	public int getWidth() {
		if (recipe instanceof ShapedRecipe shapedRecipe)
			return shapedRecipe.getWidth();
		return 0;
	}

	@Override
	public int getHeight() {
		if (recipe instanceof ShapedRecipe shapedRecipe)
			return shapedRecipe.getHeight();
		return 0;
	}

	private static ItemStack getResultItem(CraftingRecipe recipe) {
		Minecraft minecraft = Minecraft.getInstance();
		RegistryAccess registryAccess = minecraft.level != null ? minecraft.level.registryAccess() : RegistryAccess.EMPTY;
		return recipe.getResultItem(registryAccess);
	}
}
