package com.nobodiiiii.createbiotech.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.nobodiiiii.createbiotech.content.beltsurface.BeltFunnelStateExtensions;
import com.simibubi.create.content.logistics.funnel.BeltFunnelBlock;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;

/**
 * Adds the {@link BeltFunnelStateExtensions#ATTACHMENT_SURFACE} property to {@link BeltFunnelBlock}'s state definition
 * so we can persist the funnel's attached surface in the block state itself (no BE NBT required).
 * <p>
 * Six values × the existing properties stays well within the BlockState limit. The natural default is
 * {@code DOWN} (first {@code Direction.values()} entry), which is exactly the vanilla "funnel on top of belt"
 * geometry — so all existing world-frame {@code HORIZONTAL_FACING} writes keep working unchanged.
 */
@Mixin(BeltFunnelBlock.class)
public abstract class BeltFunnelBlockStateMixin {

	@Inject(method = "createBlockStateDefinition", at = @At("HEAD"))
	private void createBiotech$addAttachmentSurface(StateDefinition.Builder<Block, BlockState> builder,
		CallbackInfo ci) {
		builder.add(BeltFunnelStateExtensions.ATTACHMENT_SURFACE);
	}
}
