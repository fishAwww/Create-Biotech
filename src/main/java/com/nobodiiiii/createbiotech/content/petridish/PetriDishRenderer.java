package com.nobodiiiii.createbiotech.content.petridish;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.foundation.blockEntity.renderer.SmartBlockEntityRenderer;

import net.createmod.catnip.animation.AnimationTickHolder;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public class PetriDishRenderer extends SmartBlockEntityRenderer<PetriDishBlockEntity> {

	private static final BlockState SLIME_BLOCK = Blocks.SLIME_BLOCK.defaultBlockState();
	private static final float SLIME_BASE_Y = 4.25f / 16.0f;
	private static final float SLIME_MAX_TOP_Y = 0.96f;
	private static final float[] STAGE_SCALES = { 0.0f, 0.28f, 0.42f, 0.56f, 0.70f };
	private static final float IDLE_SPEED = 0.18f;
	private static final float IDLE_VERTICAL_STRETCH = 0.08f;
	private static final float IDLE_VERTICAL_SQUASH = 0.05f;
	private static final float IDLE_HORIZONTAL_STRETCH = 0.04f;
	private static final float IDLE_HORIZONTAL_SQUASH = 0.06f;
	private static final float GROWTH_OVERSHOOT = 1.15f;

	private final BlockRenderDispatcher blockRenderer;

	public PetriDishRenderer(BlockEntityRendererProvider.Context context) {
		super(context);
		blockRenderer = context.getBlockRenderDispatcher();
	}

	@Override
	protected void renderSafe(PetriDishBlockEntity be, float partialTicks, PoseStack poseStack,
		MultiBufferSource buffer, int packedLight, int packedOverlay) {
		super.renderSafe(be, partialTicks, poseStack, buffer, packedLight, packedOverlay);

		int growthStage = be.getRenderedSlimeStage();
		if (growthStage <= 0)
			return;

		float baseScale = getBaseScale(be, partialTicks);
		float idleWave = Mth.sin(AnimationTickHolder.getRenderTime(be.getLevel()) * IDLE_SPEED
			+ (be.getBlockPos()
				.asLong() & 31L) * 0.21f);
		float verticalFactor = idleWave >= 0.0f
			? 1.0f + idleWave * IDLE_VERTICAL_STRETCH
			: 1.0f + idleWave * IDLE_VERTICAL_SQUASH;
		float horizontalFactor = idleWave >= 0.0f
			? 1.0f - idleWave * IDLE_HORIZONTAL_STRETCH
			: 1.0f - idleWave * IDLE_HORIZONTAL_SQUASH;

		float scaleX = baseScale * horizontalFactor;
		float scaleY = baseScale * verticalFactor;
		float scaleZ = scaleX;
		scaleY = Math.min(scaleY, SLIME_MAX_TOP_Y - SLIME_BASE_Y);
		float minX = 0.5f - scaleX / 2.0f;
		float minZ = 0.5f - scaleZ / 2.0f;

		poseStack.pushPose();
		poseStack.translate(minX, SLIME_BASE_Y, minZ);
		poseStack.scale(scaleX, scaleY, scaleZ);
		blockRenderer.renderSingleBlock(SLIME_BLOCK, poseStack, buffer, packedLight, packedOverlay);
		poseStack.popPose();
	}

	private static float getBaseScale(PetriDishBlockEntity be, float partialTicks) {
		if (!be.isGrowthAnimating())
			return STAGE_SCALES[be.getRenderedSlimeStage()];

		float progress = be.getGrowthAnimationProgress(partialTicks);
		float from = STAGE_SCALES[be.getGrowthAnimationFromStage()];
		float to = STAGE_SCALES[be.getGrowthAnimationToStage()];
		return Mth.lerp(easeOutBack(progress, GROWTH_OVERSHOOT), from, to);
	}

	private static float easeOutBack(float progress, float overshoot) {
		float shifted = progress - 1.0f;
		float c3 = overshoot + 1.0f;
		return 1.0f + c3 * shifted * shifted * shifted + overshoot * shifted * shifted;
	}
}
