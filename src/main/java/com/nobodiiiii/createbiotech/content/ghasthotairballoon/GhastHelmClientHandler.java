package com.nobodiiiii.createbiotech.content.ghasthotairballoon;

import com.nobodiiiii.createbiotech.CreateBiotech;
import com.simibubi.create.content.contraptions.actors.trainControls.ControlsHandler;

import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = CreateBiotech.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class GhastHelmClientHandler {

	private static final int DOUBLE_SHIFT_TICKS = 8;

	private static int lastShiftPressTick = Integer.MIN_VALUE;
	private static boolean previousShiftDown;
	private static boolean controllingGhastBalloon;
	private static int clientTicks;

	private GhastHelmClientHandler() {}

	public static void startControlling(GhastHotAirBalloonEntity entity) {
		controllingGhastBalloon = entity != null;
		lastShiftPressTick = Integer.MIN_VALUE;
		previousShiftDown = false;
	}

	@SubscribeEvent
	public static void onClientTick(TickEvent.ClientTickEvent event) {
		if (event.phase != TickEvent.Phase.END)
			return;

		clientTicks++;
		if (!controllingGhastBalloon)
			return;

		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.player == null || minecraft.level == null) {
			reset();
			return;
		}

		if (ControlsHandler.getContraption() == null) {
			reset();
			return;
		}

		boolean shiftDown = minecraft.options.keyShift.isDown();
		if (shiftDown && !previousShiftDown) {
			if (clientTicks - lastShiftPressTick <= DOUBLE_SHIFT_TICKS) {
				ControlsHandler.stopControlling();
				reset();
				return;
			}
			lastShiftPressTick = clientTicks;
		}

		previousShiftDown = shiftDown;
	}

	private static void reset() {
		controllingGhastBalloon = false;
		lastShiftPressTick = Integer.MIN_VALUE;
		previousShiftDown = false;
	}
}
