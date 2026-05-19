package com.nobodiiiii.createbiotech.mixin.client;

import com.simibubi.create.content.fluids.transfer.FillingRecipe;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.nobodiiiii.createbiotech.compat.jei.SquidJeiRenderer;
import com.nobodiiiii.createbiotech.compat.jei.SquidPrinterJeiRecipes;

import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import net.minecraft.client.gui.GuiGraphics;

@Pseudo
@Mixin(targets = "com.simibubi.create.compat.jei.category.SpoutCategory", remap = false)
public abstract class SpoutCategoryMixin {

	@Inject(method = "draw", at = @At("TAIL"), remap = false)
	private void createBiotech$drawSquid(FillingRecipe recipe, IRecipeSlotsView recipeSlotsView, GuiGraphics graphics, double mouseX,
		double mouseY, CallbackInfo ci) {
		if (!SquidPrinterJeiRecipes.isSquidPrinterSpoutFilling(recipe.getId()))
			return;
		SquidJeiRenderer.render(graphics, 88, 56, 8.5f);
	}
}
