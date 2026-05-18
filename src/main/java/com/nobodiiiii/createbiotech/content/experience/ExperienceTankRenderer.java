package com.nobodiiiii.createbiotech.content.experience;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.nobodiiiii.createbiotech.registry.CBItems;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class ExperienceTankRenderer implements BlockEntityRenderer<ExperienceTankBlockEntity> {
	private static final ItemStack EXPERIENCE_STACK = new ItemStack(CBItems.EXPERIENCE.get());

	public ExperienceTankRenderer(BlockEntityRendererProvider.Context context) {
	}

	@Override
	public void render(ExperienceTankBlockEntity be, float partialTick, PoseStack poseStack, MultiBufferSource buffer,
		int packedLight, int packedOverlay) {
		if (!be.isController() || !be.hasWindow() || be.getStoredExperience() <= 0)
			return;
		Level level = be.getLevel();
		if (level == null)
			return;

		float fill = be.getFillState();
		int height = be.getHeight();
		int orbCount = Math.max(1, Math.min(16, (int) Math.ceil(fill * height * 6.0f)));
		float time = (level.getGameTime() + partialTick) * 0.22f;
		ItemRenderer itemRenderer = Minecraft.getInstance()
			.getItemRenderer();

		for (int i = 0; i < orbCount; i++) {
			float seed = i * 19.371f;
			double x = 0.5d + Math.sin(time * 1.7f + seed) * 0.27d;
			double z = 0.5d + Math.cos(time * 1.3f + seed * 0.7f) * 0.27d;
			double y = 0.25d + Math.abs(Math.sin(time * 2.1f + seed * 0.31f)) * Math.max(0.2d, height - 0.5d);

			poseStack.pushPose();
			poseStack.translate(x, Math.min(height - 0.18d, y), z);
			poseStack.mulPose(Axis.YP.rotationDegrees((time * 90.0f + seed * 11.0f) % 360.0f));
			poseStack.scale(0.28f, 0.28f, 0.28f);
			itemRenderer.renderStatic(EXPERIENCE_STACK, ItemDisplayContext.GROUND, packedLight, packedOverlay,
				poseStack, buffer, level, i);
			poseStack.popPose();
		}
	}
}
