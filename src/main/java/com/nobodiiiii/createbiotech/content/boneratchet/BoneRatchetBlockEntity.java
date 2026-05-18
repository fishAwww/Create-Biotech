package com.nobodiiiii.createbiotech.content.boneratchet;

import com.nobodiiiii.createbiotech.registry.CBBlockEntityTypes;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.api.stress.BlockStressValues;
import com.simibubi.create.content.kinetics.transmission.SplitShaftBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;

public class BoneRatchetBlockEntity extends SplitShaftBlockEntity {

	private static final float FALLBACK_JAM_STRESS_IMPACT = 20000f;
	private static final float CREATIVE_MOTOR_MARGIN = 1024f;

	private boolean refreshingStress;

	public BoneRatchetBlockEntity(BlockPos pos, BlockState state) {
		super(CBBlockEntityTypes.BONE_RATCHET.get(), pos, state);
	}

	@Override
	public float getRotationSpeedModifier(Direction face) {
		return 1;
	}

	@Override
	public float calculateStressApplied() {
		lastStressApplied = getDirectionalStressImpact();
		return lastStressApplied;
	}

	@Override
	public void updateFromNetwork(float maxStress, float currentStress, int networkSize) {
		super.updateFromNetwork(maxStress, currentStress, networkSize);
		refreshStressFromDirection(false);
	}

	@Override
	public void onSpeedChanged(float previousSpeed) {
		super.onSpeedChanged(previousSpeed);
		refreshStressFromDirection(true);
	}

	private void refreshStressFromDirection(boolean forceNetworkUpdate) {
		if (level == null || level.isClientSide || !hasNetwork() || refreshingStress)
			return;

		float directionalStress = getDirectionalStressImpact();
		if (!forceNetworkUpdate && Mth.equal(lastStressApplied, directionalStress))
			return;

		lastStressApplied = directionalStress;
		refreshingStress = true;
		try {
			getOrCreateNetwork().updateStressFor(this, directionalStress);
		} finally {
			refreshingStress = false;
		}
	}

	private float getDirectionalStressImpact() {
		return isReverseRotation() ? getJammingStressImpact() : 0;
	}

	private boolean isReverseRotation() {
		return getTheoreticalSpeed() < 0;
	}

	private static float getJammingStressImpact() {
		double creativeMotorCapacity = BlockStressValues.getCapacity(AllBlocks.CREATIVE_MOTOR.get());
		double impact = Math.max(FALLBACK_JAM_STRESS_IMPACT, creativeMotorCapacity + CREATIVE_MOTOR_MARGIN);
		return impact > Float.MAX_VALUE ? Float.MAX_VALUE : (float) impact;
	}
}
