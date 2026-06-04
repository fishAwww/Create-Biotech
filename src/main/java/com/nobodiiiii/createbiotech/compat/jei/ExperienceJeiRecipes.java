package com.nobodiiiii.createbiotech.compat.jei;

import java.util.List;

import com.nobodiiiii.createbiotech.CreateBiotech;
import com.nobodiiiii.createbiotech.registry.CBItems;
import com.simibubi.create.AllItems;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

public final class ExperienceJeiRecipes {

	private ExperienceJeiRecipes() {
	}

	public static List<ExperiencePumpJeiRecipe> pump() {
		ItemStack virtualExperience = new ItemStack(CBItems.EXPERIENCE.get());
		return List.of(
			new ExperiencePumpJeiRecipe(CreateBiotech.asResource("experience_pump/nugget_extraction"),
				new ItemStack(AllItems.EXP_NUGGET.get()), virtualExperience.copy(),
				List.of(Component.translatable("create_biotech.jei.experience_pump.note.nuggets"))),
			new ExperiencePumpJeiRecipe(CreateBiotech.asResource("experience_pump/open_output"),
				virtualExperience.copy(), virtualExperience.copy(),
				List.of(Component.translatable("create_biotech.jei.experience_pump.note.open_output"))));
	}
}
