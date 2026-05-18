package com.nobodiiiii.createbiotech.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.nobodiiiii.createbiotech.registry.CBBlocks;
import com.simibubi.create.content.kinetics.fan.NozzleBlock;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;

@Mixin(NozzleBlock.class)
public abstract class NozzleBlockMixin {

	@Inject(method = "canSurvive", at = @At("HEAD"), cancellable = true)
	private void createBiotech$allowExperiencePumpEnds(BlockState state, LevelReader level, BlockPos pos,
		CallbackInfoReturnable<Boolean> cir) {
		Direction nozzleFacing = state.getValue(NozzleBlock.FACING);
		BlockPos pumpPos = pos.relative(nozzleFacing.getOpposite());
		BlockState pumpState = level.getBlockState(pumpPos);
		if (!pumpState.is(CBBlocks.EXPERIENCE_PUMP.get()))
			return;
		if (pumpState.getValue(com.nobodiiiii.createbiotech.content.experience.ExperiencePumpBlock.FACING)
			.getAxis() != nozzleFacing.getAxis())
			return;
		cir.setReturnValue(true);
	}
}
