package com.nobodiiiii.createbiotech.content.petridish;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

import javax.annotation.Nullable;

import com.nobodiiiii.createbiotech.content.slimemimic.SlimeMimicHandler;
import com.nobodiiiii.createbiotech.foundation.advancement.PlacedByPlayerAdvancementTracker;
import com.nobodiiiii.createbiotech.registry.CBBlockEntityTypes;
import com.nobodiiiii.createbiotech.registry.CBFluids;
import com.nobodiiiii.createbiotech.registry.CBItems;
import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.fluid.FluidHelper;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;
import net.minecraftforge.fluids.capability.templates.FluidTank;
import net.minecraftforge.items.ItemStackHandler;

public class PetriDishBlockEntity extends SmartBlockEntity implements IHaveGoggleInformation {

	public static final int SCAN_INTERVAL = 20;
	public static final int FLUID_PER_HEALTH = 250;
	public static final int SCAN_RADIUS = 2;
	public static final int TANK_CAPACITY = 51200;

	private static final String INVENTORY_TAG = "Inventory";
	private static final String TANK_TAG = "Tank";
	private static final String RECORDED_ENTITY_ID_TAG = "RecordedEntityId";
	private static final String RECORDED_MAX_HEALTH_TAG = "RecordedMaxHealth";
	private static final String SCAN_COOLDOWN_TAG = "ScanCooldown";

	private final ItemStackHandler inventory = new ItemStackHandler(1) {
		@Override
		protected void onContentsChanged(int slot) {
			if (slot == 0 && getStackInSlot(slot).isEmpty()) {
				clearRecordedEntity();
			}
			setChanged();
			sendData();
		}

		@Override
		public int getSlotLimit(int slot) {
			return 1;
		}

		@Override
		public boolean isItemValid(int slot, ItemStack stack) {
			return stack.is(CBItems.BIONIC_MECHANISM.get());
		}
	};

	private final FluidTank fluidTank = new FluidTank(TANK_CAPACITY) {
		@Override
		public boolean isFluidValid(FluidStack stack) {
			return isAcceptedFluid(stack);
		}

		@Override
		protected void onContentsChanged() {
			setChanged();
			sendData();
		}
	};

	private final LazyOptional<ItemStackHandler> itemCapability = LazyOptional.of(() -> inventory);
	private final LazyOptional<IFluidHandler> fluidCapability = LazyOptional.of(() -> new PetriDishFluidHandler());

	@Nullable
	private ResourceLocation recordedEntityId;
	private float recordedMaxHealth;
	private int scanCooldown;
	@Nullable
	private UUID advancementOwner;

	public PetriDishBlockEntity(BlockPos pos, BlockState state) {
		super(CBBlockEntityTypes.PETRI_DISH.get(), pos, state);
	}

	@Override
	public void addBehaviours(List<BlockEntityBehaviour> behaviours) {}

	@Override
	public void tick() {
		super.tick();

		if (level == null || level.isClientSide)
			return;

		if (scanCooldown > 0)
			scanCooldown--;

		if (hasBionicMechanism() && scanCooldown <= 0) {
			scanCooldown = SCAN_INTERVAL;
			updateRecordedEntityFromNearby();
		}

		tryCompleteSpawn();
	}

