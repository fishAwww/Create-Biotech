package com.nobodiiiii.createbiotech.content.creeperblastchamber;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.nobodiiiii.createbiotech.content.cardboardbox.CapturedEntityBoxRecipeHelper;
import com.nobodiiiii.createbiotech.registry.CBRecipeTypes;
import com.simibubi.create.content.processing.recipe.HeatCondition;
import com.simibubi.create.content.processing.recipe.ProcessingOutput;
import com.simibubi.create.content.processing.recipe.ProcessingRecipe;
import com.simibubi.create.content.processing.recipe.ProcessingRecipeBuilder;
import com.simibubi.create.content.processing.recipe.ProcessingRecipeBuilder.ProcessingRecipeParams;
import com.simibubi.create.content.processing.recipe.ProcessingRecipeSerializer;
import com.simibubi.create.foundation.fluid.FluidHelper;
import com.simibubi.create.foundation.fluid.FluidIngredient;
import com.simibubi.create.foundation.recipe.IRecipeTypeInfo;

import net.minecraft.core.NonNullList;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.items.wrapper.RecipeWrapper;
import net.minecraftforge.registries.ForgeRegistries;

public class CreeperBlastChamberHighPressureRecipe extends ProcessingRecipe<RecipeWrapper> {

	private static final IRecipeTypeInfo TYPE_INFO = new IRecipeTypeInfo() {
		@Override
		public ResourceLocation getId() {
			return CBRecipeTypes.CREEPER_BLAST_CHAMBER_HIGH_PRESSURE_TYPE.getId();
		}

		@Override
		public <T extends net.minecraft.world.item.crafting.RecipeSerializer<?>> T getSerializer() {
			return (T) CBRecipeTypes.CREEPER_BLAST_CHAMBER_HIGH_PRESSURE_SERIALIZER.get();
		}

		@Override
		public <T extends net.minecraft.world.item.crafting.RecipeType<?>> T getType() {
			return (T) CBRecipeTypes.CREEPER_BLAST_CHAMBER_HIGH_PRESSURE_TYPE.get();
		}
	};

	private static final RandomSource RANDOM = RandomSource.create();

	@Nullable
	private EntityType<?> capturedEntityType;
	private final List<ResultCountRange> resultCountRanges = new ArrayList<>();

	public CreeperBlastChamberHighPressureRecipe(ProcessingRecipeParams params) {
		super(TYPE_INFO, params);
	}

	@Override
	public boolean matches(RecipeWrapper inv, Level worldIn) {
		if (inv.isEmpty())
			return false;

		ItemStack stack = inv.getItem(0);
		if (capturedEntityType != null)
			return CapturedEntityBoxRecipeHelper.matchesCapturedEntity(stack, capturedEntityType);

		return !ingredients.isEmpty() && ingredients.get(0).test(stack);
	}

	@Override
	protected int getMaxInputCount() {
		return 1;
	}

	@Override
	protected int getMaxOutputCount() {
		return 8;
	}

	@Override
	protected boolean canSpecifyDuration() {
		return true;
	}

	@Override
	public List<ItemStack> rollResults() {
		return rollResults(getRollableResults());
	}

	@Override
	public List<ItemStack> rollResults(List<ProcessingOutput> rollableResults) {
		List<ItemStack> rolledResults = new ArrayList<>();
		for (int i = 0; i < rollableResults.size(); i++) {
			ProcessingOutput output = rollableResults.get(i);
			ResultCountRange range = getResultCountRange(i);

			ItemStack stack;
			if (range == null) {
				stack = output.rollOutput();
			} else {
				if (RANDOM.nextFloat() > output.getChance())
					continue;
				stack = output.getStack().copy();
				stack.setCount(range.roll(RANDOM));
			}

			if (!stack.isEmpty())
				rolledResults.add(stack);
		}
		return rolledResults;
	}

	@Override
	public void readAdditional(JsonObject json) {
		capturedEntityType = null;
		if (GsonHelper.isValidNode(json, "captured_entity")) {
			ResourceLocation entityId = new ResourceLocation(GsonHelper.getAsString(json, "captured_entity"));
			capturedEntityType = ForgeRegistries.ENTITY_TYPES.getValue(entityId);
			if (capturedEntityType == null)
				throw new JsonSyntaxException("Unknown captured_entity: " + entityId);
			ingredients = NonNullList.of(Ingredient.EMPTY, CapturedEntityBoxRecipeHelper.displayIngredient(capturedEntityType));
		}

		resultCountRanges.clear();
		JsonArray resultsJson = GsonHelper.getAsJsonArray(json, "results");
		for (JsonElement resultElement : resultsJson) {
			JsonObject resultObject = GsonHelper.convertToJsonObject(resultElement, "result");
			if (GsonHelper.isValidNode(resultObject, "fluid"))
				continue;

			int countMin = GsonHelper.getAsInt(resultObject, "count_min",
				GsonHelper.getAsInt(resultObject, "count", 1));
			int countMax = GsonHelper.getAsInt(resultObject, "count_max", countMin);
			if (!GsonHelper.isValidNode(resultObject, "count_min") && !GsonHelper.isValidNode(resultObject, "count_max")) {
				resultCountRanges.add(null);
				continue;
			}
			if (countMin <= 0 || countMax < countMin)
				throw new JsonSyntaxException("Invalid count range for recipe " + id + ": " + countMin + "-" + countMax);
			resultCountRanges.add(new ResultCountRange(countMin, countMax));
		}
	}

