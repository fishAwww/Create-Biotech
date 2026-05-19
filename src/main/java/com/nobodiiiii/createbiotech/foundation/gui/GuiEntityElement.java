package com.nobodiiiii.createbiotech.foundation.gui;

import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;

import com.mojang.blaze3d.platform.GlStateManager.DestFactor;
import com.mojang.blaze3d.platform.GlStateManager.SourceFactor;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.createmod.catnip.animation.AnimationTickHolder;
import net.createmod.catnip.gui.ILightingSettings;
import net.createmod.catnip.gui.UIRenderHelper;
import net.createmod.catnip.gui.element.AbstractRenderElement;
import net.createmod.catnip.math.VecHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

public final class GuiEntityElement {

	public static final ILightingSettings DEFAULT_LIGHTING = Lighting::setupForEntityInInventory;

	private GuiEntityElement() {
	}

	public static <T extends Entity> GuiEntityRenderBuilder<T> of(T entity) {
		return new GuiEntityRenderBuilder<>(entity);
	}

	public static class GuiEntityRenderBuilder<T extends Entity> extends AbstractRenderElement {
		private static final Direction DEFAULT_FACING = Direction.SOUTH;

		private final T entity;

		private double xLocal;
		private double yLocal;
		private double zLocal;
		private double xRot;
		private double yRot;
		private double zRot;
		private double sceneScale = 1;
		private double entityScaleX = 1;
		private double entityScaleY = 1;
		private double entityScaleZ = 1;
		private Vec3 rotationOffset = Vec3.ZERO;
		@Nullable
		private ILightingSettings customLighting = DEFAULT_LIGHTING;
		private int packedLight = LightTexture.FULL_BRIGHT;
		private boolean renderShadow;
		private float partialTicks = AnimationTickHolder.getPartialTicks();
		@Nullable
		private Integer tickCountOverride;
		private float dispatcherYaw;
		@Nullable
		private Float renderYaw;
		@Nullable
		private Float bodyYaw;
		@Nullable
		private Float headYaw;
		@Nullable
		private Float pitch;

		private GuiEntityRenderBuilder(T entity) {
			this.entity = entity;
			face(DEFAULT_FACING);
		}

		@Override
		public GuiEntityRenderBuilder<T> at(float x, float y) {
			super.at(x, y);
			return this;
		}

		@Override
		public GuiEntityRenderBuilder<T> at(float x, float y, float z) {
			super.at(x, y, z);
			return this;
		}

		@Override
		public GuiEntityRenderBuilder<T> withBounds(int width, int height) {
			super.withBounds(width, height);
			return this;
		}

		@Override
		public GuiEntityRenderBuilder<T> withAlpha(float alpha) {
			super.withAlpha(alpha);
			return this;
		}

		public GuiEntityRenderBuilder<T> atLocal(double x, double y, double z) {
			this.xLocal = x;
			this.yLocal = y;
			this.zLocal = z;
			return this;
		}

		public GuiEntityRenderBuilder<T> rotate(double xRot, double yRot, double zRot) {
			this.xRot = xRot;
			this.yRot = yRot;
			this.zRot = zRot;
			return this;
		}

		public GuiEntityRenderBuilder<T> rotateCentered(double xRot, double yRot, double zRot) {
			return this.rotate(xRot, yRot, zRot)
				.withRotationOffset(VecHelper.getCenterOf(BlockPos.ZERO));
		}

		public GuiEntityRenderBuilder<T> scale(double scale) {
			this.sceneScale = scale;
			return this;
		}

		public GuiEntityRenderBuilder<T> scaleEntity(double scale) {
			return scaleEntity(scale, scale, scale);
		}

		public GuiEntityRenderBuilder<T> scaleEntity(double xScale, double yScale, double zScale) {
			this.entityScaleX = xScale;
			this.entityScaleY = yScale;
			this.entityScaleZ = zScale;
			return this;
		}

		public GuiEntityRenderBuilder<T> withRotationOffset(Vec3 offset) {
			this.rotationOffset = offset;
			return this;
		}

		public GuiEntityRenderBuilder<T> lighting(ILightingSettings lighting) {
			this.customLighting = lighting;
			return this;
		}

		public GuiEntityRenderBuilder<T> packedLight(int packedLight) {
			this.packedLight = packedLight;
			return this;
		}

		public GuiEntityRenderBuilder<T> renderShadow(boolean renderShadow) {
			this.renderShadow = renderShadow;
			return this;
		}

		public GuiEntityRenderBuilder<T> partialTicks(float partialTicks) {
			this.partialTicks = partialTicks;
			return this;
		}

		public GuiEntityRenderBuilder<T> ticks(int tickCount) {
			this.tickCountOverride = tickCount;
			return this;
		}

		public GuiEntityRenderBuilder<T> dispatcherYaw(float dispatcherYaw) {
			this.dispatcherYaw = dispatcherYaw;
			return this;
		}

		public GuiEntityRenderBuilder<T> yaw(float yaw) {
			this.renderYaw = yaw;
			return this;
		}

		public GuiEntityRenderBuilder<T> bodyYaw(float bodyYaw) {
			this.bodyYaw = bodyYaw;
			return this;
		}

		public GuiEntityRenderBuilder<T> headYaw(float headYaw) {
			this.headYaw = headYaw;
			return this;
		}

		public GuiEntityRenderBuilder<T> pitch(float pitch) {
			this.pitch = pitch;
			return this;
		}

		public GuiEntityRenderBuilder<T> face(Direction direction) {
			float yaw = direction.toYRot();
			return yaw(yaw).bodyYaw(yaw).headYaw(yaw);
		}

