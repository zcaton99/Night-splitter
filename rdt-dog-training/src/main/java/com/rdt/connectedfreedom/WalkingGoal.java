package com.rdt.connectedfreedom;

import java.util.EnumSet;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.*;

/** Native pathfinding, collision, gravity and wolf animations; no scripted teleport learning. */
public final class WalkingGoal extends Goal {
    private final Wolf dog;private final PupState s;private int ticks,settling;
    public WalkingGoal(Wolf dog,PupState state){this.dog=dog;s=state;setFlags(EnumSet.of(Flag.MOVE,Flag.LOOK,Flag.JUMP));}
    private ServerPlayer owner(){return dog.level().getServer().getPlayerList().getPlayer(java.util.UUID.fromString(s.owner));}
    public boolean canUse(){var p=owner();return p!=null&&p.isAlive()&&p.level()==dog.level();}
    public boolean requiresUpdateEveryTick(){return true;}
    public void stop(){dog.getNavigation().stop();dog.setIsInterested(false);s.playerLast=null;s.pendingName=false;}
    public static boolean crossed(Wolf w,ServerPlayer p){Vec3 from=p.position().add(0,.55,0),to=w.position().add(0,.45,0);var hit=w.level().clip(new ClipContext(from,to,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,w));return hit.getType()==HitResult.Type.BLOCK&&hit.getLocation().distanceTo(to)>.35;}
    private void walk(Vec3 to,double speed){dog.getNavigation().moveTo(to.x,to.y,to.z,speed);dog.getLookControl().setLookAt(to.x,to.y+.4,to.z);}
    @Override public void tick(){
        var p=owner();if(p==null)return;ticks++;Learning l=s.learning;
        dog.setTarget(null);dog.setOrderedToSit(false);
        if(dog.getY()<dog.level().getMinY()+8){dog.teleportTo(p.getX(),p.getY(),p.getZ());l.notice="Your pup was brought back from an unsafe drop. Choose a clear walking area.";return;}
        if(l.mode.equals("settle")){dog.getNavigation().stop();dog.setIsInterested(false);dog.setInSittingPose(++settling>25);return;}settling=0;dog.setInSittingPose(false);
        double distance=dog.distanceTo(p);boolean blocked=distance>1.5&&crossed(dog,p);
        if(blocked){s.clearTicks=0;s.blockedTicks++;if(!s.tangled&&s.blockedTicks>=15){s.tangled=true;l.notice="The line crosses a block. Pause the walk, retrace your route, and use Help if needed.";DogTraining.send(p,false);}if(s.helping)walk(p.position(),1.05);else dog.getNavigation().stop();return;}
        s.blockedTicks=0;
        if(s.tangled){s.clearTicks++;if(s.clearTicks>=20&&distance<s.line){s.tangled=false;s.helping=false;l.obstacle();s.exploreTarget=null;DogTraining.send(p,false);}else {walk(p.position(),.9);return;}}
        if(s.pendingName){
            dog.getLookControl().setLookAt(p,30,30);dog.setIsInterested(true);
            int age=p.level().getServer().getTickCount()-s.nameAt;
            if(!s.atLimit&&age>40){s.pendingName=false;dog.setIsInterested(false);return;}
            if(s.atLimit&&s.stillTicks>=10&&(age>=50||l.connections>=3&&age>=20||s.helping))s.returning=true;
            if(s.returning){walk(p.position(),1.05);if(distance<Math.max(1.5,s.line-1.4)){
                l.connection();s.pendingName=false;s.returning=false;s.atLimit=false;s.helping=false;s.exploreTarget=null;dog.setIsInterested(false);DogTraining.send(p,false);
            }}else dog.getNavigation().stop();return;
        }
        if(l.mode.equals("ready")){dog.getNavigation().stop();dog.getLookControl().setLookAt(p,30,30);dog.setIsInterested(true);return;}
        if(l.mode.equals("celebrate")){if(distance>1.4)walk(p.position(),1.25);else dog.getNavigation().stop();return;}
        if(l.mode.equals("investigate")||l.mode.equals("curious")){
            dog.setIsInterested(true);
            if(distance>1.6)walk(p.position(),.85);
            else {dog.getNavigation().stop();if(!l.curious){l.curious=true;l.mode="curious";l.notice="Your pup approached and investigated the toy. First curiosity! Celebrate this small moment, then Done and settle.";DogTraining.send(p,false);}}
            return;
        }
        if(distance>=s.line-.35){
            if(!s.atLimit){s.atLimit=true;l.notice="At the end of the line: pause the walk, say "+s.name+" once, and give them a moment. H offers familiar help.";DogTraining.send(p,false);}
            dog.getNavigation().stop();return;
        }
        if(s.atLimit){dog.getNavigation().stop();return;}
        if(s.exploreTarget==null||ticks%100==0||dog.position().distanceToSqr(s.exploreTarget)<.7){Vec3 forward=p.getLookAngle().horizontal().normalize();double side=(ticks/100%2==0?1:-1)*1.1;s.exploreTarget=p.position().add(forward.scale(s.line-.1)).add(-forward.z*side,0,forward.x*side);}
        walk(s.exploreTarget,.8);
    }
}
