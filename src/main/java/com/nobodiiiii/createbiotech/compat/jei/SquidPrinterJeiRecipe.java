package com.nobodiiiii.createbiotech.compat.jei;

import java.util.List;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

public record SquidPrinterJeiRecipe(ResourceLocation id, ItemStack inputBook, List<ItemStack> templateBooks,
	List<ItemStack> outputCopies, List<Component> notes) {
}
