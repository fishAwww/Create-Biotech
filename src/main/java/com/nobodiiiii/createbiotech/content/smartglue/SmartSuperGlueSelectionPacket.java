package com.nobodiiiii.createbiotech.content.smartglue;

import java.util.Set;

import com.simibubi.create.content.contraptions.glue.SuperGlueEntity;
import com.simibubi.create.content.contraptions.glue.SuperGlueSelectionHelper;
import com.simibubi.create.foundation.advancement.AllAdvancements;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.ForgeMod;
import net.minecraftforge.network.NetworkEvent.Context;

public class SmartSuperGlueSelectionPacket {

	private final BlockPos from;
	private final BlockPos to;

	public SmartSuperGlueSelectionPacket(BlockPos from, BlockPos to) {
		this.from = from;
		this.to = to;
	}

	public SmartSuperGlueSelectionPacket(FriendlyByteBuf buffer) {
		this(buffer.readBlockPos(), buffer.readBlockPos());
	}

	public void write(FriendlyByteBuf buffer) {
		buffer.writeBlockPos(from);
		buffer.writeBlockPos(to);
	}

	public boolean handle(Context context) {
		context.enqueueWork(() -> apply(context.getSender()));
		return true;
	}

	private void apply(ServerPlayer player) {
		if (player == null)
			return;

		double range = player.getAttribute(ForgeMod.BLOCK_REACH.get()).getValue() + 2;
		if (player.distanceToSqr(Vec3.atCenterOf(to)) > range * range || !to.closerThan(from, 25))
			return;

		Set<BlockPos> group = SuperGlueSelectionHelper.searchGlueGroup(player.level(), from, to, false);
		if (group == null || !group.contains(to))
			return;
		if (!SmartSuperGlueHelper.collectGlueFromInventory(player, 1, true))
			return;

		SmartSuperGlueHelper.collectGlueFromInventory(player, 1, false);
		SuperGlueEntity entity = new SuperGlueEntity(player.level(), SuperGlueEntity.span(from, to));
		player.level().addFreshEntity(entity);
		entity.spawnParticles();
		AllAdvancements.SUPER_GLUE.awardTo(player);
	}
}
