package com.nobodiiiii.createbiotech.content.slimeclutch;

import com.nobodiiiii.createbiotech.registry.CBBlockEntityTypes;
import com.simibubi.create.content.kinetics.transmission.SplitShaftBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

public class SlimeClutchBlockEntity extends SplitShaftBlockEntity {

	private boolean pendingStateUpdate;
	private boolean processingTransition;
	private boolean baselineRecorded;
	private float baselineCapacity;
	private float baselineStress;

	public SlimeClutchBlockEntity(BlockPos pos, BlockState state) {
		super(CBBlockEntityTypes.SLIME_CLUTCH.get(), pos, state);
	}

	@Override
	public float getRotationSpeedModifier(Direction face) {
		if (hasSource()) {
			if (face != getSourceFacing() && getBlockState().getValue(BlockStateProperties.POWERED))
				return 0;
		}
		return 1;
	}

	@Override
	public void updateFromNetwork(float maxStress, float currentStress, int networkSize) {
		super.updateFromNetwork(maxStress, currentStress, networkSize);
		if (level == null || level.isClientSide || processingTransition)
			return;

		boolean powered = getBlockState().getValue(BlockStateProperties.POWERED);
		if (powered) {
			if (networkSize == 0)
				return;
			if (!baselineRecorded) {
				baselineRecorded = true;
				baselineCapacity = maxStress;
				baselineStress = currentStress;
				return;
			}
			if (maxStress != baselineCapacity || currentStress != baselineStress)
				pendingStateUpdate = true;
		} else if (isOverStressed()) {
			pendingStateUpdate = true;
		}
	}

	@Override
	public void tick() {
		super.tick();
		if (level == null || level.isClientSide || !pendingStateUpdate)
			return;
		pendingStateUpdate = false;

		BlockState state = getBlockState();
		boolean powered = state.getValue(BlockStateProperties.POWERED);
		boolean shouldBePowered = !powered;

		processingTransition = true;
		try {
			if (state.getBlock() instanceof SlimeClutchBlock clutch)
				clutch.detachKinetics(level, getBlockPos(), true);
			level.setBlock(getBlockPos(), state.setValue(BlockStateProperties.POWERED, shouldBePowered), 2 | 16);
		} finally {
			processingTransition = false;
		}

		baselineRecorded = false;
	}
}
