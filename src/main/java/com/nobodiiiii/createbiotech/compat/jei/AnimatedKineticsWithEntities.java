package com.nobodiiiii.createbiotech.compat.jei;

import com.nobodiiiii.createbiotech.foundation.gui.GuiEntityElement;
import com.simibubi.create.compat.jei.category.animations.AnimatedKinetics;

import net.minecraft.world.entity.Entity;

public abstract class AnimatedKineticsWithEntities extends AnimatedKinetics {

	protected static <T extends Entity> GuiEntityElement.GuiEntityRenderBuilder<T> defaultEntityElement(T entity) {
		return GuiEntityElement.of(entity)
			.lighting(DEFAULT_LIGHTING);
	}

	protected <T extends Entity> GuiEntityElement.GuiEntityRenderBuilder<T> entityElement(T entity) {
		return defaultEntityElement(entity);
	}
}
