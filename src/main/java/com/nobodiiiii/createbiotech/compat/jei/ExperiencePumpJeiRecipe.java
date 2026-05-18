package com.nobodiiiii.createbiotech.compat.jei;

import java.util.List;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

public record ExperiencePumpJeiRecipe(ResourceLocation id, ItemStack input, ItemStack output, List<Component> notes) {
}
