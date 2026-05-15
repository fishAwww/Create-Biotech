package com.nobodiiiii.createbiotech.content.bufferpad;

import com.simibubi.create.api.behaviour.movement.MovementBehaviour;
import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.ControlledContraptionEntity;
import com.simibubi.create.content.contraptions.OrientedContraptionEntity;
import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import com.simibubi.create.content.contraptions.gantry.GantryContraptionEntity;
import com.simibubi.create.content.trains.entity.CarriageContraptionEntity;
import com.simibubi.create.content.trains.entity.Train;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.vehicle.MinecartFurnace;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class BufferPadMovementBehaviour implements MovementBehaviour {

	// The pad only protrudes half a block, so its visible impact face sits on the block center plane.
	private static final double IMPACT_SAMPLE_DISTANCE = 1.0E-4d;
	private static final double MOVEMENT_EPSILON = 1.0E-4d;
	private static final double ESCAPE_PUSH_SPEED = 0.05d;
	private static final String ESCAPE_PUSH_NORMAL_X_TAG = "EscapePushNormalX";
	private static final String ESCAPE_PUSH_NORMAL_Y_TAG = "EscapePushNormalY";
	private static final String ESCAPE_PUSH_NORMAL_Z_TAG = "EscapePushNormalZ";
	private static final String TRAIN_POSITIVE_AXIS_X_TAG = "TrainPositiveAxisX";
	private static final String TRAIN_POSITIVE_AXIS_Y_TAG = "TrainPositiveAxisY";
	private static final String TRAIN_POSITIVE_AXIS_Z_TAG = "TrainPositiveAxisZ";

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
		CarriageContraptionEntity carriageEntity =
			entity instanceof CarriageContraptionEntity carriage && carriage.getCarriage() != null ? carriage : null;

		Vec3 previousEscapePushNormal = getStoredEscapePushNormal(context);
		Vec3 collisionNormal = getCollisionNormal(context);
		boolean collision = collisionNormal != null && collidesWithWorld(context.position, context, collisionNormal);
		context.stall = false;

		if (!collision) {
			if (previousEscapePushNormal != null)
				clearEscapeInfluence(context, entity, previousEscapePushNormal);
			clearStoredEscapePushNormal(context);
			return;
		}

		boolean escapeFromBlock = carriageEntity == null && shouldApplyEscapePush(context, collisionNormal);
		applyCollisionResponse(context, entity, collisionNormal, escapeFromBlock);
		if (carriageEntity != null) {
			clearStoredEscapePushNormal(context);
			return;
		}
		if (escapeFromBlock) {
			storeEscapePushNormal(context, collisionNormal);
			return;
		}

		clearStoredEscapePushNormal(context);
	}

	private static boolean collidesWithWorld(Vec3 position, MovementContext context, Vec3 collisionNormal) {
		BlockPos blockPos = BlockPos.containing(position.add(collisionNormal.scale(IMPACT_SAMPLE_DISTANCE)));
		BlockState worldState = context.world.getBlockState(blockPos);
		if (worldState.canBeReplaced())
			return false;
		return !worldState.getCollisionShape(context.world, blockPos)
			.isEmpty();
	}

	private static Vec3 getCollisionNormal(MovementContext context) {
		if (!context.state.hasProperty(BufferPadBlock.FACING))
			return null;

		Direction collisionFace = context.state.getValue(BufferPadBlock.FACING)
			.getOpposite();
		Vec3 collisionNormal = context.rotation.apply(Vec3.atLowerCornerOf(collisionFace.getNormal()))
			.normalize();
		if (collisionNormal.lengthSqr() < MOVEMENT_EPSILON)
			return null;
		return collisionNormal;
	}

	private static void applyCollisionResponse(MovementContext context, AbstractContraptionEntity entity,
		Vec3 collisionNormal, boolean escapeFromBlock) {
		clipEntityPosition(entity, collisionNormal);
		clipEntityMotion(entity, collisionNormal, escapeFromBlock);

		if (entity instanceof CarriageContraptionEntity carriageEntity && carriageEntity.getCarriage() != null)
			clipTrainSpeed(context, context.motion, carriageEntity.getCarriage().train, collisionNormal,
				escapeFromBlock);

		if (entity instanceof OrientedContraptionEntity orientedEntity)
			clipCoupledCarts(orientedEntity, collisionNormal, escapeFromBlock);

		for (Entity current = entity.getVehicle(); current != null; current = current.getVehicle()) {
			clipEntityPosition(current, collisionNormal);
			clipEntityMotion(current, collisionNormal, escapeFromBlock);
		}
	}

	private static boolean shouldApplyEscapePush(MovementContext context, Vec3 collisionNormal) {
		double inwardMotion = context.motion.dot(collisionNormal);
		if (inwardMotion <= MOVEMENT_EPSILON)
			return true;

		Vec3 previousPosition = context.position.subtract(context.motion);
		return collidesWithWorld(previousPosition, context, collisionNormal);
	}

	private static void clipCoupledCarts(OrientedContraptionEntity entity, Vec3 collisionNormal,
		boolean escapeFromBlock) {
		var coupledCarts = entity.getCoupledCartsIfPresent();
		if (coupledCarts == null)
			return;

		clipEntityPosition(coupledCarts.getFirst()
			.cart(), collisionNormal);
		clipEntityMotion(coupledCarts.getFirst()
			.cart(), collisionNormal, escapeFromBlock);
		clipEntityPosition(coupledCarts.getSecond()
			.cart(), collisionNormal);
		clipEntityMotion(coupledCarts.getSecond()
			.cart(), collisionNormal, escapeFromBlock);
	}

	private static void trimCoupledCarts(OrientedContraptionEntity entity, Vec3 collisionNormal) {
		var coupledCarts = entity.getCoupledCartsIfPresent();
		if (coupledCarts == null)
			return;

		trimEntityMotion(coupledCarts.getFirst()
			.cart(), collisionNormal);
		trimEntityMotion(coupledCarts.getSecond()
			.cart(), collisionNormal);
	}

	private static void clipTrainSpeed(MovementContext context, Vec3 actorMotion, Train train, Vec3 collisionNormal,
		boolean escapeFromBlock) {
		Vec3 adjustedMotion = adjustMotionAgainstCollision(actorMotion, collisionNormal, escapeFromBlock);
		if (adjustedMotion.equals(actorMotion))
			return;

		double referenceSpeed = getTrainReferenceSpeed(train);
		if (Math.abs(referenceSpeed) < MOVEMENT_EPSILON && actorMotion.lengthSqr() < MOVEMENT_EPSILON)
			return;

		double adjustedSpeed = getAdjustedTrainSpeed(actorMotion, adjustedMotion, referenceSpeed, escapeFromBlock);
		train.speed = adjustedSpeed;
		if (train.speedBeforeStall != null)
			train.speedBeforeStall = adjustedSpeed;
		clipTrainTargetSpeed(context, actorMotion, train, collisionNormal, referenceSpeed);
	}

	private static void clearEscapeInfluence(MovementContext context, AbstractContraptionEntity entity,
		Vec3 collisionNormal) {
		trimEntityMotion(entity, collisionNormal);

		if (entity instanceof CarriageContraptionEntity carriageEntity && carriageEntity.getCarriage() != null)
			trimTrainSpeed(carriageEntity.getCarriage().train);

		if (entity instanceof OrientedContraptionEntity orientedEntity)
			trimCoupledCarts(orientedEntity, collisionNormal);

		for (Entity current = entity.getVehicle(); current != null; current = current.getVehicle())
			trimEntityMotion(current, collisionNormal);
	}

	private static void clipEntityPosition(Entity entity, Vec3 collisionNormal) {
		if (entity == null)
			return;

		Vec3 displacement = entity.position()
			.subtract(entity.xo, entity.yo, entity.zo);
		double intoCollisionFace = displacement.dot(collisionNormal);
		if (intoCollisionFace <= MOVEMENT_EPSILON)
			return;

		Vec3 correctedPosition = entity.position()
			.subtract(collisionNormal.scale(intoCollisionFace));
		entity.setPos(correctedPosition.x, correctedPosition.y, correctedPosition.z);
		entity.hurtMarked = true;
	}

	private static void clipEntityMotion(Entity entity, Vec3 collisionNormal, boolean escapeFromBlock) {
		if (entity == null)
			return;

		Vec3 motion = entity.getDeltaMovement();
		Vec3 adjustedMotion = adjustMotionAgainstCollision(motion, collisionNormal, escapeFromBlock);
		if (adjustedMotion.equals(motion))
			return;

		if (entity instanceof AbstractContraptionEntity contraptionEntity)
			contraptionEntity.setContraptionMotion(adjustedMotion);
		else
			entity.setDeltaMovement(adjustedMotion);
		entity.hurtMarked = true;

		if (entity instanceof MinecartFurnace furnaceCart)
			clipFurnacePush(furnaceCart, collisionNormal, escapeFromBlock);
	}

	private static void trimEntityMotion(Entity entity, Vec3 collisionNormal) {
		if (entity == null)
			return;

		Vec3 motion = entity.getDeltaMovement();
		Vec3 trimmedMotion = removeEscapeVelocityContribution(motion, collisionNormal);
		if (trimmedMotion.equals(motion))
			return;

		if (entity instanceof AbstractContraptionEntity contraptionEntity)
			contraptionEntity.setContraptionMotion(trimmedMotion);
		else
			entity.setDeltaMovement(trimmedMotion);
		entity.hurtMarked = true;

		if (entity instanceof MinecartFurnace furnaceCart)
			trimFurnacePush(furnaceCart, collisionNormal);
	}

	private static void clipFurnacePush(MinecartFurnace furnaceCart, Vec3 collisionNormal, boolean escapeFromBlock) {
		Vec3 horizontalNormal = new Vec3(collisionNormal.x, 0, collisionNormal.z);
		if (horizontalNormal.lengthSqr() < MOVEMENT_EPSILON)
			return;

		horizontalNormal = horizontalNormal.normalize();

		CompoundTag nbt = furnaceCart.serializeNBT();
		Vec3 push = new Vec3(nbt.getDouble("PushX"), 0, nbt.getDouble("PushZ"));
		Vec3 adjustedPush = adjustMotionAgainstCollision(push, horizontalNormal, escapeFromBlock);
		if (adjustedPush.equals(push))
			return;

		nbt.putDouble("PushX", adjustedPush.x);
		nbt.putDouble("PushZ", adjustedPush.z);
		furnaceCart.deserializeNBT(nbt);
	}

	private static void trimFurnacePush(MinecartFurnace furnaceCart, Vec3 collisionNormal) {
		Vec3 horizontalNormal = new Vec3(collisionNormal.x, 0, collisionNormal.z);
		if (horizontalNormal.lengthSqr() < MOVEMENT_EPSILON)
			return;

		horizontalNormal = horizontalNormal.normalize();

		CompoundTag nbt = furnaceCart.serializeNBT();
		Vec3 push = new Vec3(nbt.getDouble("PushX"), 0, nbt.getDouble("PushZ"));
		Vec3 trimmedPush = removeEscapeVelocityContribution(push, horizontalNormal);
		if (trimmedPush.equals(push))
			return;

		nbt.putDouble("PushX", trimmedPush.x);
		nbt.putDouble("PushZ", trimmedPush.z);
		furnaceCart.deserializeNBT(nbt);
	}

	private static double getTrainReferenceSpeed(Train train) {
		double currentSpeed = train.speedBeforeStall != null ? train.speedBeforeStall : train.speed;
		if (Math.abs(currentSpeed) >= MOVEMENT_EPSILON)
			return currentSpeed;
		if (Math.abs(train.targetSpeed) >= MOVEMENT_EPSILON)
			return train.targetSpeed;
		return 0;
	}

	private static double getAdjustedTrainSpeed(Vec3 actorMotion, Vec3 adjustedMotion, double referenceSpeed,
		boolean escapeFromBlock) {
		if (actorMotion.lengthSqr() < MOVEMENT_EPSILON) {
			if (!escapeFromBlock || Math.abs(referenceSpeed) < MOVEMENT_EPSILON)
				return referenceSpeed;
			return -Math.copySign(ESCAPE_PUSH_SPEED, referenceSpeed);
		}

		Vec3 travelAxis = actorMotion.normalize();
		double alongTravel = adjustedMotion.dot(travelAxis);
		if (escapeFromBlock && alongTravel > -ESCAPE_PUSH_SPEED)
			alongTravel = -ESCAPE_PUSH_SPEED;

		double speedMagnitude = Math.abs(alongTravel);
		if (speedMagnitude < MOVEMENT_EPSILON)
			return 0;

		double speed = Math.copySign(speedMagnitude, referenceSpeed);
		if (alongTravel < 0)
			speed *= -1;
		return speed;
	}

	private static void trimTrainSpeed(Train train) {
		train.speed = trimEscapeSpeedMagnitude(train.speed);
		if (train.speedBeforeStall != null)
			train.speedBeforeStall = trimEscapeSpeedMagnitude(train.speedBeforeStall);
	}

	private static void clipTrainTargetSpeed(MovementContext context, Vec3 actorMotion, Train train,
		Vec3 collisionNormal, double referenceSpeed) {
		if (Math.abs(train.targetSpeed) < MOVEMENT_EPSILON)
			return;

		Vec3 positiveAxis = getTrainPositiveAxis(context, actorMotion, referenceSpeed);
		if (positiveAxis == null)
			return;

		Vec3 targetMotion = positiveAxis.scale(train.targetSpeed);
		Vec3 adjustedTargetMotion = removeVelocityIntoCollisionFace(targetMotion, collisionNormal);
		double adjustedTargetSpeed = adjustedTargetMotion.dot(positiveAxis);
		train.targetSpeed = Math.abs(adjustedTargetSpeed) < MOVEMENT_EPSILON ? 0 : adjustedTargetSpeed;
	}

	private static Vec3 adjustMotionAgainstCollision(Vec3 motion, Vec3 collisionNormal, boolean escapeFromBlock) {
		Vec3 adjustedMotion = removeVelocityIntoCollisionFace(motion, collisionNormal);
		if (!escapeFromBlock)
			return adjustedMotion;
		return ensureEscapeVelocity(adjustedMotion, collisionNormal);
	}

	private static Vec3 ensureEscapeVelocity(Vec3 motion, Vec3 collisionNormal) {
		double normalSpeed = motion.dot(collisionNormal);
		if (normalSpeed <= -ESCAPE_PUSH_SPEED)
			return motion;
		return motion.subtract(collisionNormal.scale(normalSpeed + ESCAPE_PUSH_SPEED));
	}

	private static Vec3 removeEscapeVelocityContribution(Vec3 motion, Vec3 collisionNormal) {
		double normalSpeed = motion.dot(collisionNormal);
		if (normalSpeed >= -MOVEMENT_EPSILON)
			return motion;
		double cancelledSpeed = Math.min(-normalSpeed, ESCAPE_PUSH_SPEED);
		return motion.add(collisionNormal.scale(cancelledSpeed));
	}

	private static Vec3 removeVelocityIntoCollisionFace(Vec3 motion, Vec3 collisionNormal) {
		double intoCollisionFace = motion.dot(collisionNormal);
		if (intoCollisionFace <= MOVEMENT_EPSILON)
			return motion;
		return motion.subtract(collisionNormal.scale(intoCollisionFace));
	}

	private static double trimEscapeSpeedMagnitude(double speed) {
		double magnitude = Math.abs(speed);
		if (magnitude < MOVEMENT_EPSILON)
			return 0;
		double trimmedMagnitude = Math.max(0, magnitude - ESCAPE_PUSH_SPEED);
		if (trimmedMagnitude < MOVEMENT_EPSILON)
			return 0;
		return Math.copySign(trimmedMagnitude, speed);
	}

	private static void storeEscapePushNormal(MovementContext context, Vec3 collisionNormal) {
		context.data.putDouble(ESCAPE_PUSH_NORMAL_X_TAG, collisionNormal.x);
		context.data.putDouble(ESCAPE_PUSH_NORMAL_Y_TAG, collisionNormal.y);
		context.data.putDouble(ESCAPE_PUSH_NORMAL_Z_TAG, collisionNormal.z);
	}

	private static void clearStoredEscapePushNormal(MovementContext context) {
		context.data.remove(ESCAPE_PUSH_NORMAL_X_TAG);
		context.data.remove(ESCAPE_PUSH_NORMAL_Y_TAG);
		context.data.remove(ESCAPE_PUSH_NORMAL_Z_TAG);
	}

	private static Vec3 getStoredEscapePushNormal(MovementContext context) {
		if (!context.data.contains(ESCAPE_PUSH_NORMAL_X_TAG)
			|| !context.data.contains(ESCAPE_PUSH_NORMAL_Y_TAG)
			|| !context.data.contains(ESCAPE_PUSH_NORMAL_Z_TAG))
			return null;

		Vec3 collisionNormal = new Vec3(context.data.getDouble(ESCAPE_PUSH_NORMAL_X_TAG),
			context.data.getDouble(ESCAPE_PUSH_NORMAL_Y_TAG), context.data.getDouble(ESCAPE_PUSH_NORMAL_Z_TAG));
		if (collisionNormal.lengthSqr() < MOVEMENT_EPSILON)
			return null;
		return collisionNormal.normalize();
	}

	private static Vec3 getTrainPositiveAxis(MovementContext context, Vec3 actorMotion, double referenceSpeed) {
		if (actorMotion.lengthSqr() >= MOVEMENT_EPSILON && Math.abs(referenceSpeed) >= MOVEMENT_EPSILON) {
			Vec3 positiveAxis = actorMotion.normalize();
			if (referenceSpeed < 0)
				positiveAxis = positiveAxis.scale(-1);
			storeTrainPositiveAxis(context, positiveAxis);
			return positiveAxis;
		}

		return getStoredTrainPositiveAxis(context);
	}

	private static void storeTrainPositiveAxis(MovementContext context, Vec3 positiveAxis) {
		context.data.putDouble(TRAIN_POSITIVE_AXIS_X_TAG, positiveAxis.x);
		context.data.putDouble(TRAIN_POSITIVE_AXIS_Y_TAG, positiveAxis.y);
		context.data.putDouble(TRAIN_POSITIVE_AXIS_Z_TAG, positiveAxis.z);
	}

	private static Vec3 getStoredTrainPositiveAxis(MovementContext context) {
		if (!context.data.contains(TRAIN_POSITIVE_AXIS_X_TAG)
			|| !context.data.contains(TRAIN_POSITIVE_AXIS_Y_TAG)
			|| !context.data.contains(TRAIN_POSITIVE_AXIS_Z_TAG))
			return null;

		Vec3 positiveAxis = new Vec3(context.data.getDouble(TRAIN_POSITIVE_AXIS_X_TAG),
			context.data.getDouble(TRAIN_POSITIVE_AXIS_Y_TAG), context.data.getDouble(TRAIN_POSITIVE_AXIS_Z_TAG));
		if (positiveAxis.lengthSqr() < MOVEMENT_EPSILON)
			return null;
		return positiveAxis.normalize();
	}
}
