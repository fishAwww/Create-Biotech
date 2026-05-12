package com.nobodiiiii.createbiotech.content.ghasthotairballoon;

import java.util.List;

import com.nobodiiiii.createbiotech.registry.CBBlockEntityTypes;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.Ghast;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.AABB;

public class GhastHotAirBalloonAssemblyStationBlock extends BaseEntityBlock {

	public static final Property<Direction.Axis> HORIZONTAL_AXIS = BlockStateProperties.HORIZONTAL_AXIS;

	public GhastHotAirBalloonAssemblyStationBlock(Properties properties) {
		super(properties);
		registerDefaultState(defaultBlockState().setValue(HORIZONTAL_AXIS, Direction.Axis.Z));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(HORIZONTAL_AXIS);
		super.createBlockStateDefinition(builder);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		return defaultBlockState().setValue(HORIZONTAL_AXIS,
			context.getHorizontalDirection().getClockWise().getAxis());
	}

	@Override
	public BlockState rotate(BlockState state, Rotation rot) {
		Direction.Axis axis = state.getValue(HORIZONTAL_AXIS);
		return state.setValue(HORIZONTAL_AXIS,
			rot.rotate(Direction.get(Direction.AxisDirection.POSITIVE, axis)).getAxis());
	}

	@Override
	public BlockState mirror(BlockState state, Mirror mirror) {
		return state;
	}

	@Override
	public RenderShape getRenderShape(BlockState state) {
		return RenderShape.MODEL;
	}

	@Override
	public int getLightBlock(BlockState state, BlockGetter level, BlockPos pos) {
		return 0;
	}

	@Override
	public boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) {
		return true;
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

	public static boolean isSeatOccupied(Level world, BlockPos stationPos) {
		return !world.getEntitiesOfClass(GhastHotAirBalloonSeatEntity.class, new AABB(stationPos.above()))
			.isEmpty();
	}

	public static boolean canBePickedUp(Entity passenger) {
		if (!(passenger instanceof Ghast ghast))
			return false;
		if (!ghast.isAlive())
			return false;
		if (ghast.isPassenger())
			return false;
		if (ghast.isVehicle())
			return false;
		return true;
	}

	public static void sitDown(Level world, BlockPos stationPos, BlockState state, Ghast ghast) {
		if (world.isClientSide)
			return;
		BlockPos seatPos = stationPos.above();
		float yaw = getFacingYaw(state);
		ghast.moveTo(seatPos.getX() + 0.5, seatPos.getY(), seatPos.getZ() + 0.5, yaw, 0f);
		ghast.setYBodyRot(yaw);
		ghast.setYHeadRot(yaw);
		ghast.setNoAi(true);
		ghast.setPersistenceRequired();
		ghast.setDeltaMovement(0, 0, 0);
		GhastHotAirBalloonSeatEntity seat = new GhastHotAirBalloonSeatEntity(world, stationPos);
		if (!world.addFreshEntity(seat))
			return;
		ghast.startRiding(seat, true);
	}

	private static float getFacingYaw(BlockState state) {
		Direction.Axis axis = state.getValue(HORIZONTAL_AXIS);
		return axis == Direction.Axis.Z ? -90f : 0f;
	}

	public static List<Ghast> findGhastsToSeat(Level world, BlockPos stationPos) {
		return world.getEntitiesOfClass(Ghast.class, new AABB(stationPos.above()));
	}
}
