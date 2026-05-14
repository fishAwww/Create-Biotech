package com.nobodiiiii.createbiotech.content.aircushion;

import com.simibubi.create.api.behaviour.movement.MovementBehaviour;
import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.ControlledContraptionEntity;
import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import com.simibubi.create.content.contraptions.gantry.GantryContraptionEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

public class AirCushionMovementBehaviour implements MovementBehaviour {

	@Override
	public void tick(MovementContext context) {
		if (context.world == null || context.world.isClientSide)
			return;
		if (context.position == null)
			return;

		AbstractContraptionEntity entity = context.contraption == null ? null : context.contraption.entity;
		if (entity instanceof ControlledContraptionEntity)
			return;
		if (entity instanceof GantryContraptionEntity)
			return;

		BlockPos blockPos = BlockPos.containing(context.position);
		BlockState worldState = context.world.getBlockState(blockPos);
		if (worldState.canBeReplaced())
			return;
		if (worldState.getCollisionShape(context.world, blockPos)
			.isEmpty())
			return;

		context.stall = true;
	}
}
