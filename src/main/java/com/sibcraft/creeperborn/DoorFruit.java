package com.sibcraft.creeperborn;

import java.util.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

public final class DoorFruit {
    private static final Map<UUID,Long> credit=new HashMap<>();
    private static final Map<UUID,Vec3> positions=new HashMap<>();
    public static boolean unlocked(Player p){return p.entityTags().contains("cb_fruit_door")&&FruitClaims.ownsDoor(p);}
    public static boolean selected(Player p){return unlocked(p)&&p.entityTags().contains("cb_mode_fruit");}
    public static boolean consume(Player p,ItemStack item){
        if(!Relics.kind(item).equals("fruit:door")||item.isEmpty()||!FruitClaims.claimDoor(p))return false;
        p.addTag("cb_fruit_door");p.addTag("cb_mode_fruit");grant(p,1);item.shrink(1);return true;
    }
    public static int uses(Player p){return Mastery.get(p,"cb_door_uses_");}
    public static int seconds(Player p){return Mastery.get(p,"cb_door_time_");}
    public static int tier(Player p){return unlocked(p)?Mastery.tier(uses(p),seconds(p)):0;}
    public static void grant(Player p,int tier){
        Mastery.set(p,"cb_door_uses_",tier==3?40:tier==2?10:0);
        Mastery.set(p,"cb_door_time_",tier==3?1200:tier==2?300:0);
    }
    public static void credit(Player p){
        long now=p.level().getGameTime();if(!unlocked(p)||now-credit.getOrDefault(p.getUUID(),-10000L)<20)return;
        credit.put(p.getUUID(),now);Mastery.set(p,"cb_door_uses_",uses(p)+1);
    }
    public static int cooldown(int mode){return new int[]{200,600,900}[mode];}
    public static long ready(Player p,int mode){
        String prefix="cb_door_ready_"+mode+"_";
        for(String t:p.entityTags())if(t.startsWith(prefix))try{return Long.parseLong(t.substring(prefix.length()));}catch(NumberFormatException ignored){}
        return 0;
    }
    public static void ended(Player p,int mode){
        String prefix="cb_door_ready_"+mode+"_";
        for(String t:Set.copyOf(p.entityTags()))if(t.startsWith(prefix))p.removeTag(t);
        p.addTag(prefix+(p.level().getGameTime()+cooldown(mode)));
    }
    public static String cast(Player p,int move){
        if(DoorSpace.active(p)||DoorSpace.ownsSession(p))return DoorSpace.exit(p);
        if(!selected(p))return "Select Door-Door with G first.";
        if(!(p instanceof ServerPlayer sp))return "Door travel requires a connected player.";
        if(p.isSpectator()||!p.isAlive())return "Cannot open a door now.";
        if(WaterRules.suppressed(p))return "Fruit weakness: cannot open doors while submerged.";
        if(move<0||move>2||tier(p)<move+1)return "Door move locked: tier 2 needs 10 uses + 5 active minutes; tier 3 needs 40 uses + 20 active minutes.";
        long wait=ready(p,move)-p.level().getGameTime();
        if(wait>0)return "Door recovering: "+((wait+19)/20)+"s.";
        return DoorSpace.open(sp,move);
    }
    public static String status(Player p){return "Door-Door "+(unlocked(p)?"tier "+tier(p)+"/3 | "+uses(p)+" uses | "+seconds(p)+" active seconds":"locked");}
    public static void tick(MinecraftServer server){
        for(ServerPlayer p:server.getPlayerList().getPlayers()){
            Vec3 old=positions.put(p.getUUID(),p.position());
            if(selected(p)&&p.isAlive()&&(!p.isSpectator()||DoorSpace.active(p))&&old!=null&&old.distanceToSqr(p.position())>.04){
                int before=tier(p);Mastery.set(p,"cb_door_time_",seconds(p)+1);
                if(tier(p)>before)Creeperborn.say(p,"Door-Door mastery increased! Move "+tier(p)+" unlocked.");
            }
        }
    }
    public static void forget(Player p){credit.remove(p.getUUID());positions.remove(p.getUUID());}
    public static void clear(){credit.clear();positions.clear();}
}
