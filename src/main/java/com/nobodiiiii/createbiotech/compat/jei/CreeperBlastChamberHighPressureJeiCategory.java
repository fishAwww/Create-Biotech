package com.nobodiiiii.createbiotech.compat.jei;

import java.util.List;

import com.nobodiiiii.createbiotech.CreateBiotech;
import com.nobodiiiii.createbiotech.registry.CBBlocks;
import com.simibubi.create.compat.jei.EmptyBackground;
import com.simibubi.create.compat.jei.ItemIcon;
import com.simibubi.create.compat.jei.category.CreateRecipeCategory;
import com.simibubi.create.compat.jei.category.PressingCategory;
import com.simibubi.create.content.kinetics.press.PressingRecipe;
import com.simibubi.create.content.processing.recipe.ProcessingOutput;
import com.simibubi.create.foundation.gui.AllGuiTextures;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

public class CreeperBlastChamberHighPressureJeiCategory extends PressingCategory {
	public static final RecipeType<PressingRecipe> TYPE =
		RecipeType.create(CreateBiotech.MOD_ID, "creeper_blast_chamber_high_pressure", PressingRecipe.class);
	private static final HighPressureCreeperDrawable HIGH_PRESSURE_CREEPER =
		new HighPressureCreeperDrawable(46, 42, 20, -0.5625f, -0.2f, 13, 39, 1.2f, 1f / 1.8f, 24);

	public CreeperBlastChamberHighPressureJeiCategory() {
		super(new CreateRecipeCategory.Info<>(TYPE,
			Component.translatable("create_biotech.recipe.creeper_blast_chamber_high_pressure"),
			new EmptyBackground(177, 70),
			new ItemIcon(() -> new ItemStack(CBBlocks.CREEPER_BLAST_CHAMBER.get())),
			List::of,
			List.of()));
	}

	@Override
	public void setRecipe(IRecipeLayoutBuilder builder, PressingRecipe recipe, IFocusGroup focuses) {
		builder.addSlot(RecipeIngredientRole.CATALYST, 51, 5)
			.setBackground(getRenderedSlot(), -1, -1)
			.addItemStack(new ItemStack(CBBlocks.CREEPER_BLAST_CHAMBER.get()));

		builder.addSlot(RecipeIngredientRole.INPUT, 27, 51)
			.setBackground(getRenderedSlot(), -1, -1)
			.addIngredients(recipe.getIngredients().get(0));

		List<ProcessingOutput> results = recipe.getRollableResults();
		boolean single = results.size() == 1;
		for (int i = 0; i < results.size(); i++) {
			ProcessingOutput output = results.get(i);
			int xOffset = i % 2 == 0 ? 0 : 19;
			int yOffset = (i / 2) * -19;
			builder.addSlot(RecipeIngredientRole.OUTPUT, single ? 132 : 132 + xOffset, 51 + yOffset)
				.setBackground(getRenderedSlot(output), -1, -1)
				.addItemStack(output.getStack())
				.addRichTooltipCallback(addStochasticTooltip(output));
		}
	}

	@Override
	public void draw(PressingRecipe recipe, mezz.jei.api.gui.ingredient.IRecipeSlotsView recipeSlotsView,
		GuiGraphics graphics, double mouseX, double mouseY) {
		AllGuiTextures.JEI_SHADOW.render(graphics, 62, 57);
		AllGuiTextures.JEI_DOWN_ARROW.render(graphics, 126, 29 + (recipe.getRollableResults().size() > 2 ? -19 : 0));
		HIGH_PRESSURE_CREEPER.draw(graphics, getBackground().getWidth() / 2 - 13, 22);
	}
}
