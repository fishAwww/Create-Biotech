package com.nobodiiiii.createbiotech.content.beltsurface;

import javax.annotation.Nullable;

import com.simibubi.create.content.logistics.funnel.BeltFunnelBlock;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Single entry point for "what belt surface is the funnel at {@code funnelPos} attached to?".
 * <p>
 * Two resolution paths:
 * <ol>
 *   <li><strong>Fast path</strong> — when the block at {@code funnelPos} is already a {@link BeltFunnelBlock}, the
 *       attached surface is encoded directly in the block state via
 *       {@link BeltFunnelStateExtensions#ATTACHMENT_SURFACE}. The belt may or may not still exist; we still get a
 *       valid {@link BeltSurface} for {@code localize/worldize/transformShape} purposes. Movement direction is
 *       queried from the (possibly missing) belt neighbour; if absent, falls back to the canonical forward —
 *       behavior consumers that need an exact movement must guard for the {@code null} return of
 *       {@link BeltSurfaceHost#surfaceFor}.</li>
 *   <li><strong>Scan path</strong> — for plain {@link com.simibubi.create.content.logistics.funnel.FunnelBlock}s
 *       being considered for placement, no state encoding exists yet. Scan the six orthogonal neighbours for a
 *       {@link BeltSurfaceHost} that exposes a matching surface.</li>
 * </ol>
 */
public final class BeltSurfaceResolver {

	private BeltSurfaceResolver() {}

	@Nullable
	public static BeltSurface resolve(BlockGetter world, BlockPos funnelPos) {
		if (world == null || funnelPos == null)
			return null;

		// Fast path: BeltFunnel state already encodes the attached surface.
		BlockState selfState = world.getBlockState(funnelPos);
		if (selfState.getBlock() instanceof BeltFunnelBlock) {
			BeltSurface fromState = resolveFromBeltFunnelState(world, funnelPos, selfState);
			if (fromState != null)
				return fromState;
			// Fall through to scan in case ATTACHMENT_SURFACE happens to be at its default and a real
			// surface exists in a different direction — preserves vanilla "funnel on top of horizontal belt".
		}

		// Scan path: find a host whose outward normal points back at the funnel.
		for (Direction d : Direction.values()) {
			BlockPos neighbourPos = funnelPos.relative(d);
			if (world instanceof Level level && !level.isLoaded(neighbourPos))
				continue;
			BlockEntity be = world.getBlockEntity(neighbourPos);
			if (!(be instanceof BeltSurfaceHost host))
				continue;
			BeltSurface s = host.surfaceFor(d.getOpposite());
			if (s != null)
				return s;
		}
		return null;
	}

	@Nullable
	private static BeltSurface resolveFromBeltFunnelState(BlockGetter world, BlockPos funnelPos, BlockState state) {
		Direction attachment = state.getOptionalValue(BeltFunnelStateExtensions.ATTACHMENT_SURFACE).orElse(null);
		if (attachment == null)
			return null;
		Direction outwardNormal = attachment.getOpposite();
		BlockPos beltPos = funnelPos.relative(attachment);

		// Try to find the belt host so we can carry a real movementFacing for behaviour decisions
		// (mode determination, blocking-vs-perpendicular runtime checks). If the belt is gone, fall back
		// to the canonical forward so the constructor invariant (outwardNormal.axis != movementFacing.axis)
		// still holds — but consumers that actually rely on movementFacing during a tick will have already
		// short-circuited via their own null/state checks before reaching this point.
		BeltSurfaceHost host = null;
		Direction movementFacing = BeltSurface.canonicalForward(outwardNormal);
		int segmentIndex = 0;
		if (!(world instanceof Level level) || level.isLoaded(beltPos)) {
			BlockEntity be = world.getBlockEntity(beltPos);
			if (be instanceof BeltSurfaceHost h) {
				BeltSurface live = h.surfaceFor(outwardNormal);
				if (live != null) {
					host = h;
					movementFacing = live.movementFacing();
					segmentIndex = live.segmentIndex();
				}
			}
		}
		return BeltSurface.of(host, beltPos, segmentIndex, outwardNormal, movementFacing);
	}
}
