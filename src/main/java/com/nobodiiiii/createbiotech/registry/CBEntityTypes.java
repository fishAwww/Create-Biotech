package com.nobodiiiii.createbiotech.registry;

import com.nobodiiiii.createbiotech.CreateBiotech;
import com.nobodiiiii.createbiotech.content.ghasthotairballoon.GhastHotAirBalloonEntity;
import com.nobodiiiii.createbiotech.content.ghasthotairballoon.GhastHotAirBalloonSeatEntity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class CBEntityTypes {

	public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
		DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, CreateBiotech.MOD_ID);

	public static final RegistryObject<EntityType<GhastHotAirBalloonEntity>> GHAST_HOT_AIR_BALLOON =
		ENTITY_TYPES.register("ghast_hot_air_balloon", () -> {
			EntityType.Builder<GhastHotAirBalloonEntity> builder = EntityType.Builder
				.<GhastHotAirBalloonEntity>of(GhastHotAirBalloonEntity::new, MobCategory.MISC)
				.setTrackingRange(20)
				.setUpdateInterval(1)
				.setShouldReceiveVelocityUpdates(true)
				.fireImmune();
			GhastHotAirBalloonEntity.build(builder);
			return builder.build("ghast_hot_air_balloon");
		});

	public static final RegistryObject<EntityType<GhastHotAirBalloonSeatEntity>> GHAST_HOT_AIR_BALLOON_SEAT =
		ENTITY_TYPES.register("ghast_hot_air_balloon_seat", () -> {
			EntityType.Builder<GhastHotAirBalloonSeatEntity> builder = EntityType.Builder
				.<GhastHotAirBalloonSeatEntity>of(GhastHotAirBalloonSeatEntity::new, MobCategory.MISC)
				.setTrackingRange(5)
				.setUpdateInterval(Integer.MAX_VALUE)
				.setShouldReceiveVelocityUpdates(false);
			GhastHotAirBalloonSeatEntity.build(builder);
			return builder.build("ghast_hot_air_balloon_seat");
		});

	private CBEntityTypes() {}

	public static void register(IEventBus modEventBus) {
		ENTITY_TYPES.register(modEventBus);
	}
}
