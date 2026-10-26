package com.sibcraft.creeperborn;

import java.util.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.*;
import net.minecraft.world.phys.*;
import net.minecraft.world.level.ClipContext;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;

/** Rubber powers, independently unlocked and mastered. All authority remains on the server. */
public final class Rubber {
    public static final int MAX_GEAR_TICKS = 12000, GEAR_COOLDOWN = 2400;
    private static final Map<UUID, State> states = new HashMap<>();
    private static final Map<UUID, Vec3> positions = new HashMap<>();
    private static final class State {
        long gearUntil, gearReady, punchReady, dodgeReady, grabReady, credit = -10000, recent = -10000;
        long press = -1, guardUntil; int held = -1, direction; ServerLevel level;
        boolean guarding, grabAttempted; LivingEntity grabTarget; Vec3 anchor;
    }
    private static State state(Player p) { return states.computeIfAbsent(p.getUUID(), id -> new State()); }
    private static long now(Player p) { return p.level().getGameTime(); }
    public static boolean unlocked(Player p) { return p.entityTags().contains("cb_fruit_rubber") && (!(p.level() instanceof ServerLevel) || FruitClaims.owns(p)); }
    public static boolean selected(Player p) { return unlocked(p) && p.entityTags().contains("cb_mode_fruit"); }
    public static boolean consume(Player p, ItemStack stack) {
        if (!Relics.kind(stack).equals("fruit:rubber") || stack.isEmpty() || unlocked(p) || p.isSpectator()) return false;
        if(!FruitClaims.claim(p))return false;
        grant(p,1); p.addTag("cb_fruit_rubber"); p.addTag("cb_mode_fruit"); stack.shrink(1); return true;
    }
    public static int uses(Player p) { return Mastery.get(p,"cb_rubber_uses_"); }
    public static int seconds(Player p) { return Mastery.get(p,"cb_rubber_time_"); }
    public static int progressTier(int uses,int seconds) { return uses>=40 && seconds>=3600 ? 3 : uses>=10 && seconds>=300 ? 2 : 1; }
    public static int tier(Player p) { return unlocked(p) ? progressTier(uses(p), seconds(p)) : 0; }
    public static void grant(Player p, int tier) {
        Mastery.set(p,"cb_rubber_uses_",tier == 3 ? 40 : tier == 2 ? 10 : 0);
        Mastery.set(p,"cb_rubber_time_",tier == 3 ? 3600 : tier == 2 ? 300 : 0);
    }
    public static void credit(Player p) {
        State s = state(p); long now = now(p);
        if (!unlocked(p) || !p.isAlive() || p.isSpectator() || now - s.credit < 20) return;
        s.credit = s.recent = now; Mastery.set(p,"cb_rubber_uses_",uses(p)+1);
    }
    public static boolean gear(Player p) { State s = states.get(p.getUUID()); return s != null && p.isAlive() && s.gearUntil > now(p); }
    public static void cancelInput(Player p) {
        State s=state(p);
        if(s.grabTarget!=null||s.anchor!=null) {
            LivingEntity moved=s.grabTarget instanceof Player ? s.grabTarget : p;
            moved.setDeltaMovement(Vec3.ZERO);
            if(moved instanceof ServerPlayer sp)sp.connection.send(new ClientboundSetEntityMotionPacket(sp));
            s.grabReady=now(p)+80;
        }
        s.grabTarget=null;s.anchor=null;s.grabAttempted=false;s.held=-1;s.press=-1;s.guarding=false;
    }
    public static void forget(Player p) { State s=states.get(p.getUUID()); if(s!=null)end(p,s); states.remove(p.getUUID()); positions.remove(p.getUUID()); }
    public static void clear() { states.clear(); positions.clear(); }
    public static String status(Player p) {
        return "Rubber " + (unlocked(p) ? "tier " + tier(p) + "/3, " + uses(p) + " uses, " + seconds(p) + " active seconds" : "locked")
            + " | active: " + (selected(p) ? "Rubber" : "elements") + " | Gear 5: " + (gear(p) ? (state(p).gearUntil-now(p)+19)/20 + "s" : "OFF");
    }
    public static void press(Player p,int move) { press(p,move,0); }
    public static void press(Player p, int move,int direction) {
        if (WaterRules.suppressed(p) || !selected(p) || !p.isAlive() || p.isSpectator() || move < 0 || move > 2) return;
        State s = state(p); if (s.held >= 0) return;
        s.direction=direction & 15; s.held=move; s.press=now(p); s.level=(ServerLevel)p.level(); s.guardUntil=now(p)+15;
    }
    public static void suspendInWater(Player p){State s=state(p);cancelInput(p);end(p,s);}
    public static void heartbeat(Player p) { State s=state(p); if(s.held>=0) s.guardUntil=now(p)+15; }
    public static void release(Player p, int move) {
        State s=state(p); if (s.held!=move) return;
        long held=now(p)-s.press; cancelInput(p);
        if (!selected(p) || !p.isAlive() || p.isSpectator()) return;
        String result = cast(p,move,held); if (!result.isEmpty()) Creeperborn.say(p,result);
    }
    public static String cast(Player p, int move, long held) {
        if (!(p.level() instanceof ServerLevel level) || !selected(p) || !p.isAlive() || p.isSpectator()) return "Select Rubber with G first.";
        if (WaterRules.suppressed(p))return "Fruit weakness: powers are disabled while submerged. Master all four elements to overcome it.";
        if (move < 0 || move > 2 || tier(p) < move+1) return "Rubber move locked: tier 2 needs 10 uses + 5 active minutes; tier 3 needs 40 uses + 1 active hour.";
        State s=state(p); long now=now(p); boolean charged=held>=7;
        if (move==0) {
            if(now<s.punchReady)return "Stretch Punch cooling down."; s.punchReady=now+30;
            LivingEntity target=target(p, charged ? 14 : 8);
            if(target!=null) {
                float damage=(float)((charged ? 8 : 5) * (gear(p) ? 1.75 : 1));
                if(target.hurtServer(level,p.damageSources().playerAttack(p),damage)) credit(p);
                Vec3 push=target.position().subtract(p.position()).normalize().scale(charged ? 1 : .4);
                target.setDeltaMovement(target.getDeltaMovement().add(push.x,.15,push.z));
            }
            Abilities.particles(level,Element.AIR,p.getEyePosition().add(p.getLookAngle().scale(2)),12);
            return charged ? "Charged Stretch Punch!" : "Stretch Punch!";
        }
        if(move==1) {
            if(charged)return ""; // Grapple runs while held; release only detaches.
            if(now<s.dodgeReady)return "Dodge cooling down.";s.dodgeReady=now+16;
            impulse(p,dodgeMotion(p.getLookAngle(),s.direction)); return "Rubber dodge!";
        }
        if(charged)return ""; // Hold C is maintained defense, never a transformation on release.
        if(gear(p))return "Gear 5 is already active.";
        s.gearReady=Math.max(s.gearReady,recovery(p));
        if(now<s.gearReady)return "Gear 5 recovering: " + ((s.gearReady-now+19)/20) + "s.";
        if(p.getFoodData().getFoodLevel()<=6)return "Eat above three food icons to enter Gear 5.";
        s.gearUntil=now+MAX_GEAR_TICKS;s.level=level;
        return "Gear 5! Food fuels it, up to 10 minutes.";
    }
    public static boolean canGrabPlayer(Player p,Player target) {
        return target!=p && target.isAlive() && !target.isCreative() && !target.isSpectator()
            && p.level() instanceof ServerLevel level && level.isPvpAllowed() && p.canHarmPlayer(target);
    }
    private static void grapple(Player p,State s) {
        if(!s.grabAttempted) {
            s.grabAttempted=true;
            if(now(p)<s.grabReady){Creeperborn.say(p,"Stretch Grab cooling down.");return;}
            Vec3 from=p.getEyePosition(),end=from.add(p.getLookAngle().scale(14));
            var block=p.level().clip(new ClipContext(from,end,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,p));
            if(block.getType()!=HitResult.Type.MISS)end=block.getLocation();
            double nearest=from.distanceTo(end);
            for(LivingEntity t:p.level().getEntitiesOfClass(LivingEntity.class,p.getBoundingBox().inflate(14))) {
                if(t==p||!t.isAlive()||t.isSpectator()||!p.hasLineOfSight(t)||t instanceof Player other&&!canGrabPlayer(p,other))continue;
                var intersection=t.getBoundingBox().inflate(.4).clip(from,end);
                if(intersection.isPresent()&&from.distanceTo(intersection.get())<nearest){s.grabTarget=t;nearest=from.distanceTo(intersection.get());}
            }
            if(s.grabTarget==null&&block.getType()!=HitResult.Type.MISS)s.anchor=block.getLocation();
            if(s.grabTarget!=null||s.anchor!=null){credit(p);Creeperborn.say(p,s.grabTarget instanceof Player?"Stretch Grab — pulling player!":"Stretch Grab — pulling you! Release X to let go.");}
            else Creeperborn.say(p,"Stretch Grab missed.");
        }
        if(s.grabTarget==null&&s.anchor==null)return;
        if(s.grabTarget!=null&&(!s.grabTarget.isAlive()||s.grabTarget.level()!=p.level()
            ||!p.hasLineOfSight(s.grabTarget)||s.grabTarget instanceof Player other&&!canGrabPlayer(p,other))){cancelInput(p);return;}
        Vec3 destination=s.grabTarget!=null?s.grabTarget.getEyePosition():s.anchor;
        if(p.getEyePosition().distanceTo(destination)>20){cancelInput(p);return;}
        if(s.anchor!=null) {
            var block=p.level().clip(new ClipContext(p.getEyePosition(),destination,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,p));
            if(block.getType()!=HitResult.Type.MISS&&block.getLocation().distanceTo(destination)>.6){cancelInput(p);return;}
        }
        LivingEntity moved=s.grabTarget instanceof Player?s.grabTarget:p;
        Vec3 delta=s.grabTarget instanceof Player?p.getEyePosition().subtract(s.grabTarget.getEyePosition()):destination.subtract(p.getEyePosition());
        if(delta.length()<2){moved.setDeltaMovement(Vec3.ZERO);return;}
        Vec3 motion=delta.normalize().scale(Math.min(.75,(delta.length()-2)*.25));
        if(moved instanceof Player player)impulse(player,motion);else moved.setDeltaMovement(motion);
        Abilities.particles((ServerLevel)p.level(),Element.AIR,p.getEyePosition().add(delta.scale(.5)),2);
    }
    public static Vec3 dodgeMotion(Vec3 look,int keys) {
        Vec3 forward=new Vec3(look.x,0,look.z).normalize();
        if(forward.lengthSqr()<.01)forward=new Vec3(0,0,1);
        Vec3 right=new Vec3(-forward.z,0,forward.x);
        int f=((keys&1)!=0?1:0)-((keys&2)!=0?1:0);
        int r=((keys&8)!=0?1:0)-((keys&4)!=0?1:0);
        Vec3 direction=forward.scale(f).add(right.scale(r));
        if(direction.lengthSqr()<.01)direction=forward.scale(-1);
        return direction.normalize().scale(1.2).add(0,.18,0);
    }
    public static void impulse(Player p, Vec3 motion) {
        p.setIgnoreFallDamageFromCurrentImpulse(true,p.position()); p.setDeltaMovement(motion);
        if(p instanceof ServerPlayer sp) {
            ((com.sibcraft.creeperborn.mixin.AirStepConnectionAccessor)sp.connection).creeperborn$resetAirStepCounter(0);
            sp.connection.send(new ClientboundSetEntityMotionPacket(p));
        }
    }
    private static LivingEntity target(Player p, double distance) {
        Vec3 from=p.getEyePosition(), end=from.add(p.getLookAngle().scale(distance));
        var hit=p.level().clip(new ClipContext(from,end,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,p));
        if(hit.getType()!=HitResult.Type.MISS)end=hit.getLocation();
        LivingEntity best=null;double nearest=from.distanceTo(end);
        for(LivingEntity t:p.level().getEntitiesOfClass(LivingEntity.class,p.getBoundingBox().inflate(distance))) {
            if(!(t instanceof Enemy)||!t.isAlive()||!p.hasLineOfSight(t))continue;
            var intersection=t.getBoundingBox().inflate(.4).clip(from,end);
            if(intersection.isPresent()&&from.distanceTo(intersection.get())<nearest){best=t;nearest=from.distanceTo(intersection.get());}
        }
        return best;
    }
    public static void tick(MinecraftServer server) {
        for(ServerPlayer p:server.getPlayerList().getPlayers()) tickPlayer(p);
    }
    public static void tickPlayer(Player p) {
            State s=state(p);long now=now(p);
            if(WaterRules.suppressed(p)){suspendInWater(p);return;}
            if(!p.isAlive()||p.isSpectator()||s.level!=null&&s.level!=p.level()) {
                end(p,s);cancelInput(p);return;
            }
            if(s.held>=0 && (!selected(p)||now>s.guardUntil))cancelInput(p);
            if(s.held==1 && now-s.press>=7 && tier(p)>=2)grapple(p,s);
            s.guarding=s.held==2 && now-s.press>=7 && !gear(p) && tier(p)>=3 && p.getFoodData().getFoodLevel()>6;
            if(s.gearUntil>0 && (now>=s.gearUntil || p.getFoodData().getFoodLevel()<=6))end(p,s);
            if(gear(p)) {
                // Short, refreshable effects avoid permanently altering movement attributes.
                p.addEffect(new MobEffectInstance(MobEffects.SPEED,25,1,false,false));
                p.addEffect(new MobEffectInstance(MobEffects.STRENGTH,25,1,false,false));
                p.addEffect(new MobEffectInstance(MobEffects.JUMP_BOOST,25,2,false,false));
                if(now%40==0)p.getFoodData().setFoodLevel(Math.max(6,p.getFoodData().getFoodLevel()-1));
                if(now%20==0)Abilities.particles((ServerLevel)p.level(),Element.AIR,p.position().add(0,1,0),6);
            } else if(s.guarding && now%40==0)p.getFoodData().setFoodLevel(Math.max(6,p.getFoodData().getFoodLevel()-1));
            if(now%20==0) {
                Vec3 previous=positions.put(p.getUUID(),p.position());
                if(selected(p) && (previous!=null&&previous.distanceToSqr(p.position())>.04 || now-s.recent<200)) {
                    int before=tier(p);Mastery.set(p,"cb_rubber_time_",seconds(p)+1);
                    if(tier(p)>before)Creeperborn.say(p,"Rubber mastery increased! Move " + tier(p) + " unlocked.");
                }
            }
    }
    private static void end(Player p,State s) {
        if(s.gearUntil>0){s.gearUntil=0;s.gearReady=now(p)+GEAR_COOLDOWN;
            for(String tag:Set.copyOf(p.entityTags()))if(tag.startsWith("cb_rubber_recovery_"))p.removeTag(tag);
            p.addTag("cb_rubber_recovery_"+s.gearReady);Creeperborn.say(p,"Gear 5 ended — recovering for 2 minutes.");}
    }
    private static long recovery(Player p) {
        for(String tag:p.entityTags())if(tag.startsWith("cb_rubber_recovery_"))try{return Long.parseLong(tag.substring("cb_rubber_recovery_".length()));}catch(NumberFormatException ignored){}
        return 0;
    }
    public static boolean sharp(DamageSource source) {
        if(!(source.getDirectEntity() instanceof LivingEntity attacker))return false;
        ItemStack item=attacker.getMainHandItem();
        boolean material=item.is(Items.DIAMOND_SWORD)||item.is(Items.NETHERITE_SWORD)||item.is(Items.DIAMOND_AXE)||item.is(Items.NETHERITE_AXE);
        return material && EnchantmentHelper.getItemEnchantmentLevel(attacker.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.SHARPNESS),item)>0;
    }
    private static boolean fireBending;
    public static void fireBending(boolean value) { fireBending=value; }
    public static boolean heat(DamageSource source) {
        return fireBending || source.is(DamageTypeTags.IS_FIRE) || source.is(net.minecraft.world.damagesource.DamageTypes.ARROW) && source.getDirectEntity()!=null && source.getDirectEntity().isOnFire();
    }
    public static float damage(Player p, DamageSource source, float amount) {
        if(Mastery.harmony(p)&&(source.is(DamageTypeTags.IS_FIRE)||source.is(DamageTypeTags.IS_FALL)))return 0;
        if(!unlocked(p))return amount;
        if(source.typeHolder().equals(p.damageSources().fellOutOfWorld().typeHolder())||source.is(net.minecraft.world.damagesource.DamageTypes.GENERIC_KILL))return amount;
        if(source.typeHolder().equals(p.damageSources().drown().typeHolder()))return WaterRules.mastered(p)?0:amount;
        boolean heat=heat(source);
        if(gear(p)) {
            if(source.is(DamageTypeTags.IS_FALL))return 0;
            return heat||sharp(source) ? amount*.5f : 0;
        }
        if(heat || source.is(net.minecraft.world.damagesource.DamageTypes.ARROW) || source.getDirectEntity() instanceof LivingEntity a && a.getMainHandItem().is(net.minecraft.tags.ItemTags.SWORDS)
                || source.getDirectEntity() instanceof LivingEntity a && a.getMainHandItem().is(net.minecraft.tags.ItemTags.AXES))amount*=1.5f;
        State s=state(p);
        if(s.guarding && source.getEntity() instanceof LivingEntity attacker) {
            amount*=.35f;credit(p);Vec3 push=attacker.position().subtract(p.position()).normalize();attacker.setDeltaMovement(push.x*.8,.25,push.z*.8);
        }
        return amount;
    }
}


