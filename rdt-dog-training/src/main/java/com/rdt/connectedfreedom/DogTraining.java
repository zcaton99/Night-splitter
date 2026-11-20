package com.rdt.connectedfreedom;

import java.nio.file.Path;
import java.util.*;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.*;
import net.fabricmc.fabric.api.event.player.*;
import net.fabricmc.fabric.api.networking.v1.*;
import net.minecraft.commands.Commands;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.*;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.*;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.phys.Vec3;
import com.rdt.connectedfreedom.mixin.MobGoalsAccessor;

public final class DogTraining implements ModInitializer {
    public static Item JOURNAL,TOY;
    public static final class Data {public Map<String,PupState> pups=new HashMap<>();}
    private static Data data=new Data();private static Path path;
    private static final Map<UUID,Integer> requests=new HashMap<>();
    public static boolean managed(Entity e){return e instanceof Wolf&&e.entityTags().contains("rdt_pup");}
    public static PupState state(Player p){return data.pups.get(p.getUUID().toString());}
    public static PupState state(Wolf dog){return data.pups.values().stream().filter(s->dog.getUUID().toString().equals(s.dog)).findFirst().orElse(null);}
    public static Wolf dog(MinecraftServer server,PupState s){if(s==null)return null;for(var level:server.getAllLevels()){Entity e=level.getEntity(UUID.fromString(s.dog));if(e instanceof Wolf w&&w.isAlive())return w;}return null;}
    private static Item item(String name){var id=Identifier.fromNamespaceAndPath("rdt",name);return Registry.register(BuiltInRegistries.ITEM,id,new Item(new Item.Properties().setId(ResourceKey.create(Registries.ITEM,id)).stacksTo(1)));}
    @Override public void onInitialize(){
        JOURNAL=item("field_guide");TOY=item("tug_toy");
        PayloadTypeRegistry.serverboundPlay().register(JournalPayload.TYPE,JournalPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(CoachPayload.TYPE,CoachPayload.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(JournalPayload.TYPE,(packet,ctx)->{
            var p=ctx.player();int tick=p.level().getServer().getTickCount();int last=requests.getOrDefault(p.getUUID(),-100);
            if(packet.valid()&&tick-last>=3){requests.put(p.getUUID(),tick);action(p,packet.action(),packet.value());}
        });
        ServerLifecycleEvents.SERVER_STARTING.register(DogTraining::load);
        ServerLifecycleEvents.SERVER_STOPPING.register(s->save());
        ServerLifecycleEvents.SERVER_STOPPED.register(s->{data=new Data();requests.clear();path=null;});
        ServerEntityEvents.ENTITY_LOAD.register((e,l)->{if(e instanceof Wolf w&&managed(w))install(w);});
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((e,source,amount)->!managed(e));
        ServerTickEvents.END_SERVER_TICK.register(DogTraining::tick);
        ServerPlayConnectionEvents.DISCONNECT.register((h,s)->{requests.remove(h.player.getUUID());save();});
        ServerPlayConnectionEvents.JOIN.register((h,sender,server)->{
            var p=h.player;var s=state(p);if(s!=null){s.playerLast=null;s.stillTicks=0;s.pendingName=false;var w=dog(server,s);if(w!=null&&s.leashed&&w.level()==p.level())w.setLeashedTo(p,true);}
            p.awardRecipesByKey(List.of(ResourceKey.create(Registries.RECIPE,Identifier.fromNamespaceAndPath("rdt","field_guide"))));
            send(p,false);
        });
        UseItemCallback.EVENT.register((p,l,hand)->{if(p.getItemInHand(hand).is(JOURNAL)){if(p instanceof ServerPlayer sp)send(sp,true);return InteractionResult.SUCCESS;}if(p.getItemInHand(hand).is(TOY)){if(p instanceof ServerPlayer sp)action(sp,"ready","");return InteractionResult.SUCCESS;}return InteractionResult.PASS;});
        UseEntityCallback.EVENT.register((p,l,hand,e,hit)->{if(!managed(e))return InteractionResult.PASS;if(p instanceof ServerPlayer sp){var s=state(sp);if(s!=null&&e.getUUID().toString().equals(s.dog))send(sp,true);else sp.sendSystemMessage(Component.literal("This pup already has a walking partner. Open your own RDT field guide to meet yours."));}return InteractionResult.SUCCESS;});
        CommandRegistrationCallback.EVENT.register((dispatcher,access,env)->dispatcher.register(Commands.literal("rdt").executes(ctx->{send(ctx.getSource().getPlayerOrException(),true);return 1;})));
    }
    public static void load(MinecraftServer server){path=server.getWorldPath(LevelResource.ROOT).resolve("rdt-progress.json");data=WorldStore.read(path,Data.class,new Data());if(data.pups==null)throw new IllegalStateException("Invalid RDT progress");for(var entry:data.pups.entrySet()){var s=entry.getValue();if(s==null||s.learning==null||s.dog==null||!entry.getKey().equals(s.owner))throw new IllegalStateException("Invalid RDT pup record");UUID.fromString(s.dog);s.runtime();s.installed=false;s.pendingName=false;s.exploreTarget=null;}}
    public static void save(){if(path!=null)WorldStore.write(path,data);}
    public static String cleanName(String text){String name=text.strip().replaceAll("[^\\p{L}\\p{N} '\\-]","");return name.isBlank()?"Pup":name.substring(0,Math.min(24,name.length()));}
    public static PupState adopt(ServerPlayer p,String name){
        if(state(p)!=null)return state(p);
        if(p.level().dimension()!=Level.OVERWORLD||!p.isAlive()||p.isSpectator()||!p.onGround()||p.isInWater())return null;
        var level=(ServerLevel)p.level();Wolf dog=EntityTypes.WOLF.create(level,EntitySpawnReason.EVENT);if(dog==null)return null;
        Vec3 at=p.position().add(p.getLookAngle().horizontal().normalize().scale(2));dog.snapTo(at.x,p.getY(),at.z);
        if(!level.noCollision(dog)){dog.snapTo(p.getX(),p.getY(),p.getZ());if(!level.noCollision(dog))return null;}
        PupState s=new PupState();s.owner=p.getUUID().toString();s.dog=dog.getUUID().toString();s.name=cleanName(name);s.dimension=level.dimension().identifier().toString();s.x=dog.getX();s.y=dog.getY();s.z=dog.getZ();
        dog.addTag("rdt_pup");dog.setTame(true,false);dog.setOwner(p);dog.setCustomName(Component.literal(s.name));dog.setCustomNameVisible(true);dog.setPersistenceRequired();dog.setPermanentlyInvulnerable(true);
        data.pups.put(s.owner,s);
        if(!level.addFreshEntity(dog)){data.pups.remove(s.owner);return null;}
        install(dog);dog.setLeashedTo(p,true);give(p,JOURNAL);give(p,TOY);save();return s;
    }
    private static void give(ServerPlayer p,Item item){if(!p.getInventory().contains(new ItemStack(item)))if(!p.addItem(new ItemStack(item)))p.spawnAtLocation((ServerLevel)p.level(),new ItemStack(item));}
    public static void install(Wolf dog){PupState s=state(dog);if(s==null)return;s.runtime();if(s.installed)return;
        MobGoalsAccessor goals=(MobGoalsAccessor)dog;goals.rdt$goals().removeAllGoals(g->true);goals.rdt$targets().removeAllGoals(g->true);goals.rdt$goals().addGoal(0,new WalkingGoal(dog,s));
        dog.setTarget(null);dog.setOrderedToSit(false);dog.setPersistenceRequired();dog.setPermanentlyInvulnerable(true);s.installed=true;
    }
    public static void action(ServerPlayer p,String action,String value){
        if(action.equals("open")){send(p,true);return;}
        if(!p.isAlive()||p.isSpectator())return;
        if(action.equals("adopt")){var s=adopt(p,value);if(s==null)p.sendSystemMessage(Component.literal("Meet your pup on clear, dry ground in the Overworld."));send(p,true);return;}
        PupState s=state(p);if(s==null){send(p,true);return;}Wolf w=dog(p.level().getServer(),s);
        if(w==null||w.level()!=p.level()||w.distanceToSqr(p)>32*32){s.learning.notice="Your pup is waiting near "+(int)s.x+", "+(int)s.y+", "+(int)s.z+" in "+s.dimension+". Return to continue together.";send(p,false);return;}
        int now=p.level().getServer().getTickCount();Learning l=s.learning;
        switch(action){
            case "yes"->{l.yes();s.exploreTarget=null;s.pendingName=false;w.setOrderedToSit(false);}
            case "name"->{
                if(now-s.lastCue<40){l.notice="Say the name once, then give your pup time. Help them succeed instead of repeating it.";break;}
                s.lastCue=now;s.nameAt=now;s.pendingName=true;w.setIsInterested(true);
                l.notice=s.atLimit?"Pause. Say "+s.name+" once. A turn toward you is a connection, not a front-sit recall. Help follows if needed.":"Say "+s.name+" once. Notice their response. Practice the line lesson when the pup reaches its limit.";
            }
            case "help"->{if(s.tangled){s.helping=true;l.notice="Walk back toward the same side of the obstacle and encourage your pup. Help means finding a clear path, not dragging.";w.getNavigation().moveTo(p,1.0);}else if(s.pendingName&&now-s.nameAt>=15){s.helping=true;s.returning=true;l.notice="Help the pup turn and come closer. Your name cue is still being learned.";}else l.notice="Give the pup a moment after your cue. At a tangle, pause and help them find the route back.";}
            case "celebrate"->{if(s.celebrateAt>=0){l.notice="Celebrate together for a short lap; let your pup move with you.";break;}if(l.connections+l.obstacles<=l.celebrations){l.notice="Build a successful check-in or solve a line problem first. Then share a moving celebration.";break;}s.celebrateAt=now;s.celebrateTicks=0;s.celebrateLast=p.position();l.mode="celebrate";l.notice="Celebrate! Move in a small, clear loop together. Match this pup's pace and give them room.";}
            case "ready"->{if(l.invite()){give(p,TOY);w.setOrderedToSit(false);}s.exploreTarget=null;}
            case "done"->{l.done();s.pendingName=false;s.helping=false;s.returning=false;s.celebrateAt=-1;w.getNavigation().stop();w.setIsInterested(false);}
            case "line"->{s.line=s.line<4?5:s.line<6?7:3;s.exploreTarget=null;l.notice="Line length: "+(int)s.line+" blocks. Give room where it is suitable; shorten it before tight spaces.";}
            case "good"->l.notice="Good reassures the pup to continue. It is different from Yes, which releases them.";
            default->{return;}
        }
        save();send(p,false);
    }
    public static CoachPayload.View view(ServerPlayer p){PupState s=state(p);if(s==null)return new CoachPayload.View("",-1,0,0,0,0,0,20,10,5,false,false,"welcome","Meet a hesitant, curious pup. They do not know your words yet.","Choose a name. We will begin with a shared walk, not a demand to play.");Wolf w=dog(p.level().getServer(),s);var l=s.learning;return new CoachPayload.View(s.name,w==null?-1:w.getId(),l.chapterStep(),l.connections,l.celebrations,l.obstacles,l.walkBlocks,(int)l.comfort,(int)l.engagement,s.line,s.leashed,l.complete,l.mode,l.notice,l.next());}
    public static void send(ServerPlayer p,boolean open){if(ServerPlayNetworking.canSend(p,CoachPayload.TYPE))ServerPlayNetworking.send(p,new CoachPayload(WorldStore.JSON.toJson(view(p)),open));}
    public static void tick(MinecraftServer server){
        for(var p:server.getPlayerList().getPlayers()){
            var s=state(p);if(s==null)continue;Wolf w=dog(server,s);if(w==null||w.level()!=p.level()||!p.isAlive()){s.playerLast=null;continue;}if(!s.installed)install(w);
            s.x=w.getX();s.y=w.getY();s.z=w.getZ();s.dimension=w.level().dimension().identifier().toString();
            double dogMoved=s.dogLast==null?0:w.position().distanceTo(s.dogLast);s.dogLast=w.position();
            double moved=s.playerLast==null?0:p.position().distanceTo(s.playerLast);s.playerLast=p.position();s.stillTicks=moved<.03?s.stillTicks+1:0;
            if(dogMoved>.01&&moved<2&&s.learning.released&&w.distanceToSqr(p)<s.line*s.line&&w.position().distanceTo(p.position())<12){s.learning.walked(moved);s.travelRemainder+=moved;if(s.travelRemainder>=1){int blocks=(int)s.travelRemainder;s.learning.walkBlocks+=blocks;s.travelRemainder-=blocks;}}
            if(s.celebrateAt>=0){if(dogMoved>.01&&moved>.08&&moved<1.5&&w.distanceToSqr(p)<16)s.celebrateTicks++;if(s.celebrateTicks>=45){s.learning.celebrate();s.learning.mode="explore";s.celebrateAt=-1;s.exploreTarget=null;send(p,false);}else if(server.getTickCount()-s.celebrateAt>240){s.celebrateAt=-1;s.learning.mode="explore";s.learning.notice="Try a smaller celebration together when you have room. There is no hurry.";}}
            if(server.getTickCount()%20==0)send(p,false);
        }
        if(server.getTickCount()%100==0)save();
    }
}
