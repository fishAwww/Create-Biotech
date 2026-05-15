package com.nobodiiiii.createbiotech.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.nobodiiiii.createbiotech.content.bufferpad.BufferPadCollisionHelper;
import com.simibubi.create.content.contraptions.AbstractContraptionEntity;

@Mixin(AbstractContraptionEntity.class)
public abstract class AbstractContraptionEntityBufferPadMixin {

	@Inject(method = "tick()V", at = @At("RETURN"))
	private void createBiotech$tickStaticBufferPadCollision(CallbackInfo ci) {
		BufferPadCollisionHelper.tickStaticWorldCollision((AbstractContraptionEntity) (Object) this);
	}
}
