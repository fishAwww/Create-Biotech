package com.nobodiiiii.createbiotech.foundation.advancement;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import com.nobodiiiii.createbiotech.CreateBiotech;

import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.AdvancementEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = CreateBiotech.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class CBAdvancements {
	public static final ResourceLocation ROOT = CreateBiotech.asResource("root");
	public static final ResourceLocation BELT_COMPAT = CreateBiotech.asResource("belt_compat");
	public static final ResourceLocation SLIME_BELT = CreateBiotech.asResource("slime_belt");
	public static final ResourceLocation MAGMA_BELT = CreateBiotech.asResource("magma_belt");
	public static final ResourceLocation POWER_BELT = CreateBiotech.asResource("power_belt");
	public static final ResourceLocation EXPERIENCE_PUMP = CreateBiotech.asResource("experience_pump");
	public static final ResourceLocation EXPERIENCE_TANK = CreateBiotech.asResource("experience_tank");
	public static final ResourceLocation BUDDING_EXPERIENCE = CreateBiotech.asResource("budding_experience");
	public static final ResourceLocation SQUID_PRINTER = CreateBiotech.asResource("squid_printer");
	public static final ResourceLocation EVOKER_ENCHANTING_CHAMBER =
		CreateBiotech.asResource("evoker_enchanting_chamber");
	public static final ResourceLocation GHAST_HOT_AIR_BALLOON = CreateBiotech.asResource("ghast_hot_air_balloon");

	private static final ResourceLocation CREATE_BELT = new ResourceLocation("create", "belt");

	private CBAdvancements() {}

	public static boolean award(ServerPlayer player, ResourceLocation advancementId) {
		Advancement advancement = getAdvancement(player, advancementId);
		if (advancement == null)
			return false;

		AdvancementProgress progress = player.getAdvancements()
			.getOrStartProgress(advancement);
		List<String> remainingCriteria = new ArrayList<>();
		for (String criterion : progress.getRemainingCriteria())
			remainingCriteria.add(criterion);
		for (String criterion : remainingCriteria)
			player.getAdvancements()
				.award(advancement, criterion);
		return !remainingCriteria.isEmpty();
	}

	public static boolean has(ServerPlayer player, ResourceLocation advancementId) {
		Advancement advancement = getAdvancement(player, advancementId);
		return advancement != null && player.getAdvancements()
			.getOrStartProgress(advancement)
			.isDone();
	}

	@Nullable
	private static Advancement getAdvancement(ServerPlayer player, ResourceLocation advancementId) {
		return player.server.getAdvancements()
			.getAdvancement(advancementId);
	}

	@SubscribeEvent
	public static void onAdvancementEarned(AdvancementEvent.AdvancementEarnEvent event) {
		if (!(event.getEntity() instanceof ServerPlayer player))
			return;
		if (!CREATE_BELT.equals(event.getAdvancement().getId()))
			return;
		award(player, BELT_COMPAT);
	}

	@SubscribeEvent
	public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
		if (!(event.getEntity() instanceof ServerPlayer player))
			return;
		if (!has(player, CREATE_BELT))
			return;
		award(player, BELT_COMPAT);
	}
}
