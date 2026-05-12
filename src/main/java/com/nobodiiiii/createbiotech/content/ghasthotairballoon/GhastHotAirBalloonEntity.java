package com.nobodiiiii.createbiotech.content.ghasthotairballoon;

import java.util.Collection;

import com.nobodiiiii.createbiotech.registry.CBEntityTypes;
import com.simibubi.create.content.contraptions.OrientedContraptionEntity;

import net.createmod.catnip.math.VecHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.monster.Ghast;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class GhastHotAirBalloonEntity extends OrientedContraptionEntity {

	private static final double CONTROL_SPEED = 0.35d;
	private static final double VERTICAL_SPEED = 0.25d;
	private static final float TURN_SPEED = 6.0f;

	public GhastHotAirBalloonEntity(EntityType<?> type, Level level) {
		super(type, level);
	}

	public static GhastHotAirBalloonEntity create(Level level, GhastHotAirBalloonContraption contraption,
												  Direction initialOrientation) {
		GhastHotAirBalloonEntity entity =
			new GhastHotAirBalloonEntity(CBEntityTypes.GHAST_HOT_AIR_BALLOON.get(), level);
		entity.setContraption(contraption);
		entity.setInitialOrientation(initialOrientation);
		entity.startAtInitialYaw();
		return entity;
	}

	@Override
	public double getMyRidingOffset() {
		Entity vehicle = getVehicle();
		if (vehicle == null)
			return 0;
		return -getCordOffset() - vehicle.getPassengersRidingOffset();
	}

	private double getCordOffset() {
		if (getContraption() instanceof GhastHotAirBalloonContraption gc)
			return gc.getInitialOffset();
		return 0;
	}

	@Override
	public boolean startControlling(BlockPos controlsLocalPos, Player player) {
		if (player == null || player.isSpectator())
			return false;
		if (!(getVehicle() instanceof Ghast ghast) || !ghast.isAlive())
			return false;
		if (!toGlobalVector(VecHelper.getCenterOf(controlsLocalPos), 1).closerThan(player.position(), 8))
			return false;
		return true;
	}

	@Override
	public boolean control(BlockPos controlsLocalPos, Collection<Integer> heldControls, Player player) {
		if (level().isClientSide)
			return true;
		if (player == null || player.isSpectator())
			return false;
		if (!(getVehicle() instanceof Ghast ghast) || !ghast.isAlive())
			return false;
		if (!toGlobalVector(VecHelper.getCenterOf(controlsLocalPos), 1).closerThan(player.position(), 16))
			return false;

		ghast.setNoAi(true);

		int forward = heldControls.contains(0) ? 1 : 0;
		int backward = heldControls.contains(1) ? 1 : 0;
		int left = heldControls.contains(2) ? 1 : 0;
		int right = heldControls.contains(3) ? 1 : 0;
		int up = heldControls.contains(4) ? 1 : 0;
		int down = heldControls.contains(5) ? 1 : 0;

		float yaw = ghast.getYRot() + (right - left) * TURN_SPEED;
		ghast.setYRot(yaw);
		ghast.setYBodyRot(yaw);
		ghast.setYHeadRot(yaw);
		ghast.yRotO = yaw;
		ghast.yBodyRotO = yaw;
		ghast.yHeadRotO = yaw;

		double forwardAmount = forward - backward;
		Vec3 forwardVec = Vec3.directionFromRotation(0, yaw).scale(forwardAmount * CONTROL_SPEED);
		double verticalAmount = (up - down) * VERTICAL_SPEED;
		Vec3 movement = new Vec3(forwardVec.x, verticalAmount, forwardVec.z);

		ghast.move(MoverType.SELF, movement);
		ghast.setDeltaMovement(movement);
		ghast.hasImpulse = true;
		ghast.hurtMarked = true;
		return true;
	}

	@Override
	public void stopControlling(BlockPos controlsLocalPos) {
		super.stopControlling(controlsLocalPos);
		if (getVehicle() instanceof Ghast ghast && ghast.isAlive()) {
			ghast.setDeltaMovement(Vec3.ZERO);
			ghast.hasImpulse = true;
		}
	}

	public static EntityType.Builder<?> build(EntityType.Builder<?> builder) {
		@SuppressWarnings("unchecked")
		EntityType.Builder<GhastHotAirBalloonEntity> entityBuilder = (EntityType.Builder<GhastHotAirBalloonEntity>) builder;
		return entityBuilder.sized(5f, 1f).fireImmune();
	}
}
