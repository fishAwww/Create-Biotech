package com.nobodiiiii.createbiotech.content.schrodingerscat;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.nobodiiiii.createbiotech.CreateBiotech;
import com.simibubi.create.foundation.blockEntity.renderer.SmartBlockEntityRenderer;

import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.createmod.catnip.render.CachedBuffers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;

public class SchrodingersCatRenderer extends SmartBlockEntityRenderer<SchrodingersCatBlockEntity> {

	private static final PartialModel TORCH_OFF =
		PartialModel.of(CreateBiotech.asResource("block/schrodingers_cat/redstone_torch_off"));
	private static final PartialModel TORCH_ON =
		PartialModel.of(CreateBiotech.asResource("block/schrodingers_cat/redstone_torch_on"));

	public SchrodingersCatRenderer(BlockEntityRendererProvider.Context context) {
		super(context);
	}

	@Override
	protected void renderSafe(SchrodingersCatBlockEntity blockEntity, float partialTicks, PoseStack poseStack,
		MultiBufferSource buffer, int packedLight, int packedOverlay) {
		super.renderSafe(blockEntity, partialTicks, poseStack, buffer, packedLight, packedOverlay);

		BlockState state = blockEntity.getBlockState();
		if (!state.hasProperty(SchrodingersCatBlock.FACING))
			return;

		Direction facing = state.getValue(SchrodingersCatBlock.FACING);
		renderTorch(blockEntity, poseStack, buffer, packedLight, state, facing);
	}

	private void renderTorch(SchrodingersCatBlockEntity blockEntity, PoseStack poseStack, MultiBufferSource buffer,
		int packedLight, BlockState state, Direction facing) {
		boolean powered = blockEntity.getOutputSignal() > 0;
		VertexConsumer cutoutBuffer = buffer.getBuffer(RenderType.cutout());

		CachedBuffers.partial(powered ? TORCH_ON : TORCH_OFF, state)
			.center()
			.rotateYDegrees(torchModelYRotation(facing))
			.uncenter()
			.light(packedLight)
			.renderInto(poseStack, cutoutBuffer);
	}

	private static int torchModelYRotation(Direction facing) {
		return switch (facing) {
			case EAST -> 270;
			case SOUTH -> 180;
			case WEST -> 90;
			default -> 0;
		};
	}
}
