package com.nobodiiiii.createbiotech.content.fluid;

import java.util.function.Consumer;

import com.nobodiiiii.createbiotech.CreateBiotech;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions;
import net.minecraftforge.fluids.FluidType;

public class LiquidLivingSlimeFluidType extends FluidType {

	public LiquidLivingSlimeFluidType(Properties properties) {
		super(properties);
	}

	@Override
	public void initializeClient(Consumer<IClientFluidTypeExtensions> consumer) {
		consumer.accept(new IClientFluidTypeExtensions() {

			@Override
			public ResourceLocation getStillTexture() {
				return new ResourceLocation("create_biotech", "fluid/liquid_living_slime_still");
			}

			@Override
			public ResourceLocation getFlowingTexture() {
				return new ResourceLocation("create_biotech", "fluid/liquid_living_slime_flow");
			}

			@Override
			public int getTintColor() {
				return 0xFFFFFFFF;
			}
		});
	}
}
