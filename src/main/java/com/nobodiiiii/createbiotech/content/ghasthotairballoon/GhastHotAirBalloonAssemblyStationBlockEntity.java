package com.nobodiiiii.createbiotech.content.ghasthotairballoon;

import com.nobodiiiii.createbiotech.registry.CBBlockEntityTypes;
import com.simibubi.create.api.contraption.BlockMovementChecks;
import com.simibubi.create.content.contraptions.AssemblyException;
import com.simibubi.create.infrastructure.config.AllConfigs;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Ghast;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class GhastHotAirBalloonAssemblyStationBlockEntity extends BlockEntity {

	private boolean wasPowered;
	private boolean queuedAssemble;

	public GhastHotAirBalloonAssemblyStationBlockEntity(BlockPos pos, BlockState state) {
		super(CBBlockEntityTypes.GHAST_HOT_AIR_BALLOON_ASSEMBLY_STATION.get(), pos, state);
	}

	public static void tick(Level level, BlockPos pos, BlockState state,
		GhastHotAirBalloonAssemblyStationBlockEntity be) {
		if (level.isClientSide)
			return;
		if (!be.queuedAssemble)
			return;
		be.queuedAssemble = false;
		be.tryAssemble();
	}

	public void onNeighborSignalChanged(boolean powered) {
		if (level == null || level.isClientSide)
			return;
		if (powered && !wasPowered)
			queuedAssemble = true;
		wasPowered = powered;
		setChanged();
	}

	private void tryAssemble() {
		if (level == null || level.isClientSide)
			return;

		int maxLength = AllConfigs.server().kinetics.maxRopeLength.get();
		BlockPos anchorPos = null;
		int ropeLength = 0;
		for (int distance = 1; distance <= maxLength; distance++) {
			BlockPos checkPos = worldPosition.below(distance);
			if (level.isOutsideBuildHeight(checkPos))
				return;
			BlockState checkState = level.getBlockState(checkPos);
			if (checkState.isAir())
				continue;
			if (!BlockMovementChecks.isMovementNecessary(checkState, level, checkPos))
				continue;
			if (BlockMovementChecks.isBrittle(checkState))
				return;
			anchorPos = checkPos;
			ropeLength = distance;
			break;
		}
		if (anchorPos == null)
			return;

		GhastHotAirBalloonContraption contraption = new GhastHotAirBalloonContraption(ropeLength);
		try {
			if (!contraption.assemble(level, anchorPos))
				return;
		} catch (AssemblyException e) {
			return;
		}
		if (contraption.getBlocks().isEmpty())
			return;

		contraption.removeBlocksFromWorld(level, BlockPos.ZERO);

		Ghast ghast = EntityType.GHAST.create(level);
		if (ghast == null)
			return;
		double gx = worldPosition.getX() + 0.5;
		double gy = worldPosition.getY();
		double gz = worldPosition.getZ() + 0.5;
		ghast.moveTo(gx, gy, gz, 0f, 0f);
		ghast.setPersistenceRequired();
		if (!level.addFreshEntity(ghast))
			return;

		GhastHotAirBalloonEntity contraptionEntity =
			GhastHotAirBalloonEntity.create(level, contraption, Direction.fromYRot(ghast.getYRot()));
		contraptionEntity.setPos(anchorPos.getX() + 0.5, anchorPos.getY(), anchorPos.getZ() + 0.5);
		level.addFreshEntity(contraptionEntity);
		contraptionEntity.startRiding(ghast, true);
	}

	@Override
	protected void saveAdditional(CompoundTag tag) {
		super.saveAdditional(tag);
		tag.putBoolean("WasPowered", wasPowered);
	}

	@Override
	public void load(CompoundTag tag) {
		super.load(tag);
		wasPowered = tag.getBoolean("WasPowered");
	}
}
