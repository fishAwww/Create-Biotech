package com.nobodiiiii.createbiotech.content.boneratchet;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.kinetics.base.DirectionalKineticBlock;
import com.simibubi.create.content.kinetics.base.IRotate;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;

import dev.engine_room.flywheel.api.visualization.VisualizationManager;
import net.createmod.catnip.data.Iterate;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.Direction.AxisDirection;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public class BoneRatchetRenderer extends KineticBlockEntityRenderer<BoneRatchetBlockEntity> {

	public BoneRatchetRenderer(BlockEntityRendererProvider.Context context) {
		super(context);
	}

	@Override
	protected void renderSafe(BoneRatchetBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer,
		int light, int overlay) {
		super.renderSafe(be, partialTicks, ms, buffer, light, overlay);
		if (VisualizationManager.supportsVisualization(be.getLevel()))
			return;

		BlockState blockState = be.getBlockState();
		Block block = blockState.getBlock();
		if (!(block instanceof IRotate def))
			return;

		Axis axis = getRotationAxisOf(be);
		BlockPos pos = be.getBlockPos();
		float angle = getAngleForBe(be, pos, axis);

		for (Direction d : Iterate.directionsInAxis(axis)) {
			if (!def.hasShaftTowards(be.getLevel(), be.getBlockPos(), blockState, d))
				continue;
			SuperByteBuffer shaft = CachedBuffers.partialFacing(AllPartialModels.SHAFT_HALF, blockState, d);
			kineticRotationTransform(shaft, be, axis, angle, light);
			shaft.renderInto(ms, buffer.getBuffer(RenderType.solid()));
		}
	}

	@Override
	protected SuperByteBuffer getRotatedModel(BoneRatchetBlockEntity be, BlockState state) {
		Axis axis = state.getValue(DirectionalKineticBlock.FACING).getAxis();
		return CachedBuffers.partialFacingVertical(AllPartialModels.SHAFTLESS_COGWHEEL, state,
			Direction.fromAxisAndDirection(axis, AxisDirection.POSITIVE));
	}
}
