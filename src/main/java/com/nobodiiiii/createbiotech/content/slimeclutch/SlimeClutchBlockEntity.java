package com.nobodiiiii.createbiotech.content.slimeclutch;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;

import com.nobodiiiii.createbiotech.registry.CBBlockEntityTypes;
import com.simibubi.create.content.kinetics.RotationPropagator;
import com.simibubi.create.content.kinetics.base.IRotate;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.transmission.SplitShaftBlockEntity;

import net.createmod.catnip.data.Iterate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

public class SlimeClutchBlockEntity extends SplitShaftBlockEntity {

	private static final int RECHECK_PERIOD = 20;
	private static final int MAX_WALK = 1024;
	private static final float SAFETY_MARGIN = 0.001f;

	private boolean pendingTrip;
	private boolean processingTransition;
	private boolean requestRecheck;
	private int recheckCounter;

	private float preTripStress;
	private boolean estimateValid;
	private float downstreamStressBaseline;
	private float downstreamImpactBaseline;

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
		if (!powered) {
			if (isOverStressed()) {
				preTripStress = currentStress;
				pendingTrip = true;
			}
		} else {
			if (!estimateValid && networkSize > 0) {
				downstreamStressBaseline = Math.max(0f, preTripStress - currentStress);
				downstreamImpactBaseline = walkDownstreamImpactSum();
				estimateValid = true;
			} else {
				requestRecheck = true;
			}
		}
	}

	@Override
	public void tick() {
		super.tick();
		if (level == null || level.isClientSide)
			return;

		BlockState state = getBlockState();
		boolean powered = state.getValue(BlockStateProperties.POWERED);

		if (!powered) {
			if (pendingTrip) {
				pendingTrip = false;
				estimateValid = false;
				performTransition(true);
				recheckCounter = RECHECK_PERIOD;
				requestRecheck = false;
			}
			return;
		}

		pendingTrip = false;

		if (requestRecheck) {
			requestRecheck = false;
			recheckCounter = RECHECK_PERIOD;
			tryRecover();
			return;
		}

		if (--recheckCounter > 0)
			return;
		recheckCounter = RECHECK_PERIOD;
		tryRecover();
	}

	private void tryRecover() {
		if (!estimateValid)
			return;
		float currentImpactSum = walkDownstreamImpactSum();
		float estimate;
		if (downstreamImpactBaseline <= 0f)
			estimate = downstreamStressBaseline;
		else
			estimate = downstreamStressBaseline * (currentImpactSum / downstreamImpactBaseline);

		float remainingCapacity = capacity - stress;
		if (remainingCapacity >= estimate + SAFETY_MARGIN) {
			estimateValid = false;
			performTransition(false);
		}
	}

	private void performTransition(boolean toPowered) {
		BlockState state = getBlockState();
		processingTransition = true;
		try {
			if (state.getBlock() instanceof SlimeClutchBlock clutch)
				clutch.detachKinetics(level, getBlockPos(), true);
			level.setBlock(getBlockPos(), state.setValue(BlockStateProperties.POWERED, toPowered), 2 | 16);
		} finally {
			processingTransition = false;
		}
	}

	private float walkDownstreamImpactSum() {
		if (!hasSource() || level == null)
			return 0f;
		Direction downstreamFace = getSourceFacing().getOpposite();
		BlockPos startPos = getBlockPos().relative(downstreamFace);
		BlockEntity firstBe = level.getBlockEntity(startPos);
		if (!(firstBe instanceof KineticBlockEntity firstKbe))
			return 0f;

		Set<BlockPos> visited = new HashSet<>();
		visited.add(getBlockPos());
		visited.add(startPos);
		Queue<KineticBlockEntity> frontier = new ArrayDeque<>();
		frontier.add(firstKbe);

		float impactSum = 0f;
		int walked = 0;

		while (!frontier.isEmpty()) {
			if (++walked > MAX_WALK)
				return Float.MAX_VALUE;
			KineticBlockEntity kbe = frontier.poll();
			impactSum += kbe.calculateStressApplied();

			for (KineticBlockEntity neighbour : connectedKinetics(kbe)) {
				BlockPos np = neighbour.getBlockPos();
				if (!visited.add(np))
					continue;
				frontier.add(neighbour);
			}
		}
		return impactSum;
	}

	private List<KineticBlockEntity> connectedKinetics(KineticBlockEntity from) {
		List<KineticBlockEntity> result = new LinkedList<>();
		List<BlockPos> candidates = new LinkedList<>();
		BlockPos bp = from.getBlockPos();
		for (Direction d : Iterate.directions)
			candidates.add(bp.relative(d));
		BlockState s = from.getBlockState();
		if (s.getBlock() instanceof IRotate rotate)
			candidates = from.addPropagationLocations(rotate, s, candidates);

		for (BlockPos p : candidates) {
			BlockEntity be = level.getBlockEntity(p);
			if (!(be instanceof KineticBlockEntity kbe))
				continue;
			if (kbe == this)
				continue;
			if (!RotationPropagator.isConnected(from, kbe) && !RotationPropagator.isConnected(kbe, from))
				continue;
			result.add(kbe);
		}
		return result;
	}

	@Override
	protected void write(CompoundTag compound, boolean clientPacket) {
		super.write(compound, clientPacket);
		if (clientPacket)
			return;
		if (estimateValid) {
			compound.putBoolean("EstimateValid", true);
			compound.putFloat("DownstreamStress", downstreamStressBaseline);
			compound.putFloat("DownstreamImpact", downstreamImpactBaseline);
		}
		compound.putFloat("PreTripStress", preTripStress);
	}

	@Override
	protected void read(CompoundTag compound, boolean clientPacket) {
		super.read(compound, clientPacket);
		if (clientPacket)
			return;
		estimateValid = compound.getBoolean("EstimateValid");
		downstreamStressBaseline = compound.getFloat("DownstreamStress");
		downstreamImpactBaseline = compound.getFloat("DownstreamImpact");
		preTripStress = compound.getFloat("PreTripStress");
	}
}
