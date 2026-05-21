package com.nobodiiiii.createbiotech.compat.jei;

import org.jetbrains.annotations.Nullable;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.nobodiiiii.createbiotech.content.experience.ExperienceOrbModelRenderer;

import net.createmod.catnip.animation.AnimationTickHolder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.BookModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public class AnimatedEvokerEnchanting extends AnimatedKineticsWithEntities {

	private static final ResourceLocation BOOK_TEXTURE =
		new ResourceLocation("minecraft", "textures/entity/enchanting_table_book.png");
	private static final BlockState ENCHANTING_TABLE = Blocks.ENCHANTING_TABLE.defaultBlockState();
	private static final Direction FACING = Direction.NORTH;
	private static final int SCENE_SCALE = 20;
	private static final int PACKED_LIGHT = LightTexture.FULL_BRIGHT;
	private static final float ENCHANTING_TABLE_TOP_Y = 14f / 16f;
	private static final float BOOK_FRONT_OFFSET = 4f / 16f;
	private static final float BOOK_BASE_Y = ENCHANTING_TABLE_TOP_Y + 4f / 16f;
	private static final float BOOK_BOB_AMPLITUDE = 0.04f;
	private static final float BOOK_BOB_SPEED = 0.08f;
	private static final float BOOK_Z_ROTATION = 80f;
	private static final float ITEM_BASE_Y = BOOK_BASE_Y + 6f / 16f;
	private static final float ITEM_BOB_AMPLITUDE = 0.06f;
	private static final float ITEM_BOB_SPEED = 0.1f;
	private static final float ITEM_SPIN_SPEED = 2.5f;
	private static final float ITEM_SCALE = 0.7f;
	private static final float DISPLAY_TRANSFORM_CYCLE = 80f;
	private static final float OUTPUT_PHASE_START = 0.72f;
	private static final float PARTICLE_SOURCE_SPREAD = 0.72f;
	private static final float PARTICLE_TARGET_SPREAD = 0.16f;
	private static final float PARTICLE_SPEED = 0.09f;
	private static final int PARTICLE_COUNT = 7;
	private static final float PARTICLE_ORB_SCALE = 0.18f;

	private ItemStack inputCopy = ItemStack.EMPTY;
	private ItemStack outputBook = ItemStack.EMPTY;
	@Nullable
	private BookModel bookModel;

	public AnimatedEvokerEnchanting withRecipe(EvokerEnchantingChamberJeiRecipe recipe) {
		inputCopy = recipe.inputCopy()
			.copy();
		outputBook = recipe.outputBook()
			.copy();
		return this;
	}

	@Override
	public void draw(GuiGraphics graphics, int xOffset, int yOffset) {
		ClientLevel level = Minecraft.getInstance().level;
		if (level == null)
			return;

		PoseStack poseStack = graphics.pose();
		poseStack.pushPose();
		poseStack.translate(xOffset, yOffset, 100);
		poseStack.mulPose(Axis.XP.rotationDegrees(-15.5f));
		poseStack.mulPose(Axis.YP.rotationDegrees(22.5f));

		blockElement(ENCHANTING_TABLE)
			.scale(SCENE_SCALE)
			.render(graphics);

		DEFAULT_LIGHTING.applyLighting();
		try {
			renderBook(graphics);
			renderEnchantingItem(level, graphics);
			renderEnchantParticles(graphics);
		} finally {
			Lighting.setupFor3DItems();
		}

		poseStack.popPose();
	}

	private void renderBook(GuiGraphics graphics) {
		BookModel bookModel = getBookModel();
		if (bookModel == null)
			return;

		float renderTime = AnimationTickHolder.getRenderTime();
		float bob = Mth.sin(renderTime * BOOK_BOB_SPEED) * BOOK_BOB_AMPLITUDE;
		double bookX = 0.5d + FACING.getStepX() * BOOK_FRONT_OFFSET;
		double bookZ = 0.5d + FACING.getStepZ() * BOOK_FRONT_OFFSET;
		double bookY = BOOK_BASE_Y + bob;
		float bookYRot = (180f - FACING.toYRot()) - 90f;
		float pageFlutter = Mth.sin(renderTime * 0.135f) * 0.05f;

		graphics.pose()
			.pushPose();
		graphics.pose()
			.scale(SCENE_SCALE, SCENE_SCALE, SCENE_SCALE);
		graphics.pose()
			.translate(bookX, bookY, bookZ);
		graphics.pose()
			.mulPose(Axis.YP.rotationDegrees(bookYRot));
		graphics.pose()
			.mulPose(Axis.ZP.rotationDegrees(BOOK_Z_ROTATION));

		bookModel.setupAnim(renderTime, pageFlutter, pageFlutter, 1.0f);
		VertexConsumer bookConsumer = graphics.bufferSource()
			.getBuffer(bookModel.renderType(BOOK_TEXTURE));
		bookModel.renderToBuffer(graphics.pose(), bookConsumer, PACKED_LIGHT, OverlayTexture.NO_OVERLAY, 1.0f, 1.0f,
			1.0f, 1.0f);
		graphics.pose()
			.popPose();
	}

	private void renderEnchantingItem(ClientLevel level, GuiGraphics graphics) {
		ItemStack displayStack = getDisplayedItem(AnimationTickHolder.getRenderTime());
		if (displayStack.isEmpty())
			return;

		float renderTime = AnimationTickHolder.getRenderTime();
		float bob = Mth.sin(renderTime * ITEM_BOB_SPEED) * ITEM_BOB_AMPLITUDE;
		float spin = renderTime * ITEM_SPIN_SPEED;
		double itemX = 0.5d + FACING.getStepX() * BOOK_FRONT_OFFSET;
		double itemZ = 0.5d + FACING.getStepZ() * BOOK_FRONT_OFFSET;
		double itemY = ITEM_BASE_Y + bob;

		graphics.pose()
			.pushPose();
		graphics.pose()
			.scale(SCENE_SCALE, SCENE_SCALE, SCENE_SCALE);
		graphics.pose()
			.translate(itemX, itemY, itemZ);
		graphics.pose()
			.mulPose(Axis.YP.rotationDegrees(spin));
		graphics.pose()
			.scale(ITEM_SCALE, ITEM_SCALE, ITEM_SCALE);

		Minecraft.getInstance()
			.getItemRenderer()
			.renderStatic(displayStack, ItemDisplayContext.GROUND, PACKED_LIGHT, OverlayTexture.NO_OVERLAY,
				graphics.pose(), graphics.bufferSource(), level, 0);
		graphics.pose()
			.popPose();
	}

	private void renderEnchantParticles(GuiGraphics graphics) {
		float renderTime = AnimationTickHolder.getRenderTime();
		float sourceY = BOOK_BASE_Y + Mth.sin(renderTime * BOOK_BOB_SPEED) * BOOK_BOB_AMPLITUDE - 0.02f;
		float targetY = ITEM_BASE_Y + Mth.sin(renderTime * ITEM_BOB_SPEED) * ITEM_BOB_AMPLITUDE;
		float anchorX = 0.5f + FACING.getStepX() * BOOK_FRONT_OFFSET;
		float anchorZ = 0.5f + FACING.getStepZ() * BOOK_FRONT_OFFSET;

		for (int i = 0; i < PARTICLE_COUNT; i++) {
			float progress = (renderTime * PARTICLE_SPEED + i / (float) PARTICLE_COUNT) % 1.0f;
			float eased = progress * progress * (3.0f - 2.0f * progress);
			float sourceAngle = i * 1.91f + renderTime * 0.07f;
			float targetAngle = i * 1.37f + renderTime * 0.11f;
			float startX = anchorX + Mth.sin(sourceAngle) * PARTICLE_SOURCE_SPREAD * 0.5f;
			float startZ = anchorZ + Mth.cos(sourceAngle) * PARTICLE_SOURCE_SPREAD * 0.5f;
			float endX = anchorX + Mth.sin(targetAngle) * PARTICLE_TARGET_SPREAD;
			float endZ = anchorZ + Mth.cos(targetAngle) * PARTICLE_TARGET_SPREAD;
			float x = Mth.lerp(eased, startX, endX);
			float y = Mth.lerp(eased, sourceY, targetY);
			float z = Mth.lerp(eased, startZ, endZ);
			renderParticle(graphics, x, y, z, renderTime, i);
		}
	}

	private void renderParticle(GuiGraphics graphics, float x, float y, float z, float renderTime, int index) {
		graphics.pose()
			.pushPose();
		graphics.pose()
			.scale(SCENE_SCALE, SCENE_SCALE, SCENE_SCALE);
		graphics.pose()
			.translate(x, y, z);
		ExperienceOrbModelRenderer.render(graphics.pose(), graphics.bufferSource(), PACKED_LIGHT,
			renderTime * 2.0f + index * 5.0f, index & 0x0F, PARTICLE_ORB_SCALE);
		graphics.pose()
			.popPose();
	}

	private ItemStack getDisplayedItem(float renderTime) {
		if (inputCopy.isEmpty())
			return outputBook;
		if (outputBook.isEmpty())
			return inputCopy;

		float cycle = (renderTime % DISPLAY_TRANSFORM_CYCLE) / DISPLAY_TRANSFORM_CYCLE;
		return cycle >= OUTPUT_PHASE_START ? outputBook : inputCopy;
	}

	private @Nullable BookModel getBookModel() {
		if (bookModel == null && Minecraft.getInstance().getEntityModels() != null)
			bookModel = new BookModel(Minecraft.getInstance()
				.getEntityModels()
				.bakeLayer(ModelLayers.BOOK));
		return bookModel;
	}
}