	public InteractionResult use(Player player, InteractionHand hand) {
		if (level == null)
			return InteractionResult.PASS;

		ItemStack heldItem = player.getItemInHand(hand);
		if (FluidHelper.tryEmptyItemIntoBE(level, player, hand, heldItem, this))
			return InteractionResult.SUCCESS;
		if (FluidHelper.tryFillItemFromBE(level, player, hand, heldItem, this))
			return InteractionResult.SUCCESS;

		if (heldItem.is(CBItems.BIONIC_MECHANISM.get())) {
			if (!inventory.getStackInSlot(0).isEmpty())
				return InteractionResult.SUCCESS;
			if (level.isClientSide)
				return InteractionResult.SUCCESS;

			ItemStack inserted = heldItem.copyWithCount(1);
			inventory.setStackInSlot(0, inserted);
			if (!player.isCreative())
				heldItem.shrink(1);
			level.playSound(null, worldPosition, SoundEvents.ITEM_FRAME_ADD_ITEM, SoundSource.BLOCKS, 0.5f, 1.0f);
			updateRecordedEntityFromNearby();
			return InteractionResult.SUCCESS;
		}

		if (heldItem.isEmpty() && hasBionicMechanism()) {
			if (level.isClientSide)
				return InteractionResult.SUCCESS;
			ItemStack extracted = inventory.extractItem(0, 1, false);
			player.getInventory().placeItemBackInInventory(extracted);
			level.playSound(null, worldPosition, SoundEvents.ITEM_FRAME_REMOVE_ITEM, SoundSource.BLOCKS, 0.5f, 1.0f);
			fluidTank.setFluid(FluidStack.EMPTY);
			clearRecordedEntity();
			return InteractionResult.SUCCESS;
		}

		return InteractionResult.PASS;
	}

	public void setAdvancementOwner(@Nullable LivingEntity placer) {
		advancementOwner = PlacedByPlayerAdvancementTracker.ownerFrom(placer);
		setChanged();
	}

	public ItemStack getBionicMechanism() {
		return inventory.getStackInSlot(0);
	}

	public FluidStack getFluid() {
		return fluidTank.getFluid();
	}

	@Nullable
	public ResourceLocation getRecordedEntityId() {
		return recordedEntityId;
	}

	public float getRecordedMaxHealth() {
		return recordedMaxHealth;
	}

	public int getRequiredFluidAmount() {
		if (recordedEntityId == null || recordedMaxHealth <= 0)
			return 0;
		return Math.max(1, (int) Math.ceil(recordedMaxHealth)) * FLUID_PER_HEALTH;
	}

	public boolean canAcceptFluidNow() {
		if (!hasBionicMechanism())
			return false;
		if (recordedEntityId == null) {
			updateRecordedEntityFromNearby();
			return recordedEntityId != null;
		}
		return hasMatchingEntityNearby();
	}

	private boolean hasBionicMechanism() {
		return inventory.getStackInSlot(0).is(CBItems.BIONIC_MECHANISM.get());
	}

	private boolean isAcceptedFluid(FluidStack stack) {
		if (stack.isEmpty())
			return false;
		if (stack.getFluid() != CBFluids.LIQUID_LIVING_SLIME.get())
			return false;
		if (!canAcceptFluidNow())
			return false;
		int required = getRequiredFluidAmount();
		if (required <= 0)
			return false;
		FluidStack stored = fluidTank.getFluid();
		if (!stored.isEmpty() && !stored.isFluidEqual(stack))
			return false;
		return stored.getAmount() < required;
	}

	private void tryCompleteSpawn() {
		if (!(level instanceof ServerLevel serverLevel))
			return;
		if (!hasBionicMechanism())
			return;
		if (recordedEntityId == null || recordedMaxHealth <= 0)
			return;
		if (!hasMatchingEntityNearby())
			return;

		int required = getRequiredFluidAmount();
		if (required <= 0 || fluidTank.getFluidAmount() < required)
			return;

		EntityType<?> entityType = net.minecraftforge.registries.ForgeRegistries.ENTITY_TYPES.getValue(recordedEntityId);
		if (entityType == null)
			return;

		BlockPos spawnPos = worldPosition.above();
		if (!level.noCollision(new AABB(spawnPos.getX(), spawnPos.getY(), spawnPos.getZ(), spawnPos.getX() + 1,
			spawnPos.getY() + 2, spawnPos.getZ() + 1)))
			return;

		Entity spawned = entityType.spawn(serverLevel, ItemStack.EMPTY, null, spawnPos, MobSpawnType.DISPENSER, true,
			false);
		if (spawned == null) {
			spawned = entityType.create(serverLevel);
			if (spawned != null) {
				spawned.moveTo(spawnPos.getX() + 0.5d, spawnPos.getY(), spawnPos.getZ() + 0.5d,
					serverLevel.random.nextFloat() * 360.0f, 0.0f);
				serverLevel.addFreshEntity(spawned);
			}
		}
		if (!(spawned instanceof LivingEntity livingEntity))
			return;

		SlimeMimicHandler.markSpawnedEntity(livingEntity);
		fluidTank.drain(required, FluidAction.EXECUTE);
		inventory.extractItem(0, 1, false);
		clearRecordedEntity();
		level.playSound(null, worldPosition, SoundEvents.SLIME_BLOCK_PLACE, SoundSource.BLOCKS, 0.7f, 0.9f);
		sendData();
	}

