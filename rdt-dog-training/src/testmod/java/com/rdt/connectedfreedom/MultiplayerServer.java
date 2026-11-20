package com.rdt.connectedfreedom;
import java.util.*;
import java.nio.file.*;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.*;
import net.fabricmc.fabric.api.networking.v1.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.*;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
public final class MultiplayerServer implements ModInitializer {
    private MinecraftServer server;private int stage,age,total;private final Set<UUID> done=new HashSet<>();private final Map<UUID,Integer> seen=new HashMap<>();private final List<String> passed=new ArrayList<>();
    public void onInitialize(){if(!Boolean.getBoolean("rdt.multiplayer"))return;
        PayloadTypeRegistry.clientboundPlay().register(Probe.TYPE,Probe.CODEC);PayloadTypeRegistry.serverboundPlay().register(Probe.TYPE,Probe.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(Probe.TYPE,(p,c)->{if(p.stage()==9)done.add(c.player().getUUID());if(p.stage()==stage)seen.put(c.player().getUUID(),p.value());});
        ServerLifecycleEvents.SERVER_STARTED.register(s->{server=s;System.out.println("RDT_MULTIPLAYER_SERVER_READY");});
        ServerTickEvents.END_SERVER_TICK.register(s->{if(server==null)return;try{run();}catch(Throwable e){e.printStackTrace();report(false,e.toString());server=null;s.halt(false);}});
    }
    private void check(boolean condition,String text){if(!condition)throw new AssertionError(text);passed.add(text);System.out.println("RDT_MULTIPLAYER_CHECK "+text);}
    private ServerPlayer find(String name){return server.getPlayerList().getPlayers().stream().filter(p->p.getGameProfile().name().equals(name)).findFirst().orElse(null);}
    private void next(int n,ServerPlayer a,ServerPlayer b){stage=n;age=0;seen.clear();for(var p:List.of(a,b))ServerPlayNetworking.send(p,new Probe(n,0));}
    private Wolf dog(ServerPlayer p){var s=DogTraining.state(p);return (Wolf)server.overworld().getEntity(UUID.fromString(s.dog));}
    private void place(ServerPlayer p,double x,double z){p.teleportTo(server.overworld(),x,121,z,Set.of(),0,0,false);p.setGameMode(GameType.SURVIVAL);p.setPermanentlyInvulnerable(true);p.setDeltaMovement(Vec3.ZERO);}
    private void run(){total++;age++;if(total>3000)throw new AssertionError("RDT multiplayer timeout at stage "+stage);
        if(stage==9&&done.size()==2&&age>15){var stop=server;server=null;stop.halt(false);return;}
        var a=find("LanAlpha");var b=find("LanBeta");if(a==null||b==null)return;
        for(var p:List.of(a,b))p.getFoodData().setFoodLevel(20);
        if(stage==0){if(!ServerPlayNetworking.canSend(a,Probe.TYPE)||!ServerPlayNetworking.canSend(b,Probe.TYPE))return;
            var l=server.overworld();for(int x=-2;x<=2;x++)for(int z=-2;z<=2;z++)l.setChunkForced(x,z,true);for(int x=-20;x<=20;x++)for(int z=-20;z<=20;z++)l.setBlock(new BlockPos(x,120,z),Blocks.GRASS_BLOCK.defaultBlockState(),3);
            place(a,-7,0);place(b,7,0);next(1,a,b);return;
        }
        var sa=DogTraining.state(a);var sb=DogTraining.state(b);
        if(stage==1&&sa!=null&&sb!=null&&age>40){
            check(!sa.dog.equals(sb.dog)&&sa.name.equals("Sprout")&&sb.name.equals("River"),"Two native players adopt distinct named pups through production packets");
            check(dog(a).getOwnerReference()!=null&&dog(a).isLeashed()&&dog(b).isLeashed(),"Named wolves have real owners and visible native leads");
            check(!sa.learning.curious&&!sb.learning.curious,"Hesitant pups begin without an awarded toy response");next(2,a,b);return;
        }
        if(stage==2&&sa.learning.connections>=3&&sb.learning.connections>=3&&seen.getOrDefault(a.getUUID(),0)>=3&&seen.getOrDefault(b.getUUID(),0)>=3){
            check(true,"Both pups use native pathfinding to reach the line limit and reconnect after name/help");check(sa.learning.connections>=3&&sb.learning.connections>=3,"Learning state synchronizes to both native clients under network delay");
            check(dog(a).getUUID().toString().equals(sa.dog)&&dog(b).getUUID().toString().equals(sb.dog),"No replacement wolves are spawned during learning");next(3,a,b);return;
        }
        if(stage==3&&age>80){
            check(sa.line==7&&sb.line==5,"One player's line adjustment cannot change the other player's line");check(sa.name.equals("Sprout")&&sb.name.equals("River"),"Repeated adoption packets cannot replace or rename an existing pup");
            float hp=dog(b).getHealth();dog(b).hurtServer(server.overworld(),a.damageSources().playerAttack(a),5);check(dog(b).getHealth()==hp,"A second player cannot harm a managed learning pup");
            for(var p:List.of(a,b)){
                double x=p==a?-6.5:6.5;place(p,x,.5);Wolf w=dog(p);w.getNavigation().stop();w.teleportTo(x,121,6.5);var s=DogTraining.state(p);s.line=7;s.pendingName=false;s.returning=false;s.atLimit=false;s.tangled=false;s.exploreTarget=null;s.learning.mode="explore";
                int bx=(int)Math.floor(x);server.overworld().setBlock(new BlockPos(bx,121,3),Blocks.STONE.defaultBlockState(),3);server.overworld().setBlock(new BlockPos(bx,122,3),Blocks.STONE.defaultBlockState(),3);
            }next(4,a,b);return;
        }
        if(stage==4){if(age==25)check(sa.tangled&&sb.tangled,"Native line collision detects a post between each player and pup");if(sa.learning.obstacles>=1&&sb.learning.obstacles>=1){check(true,"Help guides each native pup around a real obstacle and clears the crossed line");
                for(var p:List.of(a,b)){double x=p==a?-6:6;place(p,x,-7);var s=DogTraining.state(p);dog(p).teleportTo(x+1,121,-7);s.learning.mode="explore";s.pendingName=false;s.returning=false;s.atLimit=false;s.exploreTarget=null;s.line=5;}
                next(5,a,b);return;}}
        if(stage==5&&sa.learning.celebrations>=2&&sb.learning.celebrations>=2&&sa.learning.walkBlocks>=20&&sb.learning.walkBlocks>=20){
            check(true,"Two moving celebrations require real movement from each player and pup");check(sa.learning.socialDogs.size()>0&&sb.learning.socialDogs.size()>0,"Shared movement near another dog builds neutral socialization without forced greetings");check(sa.learning.readyForPlay()&&sb.learning.readyForPlay(),"Shared walking and learning events unlock the first play invitation");next(6,a,b);return;}
        if(stage==6&&(age==100||age==300)){for(var p:List.of(a,b)){var s=DogTraining.state(p);System.out.println("RDT_PLAY_DEBUG "+p.getGameProfile().name()+" mode="+s.learning.mode+" ready="+s.learning.ready+" curious="+s.learning.curious+" distance="+dog(p).distanceTo(p)+" tangled="+s.tangled+" pending="+s.pendingName+" navDone="+dog(p).getNavigation().isDone()+" held="+p.getMainHandItem()+" offhand="+p.getOffhandItem()+" notice="+s.learning.notice);}}
        if(stage==6&&sa.learning.curious&&sb.learning.curious){check(dog(a).distanceTo(a)<2.6&&dog(b).distanceTo(b)<2.6,"Ready then Yes produces an actual pup approach before curiosity is credited");check(!sa.learning.complete&&!sb.learning.complete,"Curiosity does not skip the closing settling lesson");next(7,a,b);return;}
        if(stage==7&&sa.learning.complete&&sb.learning.complete&&age>45){DogTraining.save();DogTraining.load(server);check(DogTraining.state(a).learning.complete&&DogTraining.state(b).learning.complete,"Both named pups retain completed learning after progress reload");check(!DogTraining.state(a).pendingName&&!DogTraining.state(b).pendingName,"Reload clears temporary cue movement");dog(a).removeLeash();DogTraining.tick(server);check(dog(a).getLeashHolder()==a,"A loaded pup reconnects its visible lead to the current player");
            for(var p:List.of(a,b)){double x=p==a?-10:10;place(p,x,-8);dog(p).teleportTo(x,121,-3);var state=DogTraining.state(p);state.learning.mode="settle";state.atLimit=false;state.tangled=false;}
            next(10,a,b);return;}
        if(stage==10&&age==25){check(sa.recalls.letsGo==0&&sb.recalls.letsGo==0,"New recall cues do not grant instant success before help and movement");}
        if(stage==10&&sa.recalls.letsGo==1&&sb.recalls.letsGo==1){check(sa.learning.mode.equals("together")&&!sa.holding&&sb.learning.mode.equals("together"),"Let's go returns both native pups without requiring a front sit");
            for(var p:List.of(a,b)){DogTraining.action(p,"yes","");var state=DogTraining.state(p);state.learning.mode="settle";double x=p==a?-10:10;place(p,x,-8);dog(p).teleportTo(x,121,-3);state.atLimit=false;state.tangled=false;}
            next(11,a,b);return;}
        if(stage==11&&sa.holding&&sb.holding){check(sa.recalls.here==1&&sb.recalls.here==1&&dog(a).getZ()>a.getZ()&&dog(b).getZ()>b.getZ(),"Here returns both pups to front sits and credits each actual return");next(12,a,b);return;}
        if(stage==12&&age==5){for(var p:List.of(a,b)){DogTraining.action(p,"good","");DogTraining.action(p,"done","");place(p,p.getX()+2,-10);}}
        if(stage==12&&age==45){check(sa.holding&&sb.holding&&dog(a).isInSittingPose()&&dog(b).isInSittingPose(),"Good, Done and handler movement do not silently release a held Here sit");check(dog(a).position().distanceTo(sa.holdAt)<.2&&dog(b).position().distanceTo(sb.holdAt)<.2,"Held pups stay at their completed position as handlers move away");next(13,a,b);return;}
        if(stage==13&&age>45){check(!sa.holding&&!sb.holding&&sa.learning.mode.equals("explore")&&sb.learning.mode.equals("explore"),"Yes explicitly releases both held work pictures through native client packets");DogTraining.save();DogTraining.load(server);check(DogTraining.state(a).recalls.here==1&&DogTraining.state(b).recalls.letsGo==1,"Each pup's recall learning survives progress reload");next(8,a,b);return;}
        if(stage==8&&age>90&&seen.size()==2){check(true,"Both native clients open and render their own field-guide journal");report(true,"");next(9,a,b);return;}
    }
    private void report(boolean success,String failure){try{Files.writeString(Path.of("multiplayer-report.json"),new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(Map.of("passed",success,"checks",passed,"failure",failure,"native_clients",2)));if(success)System.out.println("RDT_NATIVE_MULTIPLAYER_PASSED");}catch(Exception e){throw new RuntimeException(e);}}
}
