package com.sibcraft.creeperborn;

import java.util.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.network.chat.Component;

public final class ElementalCreepers {
    private final Map<UUID, Creeper> loaded = new HashMap<>();
    private final Map<UUID, Long> attacks = new HashMap<>();
    public void clear() { loaded.clear(); attacks.clear(); }
    public void track(Entity entity) {
        if (!(entity instanceof Creeper c) || !(c.level() instanceof ServerLevel level)) return;
        if (!FruitCreepers.fruit(c) && element(c) == null && !c.entityTags().contains("cb_checked") && !c.entityTags().contains("ns_creeper")) {
            c.addTag("cb_checked");
            if (Creeperborn.config().naturalCreepers && level.getDifficulty() != Difficulty.PEACEFUL
                    && level.getRandom().nextDouble() < Creeperborn.config().elementalChance)
                assign(c, Element.values()[level.getRandom().nextInt(4)]);
        }
        if (element(c) != null) loaded.put(c.getUUID(), c);
    }
    public void unload(Entity entity) { loaded.remove(entity.getUUID()); attacks.remove(entity.getUUID()); }
    public static Element element(Entity entity) {
        for (Element e : Element.values()) if (entity.entityTags().contains("cb_creeper_" + e.key())) return e;
        return null;
    }
    public void assign(Creeper c, Element e) {
        for (Element old : Element.values()) c.removeTag("cb_creeper_" + old.key());
        c.addTag("cb_creeper_" + e.key()); c.addTag("cb_checked");
        c.setCustomName(Component.literal(e.title + " Creeper")); c.setCustomNameVisible(true);
        loaded.put(c.getUUID(), c);
    }
    public void died(net.minecraft.world.entity.LivingEntity entity, net.minecraft.world.damagesource.DamageSource damage) {
        Element e = element(entity);
        if (e == null || !(entity instanceof Creeper c) || !(c.level() instanceof ServerLevel level)
                || !(damage.getEntity() instanceof Player) || c.entityTags().contains("cb_dropped")) return;
        c.addTag("cb_dropped"); c.spawnAtLocation(level, Relics.heart(e)); unload(c);
    }
    public void tick() {
        for (Creeper c : List.copyOf(loaded.values())) {
            if (!c.isAlive() || c.isRemoved()) { unload(c); continue; }
            ServerLevel level = (ServerLevel)c.level(); Element e = element(c);
            if (e == null) continue;
            long now = level.getGameTime();
            if (now % 20 == 0) Abilities.particles(level, e, c.position().add(0, .8, 0), 3);
            var target = c.getTarget();
            if (target != null && target.isAlive() && !target.isSpectator() && !(target instanceof Player p && p.isCreative())
                    && c.distanceToSqr(target) < 10 * 10 && c.hasLineOfSight(target)
                    && now >= attacks.getOrDefault(c.getUUID(), 0L)) {
                attacks.put(c.getUUID(), now + 120);
                Abilities.strike(level, c, target, e, 2, e == Element.AIR ? .8 : .2);
            }
        }
    }
}

