package com.nobodiiiii.createbiotech.foundation.advancement;

import java.util.UUID;

import javax.annotation.Nullable;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

public final class PlacedByPlayerAdvancementTracker {
	private static final String ADVANCEMENT_OWNER_TAG = "AdvancementOwner";

	private PlacedByPlayerAdvancementTracker() {}

	@Nullable
	public static UUID ownerFrom(@Nullable LivingEntity placer) {
		return placer == null ? null : placer.getUUID();
	}

	public static void writeOwner(CompoundTag tag, @Nullable UUID owner) {
		if (owner != null)
			tag.putUUID(ADVANCEMENT_OWNER_TAG, owner);
	}

	@Nullable
	public static UUID readOwner(CompoundTag tag) {
		return tag.hasUUID(ADVANCEMENT_OWNER_TAG) ? tag.getUUID(ADVANCEMENT_OWNER_TAG) : null;
	}

	public static boolean awardPlacedBy(@Nullable Level level, @Nullable UUID owner, ResourceLocation advancementId) {
		if (!(level instanceof ServerLevel serverLevel) || owner == null)
			return false;
		ServerPlayer player = serverLevel.getServer()
			.getPlayerList()
			.getPlayer(owner);
		return player != null && CBAdvancements.award(player, advancementId);
	}
}
