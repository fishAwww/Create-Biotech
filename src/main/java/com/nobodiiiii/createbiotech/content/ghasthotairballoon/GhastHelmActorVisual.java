package com.nobodiiiii.createbiotech.content.ghasthotairballoon;

import org.joml.Matrix4f;

import com.simibubi.create.content.contraptions.actors.trainControls.ControlsBlock;
import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import com.simibubi.create.content.contraptions.render.ActorVisual;
import com.simibubi.create.foundation.virtualWorld.VirtualRenderWorld;

import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.instance.InstanceTypes;
import dev.engine_room.flywheel.lib.instance.TransformedInstance;
import dev.engine_room.flywheel.lib.model.Models;
import net.createmod.catnip.animation.AnimationTickHolder;
import net.createmod.catnip.math.AngleHelper;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;

public class GhastHelmActorVisual extends ActorVisual {

	private final TransformedInstance cover;
	private final TransformedInstance firstLever;
	private final TransformedInstance middleLever;
	private final TransformedInstance thirdLever;
	private final Direction facing;

	public GhastHelmActorVisual(VisualizationContext visualizationContext, VirtualRenderWorld simulationWorld,
		MovementContext movementContext) {
		super(visualizationContext, simulationWorld, movementContext);

		BlockState state = movementContext.state;
		facing = state.getValue(ControlsBlock.FACING);

		cover =
			instancerProvider.instancer(InstanceTypes.TRANSFORMED, Models.partial(GhastHelmMovementBehaviour.GHAST_HELM_COVER))
				.createInstance();
		firstLever =
			instancerProvider.instancer(InstanceTypes.TRANSFORMED, Models.partial(GhastHelmMovementBehaviour.GHAST_HELM_LEVER))
				.createInstance();
		middleLever =
			instancerProvider.instancer(InstanceTypes.TRANSFORMED, Models.partial(GhastHelmMovementBehaviour.GHAST_HELM_LEVER))
				.createInstance();
		thirdLever =
			instancerProvider.instancer(InstanceTypes.TRANSFORMED, Models.partial(GhastHelmMovementBehaviour.GHAST_HELM_LEVER))
				.createInstance();

		int blockLight = localBlockLight();
		cover.light(blockLight, 0).setChanged();
		firstLever.light(blockLight, 0).setChanged();
		middleLever.light(blockLight, 0).setChanged();
		thirdLever.light(blockLight, 0).setChanged();
	}

	@Override
	public void beginFrame() {
		if (context.contraption.entity instanceof GhastHotAirBalloonEntity balloon) {
			GhastHelmMovementBehaviour.LeverAngles angles = GhastHelmMovementBehaviour.getAngles(context);
			GhastHelmMovementBehaviour.updateTargetAngles(context, balloon, angles);

			float pt = AnimationTickHolder.getPartialTicks();
			float equipAnimation = angles.equipAnimation.getValue(pt);
			float first = angles.speed.getValue(pt);
			float middle = angles.altitude.getValue(pt);
			float third = angles.steering.getValue(pt);
			updateInstances(equipAnimation, first, middle, third);
		}
	}

	private void updateInstances(float equipAnimation, float first, float middle, float third) {
		float hAngle = 180 + AngleHelper.horizontalAngle(facing);

		cover.setIdentityTransform()
			.translate(context.localPos)
			.center()
			.rotateYDegrees(hAngle)
			.uncenter()
			.setChanged();

		double yOffset = Mth.lerp(equipAnimation * equipAnimation, -0.15f, 0.05f);
		updateLever(firstLever, hAngle, Mth.clamp(first * 15, -45, 45), yOffset, 0);
		updateLever(middleLever, hAngle, Mth.clamp(middle * 15, -45, 45), yOffset, 3 / 16f);
		updateLever(thirdLever, hAngle, Mth.clamp(third * 15, -45, 45), yOffset, 6 / 16f);
	}

	private void updateLever(TransformedInstance lever, float hAngle, float vAngle, double yOffset, float xOffset) {
		Matrix4f transform = new Matrix4f()
			.translate(context.localPos.getX(), context.localPos.getY(), context.localPos.getZ())
			.translate(.5f, .5f, .5f)
			.rotateY((float) Math.toRadians(hAngle))
			.translate(-.5f, -.5f, -.5f)
			.translate(0, 4 / 16f, 4 / 16f)
			.rotateX((float) Math.toRadians(vAngle - 45))
			.translate(0, (float) yOffset, 0)
			.rotateX((float) Math.toRadians(45))
			.translate(0, -6 / 16f, -3 / 16f)
			.translate(xOffset, 0, 0);

		lever.setTransform(transform).setChanged();
	}

	@Override
	protected void _delete() {
		cover.delete();
		firstLever.delete();
		middleLever.delete();
		thirdLever.delete();
	}
}
