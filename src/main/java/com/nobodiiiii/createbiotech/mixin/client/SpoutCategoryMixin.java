package com.nobodiiiii.createbiotech.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.nobodiiiii.createbiotech.compat.jei.SquidJeiRenderer;
import com.nobodiiiii.createbiotech.compat.jei.SquidPrinterJeiRecipes;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.crafting.Recipe;

@Pseudo
@Mixin(targets = "com.simibubi.create.compat.jei.category.SpoutCategory", remap = false)
public abstract class SpoutCategoryMixin {

	@Inject(method = "draw", at = @At("TAIL"), remap = false)
	private void createBiotech$drawSquid(Object recipe, Object recipeSlotsView, GuiGraphics graphics, double mouseX,
		double mouseY, CallbackInfo ci) {
		if (!(recipe instanceof Recipe<?> mcRecipe))
			return;
		if (!SquidPrinterJeiRecipes.isSquidPrinterSpoutFilling(mcRecipe.getId()))
			return;
		SquidJeiRenderer.render(graphics, 88, 56, 8.5f);
	}
}