	private void updateRecordedEntityFromNearby() {
		if (level == null || !hasBionicMechanism())
			return;
		if (recordedEntityId != null) {
			if (!hasMatchingEntityNearby()) {
				// Keep the record, but reject further filling until the type comes back nearby.
				sendData();
			}
			return;
		}

		List<LivingEntity> entities = getNearbyLivingEntities();
		for (LivingEntity entity : entities) {
			if (!isRecordableEntity(entity))
				continue;
			recordEntity(entity);
			return;
		}
	}

	private boolean hasMatchingEntityNearby() {
		if (recordedEntityId == null)
			return false;
		for (LivingEntity entity : getNearbyLivingEntities()) {
			ResourceLocation entityId = net.minecraftforge.registries.ForgeRegistries.ENTITY_TYPES.getKey(entity.getType());
			if (Objects.equals(recordedEntityId, entityId))
				return true;
		}
		return false;
	}

	private List<LivingEntity> getNearbyLivingEntities() {
		if (level == null)
			return List.of();
		AABB bounds = new AABB(worldPosition).inflate(SCAN_RADIUS);
		return level.getEntitiesOfClass(LivingEntity.class, bounds, this::isRecordableEntity);
	}

	private boolean isRecordableEntity(LivingEntity entity) {
		if (!entity.isAlive())
			return false;
		if (entity.isSpectator())
			return false;
		if (entity.blockPosition().equals(worldPosition))
			return false;
		if (entity instanceof Player)
			return false;
		EntityType<?> type = entity.getType();
		if (type == EntityType.ARMOR_STAND)
			return false;
		ResourceLocation entityId = net.minecraftforge.registries.ForgeRegistries.ENTITY_TYPES.getKey(type);
		return entityId != null;
	}

	private void recordEntity(LivingEntity entity) {
		ResourceLocation entityId = net.minecraftforge.registries.ForgeRegistries.ENTITY_TYPES.getKey(entity.getType());
		if (entityId == null)
			return;
		recordedEntityId = entityId;
		recordedMaxHealth = entity.getMaxHealth();
		int required = getRequiredFluidAmount();
		if (fluidTank.getFluidAmount() > required) {
			fluidTank.drain(fluidTank.getFluidAmount() - required, FluidAction.EXECUTE);
		}
		setChanged();
		sendData();
	}

	private void clearRecordedEntity() {
		recordedEntityId = null;
		recordedMaxHealth = 0;
		scanCooldown = 0;
		setChanged();
		sendData();
	}

	@Override
	public void destroy() {
		if (level != null && !level.isClientSide) {
			ItemStack stack = inventory.getStackInSlot(0);
			if (!stack.isEmpty()) {
				Block.popResource(level, worldPosition, stack.copy());
				inventory.setStackInSlot(0, ItemStack.EMPTY);
			}
		}
		super.destroy();
	}

	@Override
	protected void write(CompoundTag tag, boolean clientPacket) {
		super.write(tag, clientPacket);
		tag.put(INVENTORY_TAG, inventory.serializeNBT());
		tag.put(TANK_TAG, fluidTank.writeToNBT(new CompoundTag()));
		if (recordedEntityId != null)
			tag.putString(RECORDED_ENTITY_ID_TAG, recordedEntityId.toString());
		tag.putFloat(RECORDED_MAX_HEALTH_TAG, recordedMaxHealth);
		tag.putInt(SCAN_COOLDOWN_TAG, scanCooldown);
		PlacedByPlayerAdvancementTracker.writeOwner(tag, advancementOwner);
	}

