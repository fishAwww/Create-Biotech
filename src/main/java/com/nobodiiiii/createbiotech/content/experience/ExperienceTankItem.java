package com.nobodiiiii.createbiotech.content.experience;

import java.util.List;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

public class ExperienceTankItem extends BlockItem {

	public ExperienceTankItem(Block block, Properties properties) {
		super(block, properties);
	}

	@Override
	public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
		super.appendHoverText(stack, level, tooltip, flag);
		tooltip.add(Component.translatable("block.create_biotech.experience_tank.tooltip.capacity",
			ExperienceConstants.TANK_CAPACITY_PER_BLOCK));
		tooltip.add(Component.translatable("block.create_biotech.experience_tank.tooltip.io"));
	}
}
