package com.nobodiiiii.createbiotech.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.nobodiiiii.createbiotech.content.beltsurface.BeltFunnelStateExtensions;
import com.simibubi.create.content.logistics.funnel.AbstractHorizontalFunnelBlock;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Vanilla {@link AbstractHorizontalFunnelBlock#rotate} only rotates {@code HORIZONTAL_FACING}. Our funnel state also
 * holds {@link BeltFunnelStateExtensions#ATTACHMENT_SURFACE}; if a rotation leaves it untouched the two properties
 * desync (HORIZONTAL_FACING now means something different in the canonical local frame because the attachment
 * surface — and hence the canonical forward axis — would have moved).
 * <p>
 * This mixin rotates {@code ATTACHMENT_SURFACE} alongside {@code HORIZONTAL_FACING} so structure-block /
 * contraption / schematic transforms keep both in lock-step. Vertical attachments (UP / DOWN) are unaffected by
 * a Y-axis rotation, exactly as desired. Mirror delegates to {@code rotate} in vanilla, so it's covered too.
 */
@Mixin(AbstractHorizontalFunnelBlock.class)
public abstract class AbstractHorizontalFunnelBlockMixin {

	@Inject(method = "rotate(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/block/Rotation;)Lnet/minecraft/world/level/block/state/BlockState;",
		at = @At("RETURN"), cancellable = true)
	private void createBiotech$rotateAttachmentSurface(BlockState state, Rotation rotation,
		CallbackInfoReturnable<BlockState> cir) {
		BlockState result = cir.getReturnValue();
		if (result == null || !result.hasProperty(BeltFunnelStateExtensions.ATTACHMENT_SURFACE))
			return;
		Direction attachment = result.getValue(BeltFunnelStateExtensions.ATTACHMENT_SURFACE);
		Direction rotated = rotation.rotate(attachment);
		if (rotated != attachment)
			cir.setReturnValue(result.setValue(BeltFunnelStateExtensions.ATTACHMENT_SURFACE, rotated));
	}
}