	@Override
	public void writeAdditional(JsonObject json) {
		if (capturedEntityType != null) {
			ResourceLocation entityId = ForgeRegistries.ENTITY_TYPES.getKey(capturedEntityType);
			if (entityId != null)
				json.addProperty("captured_entity", entityId.toString());
		}

		if (!json.has("results"))
			return;

		JsonArray resultsJson = json.getAsJsonArray("results");
		int itemResultIndex = 0;
		for (JsonElement resultElement : resultsJson) {
			JsonObject resultObject = resultElement.getAsJsonObject();
			if (GsonHelper.isValidNode(resultObject, "fluid"))
				continue;
			ResultCountRange range = getResultCountRange(itemResultIndex++);
			if (range == null)
				continue;
			resultObject.addProperty("count_min", range.min());
			resultObject.addProperty("count_max", range.max());
		}
	}

	@Override
	public void readAdditional(FriendlyByteBuf buffer) {
		capturedEntityType = null;
		if (buffer.readBoolean()) {
			ResourceLocation entityId = buffer.readResourceLocation();
			capturedEntityType = ForgeRegistries.ENTITY_TYPES.getValue(entityId);
			if (capturedEntityType != null)
				ingredients = NonNullList.of(Ingredient.EMPTY, CapturedEntityBoxRecipeHelper.displayIngredient(capturedEntityType));
		}

		resultCountRanges.clear();
		int rangeCount = buffer.readVarInt();
		for (int i = 0; i < rangeCount; i++) {
			if (!buffer.readBoolean()) {
				resultCountRanges.add(null);
				continue;
			}
			resultCountRanges.add(new ResultCountRange(buffer.readVarInt(), buffer.readVarInt()));
		}
	}

	@Override
	public void writeAdditional(FriendlyByteBuf buffer) {
		ResourceLocation entityId = capturedEntityType == null ? null : ForgeRegistries.ENTITY_TYPES.getKey(capturedEntityType);
		buffer.writeBoolean(entityId != null);
		if (entityId != null)
			buffer.writeResourceLocation(entityId);

		buffer.writeVarInt(results.size());
		for (int i = 0; i < results.size(); i++) {
			ResultCountRange range = getResultCountRange(i);
			buffer.writeBoolean(range != null);
			if (range != null) {
				buffer.writeVarInt(range.min());
				buffer.writeVarInt(range.max());
			}
		}
	}

	@Nullable
	public ResultCountRange getResultCountRange(int index) {
		return index >= 0 && index < resultCountRanges.size() ? resultCountRanges.get(index) : null;
	}

	public static class Serializer extends ProcessingRecipeSerializer<CreeperBlastChamberHighPressureRecipe> {

		public Serializer() {
			super(CreeperBlastChamberHighPressureRecipe::new);
		}

		@Override
		protected CreeperBlastChamberHighPressureRecipe readFromJson(ResourceLocation recipeId, JsonObject json) {
			ProcessingRecipeBuilder<CreeperBlastChamberHighPressureRecipe> builder =
				new ProcessingRecipeBuilder<>(CreeperBlastChamberHighPressureRecipe::new, recipeId);
			NonNullList<Ingredient> ingredients = NonNullList.create();
			NonNullList<FluidIngredient> fluidIngredients = NonNullList.create();
			NonNullList<ProcessingOutput> results = NonNullList.create();
			NonNullList<FluidStack> fluidResults = NonNullList.create();

			if (GsonHelper.isValidNode(json, "ingredients")) {
				for (JsonElement ingredientElement : GsonHelper.getAsJsonArray(json, "ingredients")) {
					if (FluidIngredient.isFluidIngredient(ingredientElement))
						fluidIngredients.add(FluidIngredient.deserialize(ingredientElement));
					else
						ingredients.add(Ingredient.fromJson(ingredientElement));
				}
			} else if (GsonHelper.isValidNode(json, "captured_entity")) {
				ingredients.add(CapturedEntityBoxRecipeHelper.anyBoxIngredient());
			} else {
				throw new JsonSyntaxException("High-pressure implosion recipes require either ingredients or captured_entity");
			}

			for (JsonElement resultElement : GsonHelper.getAsJsonArray(json, "results")) {
				JsonObject resultObject = resultElement.getAsJsonObject();
				if (GsonHelper.isValidNode(resultObject, "fluid"))
					fluidResults.add(FluidHelper.deserializeFluidStack(resultObject));
				else
					results.add(ProcessingOutput.deserialize(resultElement));
			}

			builder.withItemIngredients(ingredients)
				.withItemOutputs(results)
				.withFluidIngredients(fluidIngredients)
				.withFluidOutputs(fluidResults);

			if (GsonHelper.isValidNode(json, "processingTime"))
				builder.duration(GsonHelper.getAsInt(json, "processingTime"));
			if (GsonHelper.isValidNode(json, "heatRequirement"))
				builder.requiresHeat(HeatCondition.deserialize(GsonHelper.getAsString(json, "heatRequirement")));

			CreeperBlastChamberHighPressureRecipe recipe = builder.build();
			recipe.readAdditional(json);
			return recipe;
		}
	}

	public record ResultCountRange(int min, int max) {
		public int roll(RandomSource random) {
			return min + random.nextInt(max - min + 1);
		}
	}
}
