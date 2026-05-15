package com.nobodiiiii.createbiotech.content.bufferpad;

import com.simibubi.create.api.behaviour.movement.MovementBehaviour;
import com.simibubi.create.content.contraptions.behaviour.MovementContext;

public class BufferPadMovementBehaviour implements MovementBehaviour {

	@Override
	public void tick(MovementContext context) {
		context.stall = false;
		BufferPadCollisionHelper.tickMovingPad(context);
	}
}
