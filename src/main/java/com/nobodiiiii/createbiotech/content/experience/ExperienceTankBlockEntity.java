package com.nobodiiiii.createbiotech.content.experience;

import javax.annotation.Nullable;

import com.nobodiiiii.createbiotech.registry.CBBlockEntityTypes;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

public class ExperienceTankBlockEntity extends BlockEntity implements ExperienceSource, ExperienceSink {
	@Nullable
	private BlockPos controller;
	private int storedExperience;
	private int height = 1;
	private boolean window = true;

	public ExperienceTankBlockEntity(BlockPos pos, BlockState state) {
		super(CBBlockEntityTypes.EXPERIENCE_TANK.get(), pos, state);
	}

	public boolean isController() {
		return controller == null || controller.equals(worldPosition);
	}

	@Nullable
	public ExperienceTankBlockEntity getControllerBE() {
		if (isController())
			return this;
		if (level == null || controller == null)
			return null;
		BlockEntity be = level.getBlockEntity(controller);
		return be instanceof ExperienceTankBlockEntity tank ? tank : null;
	}

	@Override
	public int insertExperience(int amount, boolean simulate) {
		ExperienceTankBlockEntity controllerBE = getControllerBE();
		if (controllerBE == null || amount <= 0)
			return 0;
		if (controllerBE != this)
			return controllerBE.insertExperience(amount, simulate);
		int accepted = Math.min(amount, getExperienceSpace());
		if (!simulate && accepted > 0) {
			storedExperience += accepted;
			syncToClient();
		}
		return accepted;
	}

	@Override
	public int extractExperience(int maxAmount, boolean simulate) {
		ExperienceTankBlockEntity controllerBE = getControllerBE();
		if (controllerBE == null || maxAmount <= 0)
			return 0;
		if (controllerBE != this)
			return controllerBE.extractExperience(maxAmount, simulate);
		int extracted = Math.min(maxAmount, storedExperience);
		if (!simulate && extracted > 0) {
			storedExperience -= extracted;
			syncToClient();
		}
		return extracted;
	}

	@Override
	public int getStoredExperience() {
		ExperienceTankBlockEntity controllerBE = getControllerBE();
		return controllerBE == null ? 0 : controllerBE.storedExperience;
	}

	int getStoredExperienceDirect() {
		return storedExperience;
	}

	@Override
	public int getExperienceSpace() {
		ExperienceTankBlockEntity controllerBE = getControllerBE();
		if (controllerBE == null)
			return 0;
		return Math.max(0, controllerBE.getCapacity() - controllerBE.storedExperience);
	}

	public int getCapacity() {
		ExperienceTankBlockEntity controllerBE = getControllerBE();
		return controllerBE == null ? ExperienceConstants.TANK_CAPACITY_PER_BLOCK
			: controllerBE.height * ExperienceConstants.TANK_CAPACITY_PER_BLOCK;
	}

	public int getHeight() {
		ExperienceTankBlockEntity controllerBE = getControllerBE();
		return controllerBE == null ? height : controllerBE.height;
	}

	public boolean hasWindow() {
		ExperienceTankBlockEntity controllerBE = getControllerBE();
		return controllerBE == null ? window : controllerBE.window;
	}

	public float getFillState() {
		int capacity = getCapacity();
		return capacity <= 0 ? 0 : (float) getStoredExperience() / capacity;
	}

	public void toggleWindows() {
		ExperienceTankBlockEntity controllerBE = getControllerBE();
		if (controllerBE == null || controllerBE.level == null)
			return;
		controllerBE.window = !controllerBE.window;
		BlockPos bottom = controllerBE.worldPosition;
		BlockPos top = bottom.above(controllerBE.height - 1);
		ExperienceTankBlock.applyColumn(controllerBE.level, bottom, top, controllerBE.storedExperience,
			controllerBE.window);
	}

	void applyStructure(BlockPos controller, int height, int storedExperience, boolean window) {
		this.controller = controller;
		this.height = Math.max(1, height);
		this.storedExperience = storedExperience;
		this.window = window;
		syncToClient();
	}

	public void handleRemoved() {
		if (level == null || level.isClientSide)
			return;
		ExperienceTankBlockEntity controllerBE = getControllerBE();
		if (controllerBE == null)
			return;
		int total = controllerBE.storedExperience;
		int released = Math.min(ExperienceConstants.TANK_CAPACITY_PER_BLOCK, total);
		int remaining = total - released;
		if (released > 0)
			ExperienceHelper.spawnExperience(level, worldPosition.getCenter(), released);

		BlockPos belowTop = worldPosition.below();
		if (level.getBlockState(belowTop)
			.is(getBlockState().getBlock())) {
			BlockPos bottom = belowTop;
			while (level.getBlockState(bottom.below())
				.is(getBlockState().getBlock()))
				bottom = bottom.below();
			int heightBelow = belowTop.getY() - bottom.getY() + 1;
			int keptBelow = Math.min(remaining, heightBelow * ExperienceConstants.TANK_CAPACITY_PER_BLOCK);
			ExperienceTankBlock.applyColumn(level, bottom, belowTop, keptBelow, controllerBE.window);
			remaining -= keptBelow;
		}

		BlockPos aboveBottom = worldPosition.above();
		if (level.getBlockState(aboveBottom)
			.is(getBlockState().getBlock())) {
			BlockPos top = aboveBottom;
			while (level.getBlockState(top.above())
				.is(getBlockState().getBlock()))
				top = top.above();
			int heightAbove = top.getY() - aboveBottom.getY() + 1;
			int keptAbove = Math.min(remaining, heightAbove * ExperienceConstants.TANK_CAPACITY_PER_BLOCK);
			ExperienceTankBlock.applyColumn(level, aboveBottom, top, keptAbove, controllerBE.window);
			remaining -= keptAbove;
		}

		if (remaining > 0)
			ExperienceHelper.spawnExperience(level, worldPosition.getCenter(), remaining);
	}

	@Override
	public AABB getRenderBoundingBox() {
		if (!isController())
			return new AABB(worldPosition);
		return new AABB(worldPosition, worldPosition.offset(1, height, 1));
	}

	@Override
	protected void saveAdditional(CompoundTag tag) {
		super.saveAdditional(tag);
		if (!isController() && controller != null)
			tag.put("Controller", NbtUtils.writeBlockPos(controller));
		tag.putInt("StoredExperience", storedExperience);
		tag.putInt("Height", height);
		tag.putBoolean("Window", window);
	}

	@Override
	public void load(CompoundTag tag) {
		super.load(tag);
		controller = tag.contains("Controller") ? NbtUtils.readBlockPos(tag.getCompound("Controller")) : null;
		storedExperience = tag.getInt("StoredExperience");
		height = Math.max(1, tag.getInt("Height"));
		window = !tag.contains("Window") || tag.getBoolean("Window");
	}

	@Override
	public CompoundTag getUpdateTag() {
		return saveWithoutMetadata();
	}

	@Override
	public ClientboundBlockEntityDataPacket getUpdatePacket() {
		return ClientboundBlockEntityDataPacket.create(this);
	}

	void syncToClient() {
		if (level == null)
			return;
		setChanged();
		BlockState state = getBlockState();
		level.sendBlockUpdated(worldPosition, state, state, 3);
	}
}
