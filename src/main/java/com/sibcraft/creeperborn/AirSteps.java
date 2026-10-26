package com.sibcraft.creeperborn;

import java.util.*;
import net.minecraft.world.entity.player.Player;

/** A five-step burst refills after three seconds without an accepted step. */
public final class AirSteps {
    private static final class Burst { int remaining=5; long ready; }
    private final Map<UUID,Burst> bursts=new HashMap<>();
    public boolean use(Player player,long now){
        if(Mastery.harmony(player))return true;
        Burst b=bursts.computeIfAbsent(player.getUUID(),id->new Burst());
        if(now>=b.ready)b.remaining=5;
        if(b.remaining==0)return false;
        b.remaining--;b.ready=now+60;return true;
    }
    public int remaining(Player player,long now){
        if(Mastery.harmony(player))return -1;
        Burst b=bursts.get(player.getUUID());return b==null||now>=b.ready?5:b.remaining;
    }
    public long recoverySeconds(Player player,long now){
        Burst b=bursts.get(player.getUUID());return b==null?0:Math.max(0,(b.ready-now+19)/20);
    }
    public void clear(){bursts.clear();}
}

