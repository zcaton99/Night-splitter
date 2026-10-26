package com.sibcraft.creeperborn;

import java.util.*;
import java.nio.charset.StandardCharsets;
import com.google.gson.Gson;
import com.mojang.math.Transformation;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.phys.*;

/** Bounded spectator-backed travel, with persistent recovery snapshots and a shared owner session. */
public final class DoorSpace {
    private static final String RETURN="cb_door_return_";
    private record Saved(String dimension,double x,double y,double z,float yaw,float pitch,String mode,float speed,boolean mayfly,boolean flying,int move,boolean owner){}
    private static final class Traveler {
        ServerPlayer player; Saved saved; Session session; Vec3 last;
    }
    private static final class Session {
        ServerPlayer owner; ServerLevel level; Vec3 entrance,exit; int mode; double range; long admission,deadline,exitDeadline;
        List<Display.BlockDisplay> entranceVisuals=new ArrayList<>(),exitVisuals=new ArrayList<>();
        Set<UUID> travelers=new HashSet<>(); boolean entered;
    }
    private record Closing(ServerLevel level,Vec3 point,List<Display.BlockDisplay> visuals,long expires){}
    private static final List<Closing> closing=new ArrayList<>();
    private static final Map<UUID,Session> sessions=new HashMap<>();
    private static final Map<UUID,Traveler> travelers=new HashMap<>();
    private static final Gson GSON=new Gson();
    public static int duration(int move,boolean mastered){return (int)(new int[]{400,120,600}[move]*(mastered?1.5:1));}
    public static double range(int move,boolean mastered){return new int[]{64,20,96}[move]*(mastered?1.5:1);}
    public static int admission(boolean mastered){return mastered?160:100;}
    public static boolean active(Player p){return travelers.containsKey(p.getUUID());}
    public static boolean ownsSession(Player p){return sessions.containsKey(p.getUUID());}
    public static boolean safe(ServerLevel level,Player p,Vec3 feet){
        BlockPos at=BlockPos.containing(feet);
        if(at.getY()<=level.getMinY()||at.getY()+3>=level.getMaxY()||!level.getWorldBorder().isWithinBounds(at))return false;
        if(!level.hasChunksAt(at.getX()-1,at.getZ()-1,at.getX()+1,at.getZ()+1))return false;
        AABB box=p.getBoundingBox().move(feet.subtract(p.position()));
        // Query without the spectator entity: spectator collision queries can ignore solids.
        if(!level.noCollision(null,box)||level.getBlockState(at.below()).getCollisionShape(level,at.below()).isEmpty())return false;
        for(BlockPos pos:BlockPos.betweenClosed(BlockPos.containing(box.minX,box.minY-1,box.minZ),BlockPos.containing(box.maxX,box.maxY,box.maxZ)))
            if(!level.getFluidState(pos).isEmpty()||level.getBlockState(pos).is(Blocks.FIRE)||level.getBlockState(pos).is(Blocks.SOUL_FIRE)
                ||level.getBlockState(pos).is(Blocks.MAGMA_BLOCK)||level.getBlockState(pos).is(Blocks.CACTUS))return false;
        return true;
    }
    public static Vec3 nearbySafe(ServerLevel level,Player p,Vec3 origin){
        if(safe(level,p,origin))return origin;
        BlockPos base=BlockPos.containing(origin);
        for(int radius=0;radius<=6;radius++)for(int dy=-4;dy<=4;dy++)for(int dx=-radius;dx<=radius;dx++)for(int dz=-radius;dz<=radius;dz++){
            if(Math.max(Math.abs(dx),Math.abs(dz))!=radius)continue;
            Vec3 candidate=new Vec3(base.getX()+dx+.5,base.getY()+dy,base.getZ()+dz+.5);
            if(safe(level,p,candidate))return candidate;
        }
        return null;
    }
    public static void puff(ServerLevel level,Vec3 at){
        level.sendParticles(new DustParticleOptions(0x00DDEB,1.5f),at.x,at.y+1,at.z,36,.45,.8,.45,.06);
        level.sendParticles(ParticleTypes.CLOUD,at.x,at.y+1,at.z,8,.22,.6,.22,.025);
    }
    private static List<Display.BlockDisplay> door(ServerLevel level,Vec3 at,float yaw){
        List<Display.BlockDisplay> list=new ArrayList<>();
        for(int i=0;i<2;i++){
            Display.BlockDisplay d=EntityTypes.BLOCK_DISPLAY.create(level,EntitySpawnReason.EVENT);
            if(d==null)continue;d.addTag("cb_temporary_rock");
            d.setBlockState(Blocks.WARPED_DOOR.defaultBlockState().setValue(BlockStateProperties.DOUBLE_BLOCK_HALF,i==0?DoubleBlockHalf.LOWER:DoubleBlockHalf.UPPER));
            d.snapTo(at.x,at.y+i,at.z);
            Quaternionf rotation=new Quaternionf().rotationY((float)Math.toRadians(-yaw));
            d.setTransformation(new Transformation(new Vector3f(-.5f,0,-.5f).rotate(rotation),rotation,new Vector3f(1),new Quaternionf()));
            level.addFreshEntity(d);list.add(d);
        }
        puff(level,at);return list;
    }
    private static void close(ServerLevel level,Vec3 at,List<Display.BlockDisplay> visuals){
        if(visuals.isEmpty())return;visuals.forEach(Entity::discard);visuals.clear();puff(level,at);
    }
    public static String open(ServerPlayer owner,int mode){
        if(active(owner)||ownsSession(owner))return "Finish your current passage first.";
        Vec3 look=owner.getLookAngle();Vec3 forward=new Vec3(look.x,0,look.z).normalize();
        if(forward.lengthSqr()<.1)forward=new Vec3(0,0,1);
        ServerLevel level=(ServerLevel)owner.level();
        Vec3 returnPoint=nearbySafe(level,owner,owner.position());
        if(returnPoint==null)return "No safe return point nearby. Open a door near solid, clear ground.";
        Vec3 entrance=mode==1?owner.position():owner.position().add(forward.scale(1.7));
        if(mode!=1&&!safe(level,owner,entrance))return "Door entrance blocked. Face a clear spot with solid ground.";
        Session s=new Session();s.owner=owner;s.level=level;s.entrance=entrance;s.mode=mode;
        boolean mastered=Mastery.harmony(owner);long now=level.getGameTime();s.range=range(mode,mastered);
        s.admission=now+(mode==2?admission(mastered):160);s.deadline=s.admission;
        s.entranceVisuals=door(level,entrance,owner.getYRot());sessions.put(owner.getUUID(),s);
        if(mode==1)enter(owner,s,returnPoint);
        return mode==1?"Rescue Door! Z places a safe exit.":"Door opened — walk through. "+(mode==2?"Others may follow for "+(admission(mastered)/20)+"s.":"Z cancels before entering.");
    }
    private static Saved snapshot(ServerPlayer p,Vec3 point,Session s){return new Saved(p.level().dimension().identifier().toString(),point.x,point.y,point.z,p.getYRot(),p.getXRot(),p.gameMode().getName(),p.getAbilities().getFlyingSpeed(),p.getAbilities().mayfly,p.getAbilities().flying,s.mode,p==s.owner);}
    private static void enter(ServerPlayer p,Session s,Vec3 safeReturn){
        if(active(p)||p.isSpectator()||!p.isAlive()||WaterRules.suppressed(p))return;
        Rubber.forget(p); // End temporary Gear flight/guard before recording baseline abilities.
        Saved saved=snapshot(p,safeReturn,s);p.addTag(RETURN+Base64.getUrlEncoder().withoutPadding().encodeToString(GSON.toJson(saved).getBytes(StandardCharsets.UTF_8)));
        Traveler t=new Traveler();t.player=p;t.saved=saved;t.session=s;t.last=p.position();travelers.put(p.getUUID(),t);s.travelers.add(p.getUUID());
        Rubber.cancelInput(p);p.stopRiding();p.setGameMode(GameType.SPECTATOR);p.getAbilities().flying=true;
        p.getAbilities().setFlyingSpeed(s.mode==1?.07f:s.mode==2?.045f:.05f);p.onUpdateAbilities();p.setDeltaMovement(Vec3.ZERO);
        if(p==s.owner&&!s.entered){s.entered=true;s.deadline=s.level.getGameTime()+duration(s.mode,Mastery.harmony(p));DoorFruit.credit(p);}
        else if(p!=s.owner)DoorFruit.credit(s.owner);
    }
    public static boolean tryEnter(ServerPlayer p){
        if(active(p)||p.isSpectator()||!p.isAlive())return false;
        for(Session s:List.copyOf(sessions.values())){
            if(p.level()!=s.level||s.exit!=null||s.level.getGameTime()>=s.admission
                ||(p!=s.owner&&(s.mode!=2||!s.entered)))continue;
            if(touches(p,s.entrance)){
                Vec3 point=nearbySafe(s.level,p,p.position());
                if(point!=null&&!WaterRules.suppressed(p)){enter(p,s,point);return active(p);}
            }
        }
        return false;
    }
    private static boolean touches(Player p,Vec3 door){
        AABB aperture=new AABB(door.x-.65,door.y,door.z-.65,door.x+.65,door.y+2.4,door.z+.65);
        Vec3 motion=p.getDeltaMovement();
        return p.getBoundingBox().expandTowards(motion.scale(-1)).intersects(aperture);
    }
    private static void removeSnapshot(Player p){for(String tag:Set.copyOf(p.entityTags()))if(tag.startsWith(RETURN))p.removeTag(tag);}
    private static void restore(ServerPlayer p,Saved saved,Vec3 requested,ServerLevel destination){
        if(p.isAlive()){
            Vec3 safe=nearbySafe(destination,p,requested);
            if(safe!=null)p.teleportTo(destination,safe.x,safe.y,safe.z,Set.of(),saved.yaw(),saved.pitch(),true);
            else p.teleport(p.findRespawnPositionAndUseSpawnBlock(false,net.minecraft.world.level.portal.TeleportTransition.DO_NOTHING));
        }
        p.setGameMode(GameType.byName(saved.mode(),GameType.SURVIVAL));
        p.getAbilities().mayfly=saved.mayfly();p.getAbilities().flying=saved.flying();p.getAbilities().setFlyingSpeed(saved.speed());p.onUpdateAbilities();
        p.setDeltaMovement(Vec3.ZERO);p.setIgnoreFallDamageFromCurrentImpulse(true,p.position());removeSnapshot(p);
    }
    private static ServerLevel savedLevel(ServerPlayer p,Saved saved){
        ServerLevel level=p.level().getServer().getLevel(ResourceKey.create(Registries.DIMENSION,Identifier.parse(saved.dimension())));
        return level==null?p.level().getServer().overworld():level;
    }
    private static void leave(Traveler t,Vec3 destination,ServerLevel level){
        travelers.remove(t.player.getUUID());t.session.travelers.remove(t.player.getUUID());restore(t.player,t.saved,destination,level);
    }
    private static void finish(Session s,boolean destination){
        for(UUID id:Set.copyOf(s.travelers)){
            Traveler t=travelers.get(id);if(t==null)continue;
            if(destination&&s.exit!=null)leave(t,s.exit,s.level);
            else leave(t,new Vec3(t.saved.x(),t.saved.y(),t.saved.z()),savedLevel(t.player,t.saved));
        }
        close(s.level,s.entrance,s.entranceVisuals);if(s.exit!=null){
            if(destination&&!s.exitVisuals.isEmpty()){
                closing.add(new Closing(s.level,s.exit,List.copyOf(s.exitVisuals),s.level.getGameTime()+100));s.exitVisuals.clear();
            }else close(s.level,s.exit,s.exitVisuals);
        }
        sessions.remove(s.owner.getUUID());DoorFruit.ended(s.owner,s.mode);
    }
    public static String exit(Player player){
        Session s=sessions.get(player.getUUID());Traveler t=travelers.get(player.getUUID());
        if(s==null){
            if(t==null)return "No active passage.";
            if(t.session.exit!=null){leave(t,t.session.exit,t.session.level);return "You left the shared passage.";}
            leave(t,new Vec3(t.saved.x(),t.saved.y(),t.saved.z()),savedLevel(t.player,t.saved));return "Returned to your entrance.";
        }
        if(!active(player)){if(!s.entered)finish(s,false);return "Door closed.";}
        Vec3 at=player.position();
        if(!safe(s.level,player,at))return "Exit blocked or unsupported. Find clear standing space over solid ground; press Z again.";
        s.exit=at;s.exitDeadline=s.level.getGameTime()+100;s.exitVisuals=door(s.level,at,player.getYRot());
        if(at.distanceToSqr(s.entrance)>=16)DoorFruit.credit(s.owner);
        close(s.level,s.entrance,s.entranceVisuals);s.admission=0;
        leave(travelers.get(player.getUUID()),at,s.level);
        if(s.travelers.isEmpty())finish(s,true);
        return "Exit opened! Remaining passengers have five seconds to leave before automatic arrival.";
    }
    public static void tick(MinecraftServer server){
        closing.removeIf(c->{if(c.level().getGameTime()<c.expires())return false;
            c.visuals().forEach(Entity::discard);puff(c.level(),c.point());return true;});
        for(Session s:List.copyOf(sessions.values())){
            long now=s.level.getGameTime();
            if(!s.owner.isAlive()||s.owner.isRemoved()||s.owner.level()!=s.level){finish(s,false);continue;}
            if(s.exit!=null){
                for(UUID id:Set.copyOf(s.travelers)){Traveler t=travelers.get(id);if(t!=null&&touches(t.player,s.exit))leave(t,s.exit,s.level);}
                if(s.travelers.isEmpty()||now>=s.exitDeadline)finish(s,true);
                continue;
            }
            if(now>=s.deadline){finish(s,false);continue;}
            if(now<s.admission){
                for(ServerPlayer p:s.level.players())tryEnter(p);
            }else close(s.level,s.entrance,s.entranceVisuals);
            for(UUID id:Set.copyOf(s.travelers)){
                Traveler t=travelers.get(id);if(t==null)continue;ServerPlayer p=t.player;
                if(!p.isAlive()||p.isRemoved()||p.level()!=s.level||p.gameMode()!=GameType.SPECTATOR){
                    if(p==s.owner){finish(s,false);break;}
                    leave(t,new Vec3(t.saved.x(),t.saved.y(),t.saved.z()),savedLevel(p,t.saved));continue;
                }
                Vec3 point=p.position();BlockPos block=p.blockPosition();
                if(point.distanceToSqr(s.entrance)>s.range*s.range||!s.level.getWorldBorder().isWithinBounds(block)
                    ||block.getY()<s.level.getMinY()+1||block.getY()>s.level.getMaxY()-3
                    ||!s.level.hasChunksAt(block.getX()-1,block.getZ()-1,block.getX()+1,block.getZ()+1)){
                    p.teleportTo(t.last.x,t.last.y,t.last.z);p.setDeltaMovement(Vec3.ZERO);
                }else t.last=point;
                if(now%10==0)Creeperborn.say(p,"Door Space | "+((s.deadline-now+19)/20)+"s | "+Math.round(p.position().distanceTo(s.entrance))+" / "+(int)s.range+" blocks | Z: exit / return");
            }
        }
    }
    public static void recover(ServerPlayer p){
        for(String tag:Set.copyOf(p.entityTags()))if(tag.startsWith(RETURN)){
            try{Saved saved=GSON.fromJson(new String(Base64.getUrlDecoder().decode(tag.substring(RETURN.length())),StandardCharsets.UTF_8),Saved.class);
                restore(p,saved,new Vec3(saved.x(),saved.y(),saved.z()),savedLevel(p,saved));
                if(saved.owner())DoorFruit.ended(p,saved.move());
            }catch(Exception error){p.setGameMode(GameType.SURVIVAL);removeSnapshot(p);throw new IllegalStateException("Could not restore Door traveler",error);}
        }
    }
    public static void forget(Player p){
        Session s=sessions.get(p.getUUID());if(s!=null)finish(s,false);
        Traveler t=travelers.get(p.getUUID());if(t!=null)leave(t,new Vec3(t.saved.x(),t.saved.y(),t.saved.z()),savedLevel(t.player,t.saved));
    }
    public static void clear(){for(Session s:List.copyOf(sessions.values()))finish(s,false);sessions.clear();travelers.clear();closing.forEach(c->c.visuals().forEach(Entity::discard));closing.clear();}
}
