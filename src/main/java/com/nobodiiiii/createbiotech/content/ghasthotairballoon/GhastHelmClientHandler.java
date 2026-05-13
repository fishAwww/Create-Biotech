package com.nobodiiiii.createbiotech.content.ghasthotairballoon;

import com.nobodiiiii.createbiotech.CreateBiotech;
import com.simibubi.create.AllPackets;
import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.actors.trainControls.ControlsHandler;
import com.simibubi.create.content.contraptions.actors.trainControls.ControlsInputPacket;

import net.minecraft.core.BlockPos;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = CreateBiotech.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class GhastHelmClientHandler {

	private static boolean previousSprintDown;
	private static boolean controllingGhastBalloon;

	private GhastHelmClientHandler() {}

	public static void startControlling(GhastHotAirBalloonEntity entity) {
		controllingGhastBalloon = entity != null;
		previousSprintDown = false;
	}

	@SubscribeEvent
	public static void onClientTick(TickEvent.ClientTickEvent event) {
		if (event.phase != TickEvent.Phase.END)
			return;

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

		boolean sprintDown = minecraft.options.keySprint.isDown();
		if (sprintDown && !previousSprintDown) {
			AbstractContraptionEntity contraption = ControlsHandler.getContraption();
			BlockPos controlsPos = ControlsHandler.getControlsPos();
			if (contraption != null && controlsPos != null)
				AllPackets.getChannel().sendToServer(
					new ControlsInputPacket(ControlsHandler.currentlyPressed, false, contraption.getId(), controlsPos, true));
			ControlsHandler.stopControlling();
			reset();
			return;
		}

		previousSprintDown = sprintDown;
	}

	private static void reset() {
		controllingGhastBalloon = false;
		previousSprintDown = false;
	}
}
