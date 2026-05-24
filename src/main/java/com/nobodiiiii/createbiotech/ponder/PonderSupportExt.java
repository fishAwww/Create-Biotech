package com.nobodiiiii.createbiotech.ponder;

import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.gauge.SpeedGaugeBlockEntity;
import com.simibubi.create.content.kinetics.press.MechanicalPressBlockEntity;
import com.simibubi.create.content.kinetics.press.PressingBehaviour;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import net.createmod.ponder.api.element.ElementLink;
import net.createmod.ponder.api.element.EntityElement;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.Selection;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.TagParser;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class PonderSupportExt {
    private static final String FORGE_DATA_ROOT = "create_biotech";

    private PonderSupportExt() {
    }

    public static final class EntityAnimContext {
        final Map<String, ElementLink<EntityElement>> entityLinks = new HashMap<>();
    }

    public static EntityAnimContext newEntityAnimContext() {
        return new EntityAnimContext();
    }

    public static void createEntityWithDropIn(SceneBuilder scene, EntityAnimContext context, String entityKey,
                                              String entityId, Vec3 pos, Vec3 lookAt,
                                              Float yaw, Float pitch, String nbt,
                                              double dropHeight, int dropDurationTicks) {
        ResourceLocation loc = entityId == null ? null : ResourceLocation.tryParse(entityId);
        if (loc == null) {
            return;
        }
        Vec3 startPos = pos.add(0, dropHeight, 0);
        Float yawCap = yaw;
        Float pitchCap = pitch;
        String nbtCap = nbt;
        Vec3 lookAtCap = lookAt;
        ElementLink<EntityElement> link = scene.world().createEntity((Level level) -> {
            EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.getOptional(loc).orElse(null);
            if (type == null) {
                return null;
            }
            Entity entity = type.create(level);
            if (entity == null) {
                return null;
            }
            entity.setPosRaw(startPos.x, startPos.y, startPos.z);
            entity.setOldPosAndRot();
            Vec3 targetLook = lookAtCap == null ? startPos.add(0, 0, -1) : lookAtCap;
            entity.lookAt(EntityAnchorArgument.Anchor.FEET, targetLook);
            if (yawCap != null) {
                entity.setYRot(yawCap);
                entity.setYHeadRot(yawCap);
                entity.setYBodyRot(yawCap);
            }
            if (pitchCap != null) {
                entity.setXRot(pitchCap);
            }
            if (entity instanceof Mob mob) {
                mob.setNoAi(true);
            }
            entity.setNoGravity(true);
            entity.setDeltaMovement(Vec3.ZERO);
            if (nbtCap != null && !nbtCap.isBlank()) {
                try {
                    CompoundTag patch = TagParser.parseTag(nbtCap);
                    CompoundTag data = new CompoundTag();
                    entity.saveWithoutId(data);
                    data.merge(patch);
                    entity.load(data);
                } catch (Exception ignored) {
                }
            }
            return entity;
        });
        if (entityKey != null && !entityKey.isBlank() && link != null) {
            context.entityLinks.put(entityKey, link);
        }
        int duration = Math.max(1, dropDurationTicks);
        for (int i = 1; i <= duration; i++) {
            final double progress = (double) i / duration;
            final double targetY = startPos.y - dropHeight * progress;
            scene.world().modifyEntity(link, e -> {
                if (e == null) {
                    return;
                }
                double curX = e.getX();
                double curZ = e.getZ();
                e.xo = curX;
                e.yo = e.getY();
                e.zo = curZ;
                e.setPos(curX, targetY, curZ);
            });
            scene.idle(1);
        }
    }

    public static void clearEntitiesWithRiseOut(SceneBuilder scene, EntityAnimContext context,
                                                double riseHeight, int riseDurationTicks) {
        if (context.entityLinks.isEmpty()) {
            scene.world().modifyEntities(Entity.class, Entity::discard);
            return;
        }
        int duration = Math.max(1, riseDurationTicks);
        List<ElementLink<EntityElement>> links = new ArrayList<>(context.entityLinks.values());
        double perTickRise = riseHeight / duration;
        for (int i = 1; i <= duration; i++) {
            for (ElementLink<EntityElement> link : links) {
                scene.world().modifyEntity(link, e -> {
                    if (e == null) {
                        return;
                    }
                    double curX = e.getX();
                    double curY = e.getY();
                    double curZ = e.getZ();
                    e.xo = curX;
                    e.yo = curY;
                    e.zo = curZ;
                    e.setPos(curX, curY + perTickRise, curZ);
                });
            }
            scene.idle(1);
        }
        for (ElementLink<EntityElement> link : links) {
            scene.world().modifyEntity(link, Entity::discard);
        }
        context.entityLinks.clear();
    }

    public static void playWalkAnimation(SceneBuilder scene, EntityAnimContext context,
                                         Map<String, Float> entitySpeeds, int durationTicks) {
        if (entitySpeeds == null || entitySpeeds.isEmpty() || durationTicks <= 0) {
            if (durationTicks > 0) {
                scene.idle(durationTicks);
            }
            return;
        }
        for (int t = 0; t < durationTicks; t++) {
            for (Map.Entry<String, Float> entry : entitySpeeds.entrySet()) {
                ElementLink<EntityElement> link = context.entityLinks.get(entry.getKey());
                if (link == null) {
                    continue;
                }
                final float speed = entry.getValue() == null ? 0f : entry.getValue();
                scene.world().modifyEntity(link, e -> {
                    if (e instanceof LivingEntity living) {
                        living.walkAnimation.setSpeed(speed);
                        living.walkAnimation.update(speed, 1.0f);
                    }
                });
            }
            scene.idle(1);
        }
    }

    public static void markSmartBlockEntitiesVirtual(SceneBuilder scene, Selection selection) {
        scene.addInstruction(ponderScene -> selection.forEach(pos -> {
            BlockEntity blockEntity = ponderScene.getWorld().getBlockEntity(pos);
            if (blockEntity instanceof SmartBlockEntity smartBlockEntity) {
                smartBlockEntity.markVirtual();
            }
        }));
    }

    public static void emitParticles(SceneBuilder scene, String particleId, Vec3 location, Vec3 motion,
                                     float amountPerCycle, int cycles) {
        if (particleId == null || location == null) {
            return;
        }
        ResourceLocation loc = ResourceLocation.tryParse(particleId);
        if (loc == null) {
            return;
        }
        ParticleType<?> type = BuiltInRegistries.PARTICLE_TYPE.getOptional(loc).orElse(null);
        if (!(type instanceof SimpleParticleType simple)) {
            return;
        }
        Vec3 vel = motion == null ? Vec3.ZERO : motion;
        var emitter = scene.effects().simpleParticleEmitter(simple, vel);
        scene.effects().emitParticles(location, emitter, amountPerCycle, Math.max(1, cycles));
    }

    public static void startCompressionAnimation(SceneBuilder scene, String entityId,
                                                 int rtStart, int kineticSpeed, float peak) {
        ResourceLocation filter = entityId == null || entityId.isBlank() ? null : ResourceLocation.tryParse(entityId);
        int tickSpeed = kineticSpeed == 0 ? 0
            : (int) Mth.lerp(Mth.clamp(Math.abs(kineticSpeed) / 512f, 0f, 1f), 1f, 60f);
        scene.world().modifyEntities(Creeper.class, creeper -> {
            if (filter != null && !EntityType.getKey(creeper.getType()).equals(filter)) {
                return;
            }
            CompoundTag animTag = new CompoundTag();
            animTag.putInt("StartTick", creeper.tickCount);
            animTag.putInt("RtStart", rtStart);
            animTag.putInt("TickSpeed", tickSpeed);
            animTag.putFloat("Peak", peak);
            CompoundTag forgeData = creeper.getPersistentData();
            CompoundTag biotech = forgeData.contains(FORGE_DATA_ROOT, Tag.TAG_COMPOUND)
                ? forgeData.getCompound(FORGE_DATA_ROOT) : new CompoundTag();
            biotech.put("PonderCompressionAnim", animTag);
            biotech.remove("PonderCompression");
            forgeData.put(FORGE_DATA_ROOT, biotech);
        });
    }

    public static void clearCompressionAnimation(SceneBuilder scene, String entityId) {
        ResourceLocation filter = entityId == null || entityId.isBlank() ? null : ResourceLocation.tryParse(entityId);
        scene.world().modifyEntities(Creeper.class, creeper -> {
            if (filter != null && !EntityType.getKey(creeper.getType()).equals(filter)) {
                return;
            }
            CompoundTag forgeData = creeper.getPersistentData();
            if (!forgeData.contains(FORGE_DATA_ROOT, Tag.TAG_COMPOUND)) {
                return;
            }
            CompoundTag biotech = forgeData.getCompound(FORGE_DATA_ROOT);
            biotech.remove("PonderCompressionAnim");
            biotech.remove("PonderCompression");
        });
    }

    public static void startPressCycle(SceneBuilder scene, BlockPos pos1, BlockPos pos2, float kineticSpeed) {
        BlockPos posMin = new BlockPos(
            Math.min(pos1.getX(), pos2.getX()),
            Math.min(pos1.getY(), pos2.getY()),
            Math.min(pos1.getZ(), pos2.getZ()));
        BlockPos posMax = new BlockPos(
            Math.max(pos1.getX(), pos2.getX()),
            Math.max(pos1.getY(), pos2.getY()),
            Math.max(pos1.getZ(), pos2.getZ()));
        Selection selection = scene.getScene().getSceneBuildingUtil().select().fromTo(posMin, posMax);
        scene.world().modifyBlockEntityNBT(selection, SpeedGaugeBlockEntity.class,
            nbt -> nbt.putFloat("Value", SpeedGaugeBlockEntity.getDialTarget(kineticSpeed)));
        scene.world().modifyBlockEntityNBT(selection, KineticBlockEntity.class,
            nbt -> nbt.putFloat("Speed", kineticSpeed));
        markSmartBlockEntitiesVirtual(scene, selection);
        for (BlockPos pos : BlockPos.betweenClosed(posMin, posMax)) {
            BlockPos copy = pos.immutable();
            scene.world().modifyBlockEntity(copy, MechanicalPressBlockEntity.class,
                press -> press.getPressingBehaviour().start(PressingBehaviour.Mode.WORLD));
        }
    }
}
