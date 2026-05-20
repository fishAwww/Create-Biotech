package com.nobodiiiii.createbiotech.compat.jade;

import com.nobodiiiii.createbiotech.CreateBiotech;
import com.nobodiiiii.createbiotech.content.evokerenchantingchamber.EvokerEnchantingChamberBlock;
import com.nobodiiiii.createbiotech.content.evokerenchantingchamber.EvokerEnchantingChamberBlockEntity;
import com.nobodiiiii.createbiotech.content.experience.ExperienceCrystallizerBlock;
import com.nobodiiiii.createbiotech.content.experience.ExperienceCrystallizerBlockEntity;
import com.nobodiiiii.createbiotech.content.experience.ExperienceTankBlock;
import com.nobodiiiii.createbiotech.content.experience.ExperienceTankBlockEntity;
import com.nobodiiiii.createbiotech.registry.CBItems;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;

import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;
import snownee.jade.api.config.IPluginConfig;

@WailaPlugin
public class CreateBiotechJadePlugin implements IWailaPlugin {

	private static final String CURRENT_XP = "CurrentXp";
	private static final String MAX_XP = "MaxXp";

	public static final ResourceLocation EXPERIENCE_INFO = CreateBiotech.asResource("experience_info");

	@Override
	public void register(IWailaCommonRegistration registration) {
		registration.registerBlockDataProvider(ExperienceServerData.INSTANCE, ExperienceTankBlockEntity.class);
		registration.registerBlockDataProvider(ExperienceServerData.INSTANCE,
			ExperienceCrystallizerBlockEntity.class);
		registration.registerBlockDataProvider(ExperienceServerData.INSTANCE,
			EvokerEnchantingChamberBlockEntity.class);
	}

	@Override
	public void registerClient(IWailaClientRegistration registration) {
		registration.registerBlockComponent(ExperienceComponent.INSTANCE, ExperienceTankBlock.class);
		registration.registerBlockComponent(ExperienceComponent.INSTANCE, ExperienceCrystallizerBlock.class);
		registration.registerBlockComponent(ExperienceComponent.INSTANCE, EvokerEnchantingChamberBlock.class);
	}

	public enum ExperienceComponent implements IBlockComponentProvider {
		INSTANCE;

		@Override
		public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
			CompoundTag data = accessor.getServerData();
			if (!data.contains(CURRENT_XP) || !data.contains(MAX_XP))
				return;

			int current = data.getInt(CURRENT_XP);
			int max = data.getInt(MAX_XP);

			tooltip.add(tooltip.getElementHelper()
				.smallItem(new ItemStack(CBItems.EXPERIENCE.get())));
			tooltip.append(tooltip.getElementHelper()
				.spacer(2, 1));
			tooltip.append(Component.literal(Integer.toString(current))
				.withStyle(ChatFormatting.GOLD)
				.append(Component.literal(" / ").withStyle(ChatFormatting.GRAY))
				.append(Component.literal(max + " XP").withStyle(ChatFormatting.DARK_GRAY)));
		}

		@Override
		public ResourceLocation getUid() {
			return EXPERIENCE_INFO;
		}
	}

	public enum ExperienceServerData implements IServerDataProvider<BlockAccessor> {
		INSTANCE;

		@Override
		public void appendServerData(CompoundTag tag, BlockAccessor accessor) {
			JadeExperienceProvider provider = resolveProvider(accessor);
			if (provider == null)
				return;
			tag.putInt(CURRENT_XP, provider.getJadeCurrentXp());
			tag.putInt(MAX_XP, provider.getJadeMaxXp());
		}

		private static JadeExperienceProvider resolveProvider(BlockAccessor accessor) {
			BlockEntity be = accessor.getBlockEntity();
			if (be instanceof JadeExperienceProvider provider)
				return provider;

			BlockState state = accessor.getBlockState();
			if (state.getBlock() instanceof EvokerEnchantingChamberBlock
				&& state.getValue(EvokerEnchantingChamberBlock.HALF) == DoubleBlockHalf.UPPER) {
				BlockPos lowerPos = accessor.getPosition().below();
				BlockEntity lower = accessor.getLevel().getBlockEntity(lowerPos);
				if (lower instanceof JadeExperienceProvider provider)
					return provider;
			}
			return null;
		}

		@Override
		public ResourceLocation getUid() {
			return EXPERIENCE_INFO;
		}
	}
}
