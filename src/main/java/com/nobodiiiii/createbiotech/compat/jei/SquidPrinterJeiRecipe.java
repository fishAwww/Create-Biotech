package com.nobodiiiii.createbiotech.compat.jei;

import com.simibubi.create.foundation.fluid.FluidIngredient;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public record SquidPrinterJeiRecipe(ResourceLocation id, ItemStack inputBook, FluidIngredient requiredFluid,
	List<ItemStack> templateBooks, List<ItemStack> outputCopies) {
}
