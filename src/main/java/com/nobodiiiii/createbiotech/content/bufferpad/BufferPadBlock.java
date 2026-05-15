package com.nobodiiiii.createbiotech.content.bufferpad;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class BufferPadBlock extends Block {

	public static final DirectionProperty FACING = BlockStateProperties.FACING;

	private static final VoxelShape DOWN_SHAPE = Block.box(0, 0, 0, 16, 8, 16);
	private static final VoxelShape UP_SHAPE = Block.box(0, 8, 0, 16, 16, 16);
	private static final VoxelShape NORTH_SHAPE = Block.box(0, 0, 0, 16, 16, 8);
	private static final VoxelShape SOUTH_SHAPE = Block.box(0, 0, 8, 16, 16, 16);
	private static final VoxelShape WEST_SHAPE = Block.box(0, 0, 0, 8, 16, 16);
	private static final VoxelShape EAST_SHAPE = Block.box(8, 0, 0, 16, 16, 16);

	public BufferPadBlock(Properties properties) {
		super(properties);
		registerDefaultState(defaultBlockState().setValue(FACING, Direction.DOWN));
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		return defaultBlockState().setValue(FACING, context.getClickedFace().getOpposite());
	}

	@Override
	public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return shapeFor(state.getValue(FACING));
	}

	@Override
	public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return shapeFor(state.getValue(FACING));
	}

	private static VoxelShape shapeFor(Direction facing) {
		return switch (facing) {
		case DOWN -> DOWN_SHAPE;
		case UP -> UP_SHAPE;
		case NORTH -> NORTH_SHAPE;
		case SOUTH -> SOUTH_SHAPE;
		case WEST -> WEST_SHAPE;
		case EAST -> EAST_SHAPE;
		};
	}

	@Override
	public BlockState rotate(BlockState state, Rotation rotation) {
		return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
	}

	@Override
	public BlockState mirror(BlockState state, Mirror mirror) {
		return state.setValue(FACING, mirror.mirror(state.getValue(FACING)));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING);
		super.createBlockStateDefinition(builder);
	}
}
