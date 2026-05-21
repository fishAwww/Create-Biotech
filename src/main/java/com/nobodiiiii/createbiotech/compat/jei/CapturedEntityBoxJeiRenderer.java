package com.nobodiiiii.createbiotech.compat.jei;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.mojang.blaze3d.systems.RenderSystem;

import mezz.jei.api.gui.builder.ITooltipBuilder;
import mezz.jei.api.ingredients.IIngredientRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

public class CapturedEntityBoxJeiRenderer implements IIngredientRenderer<ItemStack> {

	public static final CapturedEntityBoxJeiRenderer INSTANCE = new CapturedEntityBoxJeiRenderer();

	private static final float GUI_SCALE = 0.72f;

	private CapturedEntityBoxJeiRenderer() {}

	@Override
	public void render(GuiGraphics guiGraphics, @Nullable ItemStack ingredient) {
		render(guiGraphics, ingredient, 0, 0);
	}

	@Override
	public void render(GuiGraphics guiGraphics, @Nullable ItemStack ingredient, int posX, int posY) {
		if (ingredient == null || ingredient.isEmpty())
			return;

		RenderSystem.enableDepthTest();

		var poseStack = guiGraphics.pose();
		poseStack.pushPose();
		poseStack.translate(posX + 8f, posY + 8f, 0f);
		poseStack.scale(GUI_SCALE, GUI_SCALE, 1f);
		poseStack.translate(-8f, -8f, 0f);
		guiGraphics.renderFakeItem(ingredient, 0, 0);
		poseStack.popPose();

		RenderSystem.disableBlend();
	}

	@SuppressWarnings("removal")
	@Override
	public List<Component> getTooltip(ItemStack ingredient, TooltipFlag tooltipFlag) {
		Minecraft minecraft = Minecraft.getInstance();
		Player player = minecraft.player;
		return ingredient.getTooltipLines(player, tooltipFlag);
	}

	@Override
	public void getTooltip(ITooltipBuilder tooltip, ItemStack ingredient, TooltipFlag tooltipFlag) {
		tooltip.addAll(getTooltip(ingredient, tooltipFlag));
	}
}
