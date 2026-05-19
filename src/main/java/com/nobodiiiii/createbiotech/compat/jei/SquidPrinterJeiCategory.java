package com.nobodiiiii.createbiotech.compat.jei;

import java.util.List;

import com.nobodiiiii.createbiotech.CreateBiotech;
import com.nobodiiiii.createbiotech.content.squidprinter.SquidPrinterBlockEntity;
import com.nobodiiiii.createbiotech.registry.CBBlocks;
import com.simibubi.create.compat.jei.category.CreateRecipeCategory;
import com.simibubi.create.compat.jei.category.animations.AnimatedSpout;
import com.simibubi.create.foundation.gui.AllGuiTextures;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.ITooltipBuilder;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.AbstractRecipeCategory;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.fluids.FluidStack;

public class SquidPrinterJeiCategory extends AbstractRecipeCategory<SquidPrinterJeiRecipe> {

	public static final RecipeType<SquidPrinterJeiRecipe> TYPE =
		RecipeType.create(CreateBiotech.MOD_ID, "squid_printer", SquidPrinterJeiRecipe.class);

	private static final int WIDTH = 177;
	private static final int HEIGHT = 60;

	private final AnimatedSpout spout = new AnimatedSpout();

	public SquidPrinterJeiCategory() {
		super(TYPE, Component.translatable("block.create_biotech.squid_printer"),
			new ItemIconDrawable(new ItemStack(CBBlocks.SQUID_PRINTER.get())), WIDTH, HEIGHT);
	}

	@Override
	public void setRecipe(IRecipeLayoutBuilder builder, SquidPrinterJeiRecipe recipe, IFocusGroup focuses) {
		builder.addSlot(RecipeIngredientRole.INPUT, 27, 38)
			.setBackground(CreateRecipeCategory.getRenderedSlot(), -1, -1)
			.addItemStack(recipe.inputBook().copy());

		builder.addSlot(RecipeIngredientRole.INPUT, 51, 5)
			.setBackground(CreateRecipeCategory.getRenderedSlot(), -1, -1)
			.addItemStacks(recipe.templateBooks());

		builder.addSlot(RecipeIngredientRole.OUTPUT, 132, 38)
			.setBackground(CreateRecipeCategory.getRenderedSlot(), -1, -1)
			.addItemStacks(recipe.outputCopies());
	}

	@Override
	public void draw(SquidPrinterJeiRecipe recipe, IRecipeSlotsView recipeSlotsView, GuiGraphics graphics,
		double mouseX, double mouseY) {
		AllGuiTextures.JEI_SHADOW.render(graphics, 62, 47);
		AllGuiTextures.JEI_DOWN_ARROW.render(graphics, 74, 10);
		spout.withFluids(List.of(new FluidStack(Fluids.WATER, SquidPrinterBlockEntity.CYCLE_WATER_COST)))
			.draw(graphics, WIDTH / 2 - 13, 14);
		SquidJeiRenderer.render(graphics, 89, 47, 9.0f);
	}

	@Override
	public void getTooltip(ITooltipBuilder tooltip, SquidPrinterJeiRecipe recipe, IRecipeSlotsView recipeSlotsView,
		double mouseX, double mouseY) {
		if (mouseX < 58 || mouseX > 118 || mouseY < 8 || mouseY > 56)
			return;
		for (Component note : recipe.notes()) {
			tooltip.add(note.copy().withStyle(ChatFormatting.GRAY));
		}
	}

	@Override
	public ResourceLocation getRegistryName(SquidPrinterJeiRecipe recipe) {
		return recipe.id();
	}
}
