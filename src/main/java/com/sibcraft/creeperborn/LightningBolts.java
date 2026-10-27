package com.sibcraft.creeperborn;
import java.util.*;
import com.mojang.math.Transformation;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/** Six-tick, nonpersistent branching bolt displays; gameplay damage stays in Lightning. */
public final class LightningBolts{
    private static final class Bolt{Player owner;ServerLevel level;Vec3 from,to;boolean hand;int age;Display.BlockDisplay display;}
    private static final List<Bolt> active=new ArrayList<>();
    public static int variant(String name){
        if(!name.startsWith("cb_lightning_bolt|"))return -1;
        String[] parts=name.split("\\|");if(parts.length!=3)return -1;
        try{int value=Integer.parseInt(parts[1]);float length=Float.parseFloat(parts[2]);
            return value>=0&&value<3&&Float.isFinite(length)&&length>=.01f&&length<=128?value:-1;
        }catch(NumberFormatException error){return -1;}
    }
    public static float length(String name){return variant(name)<0?1:Float.parseFloat(name.split("\\|")[2]);}
    public static int activeCount(){return active.size();}
    public static Vec3 hand(Player p){
        double yaw=Math.toRadians(p.getYRot()),side=p.getMainArm()==HumanoidArm.LEFT?-1:1;
        return p.getEyePosition().add(-Math.cos(yaw)*side*.32,-.38,-Math.sin(yaw)*side*.32).add(p.getLookAngle().scale(.4));
    }
    public static void launch(Player owner,Vec3 from,Vec3 to,boolean hand){
        if(!(owner.level() instanceof ServerLevel level)||from.distanceToSqr(to)<.01)return;
        Display.BlockDisplay d=EntityTypes.BLOCK_DISPLAY.create(level,EntitySpawnReason.EVENT);if(d==null)return;
        Bolt b=new Bolt();b.owner=owner;b.level=level;b.from=from;b.to=to;b.hand=hand;b.display=d;
        d.addTag("cb_temporary_rock");d.setCustomNameVisible(false);d.setBlockState(Blocks.STONE.defaultBlockState());
        var settings=(com.sibcraft.creeperborn.mixin.PunchDisplayAccessor)d;
        settings.creeperborn$duration(0);settings.creeperborn$positionDuration(0);settings.creeperborn$view(1);settings.creeperborn$width(128);settings.creeperborn$height(128);
        pose(b);
        if(level.addFreshEntity(d))active.add(b);
    }
    private static void pose(Bolt b){
        Vec3 from=b.hand?hand(b.owner):b.from,delta=b.to.subtract(from);double length=Math.max(.01,delta.length());
        Vec3 direction=delta.lengthSqr()<.0001?b.owner.getLookAngle():delta.normalize();Quaternionf rotation=new Quaternionf().rotationTo(0,1,0,(float)direction.x,(float)direction.y,(float)direction.z);
        b.display.snapTo(from.x,from.y,from.z);
        b.display.setCustomName(Component.literal("cb_lightning_bolt|"+(b.age/2%3)+"|"+(float)length));
        ((com.sibcraft.creeperborn.mixin.PunchDisplayAccessor)b.display).creeperborn$delay(0);
        b.display.setTransformation(new Transformation(new Vector3f(),rotation,new Vector3f(1,(float)length,1),new Quaternionf()));
    }
    public static void tick(){
        active.removeIf(b->{
            if(++b.age>=6||!b.owner.isAlive()||b.owner.isRemoved()||b.owner.level()!=b.level){b.display.discard();return true;}
            pose(b);return false;
        });
    }
    public static void cancel(Player owner){active.removeIf(b->{if(b.owner==owner){b.display.discard();return true;}return false;});}
    public static void clear(){active.forEach(b->b.display.discard());active.clear();}
}
