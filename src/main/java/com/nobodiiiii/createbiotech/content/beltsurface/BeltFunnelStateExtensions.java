package com.nobodiiiii.createbiotech.content.beltsurface;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.properties.DirectionProperty;

/**
 * Block-state properties added to vanilla {@link com.simibubi.create.content.logistics.funnel.BeltFunnelBlock} via mixin
 * so we can disambiguate the funnel's attached surface without an extra block-entity NBT field.
 * <p>
 * <strong>{@link #ATTACHMENT_SURFACE}</strong> — the direction of the funnel's face that touches the belt
 * (i.e. the side of the funnel block that the belt sits on). Six values; default {@code DOWN}, which corresponds
 * to the canonical vanilla geometry (funnel sits on top of a horizontal belt, attached via its bottom face).
 * <p>
 * Combined with {@code HORIZONTAL_FACING}, this fully encodes the funnel's world orientation:
 * {@code worldFacing = BeltSurface.worldizeCanonical(HORIZONTAL_FACING, ATTACHMENT_SURFACE.opposite())}.
 * When {@code ATTACHMENT_SURFACE == DOWN} (outward = UP) the mapping is the identity, so vanilla / Ponder code
 * that writes only {@code HORIZONTAL_FACING} keeps its original world-frame semantics for free.
 */
public final class BeltFunnelStateExtensions {

	public static final DirectionProperty ATTACHMENT_SURFACE =
		DirectionProperty.create("cb_attachment_surface", Direction.values());

	private BeltFunnelStateExtensions() {}
}
