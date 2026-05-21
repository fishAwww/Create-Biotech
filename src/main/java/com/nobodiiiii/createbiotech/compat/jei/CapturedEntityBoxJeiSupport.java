package com.nobodiiiii.createbiotech.compat.jei;

import java.util.List;

import com.nobodiiiii.createbiotech.content.cardboardbox.CapturedEntityBoxIngredient;

import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeSlotBuilder;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;

public final class CapturedEntityBoxJeiSupport {

	private CapturedEntityBoxJeiSupport() {}

	public static boolean containsCapturedEntityBox(CraftingRecipe recipe) {
		return recipe.getIngredients()
			.stream()
			.anyMatch(CapturedEntityBoxJeiSupport::isCapturedEntityBoxIngredient);
	}

	public static IRecipeSlotBuilder addIngredients(IRecipeSlotBuilder slotBuilder, Ingredient ingredient) {
		slotBuilder.addIngredients(ingredient);
		return applyCustomRenderer(slotBuilder, ingredient);
	}

	public static IRecipeSlotBuilder applyCustomRenderer(IRecipeSlotBuilder slotBuilder, Ingredient ingredient) {
		if (isCapturedEntityBoxIngredient(ingredient))
			slotBuilder.setCustomRenderer(VanillaTypes.ITEM_STACK, CapturedEntityBoxJeiRenderer.INSTANCE);
		return slotBuilder;
	}

	public static void applyCraftingRenderers(List<IRecipeSlotBuilder> slotBuilders, List<Ingredient> ingredients,
		int width, int height) {
		if (width <= 0 || height <= 0)
			width = height = getShapelessSize(ingredients.size());

		for (int i = 0; i < ingredients.size(); i++) {
			Ingredient ingredient = ingredients.get(i);
			if (!isCapturedEntityBoxIngredient(ingredient))
				continue;

			int slotIndex = getCraftingIndex(i, width, height);
			if (slotIndex >= 0 && slotIndex < slotBuilders.size())
				slotBuilders.get(slotIndex)
					.setCustomRenderer(VanillaTypes.ITEM_STACK, CapturedEntityBoxJeiRenderer.INSTANCE);
		}
	}

	private static boolean isCapturedEntityBoxIngredient(Ingredient ingredient) {
		return ingredient instanceof CapturedEntityBoxIngredient;
	}

	private static int getShapelessSize(int total) {
		if (total > 4)
			return 3;
		if (total > 1)
			return 2;
		return 1;
	}

	private static int getCraftingIndex(int i, int width, int height) {
		if (width == 1) {
			if (height == 3 || height == 2)
				return (i * 3) + 1;
			return 4;
		}

		if (height == 1)
			return i + 3;

		if (width == 2) {
			int index = i;
			if (i > 1) {
				index++;
				if (i > 3)
					index++;
			}
			return index;
		}

		if (height == 2)
			return i + 3;

		return i;
	}
}
