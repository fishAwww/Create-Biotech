package com.nobodiiiii.createbiotech.compat.jei;

import java.util.ArrayList;
import java.util.List;

import com.nobodiiiii.createbiotech.CreateBiotech;
import com.nobodiiiii.createbiotech.content.squidprinter.EnchantmentBookCopyItem;
import com.nobodiiiii.createbiotech.content.squidprinter.SquidPrinterRecipe;
import com.nobodiiiii.createbiotech.registry.CBItems;
import com.nobodiiiii.createbiotech.registry.CBRecipeTypes;
import com.simibubi.create.content.fluids.transfer.FillingRecipe;
import com.simibubi.create.content.kinetics.deployer.ItemApplicationRecipe;
import com.simibubi.create.content.kinetics.deployer.ManualApplicationRecipe;
import com.simibubi.create.content.processing.recipe.ProcessingRecipeBuilder;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.EnchantedBookItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.registries.ForgeRegistries;

public final class SquidPrinterJeiRecipes {

	private static final String ITEM_APPLICATION_PREFIX = "item_application/squid_printer/";
	private static final String SPOUT_FILLING_PREFIX = "spout_filling/squid_printer/";

	private SquidPrinterJeiRecipes() {
	}

	public static List<SquidPrinterJeiRecipe> create() {
		List<SquidPrinterRecipe> recipes = getRecipes();
		if (recipes.isEmpty())
			return List.of();

		List<ItemStack> templateBooks = createTemplateSamples();
		List<ItemStack> outputCopies = templateBooks.stream()
			.map(template -> EnchantmentBookCopyItem.fromTemplate(template, CBItems.ENCHANTMENT_BOOK_COPY.get()))
			.toList();

		List<SquidPrinterJeiRecipe> displays = new ArrayList<>(recipes.size());
		for (SquidPrinterRecipe recipe : recipes) {
			List<Component> notes = List.of(
				Component.translatable("create_biotech.jei.squid_printer.note.template"),
				Component.translatable("create_biotech.jei.squid_printer.note.water_cost", recipe.getWaterPerLevel(),
					recipe.getTicksPerLevel()));
			displays.add(new SquidPrinterJeiRecipe(recipe.getId(), new ItemStack(Items.BOOK), templateBooks,
				outputCopies, notes));
		}
		return displays;
	}

	public static List<ItemApplicationRecipe> createItemApplicationRecipes() {
		ItemStack previewOutput = getPreviewOutput();
		return getRecipes().stream()
			.map(recipe -> new ProcessingRecipeBuilder<>(ManualApplicationRecipe::new,
				CreateBiotech.asResource(ITEM_APPLICATION_PREFIX + recipe.getId().getPath()))
					.require(recipe.getIngredients()
						.get(0))
					.require(Items.ENCHANTED_BOOK)
					.output(previewOutput.copy())
					.toolNotConsumed()
					.build())
			.map(recipe -> (ItemApplicationRecipe) recipe)
			.toList();
	}

	public static List<FillingRecipe> createSpoutRecipes() {
		ItemStack previewOutput = getPreviewOutput();
		return getRecipes().stream()
			.map(recipe -> new ProcessingRecipeBuilder<>(FillingRecipe::new,
				CreateBiotech.asResource(SPOUT_FILLING_PREFIX + recipe.getId().getPath()))
					.withItemIngredients(recipe.getIngredients())
					.withFluidIngredients(recipe.getFluidIngredients())
					.withSingleItemOutput(previewOutput.copy())
					.build())
			.toList();
	}

	public static boolean isSquidPrinterItemApplication(ResourceLocation id) {
		return id.getNamespace()
			.equals(CreateBiotech.MOD_ID) && id.getPath()
				.startsWith(ITEM_APPLICATION_PREFIX);
	}

	public static boolean isSquidPrinterSpoutFilling(ResourceLocation id) {
		return id.getNamespace()
			.equals(CreateBiotech.MOD_ID) && id.getPath()
				.startsWith(SPOUT_FILLING_PREFIX);
	}

	private static ItemStack getPreviewOutput() {
		List<ItemStack> templates = createTemplateSamples();
		if (templates.isEmpty())
			return new ItemStack(CBItems.ENCHANTMENT_BOOK_COPY.get());
		return EnchantmentBookCopyItem.fromTemplate(templates.get(0), CBItems.ENCHANTMENT_BOOK_COPY.get());
	}

	private static List<SquidPrinterRecipe> getRecipes() {
		ClientPacketListener connection = Minecraft.getInstance()
			.getConnection();
		if (connection == null)
			return List.of();
		return connection.getRecipeManager()
			.getAllRecipesFor(CBRecipeTypes.SQUID_PRINTER_TYPE.get());
	}

	private static List<ItemStack> createTemplateSamples() {
		List<ItemStack> templates = new ArrayList<>();
		for (ResourceLocation enchantmentId : ForgeRegistries.ENCHANTMENTS.getKeys()
			.stream()
			.sorted()
			.toList()) {
			Enchantment enchantment = ForgeRegistries.ENCHANTMENTS.getValue(enchantmentId);
			if (enchantment == null)
				continue;
			int level = Math.max(1, enchantment.getMaxLevel());
			ItemStack template = new ItemStack(Items.ENCHANTED_BOOK);
			EnchantedBookItem.addEnchantment(template, new EnchantmentInstance(enchantment, level));
			templates.add(template);
		}
		if (templates.isEmpty())
			templates.add(new ItemStack(Items.ENCHANTED_BOOK));
		return templates;
	}
}
