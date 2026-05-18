package com.nobodiiiii.createbiotech.content.boneratchet;

import com.nobodiiiii.createbiotech.registry.CBBlockEntityTypes;
import com.simibubi.create.content.kinetics.base.AbstractEncasedShaftBlock;
import com.simibubi.create.foundation.block.IBE;

import net.minecraft.world.level.block.entity.BlockEntityType;

public class BoneRatchetBlock extends AbstractEncasedShaftBlock implements IBE<BoneRatchetBlockEntity> {

	public BoneRatchetBlock(Properties properties) {
		super(properties);
	}

	@Override
	public Class<BoneRatchetBlockEntity> getBlockEntityClass() {
		return BoneRatchetBlockEntity.class;
	}

	@Override
	public BlockEntityType<? extends BoneRatchetBlockEntity> getBlockEntityType() {
		return CBBlockEntityTypes.BONE_RATCHET.get();
	}
}
