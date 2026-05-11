package com.nobodiiiii.createbiotech.content.ghasthotairballoon;

import com.nobodiiiii.createbiotech.registry.CBBlockEntityTypes;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class GhastHotAirBalloonAssemblyStationBlock extends BaseEntityBlock {

	public GhastHotAirBalloonAssemblyStationBlock(Properties properties) {
		super(properties);
	}

	@Override
	public RenderShape getRenderShape(BlockState state) {
		return RenderShape.MODEL;
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new GhastHotAirBalloonAssemblyStationBlockEntity(pos, state);
	}

	@Override
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
		BlockEntityType<T> type) {
		return createTickerHelper(type, CBBlockEntityTypes.GHAST_HOT_AIR_BALLOON_ASSEMBLY_STATION.get(),
			GhastHotAirBalloonAssemblyStationBlockEntity::tick);
	}

	@Override
	public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
		super.onPlace(state, level, pos, oldState, isMoving);
		updatePoweredState(level, pos);
	}

	@Override
	public void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock,
		BlockPos fromPos, boolean isMoving) {
		super.neighborChanged(state, level, pos, neighborBlock, fromPos, isMoving);
		updatePoweredState(level, pos);
	}

	private static void updatePoweredState(Level level, BlockPos pos) {
		if (level.isClientSide)
			return;
		BlockEntity be = level.getBlockEntity(pos);
		if (be instanceof GhastHotAirBalloonAssemblyStationBlockEntity station)
			station.onNeighborSignalChanged(level.hasNeighborSignal(pos));
	}
}
