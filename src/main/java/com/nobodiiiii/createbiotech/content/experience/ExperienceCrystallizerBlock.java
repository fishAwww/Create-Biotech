package com.nobodiiiii.createbiotech.content.experience;

import com.nobodiiiii.createbiotech.registry.CBBlockEntityTypes;
import com.simibubi.create.foundation.block.IBE;
import com.simibubi.create.foundation.block.WrenchableDirectionalBlock;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;

public class ExperienceCrystallizerBlock extends WrenchableDirectionalBlock implements IBE<ExperienceCrystallizerBlockEntity> {

	public ExperienceCrystallizerBlock(Properties properties) {
		super(properties);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		return super.getStateForPlacement(context);
	}

	@Override
	public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
		BlockHitResult hit) {
		if (!(level.getBlockEntity(pos) instanceof ExperienceCrystallizerBlockEntity crystallizer))
			return InteractionResult.PASS;
		if (level.isClientSide)
			return crystallizer.hasOutput() ? InteractionResult.SUCCESS : InteractionResult.PASS;
		ItemStack extracted = crystallizer.extractOutput();
		if (extracted.isEmpty())
			return InteractionResult.PASS;
		if (!player.getInventory()
			.add(extracted))
			player.drop(extracted, false);
		return InteractionResult.CONSUME;
	}

	@Override
	public boolean isPathfindable(BlockState state, BlockGetter reader, BlockPos pos, PathComputationType type) {
		return false;
	}

	@Override
	public Class<ExperienceCrystallizerBlockEntity> getBlockEntityClass() {
		return ExperienceCrystallizerBlockEntity.class;
	}

	@Override
	public BlockEntityType<? extends ExperienceCrystallizerBlockEntity> getBlockEntityType() {
		return CBBlockEntityTypes.EXPERIENCE_CRYSTALLIZER.get();
	}
}
