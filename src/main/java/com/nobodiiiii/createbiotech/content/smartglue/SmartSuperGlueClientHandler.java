package com.nobodiiiii.createbiotech.content.smartglue;

import com.nobodiiiii.createbiotech.CreateBiotech;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = CreateBiotech.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class SmartSuperGlueClientHandler {

	private static final SmartSuperGlueSelectionHandler HANDLER = new SmartSuperGlueSelectionHandler();

	private SmartSuperGlueClientHandler() {}

	@SubscribeEvent
	public static void onClientTick(TickEvent.ClientTickEvent event) {
		if (event.phase != TickEvent.Phase.END)
			return;

		HANDLER.tick();
	}

	@SubscribeEvent
	public static void onLevelUnload(LevelEvent.Unload event) {
		if (event.getLevel().isClientSide())
			HANDLER.clear();
	}

	@SubscribeEvent
	public static void onClickInput(InputEvent.InteractionKeyMappingTriggered event) {
		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.screen != null)
			return;

		KeyMapping keyMapping = event.getKeyMapping();
		if (keyMapping != minecraft.options.keyUse && keyMapping != minecraft.options.keyAttack)
			return;

		boolean attack = keyMapping == minecraft.options.keyAttack;
		if (HANDLER.onMouseInput(attack))
			event.setCanceled(true);
	}
}