	@Override
	protected void read(CompoundTag tag, boolean clientPacket) {
		super.read(tag, clientPacket);
		inventory.deserializeNBT(tag.getCompound(INVENTORY_TAG));
		fluidTank.readFromNBT(tag.getCompound(TANK_TAG));
		recordedEntityId =
			tag.contains(RECORDED_ENTITY_ID_TAG) ? new ResourceLocation(tag.getString(RECORDED_ENTITY_ID_TAG)) : null;
		recordedMaxHealth = tag.getFloat(RECORDED_MAX_HEALTH_TAG);
		scanCooldown = tag.getInt(SCAN_COOLDOWN_TAG);
		advancementOwner = PlacedByPlayerAdvancementTracker.readOwner(tag);
	}

	@Override
	public void invalidate() {
		super.invalidate();
		itemCapability.invalidate();
		fluidCapability.invalidate();
	}

	@Override
	public <T> LazyOptional<T> getCapability(Capability<T> cap, @Nullable Direction side) {
		if (cap == ForgeCapabilities.ITEM_HANDLER)
			return itemCapability.cast();
		if (cap == ForgeCapabilities.FLUID_HANDLER)
			return fluidCapability.cast();
		return super.getCapability(cap, side);
	}

	@Override
	public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
		tooltip.add(Component.translatable("create_biotech.petri_dish.goggles.title")
			.withStyle(ChatFormatting.GRAY));
		if (hasBionicMechanism()) {
			tooltip.add(Component.translatable("create_biotech.petri_dish.goggles.bionic_loaded")
				.withStyle(ChatFormatting.GREEN));
		} else {
			tooltip.add(Component.translatable("create_biotech.petri_dish.goggles.bionic_missing")
				.withStyle(ChatFormatting.RED));
		}

		if (recordedEntityId != null) {
			tooltip.add(Component.translatable("create_biotech.petri_dish.goggles.recorded",
				Component.translatable(entityTranslationKey(recordedEntityId)))
				.withStyle(ChatFormatting.GRAY));
			tooltip.add(Component.translatable("create_biotech.petri_dish.goggles.required_fluid", getRequiredFluidAmount())
				.withStyle(ChatFormatting.AQUA));
		} else {
			tooltip.add(Component.translatable("create_biotech.petri_dish.goggles.unrecorded")
				.withStyle(ChatFormatting.YELLOW));
		}

		return containedFluidTooltip(tooltip, isPlayerSneaking, getCapability(ForgeCapabilities.FLUID_HANDLER));
	}

	private static String entityTranslationKey(ResourceLocation entityId) {
		return "entity." + entityId.getNamespace() + "." + entityId.getPath();
	}

	private class PetriDishFluidHandler implements IFluidHandler {

		@Override
		public int getTanks() {
			return 1;
		}

		@Override
		public FluidStack getFluidInTank(int tank) {
			return tank == 0 ? fluidTank.getFluid() : FluidStack.EMPTY;
		}

		@Override
		public int getTankCapacity(int tank) {
			return tank == 0 ? fluidTank.getCapacity() : 0;
		}

		@Override
		public boolean isFluidValid(int tank, FluidStack stack) {
			return tank == 0 && fluidTank.isFluidValid(stack);
		}

		@Override
		public int fill(FluidStack resource, FluidAction action) {
			if (!fluidTank.isFluidValid(resource))
				return 0;
			int required = getRequiredFluidAmount();
			if (required <= 0)
				return 0;
			int room = required - fluidTank.getFluidAmount();
			if (room <= 0)
				return 0;
			FluidStack limited = resource.copy();
			limited.setAmount(Math.min(resource.getAmount(), room));
			return fluidTank.fill(limited, action);
		}

		@Override
		public FluidStack drain(FluidStack resource, FluidAction action) {
			return fluidTank.drain(resource, action);
		}

		@Override
		public FluidStack drain(int maxDrain, FluidAction action) {
			return fluidTank.drain(maxDrain, action);
		}
	}
}
