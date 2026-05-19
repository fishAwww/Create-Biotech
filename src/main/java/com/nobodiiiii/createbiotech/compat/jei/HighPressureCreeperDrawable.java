package com.nobodiiiii.createbiotech.compat.jei;

import org.jetbrains.annotations.Nullable;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.nobodiiiii.createbiotech.mixin.client.CreeperAccessor;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.compat.jei.category.animations.AnimatedKinetics;

import net.createmod.catnip.animation.AnimationTickHolder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.level.Level;

public class HighPressureCreeperDrawable extends AnimatedKineticsWithEntities {
	private static final int PRESS_CYCLE = 30;
	private static final int PRESS_RENDER_Z = 100;
	private static final int PRESS_SCALE = 20;
	private static final double CREEPER_ATTACHMENT_Y = 2d;
	private static final float PRESS_EFFECT_START_OFFSET = 0.4f;
	private static final CompoundTag CHARGED_CREEPER_TAG = createChargedCreeperTag();

	private final int width;
	private final int height;
	private final float horizontalScale;
	private final float verticalScale;
	private final int swell;

	@Nullable
	private Creeper cachedCreeper;
	@Nullable
	private Level cachedLevel;

	public HighPressureCreeperDrawable(int width, int height, float horizontalScale, float verticalScale, int swell) {
		this.width = width;
		this.height = height;
		this.horizontalScale = horizontalScale;
		this.verticalScale = verticalScale;
		this.swell = swell;
	}

	@Override
	public int getWidth() {
		return width;
	}

	@Override
	public int getHeight() {
		return height;
	}

	@Override
	public void draw(GuiGraphics guiGraphics, int xOffset, int yOffset) {
		Minecraft minecraft = Minecraft.getInstance();
		Level level = minecraft.level;
		if (level == null)
			return;

		Creeper creeper = getOrCreateCreeper(level);
		if (creeper == null)
			return;

		float headOffset = getAnimatedHeadOffset();
		renderPressBody(guiGraphics, xOffset, yOffset);
		renderCreeper(guiGraphics, creeper, xOffset, yOffset, headOffset);
		renderPressHead(guiGraphics, xOffset, yOffset, headOffset);
	}

	private void renderPressBody(GuiGraphics guiGraphics, int xOffset, int yOffset) {
		PoseStack poseStack = guiGraphics.pose();
		poseStack.pushPose();
		poseStack.translate(xOffset, yOffset, PRESS_RENDER_Z);
		poseStack.mulPose(Axis.XP.rotationDegrees(-15.5f));
		poseStack.mulPose(Axis.YP.rotationDegrees(22.5f));

		blockElement(shaft(Direction.Axis.Z))
			.rotateBlock(0, 0, getCurrentAngle())
			.scale(PRESS_SCALE)
			.render(guiGraphics);

		blockElement(AllBlocks.MECHANICAL_PRESS.getDefaultState())
			.scale(PRESS_SCALE)
			.render(guiGraphics);

		poseStack.popPose();
	}

	private void renderPressHead(GuiGraphics guiGraphics, int xOffset, int yOffset, float headOffset) {
		PoseStack poseStack = guiGraphics.pose();
		poseStack.pushPose();
		poseStack.translate(xOffset, yOffset, PRESS_RENDER_Z);
		poseStack.mulPose(Axis.XP.rotationDegrees(-15.5f));
		poseStack.mulPose(Axis.YP.rotationDegrees(22.5f));

		blockElement(AllPartialModels.MECHANICAL_PRESS_HEAD)
			.atLocal(0, -headOffset, 0)
			.scale(PRESS_SCALE)
			.render(guiGraphics);

		poseStack.popPose();
	}

	private void renderCreeper(GuiGraphics guiGraphics, Creeper creeper, int xOffset, int yOffset, float headOffset) {
		float compression = getCompressionFromHeadOffset(headOffset);
		float pulse = 0.5f + 0.5f * Mth.sin(AnimationTickHolder.getRenderTime() * 0.9f);
		int renderSwell =
			Mth.floor(Mth.clamp(compression * Mth.lerp(pulse, 0.55f, 1f), 0f, 1f) * swell);

		CreeperAccessor accessor = (CreeperAccessor) creeper;
		accessor.createBiotech$setOldSwell(renderSwell);
		accessor.createBiotech$setSwell(renderSwell);

		float appliedHorizontalScale = Mth.lerp(compression, 1f, horizontalScale);
		float appliedVerticalScale = Mth.lerp(compression, 1f, verticalScale);
		entityElement(creeper)
			.at(xOffset, yOffset, PRESS_RENDER_Z)
			.atLocal(0.5d, CREEPER_ATTACHMENT_Y, 0.5d)
			.rotate(-15.5d, 22.5d, 0d)
			.dispatcherYaw(0f)
			.scale(PRESS_SCALE)
			.scaleEntity(appliedHorizontalScale, appliedVerticalScale, appliedHorizontalScale)
			.ticks(Mth.floor(AnimationTickHolder.getRenderTime()))
			.render(guiGraphics);
	}

	private float getAnimatedHeadOffset() {
		float cycle = (AnimationTickHolder.getRenderTime() - offset * 8) % PRESS_CYCLE;
		if (cycle < 10) {
			float progress = cycle / 10;
			return -(progress * progress * progress);
		}
		if (cycle < 15)
			return -1;
		if (cycle < 20)
			return -1 + (1 - ((20 - cycle) / 5));
		return 0;
	}

	private static float getCompressionFromHeadOffset(float headOffset) {
		float pressOffset = Mth.clamp(-headOffset, 0f, 1f);
		return Mth.clamp((pressOffset - PRESS_EFFECT_START_OFFSET) / (1f - PRESS_EFFECT_START_OFFSET), 0f, 1f);
	}

	private static CompoundTag createChargedCreeperTag() {
		CompoundTag tag = new CompoundTag();
		tag.putBoolean("powered", true);
		return tag;
	}

	@Nullable
	private Creeper getOrCreateCreeper(Level level) {
		if (cachedCreeper != null && cachedLevel == level)
			return cachedCreeper;

		Creeper creeper = net.minecraft.world.entity.EntityType.CREEPER.create(level);
		if (creeper == null)
			return null;
		creeper.setNoAi(true);
		creeper.readAdditionalSaveData(CHARGED_CREEPER_TAG);
		creeper.tickCount = 0;
		cachedLevel = level;
		cachedCreeper = creeper;
		return creeper;
	}
}
