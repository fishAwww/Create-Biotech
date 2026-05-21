package com.nobodiiiii.createbiotech.compat.jei;

import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.nobodiiiii.createbiotech.content.squidprinter.SquidPrinterBlockEntity;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllPartialModels;

import net.createmod.catnip.animation.AnimationTickHolder;
import net.createmod.catnip.gui.UIRenderHelper;
import net.createmod.catnip.platform.CatnipClientServices;
import net.createmod.catnip.platform.ForgeCatnipServices;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.MultiBufferSource.BufferSource;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.animal.Squid;
import net.minecraftforge.fluids.FluidStack;
import org.joml.Quaternionf;

public class AnimatedSquidSpout extends AnimatedKineticsWithEntities {
	private static final double SQUID_ATTACHMENT_Y = -0.2d;
	private static final double SQUID_SCALE = 0.8d;
	private static final int SCENE_SCALE = 20;
	private static final BlockPos PARTICLE_ORIGIN = BlockPos.ZERO;
	private static final int BURST_PHASE_TICKS = 3;
	private static final int CYCLE_LENGTH_TICKS = 30;

	private List<FluidStack> fluids;
	private final List<Particle> activeParticles = new ArrayList<>();
	private long lastParticleTick = Long.MIN_VALUE;
	private final PreviewCamera jeiParticleCamera = new PreviewCamera();

	public AnimatedSquidSpout withFluids(List<FluidStack> fluids) {
		this.fluids = fluids;
		return this;
	}

	@Override
	public void draw(GuiGraphics graphics, int xOffset, int yOffset) {
		PoseStack matrixStack = graphics.pose();
		matrixStack.pushPose();
		matrixStack.translate(xOffset, yOffset, 100);

		matrixStack.mulPose(Axis.XP.rotationDegrees(-15.5f));
		matrixStack.mulPose(Axis.YP.rotationDegrees(22.5f));
		int scale = SCENE_SCALE;

		blockElement(AllBlocks.SPOUT.getDefaultState())
			.scale(scale)
			.render(graphics);

		float cycle = (AnimationTickHolder.getRenderTime() - offset * 8) % 30;
		float squeeze = cycle < 20 ? Mth.sin((float) (cycle / 20f * Math.PI)) : 0;
		squeeze *= 20;

		matrixStack.pushPose();

		blockElement(AllPartialModels.SPOUT_TOP)
			.scale(scale)
			.render(graphics);
		matrixStack.translate(0, -3 * squeeze / 32f, 0);
		blockElement(AllPartialModels.SPOUT_MIDDLE)
			.scale(scale)
			.render(graphics);
		matrixStack.translate(0, -3 * squeeze / 32f, 0);
		blockElement(AllPartialModels.SPOUT_BOTTOM)
			.scale(scale)
			.render(graphics);
		matrixStack.translate(0, -3 * squeeze / 32f, 0);

		matrixStack.popPose();

		Squid squid = SquidJeiRenderer.getOrCreateSquid();
		if (squid != null) {
			entityElement(squid)
				.atLocal(0.5d, SQUID_ATTACHMENT_Y, 0.5d)
				.scale(scale)
				.scaleEntity(SQUID_SCALE)
				.stateModifier(SquidJeiRenderer::animateTentacles)
				.render(graphics);
		}

		blockElement(AllBlocks.DEPOT.getDefaultState())
			.atLocal(0, 2, 0)
			.scale(scale)
			.render(graphics);

		DEFAULT_LIGHTING.applyLighting();
		BufferSource buffer = MultiBufferSource.immediate(Tesselator.getInstance()
			.getBuilder());
		matrixStack.pushPose();
		UIRenderHelper.flipForGuiRender(matrixStack);
		matrixStack.scale(16, 16, 16);
		float from = 3f / 16f;
		float to = 17f / 16f;
		FluidStack fluidStack = fluids.get(0);
		ForgeCatnipServices.FLUID_RENDERER.renderFluidBox(fluidStack, from, from, from, to, to, to,
			graphics.bufferSource(), matrixStack, LightTexture.FULL_BRIGHT, false, true);
		matrixStack.popPose();

		renderInkParticles(graphics);
		buffer.endBatch();
		Lighting.setupFor3DItems();

		matrixStack.popPose();
	}

