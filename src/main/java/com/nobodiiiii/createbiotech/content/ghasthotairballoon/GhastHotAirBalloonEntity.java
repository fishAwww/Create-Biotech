package com.nobodiiiii.createbiotech.content.ghasthotairballoon;

import com.nobodiiiii.createbiotech.registry.CBEntityTypes;
import com.simibubi.create.content.contraptions.OrientedContraptionEntity;

import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

public class GhastHotAirBalloonEntity extends OrientedContraptionEntity {

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

	public static EntityType.Builder<?> build(EntityType.Builder<?> builder) {
		@SuppressWarnings("unchecked")
		EntityType.Builder<GhastHotAirBalloonEntity> entityBuilder = (EntityType.Builder<GhastHotAirBalloonEntity>) builder;
		return entityBuilder.sized(5f, 1f).fireImmune();
	}
}
