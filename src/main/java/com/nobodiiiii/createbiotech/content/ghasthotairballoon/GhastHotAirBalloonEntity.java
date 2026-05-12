package com.nobodiiiii.createbiotech.content.ghasthotairballoon;

import java.util.Collection;

import com.nobodiiiii.createbiotech.content.cardboardbox.CapturedEntityBoxHelper;
import com.nobodiiiii.createbiotech.registry.CBEntityTypes;
import com.simibubi.create.content.contraptions.OrientedContraptionEntity;

import net.createmod.catnip.math.VecHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.monster.Ghast;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class GhastHotAirBalloonEntity extends OrientedContraptionEntity {

	private static final double FORWARD_ACCELERATION = 0.04d;
	private static final double BACKWARD_ACCELERATION = 0.02d;
	private static final double VERTICAL_ACCELERATION = 0.03d;
	private static final double HORIZONTAL_DRAG = 0.9d;
	private static final double VERTICAL_DRAG = 0.85d;
	private static final double MAX_HORIZONTAL_SPEED = 0.35d;
	private static final double MAX_VERTICAL_SPEED = 0.25d;
	private static final float TURN_ACCELERATION = 1.0f;
	private static final float TURN_DRAG = 0.85f;
	private static final float MAX_TURN_SPEED = 6.0f;
	private static final double MOTION_EPSILON = 1.0E-4d;
	private static final int INPUT_TIMEOUT_TICKS = 8;

	private float deltaRotation;
	private int inputTimeout;
	private boolean inputForward;
	private boolean inputBackward;
	private boolean inputLeft;
	private boolean inputRight;
	private boolean inputUp;
	private boolean inputDown;

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
	public void tick() {
		super.tick();

		if (level().isClientSide)
			return;
		if (!(getVehicle() instanceof Ghast ghast) || !ghast.isAlive())
			return;

		ghast.setNoAi(true);
		CapturedEntityBoxHelper.markAiDisabledByMod(ghast);
		tickInputTimeout();
		applyControlledMovement(ghast);
	}

	@Override
	public boolean startControlling(BlockPos controlsLocalPos, Player player) {
		if (player == null || player.isSpectator())
			return false;
		if (!(getVehicle() instanceof Ghast ghast) || !ghast.isAlive())
			return false;
		if (!toGlobalVector(VecHelper.getCenterOf(controlsLocalPos), 1).closerThan(player.position(), 8))
			return false;
		clearInputs();
		inputTimeout = 0;
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
		CapturedEntityBoxHelper.markAiDisabledByMod(ghast);

		inputForward = heldControls.contains(0);
		inputBackward = heldControls.contains(1);
		inputLeft = heldControls.contains(2);
		inputRight = heldControls.contains(3);
		inputUp = heldControls.contains(4);
		inputDown = heldControls.contains(5);
		inputTimeout = INPUT_TIMEOUT_TICKS;
		return true;
	}

	@Override
	public void stopControlling(BlockPos controlsLocalPos) {
		super.stopControlling(controlsLocalPos);
		clearInputs();
		inputTimeout = 0;
	}

	private void tickInputTimeout() {
		if (inputTimeout > 0) {
			inputTimeout--;
			return;
		}
		clearInputs();
	}

	private void clearInputs() {
		inputForward = false;
		inputBackward = false;
		inputLeft = false;
		inputRight = false;
		inputUp = false;
		inputDown = false;
	}

	private void applyControlledMovement(Ghast ghast) {
		Vec3 movement = ghast.getDeltaMovement().multiply(HORIZONTAL_DRAG, VERTICAL_DRAG, HORIZONTAL_DRAG);

		if (inputLeft)
			deltaRotation -= TURN_ACCELERATION;
		if (inputRight)
			deltaRotation += TURN_ACCELERATION;

		deltaRotation = Mth.clamp(deltaRotation, -MAX_TURN_SPEED, MAX_TURN_SPEED);
		float yaw = ghast.getYRot() + deltaRotation;
		applyYaw(ghast, yaw);
		deltaRotation *= TURN_DRAG;

		double thrust = 0;
		if (inputForward)
			thrust += FORWARD_ACCELERATION;
		if (inputBackward)
			thrust -= BACKWARD_ACCELERATION;
		if (thrust != 0)
			movement = movement.add(Vec3.directionFromRotation(0, yaw).scale(thrust));

		double verticalThrust = 0;
		if (inputUp)
			verticalThrust += VERTICAL_ACCELERATION;
		if (inputDown)
			verticalThrust -= VERTICAL_ACCELERATION;
		if (verticalThrust != 0)
			movement = movement.add(0, verticalThrust, 0);

		movement = clampMovement(movement);
		ghast.setDeltaMovement(movement);
		ghast.move(MoverType.SELF, movement);
		ghast.hasImpulse = true;
		ghast.hurtMarked = true;
	}

	private static void applyYaw(Ghast ghast, float yaw) {
		ghast.setYRot(yaw);
		ghast.setYBodyRot(yaw);
		ghast.setYHeadRot(yaw);
		ghast.yRotO = yaw;
		ghast.yBodyRotO = yaw;
		ghast.yHeadRotO = yaw;
	}

	private static Vec3 clampMovement(Vec3 movement) {
		double horizontalSpeed = movement.horizontalDistance();
		if (horizontalSpeed > MAX_HORIZONTAL_SPEED) {
			double scale = MAX_HORIZONTAL_SPEED / horizontalSpeed;
			movement = new Vec3(movement.x * scale, movement.y, movement.z * scale);
		}

		double verticalSpeed = Mth.clamp(movement.y, -MAX_VERTICAL_SPEED, MAX_VERTICAL_SPEED);
		movement = new Vec3(movement.x, verticalSpeed, movement.z);

		double x = Math.abs(movement.x) < MOTION_EPSILON ? 0 : movement.x;
		double y = Math.abs(movement.y) < MOTION_EPSILON ? 0 : movement.y;
		double z = Math.abs(movement.z) < MOTION_EPSILON ? 0 : movement.z;
		return new Vec3(x, y, z);
	}

	public static EntityType.Builder<?> build(EntityType.Builder<?> builder) {
		@SuppressWarnings("unchecked")
		EntityType.Builder<GhastHotAirBalloonEntity> entityBuilder = (EntityType.Builder<GhastHotAirBalloonEntity>) builder;
		return entityBuilder.sized(5f, 1f).fireImmune();
	}
}
