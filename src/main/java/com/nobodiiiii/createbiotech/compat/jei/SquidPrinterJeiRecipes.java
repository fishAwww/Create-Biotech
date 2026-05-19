package com.nobodiiiii.createbiotech.compat.jei;

import java.util.ArrayList;
import java.util.List;

import com.nobodiiiii.createbiotech.CreateBiotech;
import com.nobodiiiii.createbiotech.content.squidprinter.EnchantmentBookCopyItem;
import com.nobodiiiii.createbiotech.content.squidprinter.SquidPrinterRecipe;
import com.nobodiiiii.createbiotech.registry.CBItems;
import com.nobodiiiii.createbiotech.registry.CBRecipeTypes;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
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
			displays.add(new SquidPrinterJeiRecipe(recipe.getId(), new ItemStack(Items.BOOK), recipe.getRequiredFluid(),
				templateBooks, outputCopies));
		}
		return displays;
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