	private void renderInkParticles(GuiGraphics graphics) {
		ClientLevel level = Minecraft.getInstance().level;
		if (level == null)
			return;

		syncInkParticles(level);
		if (activeParticles.isEmpty())
			return;

		Camera camera = setupJeiParticleCamera();
		PoseStack poseStack = graphics.pose();
		poseStack.pushPose();
		poseStack.translate(SCENE_SCALE / 2f, SCENE_SCALE * 1.5f, SCENE_SCALE / 2f);
		UIRenderHelper.flipForGuiRender(poseStack);
		poseStack.scale(16, 16, 16);

		ParticleRenderType renderType = ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
		LightTexture lightTexture = Minecraft.getInstance().gameRenderer.lightTexture();
		lightTexture.turnOnLightLayer();
		RenderSystem.setShaderTexture(0, TextureAtlas.LOCATION_PARTICLES);
		RenderSystem.disableCull();
		RenderSystem.enableDepthTest();
		RenderSystem.enableBlend();
		RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
		RenderSystem.depthMask(true);

		try {
			BufferBuilder builder = Tesselator.getInstance().getBuilder();
			renderType.begin(builder, Minecraft.getInstance().textureManager);
			VertexConsumer transformed = new PoseStackVertexConsumer(builder, poseStack.last().pose());
			float partialTicks = AnimationTickHolder.getPartialTicks();
			for (Particle particle : activeParticles)
				particle.render(transformed, camera, partialTicks);
			renderType.end(Tesselator.getInstance());
		} finally {
			RenderSystem.enableCull();
			RenderSystem.disableBlend();
			lightTexture.turnOffLightLayer();
		}

		poseStack.popPose();
	}

	private void syncInkParticles(ClientLevel level) {
		long currentTick = level.getGameTime();
		if (currentTick == lastParticleTick)
			return;

		lastParticleTick = currentTick;
		Iterator<Particle> iterator = activeParticles.iterator();
		while (iterator.hasNext()) {
			Particle particle = iterator.next();
			particle.tick();
			if (!particle.isAlive())
				iterator.remove();
		}

		int cycleTick = (int) ((AnimationTickHolder.getTicks() - offset * 8) % CYCLE_LENGTH_TICKS);
		if (cycleTick < 0)
			cycleTick += CYCLE_LENGTH_TICKS;

		if (cycleTick >= BURST_PHASE_TICKS)
			SquidPrinterBlockEntity.forEachBurstInkParticle(level, PARTICLE_ORIGIN, this::spawnInkParticle);
		if (currentTick % 3 == 0 && cycleTick >= BURST_PHASE_TICKS)
			SquidPrinterBlockEntity.forEachAmbientInkParticle(level, PARTICLE_ORIGIN, this::spawnInkParticle);
	}

	private void spawnInkParticle(double x, double y, double z, double dx, double dy, double dz) {
		ClientLevel level = Minecraft.getInstance().level;
		if (level == null)
			return;
		Particle particle = CatnipClientServices.CLIENT_HOOKS.createParticleFromData(ParticleTypes.SQUID_INK, level, x,
			y, z, dx, dy, dz);
		if (particle != null)
			activeParticles.add(particle);
	}

	private Camera setupJeiParticleCamera() {
		Quaternionf inverseSceneRotation = new Quaternionf()
			.rotateY((float) Math.toRadians(-22.5f))
			.rotateX((float) Math.toRadians(15.5f));
		jeiParticleCamera.configure(0.0d, 0.0d, 0.0d, inverseSceneRotation);
		return jeiParticleCamera;
	}

	private static class PoseStackVertexConsumer implements VertexConsumer {
		private final VertexConsumer delegate;
		private final org.joml.Matrix4f pose;

		private PoseStackVertexConsumer(VertexConsumer delegate, org.joml.Matrix4f pose) {
			this.delegate = delegate;
			this.pose = new org.joml.Matrix4f(pose);
		}

		@Override
		public VertexConsumer vertex(double x, double y, double z) {
			return delegate.vertex(pose, (float) x, (float) y, (float) z);
		}

		@Override
		public VertexConsumer color(int red, int green, int blue, int alpha) {
			return delegate.color(red, green, blue, alpha);
		}

		@Override
		public VertexConsumer uv(float u, float v) {
			return delegate.uv(u, v);
		}

		@Override
		public VertexConsumer overlayCoords(int u, int v) {
			return delegate.overlayCoords(u, v);
		}

		@Override
		public VertexConsumer uv2(int u, int v) {
			return delegate.uv2(u, v);
		}

		@Override
		public VertexConsumer normal(float x, float y, float z) {
			return delegate.normal(x, y, z);
		}

		@Override
		public void endVertex() {
			delegate.endVertex();
		}

		@Override
		public void defaultColor(int red, int green, int blue, int alpha) {
			delegate.defaultColor(red, green, blue, alpha);
		}

		@Override
		public void unsetDefaultColor() {
			delegate.unsetDefaultColor();
		}
	}

	private static class PreviewCamera extends Camera {
		private Quaternionf rotation = new Quaternionf();

		private void configure(double x, double y, double z, Quaternionf rotation) {
			super.setPosition(x, y, z);
			this.rotation = new Quaternionf(rotation);
		}

		@Override
		public Quaternionf rotation() {
			return new Quaternionf(rotation);
		}
	}
}