		@Override
		public void render(GuiGraphics graphics) {
			PoseStack poseStack = graphics.pose();
			prepareMatrix(poseStack);

			poseStack.translate(x, y, z);
			poseStack.scale((float) sceneScale, (float) sceneScale, (float) sceneScale);
			poseStack.translate(xLocal, yLocal, zLocal);
			UIRenderHelper.flipForGuiRender(poseStack);
			poseStack.scale((float) entityScaleX, (float) entityScaleY, (float) entityScaleZ);
			poseStack.translate(rotationOffset.x, rotationOffset.y, rotationOffset.z);
			poseStack.mulPose(Axis.ZP.rotationDegrees((float) zRot));
			poseStack.mulPose(Axis.XP.rotationDegrees((float) xRot));
			poseStack.mulPose(Axis.YP.rotationDegrees((float) yRot));
			poseStack.translate(-rotationOffset.x, -rotationOffset.y, -rotationOffset.z);
			Quaternionf sceneCameraOrientation = new Quaternionf()
				.rotateZ((float) Math.toRadians(-zRot))
				.rotateX((float) Math.toRadians(-xRot))
				.rotateY((float) Math.toRadians(yRot));
			renderEntity(graphics, poseStack, sceneCameraOrientation);
			cleanUpMatrix(poseStack);
		}

		private void renderEntity(GuiGraphics graphics, PoseStack poseStack, Quaternionf sceneCameraOrientation) {
			EntityRenderDispatcher dispatcher = Minecraft.getInstance()
				.getEntityRenderDispatcher();
			Quaternionf previousCamera = dispatcher.cameraOrientation() == null ? null
				: new Quaternionf(dispatcher.cameraOrientation());
			MultiBufferSource.BufferSource buffer = graphics.bufferSource();
			EntityRenderState state = EntityRenderState.capture(entity);

			try {
				applyEntityState();
				dispatcher.overrideCameraOrientation(new Quaternionf(sceneCameraOrientation).conjugate());
				dispatcher.setRenderShadow(renderShadow);
				RenderSystem.runAsFancy(
					() -> dispatcher.render(entity, 0, 0, 0, dispatcherYaw, partialTicks, poseStack, buffer, packedLight));
				buffer.endBatch();
			} finally {
				dispatcher.setRenderShadow(true);
				if (previousCamera != null)
					dispatcher.overrideCameraOrientation(previousCamera);
				state.restore(entity);
			}
		}

		private void prepareMatrix(PoseStack poseStack) {
			poseStack.pushPose();
			RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, alpha);
			RenderSystem.enableDepthTest();
			RenderSystem.enableBlend();
			RenderSystem.blendFunc(SourceFactor.SRC_ALPHA, DestFactor.ONE_MINUS_SRC_ALPHA);
			prepareLighting();
		}

		private void cleanUpMatrix(PoseStack poseStack) {
			poseStack.popPose();
			cleanUpLighting();
		}

		private void prepareLighting() {
			if (customLighting != null) {
				customLighting.applyLighting();
			} else {
				Lighting.setupFor3DItems();
			}
		}

		private void cleanUpLighting() {
			if (customLighting != null)
				Lighting.setupFor3DItems();
		}

		private void applyEntityState() {
			if (tickCountOverride != null)
				entity.tickCount = tickCountOverride;

			Float appliedYaw = renderYaw;
			if (appliedYaw != null) {
				entity.setYRot(appliedYaw);
				entity.yRotO = appliedYaw;
			}

			if (pitch != null) {
				entity.setXRot(pitch);
				entity.xRotO = pitch;
			}

			if (entity instanceof LivingEntity livingEntity) {
				float appliedBodyYaw = bodyYaw != null ? bodyYaw : appliedYaw != null ? appliedYaw : livingEntity.yBodyRot;
				float appliedHeadYaw = headYaw != null ? headYaw : appliedYaw != null ? appliedYaw : livingEntity.yHeadRot;
				livingEntity.setYBodyRot(appliedBodyYaw);
				livingEntity.yBodyRotO = appliedBodyYaw;
				livingEntity.yHeadRot = appliedHeadYaw;
				livingEntity.yHeadRotO = appliedHeadYaw;
			}
		}
	}

	private record EntityRenderState(float yRot, float yRotO, float xRot, float xRotO, int tickCount,
		float bodyYaw, float bodyYawO, float headYaw, float headYawO, boolean living) {

		private static EntityRenderState capture(Entity entity) {
			if (entity instanceof LivingEntity livingEntity)
				return new EntityRenderState(entity.getYRot(), entity.yRotO, entity.getXRot(), entity.xRotO,
					entity.tickCount, livingEntity.yBodyRot, livingEntity.yBodyRotO, livingEntity.yHeadRot,
					livingEntity.yHeadRotO, true);
			return new EntityRenderState(entity.getYRot(), entity.yRotO, entity.getXRot(), entity.xRotO,
				entity.tickCount, 0, 0, 0, 0, false);
		}

		private void restore(Entity entity) {
			entity.setYRot(yRot);
			entity.yRotO = yRotO;
			entity.setXRot(xRot);
			entity.xRotO = xRotO;
			entity.tickCount = tickCount;
			if (living && entity instanceof LivingEntity livingEntity) {
				livingEntity.setYBodyRot(bodyYaw);
				livingEntity.yBodyRotO = bodyYawO;
				livingEntity.yHeadRot = headYaw;
				livingEntity.yHeadRotO = headYawO;
			}
		}
	}
}
