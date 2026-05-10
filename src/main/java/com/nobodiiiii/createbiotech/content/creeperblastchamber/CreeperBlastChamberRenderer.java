package com.nobodiiiii.createbiotech.content.creeperblastchamber;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.nobodiiiii.createbiotech.CreateBiotech;

import net.createmod.catnip.data.Iterate;
import net.createmod.catnip.render.CachedBuffers;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public class CreeperBlastChamberRenderer implements BlockEntityRenderer<CreeperBlastChamberBlockEntity> {

	private static final float CREEPER_ANIMATION_Y_OFFSET = 0.12f;
	private static final float CREEPER_ANIMATION_START_SCALE = 0.92f;
	private static final PartialModel DISPLAY_PANEL =
		PartialModel.of(CreateBiotech.asResource("block/blast_chamber_display/panel"));
	private static final PartialModel DISPLAY_DIAL =
		PartialModel.of(CreateBiotech.asResource("block/blast_chamber_display/dial"));
	private static final PartialModel CREEPER_FACE =
		PartialModel.of(CreateBiotech.asResource("block/blast_chamber_display/creeper_face"));
	private final EntityRenderDispatcher entityRenderDispatcher;

	public CreeperBlastChamberRenderer(BlockEntityRendererProvider.Context context) {
		entityRenderDispatcher = Minecraft.getInstance().getEntityRenderDispatcher();
	}

	@Override
	public void render(CreeperBlastChamberBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer,
		int light, int overlay) {
		Level level = be.getLevel();
		if (level == null)
			return;

		BlockState blockState = be.getBlockState();
		VertexConsumer vb = buffer.getBuffer(RenderType.cutout());
		float progress = be.displayGauge.getValue(partialTicks);

		if (be.isStructureValid()) {
			RenderSystem.disableDepthTest();
			renderFormedPanels(be, ms, vb, blockState, level, progress);
			RenderSystem.enableDepthTest();
			if (be.shouldRenderCreeperFace())
				renderCreeperFace(be, ms, vb, blockState, level);
			renderAnimatedCreepers(be, partialTicks, ms, buffer);
			return;
		}

		renderStandalonePanels(be, ms, vb, blockState, level, progress);
	}

	private void renderFormedPanels(CreeperBlastChamberBlockEntity be, PoseStack ms, VertexConsumer vb,
		BlockState blockState, Level level, float progress) {
		BlockPos origin = be.getStructureOrigin();
		if (origin == null)
			return;

		int size = be.getStructureSize();
		int half = size / 2;
		for (Direction d : Iterate.horizontalDirections) {
			BlockPos wallPos = origin.offset(
				d.getAxis() == Direction.Axis.X ? (d.getAxisDirection() == Direction.AxisDirection.POSITIVE ? size - 1 : 0)
					: half,
				0,
				d.getAxis() == Direction.Axis.Z ? (d.getAxisDirection() == Direction.AxisDirection.POSITIVE ? size - 1 : 0)
					: half);

			ms.pushPose();
			ms.translate(
				wallPos.getX() - be.getBlockPos().getX() + 0.5d,
				wallPos.getY() - be.getBlockPos().getY() + 0.5d,
				wallPos.getZ() - be.getBlockPos().getZ() + 0.5d);
			renderGauge(ms, vb, blockState, d, LevelRenderer.getLightColor(level, wallPos.relative(d)), progress);
			ms.popPose();
		}
	}

	private void renderStandalonePanels(CreeperBlastChamberBlockEntity be, PoseStack ms, VertexConsumer vb,
		BlockState blockState, Level level, float progress) {
		BlockPos pos = be.getBlockPos();
		for (Direction d : Iterate.horizontalDirections) {
			ms.pushPose();
			ms.translate(0.5, 0.5, 0.5);
			renderGauge(ms, vb, blockState, d, LevelRenderer.getLightColor(level, pos.relative(d)), progress);
			ms.popPose();
		}
	}

	private void renderGauge(PoseStack ms, VertexConsumer vb, BlockState blockState, Direction side, int light,
		float progress) {
		float dialPivotY = 6f / 16;
		float dialPivotZ = 8f / 16;
		float yRot = -side.toYRot() - 90;

		CachedBuffers.partial(DISPLAY_PANEL, blockState)
			.rotateYDegrees(yRot)
			.uncenter()
			.translate(1f / 2f - 6f / 16f, 0, 0)
			.light(light)
			.renderInto(ms, vb);
		CachedBuffers.partial(DISPLAY_DIAL, blockState)
			.rotateYDegrees(yRot)
			.uncenter()
			.translate(1f / 2f - 6f / 16f, 0, 0)
			.translate(0, dialPivotY, dialPivotZ)
			.rotateXDegrees(-145 * progress + 90)
			.translate(0, -dialPivotY, -dialPivotZ)
			.light(light)
			.renderInto(ms, vb);
	}

	private void renderCreeperFace(CreeperBlastChamberBlockEntity be, PoseStack ms, VertexConsumer vb,
		BlockState blockState, Level level) {
		BlockPos pos = be.getBlockPos();
		for (Direction d : Iterate.horizontalDirections) {
			ms.pushPose();
			ms.translate(0.5, 0.5, 0.5);
			CachedBuffers.partial(CREEPER_FACE, blockState)
				.rotateYDegrees(-d.toYRot() - 90)
				.uncenter()
				.light(LevelRenderer.getLightColor(level, pos.relative(d)))
				.renderInto(ms, vb);
			ms.popPose();
		}
	}

	private void renderAnimatedCreepers(CreeperBlastChamberBlockEntity be, float partialTicks, PoseStack ms,
		MultiBufferSource buffer) {
		Level level = be.getLevel();
		if (level == null)
			return;

		for (CreeperBlastChamberBlockEntity.RenderCreeperAnimation animation : be.getRenderAnimations()) {
			Creeper creeper = be.getAnimatedCreeper(animation.creeperUuid(), animation.packagerPos());
			if (creeper == null)
				continue;

			float linearProgress = Mth.clamp(
				(animation.totalTicks() - animation.ticksRemaining() + partialTicks) / animation.totalTicks(), 0f, 1f);
			float progress = linearProgress * linearProgress * (3f - 2f * linearProgress);
			float scale = animation.exiting()
				? Mth.lerp(progress, 1f, CREEPER_ANIMATION_START_SCALE)
				: Mth.lerp(progress, CREEPER_ANIMATION_START_SCALE, 1f);
			float yOffset = animation.exiting()
				? Mth.lerp(progress, 0f, -CREEPER_ANIMATION_Y_OFFSET)
				: Mth.lerp(progress, -CREEPER_ANIMATION_Y_OFFSET, 0f);

			ms.pushPose();
			ms.translate(
				animation.packagerPos().getX() - be.getBlockPos().getX() + 0.5d,
				animation.packagerPos().getY() - be.getBlockPos().getY() + 1d + yOffset,
				animation.packagerPos().getZ() - be.getBlockPos().getZ() + 0.5d);
			ms.scale(scale, scale, scale);

			boolean wasInvisible = creeper.isInvisible();
			creeper.setInvisible(false);
			entityRenderDispatcher.setRenderShadow(false);
			RenderSystem.runAsFancy(() -> entityRenderDispatcher.render(creeper, 0, 0, 0,
				Mth.rotLerp(partialTicks, creeper.yRotO, creeper.getYRot()), partialTicks, ms, buffer,
				LevelRenderer.getLightColor(level, animation.packagerPos().above())));
			entityRenderDispatcher.setRenderShadow(true);
			creeper.setInvisible(wasInvisible);

			ms.popPose();
		}
	}

	@Override
	public boolean shouldRenderOffScreen(CreeperBlastChamberBlockEntity be) {
		return be.isStructureValid();
	}
}
