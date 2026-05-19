package com.nobodiiiii.createbiotech.content.squidprinter;

import com.nobodiiiii.createbiotech.registry.CBItems;
import com.nobodiiiii.createbiotech.registry.CBRecipeTypes;
import com.simibubi.create.content.processing.recipe.ProcessingRecipe;
import com.simibubi.create.content.processing.recipe.ProcessingRecipeBuilder.ProcessingRecipeParams;
import com.simibubi.create.foundation.fluid.FluidIngredient;
import com.simibubi.create.foundation.recipe.IRecipeTypeInfo;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.items.wrapper.RecipeWrapper;

public class SquidPrinterRecipe extends ProcessingRecipe<RecipeWrapper> {

	private static final IRecipeTypeInfo TYPE_INFO = new IRecipeTypeInfo() {
		@Override
		public ResourceLocation getId() {
			return CBRecipeTypes.SQUID_PRINTER_TYPE.getId();
		}

		@Override
		@SuppressWarnings("unchecked")
		public <T extends net.minecraft.world.item.crafting.RecipeSerializer<?>> T getSerializer() {
			return (T) CBRecipeTypes.SQUID_PRINTER_SERIALIZER.get();
		}

		@Override
		@SuppressWarnings("unchecked")
		public <T extends net.minecraft.world.item.crafting.RecipeType<?>> T getType() {
			return (T) CBRecipeTypes.SQUID_PRINTER_TYPE.get();
		}
	};

	public SquidPrinterRecipe(ProcessingRecipeParams params) {
		super(TYPE_INFO, params);
	}

	@Override
	public boolean matches(RecipeWrapper inv, Level level) {
		return !ingredients.isEmpty() && ingredients.get(0)
			.test(inv.getItem(0));
	}

	@Override
	protected int getMaxInputCount() {
		return 1;
	}

	@Override
	protected int getMaxOutputCount() {
		return 1;
	}

	@Override
	protected int getMaxFluidInputCount() {
		return 1;
	}

	@Override
	protected boolean canSpecifyDuration() {
		return true;
	}

	public FluidIngredient getRequiredFluid() {
		if (fluidIngredients.isEmpty())
			throw new IllegalStateException("Squid Printer Recipe: " + id + " has no fluid ingredient");
		return fluidIngredients.get(0);
	}

	public boolean matchesTemplate(ItemStack template) {
		return EnchantmentBookCopyItem.hasCopyableEnchantments(template);
	}

	public int getTicksPerLevel() {
		return Math.max(1, getProcessingDuration());
	}

	public int getWaterPerLevel() {
		return Math.max(1, getRequiredFluid().getRequiredAmount());
	}

	public int getTemplateLevelTotal(ItemStack template) {
		return Math.max(1, EnchantmentBookCopyItem.sumCopySourceEnchantmentLevels(template));
	}

	public int getRequiredTicks(ItemStack template) {
		return getTicksPerLevel() * getTemplateLevelTotal(template);
	}

	public int getRequiredWater(ItemStack template) {
		return getWaterPerLevel() * getTemplateLevelTotal(template);
	}

	public ItemStack createResult(ItemStack template) {
		return EnchantmentBookCopyItem.fromTemplate(template, CBItems.ENCHANTMENT_BOOK_COPY.get());
	}
}
