package com.sibcraft.creeperborn;

import java.util.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;

public final class Shackles {
    private record Binding(Player caster,LivingEntity target,ServerLevel level,long expires,List<Display.BlockDisplay> slabs){}
    private static final Map<UUID,Binding> bindings=new HashMap<>();
    private Shackles(){}
    private static boolean valid(Binding b){
        return b.target().isAlive()&&!b.target().isRemoved()&&b.caster().isAlive()&&!b.caster().isRemoved()
            &&b.target().level()==b.level()&&b.caster().level()==b.level()&&b.level().getGameTime()<b.expires();
    }
    public static boolean rooted(LivingEntity target){Binding b=bindings.get(target.getUUID());return b!=null&&valid(b);}
    public static long remaining(LivingEntity target){Binding b=bindings.get(target.getUUID());return b==null||!valid(b)?0:b.expires()-b.level().getGameTime();}
    public static void bind(Player caster,LivingEntity target,int ticks){
        if(!(target instanceof Enemy)||!target.isAlive()||!(caster.level() instanceof ServerLevel level))return;
        Binding old=bindings.remove(target.getUUID());if(old!=null)old.slabs().forEach(Entity::discard);
        List<Display.BlockDisplay> slabs=new ArrayList<>();
        for(int i=0;i<4;i++){var slab=Rocks.visual(level,target.position(),.85f);slab.setBlockState(Blocks.STONE_SLAB.defaultBlockState());slabs.add(slab);}
        Binding b=new Binding(caster,target,level,level.getGameTime()+ticks,slabs);bindings.put(target.getUUID(),b);
        target.setDeltaMovement(0,0,0);if(target instanceof Mob mob)mob.getNavigation().stop();
        position(b);Mastery.credit(caster,Element.EARTH);
    }
    private static void position(Binding b){
        double radius=Math.max(.55,b.target().getBbWidth()/2+.2);
        for(int i=0;i<4;i++){
            double angle=i*Math.PI/2;
            b.slabs().get(i).snapTo(b.target().getX()+Math.cos(angle)*radius,b.target().getY()+.5,b.target().getZ()+Math.sin(angle)*radius);
        }
    }
    public static void tick(){
        Iterator<Binding> it=bindings.values().iterator();
        while(it.hasNext()){
            Binding b=it.next();
            if(!valid(b)){b.slabs().forEach(Entity::discard);it.remove();continue;}
            b.target().setDeltaMovement(0,0,0);if(b.target() instanceof Mob mob)mob.getNavigation().stop();position(b);
        }
    }
    public static void clear(){bindings.values().forEach(b->b.slabs().forEach(Entity::discard));bindings.clear();}
}
