package com.nobodiiiii.createbiotech.content.experience;

import com.nobodiiiii.createbiotech.registry.CBBlockEntityTypes;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.content.fluids.tank.FluidTankBlock.Shape;
import com.simibubi.create.foundation.block.IBE;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.pathfinder.PathComputationType;

public class ExperienceTankBlock extends Block implements IWrenchable, IBE<ExperienceTankBlockEntity> {
	public static final BooleanProperty TOP = BooleanProperty.create("top");
	public static final BooleanProperty BOTTOM = BooleanProperty.create("bottom");
	public static final EnumProperty<Shape> SHAPE = EnumProperty.create("shape", Shape.class);

	public ExperienceTankBlock(Properties properties) {
		super(properties);
		registerDefaultState(defaultBlockState().setValue(TOP, true)
			.setValue(BOTTOM, true)
			.setValue(SHAPE, Shape.WINDOW));
	}

	@Override
	protected void createBlockStateDefinition(Builder<Block, BlockState> builder) {
		builder.add(TOP, BOTTOM, SHAPE);
	}

	@Override
	public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean moved) {
		if (oldState.getBlock() == state.getBlock() || moved)
			return;
		if (!level.isClientSide)
			recomputeColumn(level, pos);
	}

	@Override
	public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor level,
		BlockPos currentPos, BlockPos neighborPos) {
		if ((direction == Direction.UP || direction == Direction.DOWN) && !level.isClientSide())
			level.scheduleTick(currentPos, this, 1);
		return state;
	}

	@Override
	public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
		recomputeColumn(level, pos);
	}

	@Override
	public InteractionResult onWrenched(BlockState state, UseOnContext context) {
		if (!context.getLevel().isClientSide && context.getLevel()
			.getBlockEntity(context.getClickedPos()) instanceof ExperienceTankBlockEntity tank)
			tank.toggleWindows();
		return InteractionResult.SUCCESS;
	}

	@Override
	public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
		if (state.hasBlockEntity() && state.getBlock() != newState.getBlock()
			&& level.getBlockEntity(pos) instanceof ExperienceTankBlockEntity tank)
			tank.handleRemoved();
		super.onRemove(state, level, pos, newState, isMoving);
		if (!level.isClientSide && state.getBlock() != newState.getBlock()) {
			if (level.getBlockState(pos.above())
				.is(this))
				level.scheduleTick(pos.above(), this, 1);
			if (level.getBlockState(pos.below())
				.is(this))
				level.scheduleTick(pos.below(), this, 1);
		}
	}

	@Override
	public boolean hasAnalogOutputSignal(BlockState state) {
		return true;
	}

	@Override
	public int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
		if (!(level.getBlockEntity(pos) instanceof ExperienceTankBlockEntity tank))
			return 0;
		float fill = tank.getFillState();
		return fill <= 0 ? 0 : Mth.clamp(Mth.ceil(fill * 15.0f), 1, 15);
	}

	@Override
	public boolean isPathfindable(BlockState state, BlockGetter reader, BlockPos pos, PathComputationType type) {
		return false;
	}

	@Override
	public Class<ExperienceTankBlockEntity> getBlockEntityClass() {
		return ExperienceTankBlockEntity.class;
	}

	@Override
	public BlockEntityType<? extends ExperienceTankBlockEntity> getBlockEntityType() {
		return CBBlockEntityTypes.EXPERIENCE_TANK.get();
	}

	public static void recomputeColumn(Level level, BlockPos pos) {
		if (level.isClientSide)
			return;
		BlockPos bottom = pos;
		while (level.getBlockState(bottom.below())
			.is(level.getBlockState(pos)
				.getBlock()))
			bottom = bottom.below();

		BlockPos top = pos;
		while (level.getBlockState(top.above())
			.is(level.getBlockState(pos)
				.getBlock()))
			top = top.above();

		int stored = 0;
		boolean window = false;
		for (BlockPos cursor = bottom; cursor.getY() <= top.getY(); cursor = cursor.above()) {
			if (!(level.getBlockEntity(cursor) instanceof ExperienceTankBlockEntity tank))
				continue;
			if (tank.isController())
				stored += tank.getStoredExperienceDirect();
			window |= tank.hasWindow();
		}
		applyColumn(level, bottom, top, stored, window);
	}

	static void applyColumn(Level level, BlockPos bottom, BlockPos top, int stored, boolean window) {
		int height = top.getY() - bottom.getY() + 1;
		int capacity = height * ExperienceConstants.TANK_CAPACITY_PER_BLOCK;
		int clampedStored = Math.min(stored, capacity);
		if (stored > clampedStored)
			ExperienceHelper.spawnExperience(level, bottom.getCenter(), stored - clampedStored);

		for (BlockPos cursor = bottom; cursor.getY() <= top.getY(); cursor = cursor.above()) {
			if (!(level.getBlockEntity(cursor) instanceof ExperienceTankBlockEntity tank))
				continue;
			boolean isBottom = cursor.getY() == bottom.getY();
			boolean isTop = cursor.getY() == top.getY();
			tank.applyStructure(bottom, height, isBottom ? clampedStored : 0, window);
			BlockState state = level.getBlockState(cursor);
			if (state.getBlock() instanceof ExperienceTankBlock)
				level.setBlock(cursor, state.setValue(BOTTOM, isBottom)
					.setValue(TOP, isTop)
					.setValue(SHAPE, window ? Shape.WINDOW : Shape.PLAIN), 22);
		}
	}
}
