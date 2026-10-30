package com.sibcraft.creeperborn;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.InteractionHand;

import com.mojang.authlib.GameProfile;
import java.util.UUID;
import java.util.List;
import java.util.ArrayList;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;

public final class Regression implements ModInitializer {
    private MinecraftServer server;
    private int ticks;
    private double y;
    private Player rubberLive;
    private Zombie rubberTarget;
    private int rubberTicks;
    private Zombie slabNormal,slabMaster;
    @Override public void onInitialize() {
        ServerLifecycleEvents.SERVER_STARTED.register(s -> {
            server = s; Creeperborn.config().naturalCreepers = false;
            for (int x = -1; x <= 0; x++) for (int z = -1; z <= 2; z++) s.overworld().setChunkForced(x, z, true);
        });
        ServerTickEvents.END_SERVER_TICK.register(s -> {
            if (server == null || ++ticks < 30) return;
            if (rubberLive != null) {
                try {
                    if (runRubberLive()) { System.out.println("CREEPERBORN_REGRESSION_PASSED"); server=null; s.halt(false); }
                } catch (Throwable error) { error.printStackTrace(); server=null; s.halt(false); }
                return;
            }
            boolean ready = true;
            for (int x = -1; x <= 0; x++) for (int z = -1; z <= 2; z++)
                ready &= s.overworld().areEntitiesActuallyLoadedAndTicking(new net.minecraft.world.level.ChunkPos(x,z));
            if (!ready && ticks < 600) return;
            try { run(s.overworld()); startRubberLive(s.overworld()); }
            catch (Throwable error) { error.printStackTrace(); server=null; s.halt(false); }
        });
    }

    private void checkVortex(ServerLevel level){
        Player caster=player(level);caster.snapTo(-8,y+12,8);
        Zombie inside=EntityTypes.ZOMBIE.create(level,EntitySpawnReason.EVENT);
        inside.snapTo(1,y+12,8);level.addFreshEntity(inside);
        Zombie outside=EntityTypes.ZOMBIE.create(level,EntitySpawnReason.EVENT);
        outside.snapTo(3,y+12,8);level.addFreshEntity(outside);
        Vortices v=new Vortices();v.start(caster,1);v.tick();
        check(inside.getDeltaMovement().x<0,"Vortex pulls hostile nine blocks from its fixed center");
        check(outside.getDeltaMovement().lengthSqr()==0,"Normal vortex excludes targets beyond ten blocks");
        inside.setDeltaMovement(0,0,0);v.tick();
        check(inside.getDeltaMovement().x<0,"Vortex applies repeated inward force");
        for(int i=2;i<99;i++)v.tick();
        check(v.activeCount()==1&&inside.getDeltaMovement().x<0,"Vortex keeps swirling through tick 99");
        v.tick();
        check(v.activeCount()==0&&inside.getDeltaMovement().x>0&&inside.getDeltaMovement().y>0,"Vortex launches outward and upward at five seconds");
        for(Element e:Element.values()){Mastery.set(caster,"cb_uses_"+e.key()+"_",40);Mastery.set(caster,"cb_time_"+e.key()+"_",1200);}
        // Unlock tags as well as progress are required for Harmony.
        for(Element e:Element.values())Progress.consume(caster,Relics.heart(e));
        v.start(caster,1);v.tick();
        check(outside.getDeltaMovement().x<0,"Harmony vortex includes hostile eleven blocks away");
        caster.setHealth(0);v.tick();check(v.activeCount()==0,"Caster death cancels vortex");
        inside.discard();outside.discard();v.clear();
    }


    private void checkAirSteps(ServerLevel level){
        Player p=player(level);AirSteps steps=new AirSteps();
        for(int i=0;i<5;i++)check(steps.use(p,100+i*4),"Five Air Steps available in a burst");
        check(!steps.use(p,120)&&steps.remaining(p,120)==0,"Sixth Air Step blocked");
        check(!steps.use(p,175),"Air Step recharge lasts full three seconds after fifth step");
        check(steps.use(p,176)&&steps.remaining(p,176)==4,"Air Step fully refills at recharge boundary");
        check(steps.remaining(p,236)==5,"Partial burst also refills after three seconds without stepping");
        for(Element e:Element.values())Mastery.grant(p,e,3);
        for(int i=0;i<30;i++)check(steps.use(p,240+i),"All-four mastery permits unlimited Air Steps");
        check(steps.remaining(p,270)==-1,"Mastered Air Step status is unlimited");
        Player routed=player(level);Mastery.grant(routed,Element.AIR,2);Progress.select(routed,Element.AIR);
        Creeperborn.ABILITIES.move(routed,1);
        for(int i=0;i<5;i++)check(Creeperborn.ABILITIES.cast(routed).equals("Air Step!"),"Actual cast accepts five steps");
        routed.setDeltaMovement(0,0,0);
        check(Creeperborn.ABILITIES.cast(routed).startsWith("Air Step recharging")&&routed.getDeltaMovement().lengthSqr()==0,
            "Exhausted Air Step cast does not launch player");
        routed.setOnGround(true);double grounded=EnvironmentPower.strength(routed,Element.AIR);
        routed.setOnGround(false);
        check(grounded==.75&&EnvironmentPower.strength(routed,Element.AIR)==1.25,"Air has enhanced airborne environmental strength");
        Creeperborn.ABILITIES.clear();
    }


    private void checkGuardMaterials(ServerLevel level){
        Player p=player(level);p.snapTo(-10,y+14,22);
        Rocks rocks=new Rocks();rocks.guard(p,100);
        var area=p.getBoundingBox().inflate(4);
        var walls=level.getEntitiesOfClass(net.minecraft.world.entity.Display.BlockDisplay.class,area);
        check(walls.size()==3&&walls.stream().allMatch(d->d.getBlockState().is(Blocks.MUD_BRICKS)),
            "Basic guard uses three mud-brick panels");
        rocks.clear();
        for(Element e:Element.values())Mastery.grant(p,e,3);
        rocks.guard(p,160);
        walls=level.getEntitiesOfClass(net.minecraft.world.entity.Display.BlockDisplay.class,area);
        check(walls.size()==3&&walls.stream().allMatch(d->d.getBlockState().is(Blocks.STONE_BRICKS)),
            "All-four mastered guard upgrades to stone bricks");
        check(rocks.guardTicks(p)==160,"Mastered guard retains requested eight-second duration");
        Zombie attacker=EntityTypes.ZOMBIE.create(level,EntitySpawnReason.EVENT);
        attacker.snapTo(-10,y+14,25);
        check(rocks.blocks(p,attacker.damageSources().mobAttack(attacker)),"Mastered guard blocks frontal hit");
        attacker.snapTo(-10,y+14,19);
        check(!rocks.blocks(p,attacker.damageSources().mobAttack(attacker)),"Mastered guard still allows rear hits");
        rocks.clear();attacker.discard();
    }


    private void checkQuietMastery(ServerLevel level){
        int[] sounds={0};
        Player p=new Player(level,new GameProfile(UUID.randomUUID(),"QuietMaster")){
            @Override public GameType gameMode(){return GameType.SURVIVAL;}
            @Override protected void playHurtSound(net.minecraft.world.damagesource.DamageSource source){sounds[0]++;}
        };
        p.snapTo(.5,y+10,.5);p.setHealth(20);
        for(Element e:Element.values())Mastery.grant(p,e,3);
        for(var source:new net.minecraft.world.damagesource.DamageSource[]{
            p.damageSources().lava(),p.damageSources().onFire(),p.damageSources().fall(),p.damageSources().drown(),p.damageSources().freeze()
        }){
            p.damageCooldownTime=0;p.hurtTime=0;p.setDeltaMovement(.1,0,.2);
            var before=p.getDeltaMovement();
            check(!p.hurtServer(level,source,4),"Mastered environmental damage is rejected as an actual hit");
            check(p.getHealth()==20&&p.hurtTime==0&&sounds[0]==0,"Immune hit has no health loss, flinch or hurt sound");
            check(p.getDeltaMovement().equals(before),"Immune hit adds no knockback");
        }
        p.damageCooldownTime=0;
        check(p.hurtServer(level,p.damageSources().generic(),4)&&p.getHealth()<20&&sounds[0]>0,
            "Ordinary nonimmune damage still hurts and makes sound");
        Mastery.grant(p,Element.AIR,2);p.damageCooldownTime=0;int previous=sounds[0];
        check(p.hurtServer(level,p.damageSources().fall(),4)&&sounds[0]>previous,
            "Losing mastery restores normal hurt feedback for fall damage");
        check(Element.EARTH.moves[0].equals("Boulder Smash"),"Earth first move uses Boulder Smash display name");
    }


    private net.minecraft.server.level.ServerPlayer doorPlayer(ServerLevel level,String name){
        var p=new net.minecraft.server.level.ServerPlayer(server,level,new GameProfile(UUID.randomUUID(),name),net.minecraft.server.level.ClientInformation.createDefault()){
            private GameType mode=GameType.SURVIVAL;
            @Override public GameType gameMode(){return mode==null?GameType.SURVIVAL:mode;}
            @Override public boolean setGameMode(GameType value){mode=value;getAbilities().mayfly=value==GameType.SPECTATOR;getAbilities().flying=value==GameType.SPECTATOR;return true;}
            @Override public void onUpdateAbilities(){}
            @Override public boolean isInvulnerableTo(ServerLevel destination,net.minecraft.world.damagesource.DamageSource source){return isSpectator()||isCreative();}
            @Override public void sendOverlayMessage(Component message){}
            @Override public void sendSystemMessage(Component message){}
            @Override public void snapTo(double x,double yy,double z){setPos(x,yy,z);}
            @Override public void teleportTo(double x,double yy,double z){snapTo(x,yy,z);}
            @Override public boolean teleportTo(ServerLevel destination,double x,double yy,double z,java.util.Set<net.minecraft.world.entity.Relative> relative,float yaw,float pitch,boolean resetCamera){
                if(destination!=level)throw new AssertionError("Unexpected test dimension");snapTo(x,yy,z);setYRot(yaw);setXRot(pitch);return true;
            }
        };
        p.snapTo(-10.5,y+20,22.5);p.setHealth(20);p.setYRot(0);p.setXRot(0);return p;
    }
    private void checkDoorFruit(ServerLevel level){
        DoorSpace.clear();DoorFruit.clear();
        for(int x=-14;x<=-2;x++)for(int z=18;z<=35;z++){
            level.setBlockAndUpdate(new BlockPos(x,(int)y+19,z),Blocks.STONE.defaultBlockState());
            for(int dy=20;dy<=23;dy++)level.setBlockAndUpdate(new BlockPos(x,(int)y+dy,z),Blocks.AIR.defaultBlockState());
        }
        var owner=doorPlayer(level,"DoorOwner");var friend=doorPlayer(level,"DoorFriend");friend.snapTo(-9.5,y+20,22.5);
        check(DoorFruit.consume(owner,Relics.doorFruit()),"Door fruit claims and consumes");
        check(DoorFruit.selected(owner)&&CombatNetwork.selection(owner)==5,"G selection distinguishes Door from Rubber");
        check(!Rubber.consume(owner,Relics.fruit()),"Door owner cannot consume a second fruit");
        check(!DoorFruit.consume(friend,Relics.doorFruit()),"A fruit has only one living owner per world");
        check((WaterRules.flags(owner)&1)!=0&&WaterRules.vulnerable(owner),"Door fruit carries the existing water curse");
        FruitClaims.load(server);check(FruitClaims.ownsDoor(owner),"Door ownership survives world claim reload");
        check(DoorFruit.tier(owner)==1&&DoorFruit.cast(owner,1).contains("locked"),"Door progression starts at Passage only");
        DoorFruit.grant(owner,2);check(DoorFruit.tier(owner)==2&&DoorFruit.seconds(owner)==300,"Door tier two uses five-minute progression");
        DoorFruit.grant(owner,3);
        check(DoorSpace.duration(0,false)==400&&DoorSpace.duration(1,false)==120&&DoorSpace.duration(2,false)==600,"Door travel timers match twenty, six and thirty seconds");
        check(DoorSpace.range(0,true)==96&&DoorSpace.range(2,true)==144&&DoorSpace.duration(2,true)==900,"All-four mastery extends time and range by fifty percent");
        check(DoorSpace.admission(false)==100&&DoorSpace.admission(true)==160,"Shared entrance window grows from five to eight seconds");
        check(DoorFruit.cooldown(0)==200&&DoorFruit.cooldown(1)==600&&DoorFruit.cooldown(2)==900,"Door cooldowns are ten, thirty and forty-five seconds");
        var start=owner.position();check(DoorSpace.safe(level,owner,start),"Clear floor is valid Door exit");
        level.setBlockAndUpdate(owner.blockPosition().above(),Blocks.STONE.defaultBlockState());
        check(!DoorSpace.safe(level,owner,start),"Solid head space rejects exit");level.setBlockAndUpdate(owner.blockPosition().above(),Blocks.AIR.defaultBlockState());
        check(!DoorSpace.safe(level,owner,start.add(0,5,0))&&DoorSpace.fits(level,owner,start.add(0,5,0)),"Midair fits a deliberate exit but is not a safe automatic return");
        check(DoorFruit.cast(owner,1).startsWith("Rescue Door!")&&DoorSpace.active(owner)&&owner.isSpectator(),"Rescue immediately enters bounded spectator-backed travel");
        check(owner.entityTags().stream().anyMatch(t->t.startsWith("cb_door_return_")),"Travel saves a persistent recovery snapshot");
        check(DoorFruit.ready(owner,1)==0,"Cooldown does not begin while passage is active");
        owner.snapTo(15,y+20,22.5);DoorSpace.tick(server);
        check(owner.position().distanceTo(start)<.01,"Travel beyond Rescue range returns to last in-range point");
        owner.snapTo(-10.5,y+20,26.5);owner.setYRot(123);owner.setXRot(-22);
        check(DoorSpace.exit(owner).startsWith("Exit opened")&&!DoorSpace.active(owner)&&owner.gameMode()==GameType.SURVIVAL,"Valid exit restores Survival");
        check(owner.getYRot()==123&&owner.getXRot()==-22,"Door exit preserves owner current yaw and pitch");
        owner.setYRot(0);owner.setXRot(0);
        check(DoorFruit.ready(owner,1)==level.getGameTime()+600,"Rescue cooldown starts when passage ends");
        check(owner.entityTags().stream().noneMatch(t->t.startsWith("cb_door_return_")),"Exit removes recovery marker");
        check(DoorFruit.cast(owner,1).contains("recovering"),"Immediate rescue reuse is blocked");
        owner.snapTo(-10.5,y+20,22.5);
        long unusedReady=DoorFruit.ready(owner,0);int unusedUses=DoorFruit.uses(owner);
        check(DoorFruit.cast(owner,0).startsWith("Door opened"),"Normal Passage creates a walk-through entrance");
        check(DoorSpace.exit(owner).equals("Door closed.")&&!DoorSpace.ownsSession(owner),"Unused entrance can be cancelled before walking in");
        check(DoorFruit.ready(owner,0)==unusedReady&&DoorFruit.uses(owner)==unusedUses,"Cancelling unused door spends neither cooldown nor progression use");
        check(DoorFruit.cast(owner,0).startsWith("Door opened"),"Cancelled unused Door can be reopened immediately");
        check(!DoorSpace.active(owner),"Normal Passage does not enter before walking through");
        owner.snapTo(-10.5,y+20,24.2);check(DoorSpace.tryEnter(owner),"Walking into entrance starts travel");
        owner.snapTo(12,y+20,24.2);DoorSpace.tick(server);
        // Normal range is 64, so this location remains in range; unloaded-world bounds may restore the last position.
        DoorSpace.forget(owner);check(!DoorSpace.active(owner)&&!DoorSpace.ownsSession(owner)&&owner.gameMode()==GameType.SURVIVAL,"Disconnect cleanup restores owner state");
        owner.snapTo(-10.5,y+20,22.5);
        check(Rubber.consume(friend,Relics.fruit()),"A different fruit owner can join shared Door travel");
        Rubber.grant(friend,3);friend.getFoodData().setFoodLevel(20);Rubber.cast(friend,2,0);
        check(Rubber.gear(friend)&&friend.getAbilities().mayfly,"Shared passenger begins in Gear flight");
        check(DoorFruit.cast(owner,2).startsWith("Door opened"),"Shared Passage creates entrance");
        owner.snapTo(-10.5,y+20,24.2);check(DoorSpace.tryEnter(owner),"Shared owner enters first");
        friend.snapTo(-10.5,y+20,24.2);check(DoorSpace.tryEnter(friend)&&DoorSpace.active(friend),"Friend voluntarily joins shared entrance");
        check(!Rubber.gear(friend),"Entering shared travel ends temporary Gear flight before snapshot");
        owner.snapTo(-10.5,y+20,28.5);check(DoorSpace.exit(owner).startsWith("Exit opened"),"Owner places group exit");
        check(!DoorSpace.active(owner)&&DoorSpace.active(friend),"Passenger may independently reach the exit");
        friend.snapTo(-10.5,y+20,28.5);friend.setYRot(-75);friend.setXRot(18);DoorSpace.tick(server);
        check(!DoorSpace.active(friend)&&friend.gameMode()==GameType.SURVIVAL&&!friend.getAbilities().mayfly,"Passenger returns to Survival without retaining temporary Gear flight");
        check(friend.getYRot()==-75&&friend.getXRot()==18,"Shared exit preserves each passenger current facing");
        FruitClaims.releaseOnDeath(friend);
        check(!DoorSpace.ownsSession(owner)&&DoorFruit.ready(owner,2)>level.getGameTime(),"Shared cooldown begins after final passenger exits");
        // Simulate a server restart: snapshot recovery must work without the in-memory session.
        ((net.minecraft.world.level.storage.ServerLevelData)level.getLevelData()).setGameTime(level.getGameTime()+601);owner.snapTo(-10.5,y+20,22.5);
        DoorFruit.cast(owner,1);check(DoorSpace.active(owner),"Recovery fixture enters rescue");
        DoorSpace.recover(owner);DoorSpace.forget(owner);
        check(owner.gameMode()==GameType.SURVIVAL&&!DoorSpace.active(owner),"Recovery snapshot restores Survival and safe return");
        ((net.minecraft.world.level.storage.ServerLevelData)level.getLevelData()).setGameTime(level.getGameTime()+601);
        owner.snapTo(-10.5,y+20,22.5);owner.setYRot(0);owner.setXRot(0);var timeoutStart=owner.position();
        DoorFruit.cast(owner,1);owner.snapTo(-8.5,y+21,26.5);owner.setYRot(90);owner.setXRot(35);
        ((net.minecraft.world.level.storage.ServerLevelData)level.getLevelData()).setGameTime(level.getGameTime()+121);
        DoorSpace.tick(server);
        check(!DoorSpace.active(owner)&&owner.gameMode()==GameType.SURVIVAL&&owner.position().distanceTo(timeoutStart)<.01,
            "Rescue timer expiry returns safely to saved entrance, not current indoor position");
        check(owner.getYRot()==0&&owner.getXRot()==0,"Timeout return restores saved entrance facing");
        ((net.minecraft.world.level.storage.ServerLevelData)level.getLevelData()).setGameTime(level.getGameTime()+901);
        owner.snapTo(-10.5,y+20,22.5);DoorFruit.cast(owner,2);owner.snapTo(-10.5,y+20,24.2);DoorSpace.tryEnter(owner);
        friend.snapTo(-10.5,y+20,24.2);DoorSpace.tryEnter(friend);
        owner.snapTo(-8.5,y+21,26.5);friend.snapTo(-7.5,y+21,27.5);
        ((net.minecraft.world.level.storage.ServerLevelData)level.getLevelData()).setGameTime(level.getGameTime()+601);
        DoorSpace.tick(server);
        check(!DoorSpace.active(owner)&&!DoorSpace.active(friend)&&owner.gameMode()==GameType.SURVIVAL&&friend.gameMode()==GameType.SURVIVAL,
            "Shared timer expiry restores both owner and guest without stranding either");
        FruitClaims.releaseOnDeath(owner);
        check(FruitClaims.doorAvailable()&&!DoorFruit.unlocked(owner),"Death releases Door and clears only its fruit progression");
        check(Rubber.consume(friend,Relics.fruit())&&!DoorFruit.consume(friend,Relics.doorFruit()),"Rubber owner also cannot eat Door");
        FruitClaims.releaseOnDeath(friend);DoorSpace.clear();DoorFruit.clear();
    }


    private void checkDangerousDoors(ServerLevel level){
        DoorSpace.clear();DoorFruit.clear();
        var p=doorPlayer(level,"DangerDoor");p.snapTo(-6.5,y+20,22.5);
        check(DoorFruit.consume(p,Relics.doorFruit()),"Dangerous-exit fixture owns Door");DoorFruit.grant(p,3);
        for(var fluid:new net.minecraft.world.level.block.Block[]{Blocks.WATER,Blocks.LAVA}){
            for(int dx=-1;dx<=1;dx++)for(int dz=-1;dz<=1;dz++)for(int dy=0;dy<3;dy++)
                level.setBlockAndUpdate(new BlockPos(-7+dx,(int)y+20+dy,29+dz),fluid.defaultBlockState());
            var point=new net.minecraft.world.phys.Vec3(-6.5,y+20,29.5);
            check(DoorSpace.fits(level,p,point)&&!DoorSpace.safe(level,p,point),"Fluid permits deliberate exits but not automatic returns: "+fluid);
            check(DoorSpace.open(p,1).startsWith("Rescue Door!"),"Rescue enters before dangerous exit test");
            p.snapTo(point.x,point.y,point.z);
            check(DoorSpace.exit(p).startsWith("Exit opened")&&!DoorSpace.active(p)&&p.position().distanceTo(point)<.001,
                "Chosen fluid exit uses exact position without rerouting to dry land");
            check(p.gameMode()==GameType.SURVIVAL&&!Mastery.harmony(p),"Dangerous exit restores nonimmune Survival");
            p.damageCooldownTime=0;float before=p.getHealth();
            p.hurtServer(level,fluid==Blocks.LAVA?p.damageSources().lava():p.damageSources().drown(),2);
            check(p.getHealth()<before,"Door exit does not grant lava or drowning immunity");
            p.setHealth(20);p.clearFire();p.snapTo(-6.5,y+20,22.5);
            DoorSpace.clear();
            for(int dx=-1;dx<=1;dx++)for(int dz=-1;dz<=1;dz++)for(int dy=0;dy<3;dy++)
                level.setBlockAndUpdate(new BlockPos(-7+dx,(int)y+20+dy,29+dz),Blocks.AIR.defaultBlockState());
        }
        DoorSpace.open(p,1);p.snapTo(-6.5,y+24,24.5);var airborne=p.position();
        check(DoorSpace.exit(p).startsWith("Exit opened")&&p.position().distanceTo(airborne)<.001,"Unsupported air exit stays at chosen height");
        p.damageCooldownTime=0;float before=p.getHealth();p.hurtServer(level,p.damageSources().fall(),4);
        check(p.getHealth()<before,"Door exit does not grant fall immunity");p.setHealth(20);DoorSpace.clear();
        p.snapTo(-6.5,y+20,22.5);DoorSpace.open(p,1);
        p.snapTo(-6.5,y+20,24.5);level.setBlockAndUpdate(p.blockPosition().above(),Blocks.STONE.defaultBlockState());
        check(DoorSpace.exit(p).startsWith("Exit blocked")&&DoorSpace.active(p),"Solid block at head still prevents exit");
        level.setBlockAndUpdate(p.blockPosition().above(),Blocks.AIR.defaultBlockState());DoorSpace.forget(p);DoorSpace.clear();
        p.snapTo(-6.5,y+24,22.5);
        check(DoorSpace.open(p,0).startsWith("Door opened"),"Door entrance can be placed without supporting ground");
        DoorSpace.forget(p);DoorSpace.clear();
        for(var fluid:new net.minecraft.world.level.block.Block[]{Blocks.WATER,Blocks.LAVA}){
            p.snapTo(-6.5,y+20,22.5);
            for(int dz=1;dz<=3;dz++)for(int dy=0;dy<2;dy++)level.setBlockAndUpdate(new BlockPos(-7,(int)y+20+dy,22+dz),fluid.defaultBlockState());
            check(DoorSpace.open(p,0).startsWith("Door opened"),"Door entrance can be placed in fluid: "+fluid);
            DoorSpace.forget(p);DoorSpace.clear();
            for(int dz=1;dz<=3;dz++)for(int dy=0;dy<2;dy++)level.setBlockAndUpdate(new BlockPos(-7,(int)y+20+dy,22+dz),Blocks.AIR.defaultBlockState());
        }
        FruitClaims.releaseOnDeath(p);DoorFruit.clear();
    }
    private void checkTridentCuring(ServerLevel level){
        Player owner=player(level),healer=player(level);
        for(Element e:Element.values()){Mastery.grant(owner,e,1);Mastery.grant(healer,e,1);}
        Villager guard=EntityTypes.VILLAGER.create(level,EntitySpawnReason.EVENT);
        guard.snapTo(-3.5,y+20,32.5);guard.setNoAi(true);guard.setVillagerDataFinalized(true);
        guard.setVillagerData(guard.getVillagerData().withProfession(level.registryAccess(),net.minecraft.world.entity.npc.villager.VillagerProfession.CLERIC));
        guard.setVillagerXp(23);
        var offers=new net.minecraft.world.item.trading.MerchantOffers();
        offers.add(new net.minecraft.world.item.trading.MerchantOffer(new net.minecraft.world.item.trading.ItemCost(Items.EMERALD,2),new ItemStack(Items.BREAD,3),12,5,.05f));
        guard.setOffers(offers);level.addFreshEntity(guard);
        check(Creeperborn.GUARDIANS.assign(owner,guard,Element.FIRE),"Cure fixture is a Fire guardian");
        BlockPos post=new BlockPos(-4,(int)y+20,32);Creeperborn.GUARDIANS.post(guard,post);
        Progress.select(healer,Element.WATER);
        Zombie infector=EntityTypes.ZOMBIE.create(level,EntitySpawnReason.EVENT);
        infector.snapTo(-3.5,y+20,34.5);infector.setNoAi(true);
        check(infector.convertVillagerToZombieVillager(level,guard),"Native zombification succeeds");
        var area=new AABB(-5,y+19,31,-2,y+24,34);
        var zombies=level.getEntitiesOfClass(net.minecraft.world.entity.monster.zombie.ZombieVillager.class,area);
        check(zombies.size()==1,"Zombified guardian found");
        var zombie=zombies.getFirst();
        check(Guardians.element(zombie)==Element.FIRE&&zombie.entityTags().contains("cb_owner_"+owner.getUUID()),
            "Native zombification preserves guardian element and owner");
        check(!Creeperborn.GUARDIANS.cure(player(level),zombie),"Trident curing requires all four unlocked elements");
        check(Creeperborn.GUARDIANS.cure(healer,zombie),"Trident instantly performs native zombie-villager cure");
        var restored=level.getEntitiesOfClass(Villager.class,area);
        check(restored.size()==1,"Cured villager found");Villager v=restored.getFirst();
        check(Guardians.element(v)==Element.FIRE&&Guardians.owns(owner,v)&&!Guardians.owns(healer,v),
            "Cure retains original Fire faction and owner rather than healer selected Water");
        check(Creeperborn.GUARDIANS.post(v).equals(post),"Guardian post survives zombification and cure");
        check(v.getVillagerXp()==23&&v.getOffers().size()==1&&v.getOffers().getFirst().getResult().is(Items.BREAD)
            &&v.getOffers().getFirst().getResult().getCount()==3,"Native cure preserves stored experience and trade offers");
        Zombie enemy=EntityTypes.ZOMBIE.create(level,EntitySpawnReason.EVENT);enemy.snapTo(-3.5,y+20,29.5);enemy.setNoAi(true);level.addFreshEntity(enemy);
        Creeperborn.GUARDIANS.tick();
        check(enemy.getHealth()<enemy.getMaxHealth()&&enemy.isOnFire(),"Restored Fire guardian resumes elemental defense");
        enemy.discard();v.discard();infector.discard();Creeperborn.GUARDIANS.clear();
        var ordinary=EntityTypes.ZOMBIE_VILLAGER.create(level,EntitySpawnReason.EVENT);
        ordinary.snapTo(-3.5,y+20,32.5);ordinary.setNoAi(true);level.addFreshEntity(ordinary);
        check(Creeperborn.GUARDIANS.cure(healer,ordinary),"Trident also cures ordinary zombie villagers");
        var normal=level.getEntitiesOfClass(Villager.class,area);
        check(normal.size()==1&&Guardians.element(normal.getFirst())==null,"Ordinary cure does not invent elemental powers");
        normal.forEach(net.minecraft.world.entity.Entity::discard);
    }
    private void checkExplicitFruitCommands(ServerLevel level){
        var dispatcher=server.getCommands().getDispatcher();
        var root=dispatcher.getRoot().getChild("creeperborn").getChild("testfruit");
        check(root.getChild("rubber")!=null&&root.getChild("door")!=null&&root.getChild("phoenix")!=null,"All three fruit names appear in command completion");
        var p=doorPlayer(level,"FruitCommands");
        try{
            dispatcher.execute("creeperborn testfruit",p.createCommandSourceStack().withPermission(net.minecraft.server.permissions.PermissionSet.ALL_PERMISSIONS));
            check(count(p,"fruit:rubber")==0&&count(p,"fruit:door")==0,"Bare testfruit command grants no default fruit");
            dispatcher.execute("creeperborn testfruit rubber",p.createCommandSourceStack().withPermission(net.minecraft.server.permissions.PermissionSet.ALL_PERMISSIONS));
            dispatcher.execute("creeperborn testfruit door",p.createCommandSourceStack().withPermission(net.minecraft.server.permissions.PermissionSet.ALL_PERMISSIONS));
            dispatcher.execute("creeperborn testfruit phoenix",p.createCommandSourceStack().withPermission(net.minecraft.server.permissions.PermissionSet.ALL_PERMISSIONS));
            check(count(p,"fruit:rubber")==1&&count(p,"fruit:door")==1&&count(p,"fruit:phoenix")==1,"Explicit fruit commands grant requested fruit only");
            var source=p.createCommandSourceStack().withPermission(net.minecraft.server.permissions.PermissionSet.ALL_PERMISSIONS);
            check(dispatcher.execute("creeperborn fruitmastery 3",source)==0,"Generic mastery refuses to grant an uneaten fruit");
            for(int fruit=0;fruit<4;fruit++){
                check(fruit==0?Rubber.consume(p,Relics.fruit()):fruit==1?DoorFruit.consume(p,Relics.doorFruit()):fruit==2?Phoenix.consume(p,Relics.phoenixFruit()):Lightning.consume(p,Relics.lightningFruit()),"Command test eats its own fruit");
                for(int tier:new int[]{2,3,1}){
                    check(dispatcher.execute("creeperborn fruitmastery "+tier,source)==1,"Generic fruitmastery accepts tier");
                    check((fruit==0?Rubber.tier(p):fruit==1?DoorFruit.tier(p):fruit==2?Phoenix.tier(p):Lightning.tier(p))==tier,"Generic mastery applies to the owned fruit");
                }
                FruitClaims.releaseOnDeath(p);
            }
        }catch(com.mojang.brigadier.exceptions.CommandSyntaxException error){throw new AssertionError(error);}
    }


    private void clock(ServerLevel level,long time){((net.minecraft.world.level.storage.ServerLevelData)level.getLevelData()).setGameTime(time);}
    private void checkPhoenixTalonForm(ServerLevel level,Player p,int range){
        check(Phoenix.talonRange(p)==range,"Talon range follows active Phoenix form");
        Zombie target=EntityTypes.ZOMBIE.create(level,EntitySpawnReason.EVENT);
        target.setNoAi(true);target.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ARMOR).setBaseValue(0);
        target.snapTo(p.getX(),p.getY(),p.getZ()+range-1);level.addFreshEntity(target);
        clock(level,level.getGameTime()+101);
        check(Phoenix.cast(p,0,0).equals("Phoenix Talon!")&&Math.abs(target.getHealth()-14)<.001,"Transformed Talon hits near boosted reach without increasing damage");
        target.snapTo(p.getX(),p.getY(),p.getZ()+range+1);target.setHealth(20);target.damageCooldownTime=0;
        clock(level,level.getGameTime()+21);Phoenix.cast(p,0,0);
        check(target.getHealth()==20,"Transformed Talon cannot exceed its own range cap");
        target.discard();
    }
    private void checkPhoenixMarker(ServerLevel level,Player p,boolean hidden){
        try{
            var field=net.minecraft.world.entity.Entity.class.getDeclaredField("firstTick");field.setAccessible(true);field.setBoolean(p,false);
        }catch(ReflectiveOperationException error){throw new AssertionError(error);}
        var viewer=doorPlayer(level,"PhoenixViewer");viewer.snapTo(p.getX()+2,p.getY(),p.getZ());
        p.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.WAYPOINT_TRANSMIT_RANGE).setBaseValue(128);
        viewer.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.WAYPOINT_RECEIVE_RANGE).setBaseValue(128);
        check(p.makeWaypointConnectionWith(viewer).isEmpty()==hidden,"Locator marker follows full Phoenix form only");
    }
    private void checkPhoenix(ServerLevel level){
        Phoenix.clear();FruitCreepers.clear();
        Player p=player(level);p.snapTo(-12.5,y+20,20.5);p.setYRot(0);p.setXRot(0);
        check(Phoenix.consume(p,Relics.phoenixFruit())&&Phoenix.tier(p)==1,"Phoenix fruit unlocks independently at tier one");
        check(CombatNetwork.selection(p)==6&&!Rubber.consume(p,Relics.fruit())&&!DoorFruit.consume(p,Relics.doorFruit()),"Phoenix selection and one-fruit cap");
        FruitClaims.load(server);check(Phoenix.unlocked(p),"Phoenix ownership persists across claim reload");
        check((WaterRules.flags(p)&1)!=0&&WaterRules.vulnerable(p),"Phoenix inherits the fruit water curse");
        check(Phoenix.cast(p,1,0).contains("needs")&&Phoenix.cast(p,1,8).contains("needs"),"Mastery gates partial and full flight");
        Zombie target=EntityTypes.ZOMBIE.create(level,EntitySpawnReason.EVENT);target.snapTo(-12.5,y+20,24.5);target.setNoAi(true);
        target.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ARMOR).setBaseValue(0);level.addFreshEntity(target);
        check(Phoenix.cast(p,0,0).equals("Phoenix Talon!")&&Math.abs(target.getHealth()-14)<.001,"Talon deals six damage to an aimed hostile");
        check(Phoenix.uses(p)==1&&Phoenix.cast(p,0,0).contains("recovering"),"Successful Talon credits mastery and observes cooldown");
        target.snapTo(-12.5,y+20,29.5);target.setHealth(20);target.damageCooldownTime=0;clock(level,level.getGameTime()+21);
        Phoenix.cast(p,0,0);check(Math.abs(target.getHealth()-14)<.001,"Talon reaches hostile nine blocks away");
        target.snapTo(-12.5,y+20,31.5);target.setHealth(20);target.damageCooldownTime=0;clock(level,level.getGameTime()+21);
        Phoenix.cast(p,0,0);check(target.getHealth()==20,"Talon excludes hostile beyond ten blocks");
        target.snapTo(-12.5,y+20,29.5);
        for(int dy=20;dy<=22;dy++)level.setBlockAndUpdate(new BlockPos(-13,(int)y+dy,25),Blocks.STONE.defaultBlockState());
        clock(level,level.getGameTime()+21);Phoenix.cast(p,0,0);
        check(target.getHealth()==20,"Longer Talon still cannot attack through a wall");
        for(int dy=20;dy<=22;dy++)level.setBlockAndUpdate(new BlockPos(-13,(int)y+dy,25),Blocks.AIR.defaultBlockState());
        target.snapTo(-12.5,y+20,24.5);
        target.setHealth(20);target.damageCooldownTime=0;clock(level,level.getGameTime()+21);
        Phoenix.cast(p,0,8);
        for(int i=0;i<8;i++){clock(level,level.getGameTime()+1);Phoenix.tickPlayer(p);p.move(net.minecraft.world.entity.MoverType.SELF,p.getDeltaMovement());}
        check(target.getHealth()<20,"Swept Phoenix Dive hits a hostile along movement path");
        p.snapTo(-12.5,y+20,20.5);target.setHealth(20);target.damageCooldownTime=0;clock(level,level.getGameTime()+101);
        for(int dy=20;dy<=22;dy++)level.setBlockAndUpdate(new BlockPos(-13,(int)y+dy,22),Blocks.STONE.defaultBlockState());
        Phoenix.cast(p,0,8);
        for(int i=0;i<8;i++){clock(level,level.getGameTime()+1);Phoenix.tickPlayer(p);p.move(net.minecraft.world.entity.MoverType.SELF,p.getDeltaMovement());}
        check(target.getHealth()==20,"Solid wall stops Phoenix Dive before a hostile");
        for(int dy=20;dy<=22;dy++)level.setBlockAndUpdate(new BlockPos(-13,(int)y+dy,22),Blocks.AIR.defaultBlockState());
        target.discard();p.snapTo(-12.5,y+20,20.5);
        Phoenix.grant(p,2);check(Phoenix.cast(p,1,0).startsWith("Wing Flight!")&&Phoenix.wing(p)&&p.getAbilities().flying,"Tap X grants wing flight at tier two");
        checkPhoenixMarker(level,p,false);
        checkPhoenixTalonForm(level,p,15);
        clock(level,level.getGameTime()+601);Phoenix.tickPlayer(p);
        check(!Phoenix.wing(p)&&!p.getAbilities().mayfly&&!p.getAbilities().flying,"Thirty-second flight expires and restores permissions");
        check(Phoenix.talonRange(p)==10,"Ending flight restores normal Talon reach");
        check(Phoenix.cast(p,1,0).contains("recovering"),"Wing Flight has five-second recovery");
        Phoenix.grant(p,3);p.getFoodData().setFoodLevel(20);
        check(Phoenix.cast(p,1,8).startsWith("Phoenix Form!")&&Phoenix.form(p),"Hold X activates full Phoenix form at tier three");
        checkPhoenixMarker(level,p,true);
        checkPhoenixTalonForm(level,p,20);
        check(Phoenix.damage(p,p.damageSources().generic(),10)==5,"Normal form reduces ordinary damage by half");
        p.damageCooldownTime=0;p.setHealth(20);p.hurtServer(level,p.damageSources().generic(),4);
        check(Math.abs(p.getHealth()-18)<.001,"Native damage path applies normal-form mitigation");
        check(Phoenix.immune(p,p.damageSources().lava())&&Phoenix.immune(p,p.damageSources().fall()),"Phoenix grants fire and fall immunity without elemental mastery");
        check(!Phoenix.revive(p,p.damageSources().fellOutOfWorld())&&!Phoenix.spent(p),"Void bypasses revival without consuming it");
        p.setHealth(10);clock(level,(level.getGameTime()/40+1)*40);Phoenix.tickPlayer(p);
        check(Math.abs(p.getHealth()-11)<.001,"Phoenix Form passively heals half a heart every two seconds");
        Villager near=EntityTypes.VILLAGER.create(level,EntitySpawnReason.EVENT),far=EntityTypes.VILLAGER.create(level,EntitySpawnReason.EVENT);
        near.snapTo(-12.5,y+20,29);far.snapTo(-12.5,y+20,32);near.setNoAi(true);far.setNoAi(true);near.setHealth(10);far.setHealth(10);level.addFreshEntity(near);level.addFreshEntity(far);
        p.setHealth(10);Phoenix.press(p,2);clock(level,level.getGameTime()+1);Phoenix.release(p,2);
        check(Phoenix.healing(p),"Tap C toggles Healing Flames on without holding");
        clock(level,(level.getGameTime()/40+1)*40);Phoenix.tickPlayer(p);
        check(Phoenix.healing(p)&&Math.abs(p.getHealth()-12)<.001,"Channel healing replaces rather than stacks passive self healing");
        check(Math.abs(near.getHealth()-12)<.001&&far.getHealth()==10,"Healing Flames reaches ten-block radius only");
        Phoenix.press(p,2);clock(level,level.getGameTime()+40);Phoenix.heartbeat(p);Phoenix.tickPlayer(p);
        check(Phoenix.blessings()==1,"Holding C applies a timed blessing to nearby allies");
        Phoenix.release(p,2);
        near.setHealth(10);near.snapTo(-12.5,y+20,34);
        clock(level,level.getGameTime()+40);Phoenix.tickBlessings();
        check(Math.abs(near.getHealth()-11.5)<.001,"Blessing follows allies beyond the circle at seventy-five percent healing strength");
        near.snapTo(-12.5,y+20,29);near.setHealth(10);p.setHealth(20);
        clock(level,(level.getGameTime()/40+1)*40);Phoenix.tickPlayer(p);Phoenix.tickBlessings();
        check(Math.abs(near.getHealth()-12)<.001,"Circle healing and lingering blessing do not stack");
        check(Phoenix.cast(p,2,0).contains("stopped")&&!Phoenix.healing(p),"Second C tap stops Healing Flames");
        Phoenix.cancelInput(p);near.discard();far.discard();
        Mastery.set(p,"cb_phoenix_fuel_",0);p.getFoodData().setFoodLevel(20);
        for(int i=0;i<60;i++){clock(level,(level.getGameTime()/20+1)*20);Phoenix.tickPlayer(p);}
        check(p.getFoodData().getFoodLevel()==19,"Flight consumes half a hunger icon per minute of active flight");
        clock(level,level.getGameTime()+601);Phoenix.tickPlayer(p);
        check(!Phoenix.form(p)&&Phoenix.cast(p,1,8).contains("recovering"),"Normal full form stops at ninety seconds and starts thirty-second recovery");
        for(Element e:Element.values())Mastery.grant(p,e,3);
        clock(level,level.getGameTime()+601);p.getFoodData().setFoodLevel(20);Phoenix.cast(p,1,8);
        clock(level,level.getGameTime()+18000);Phoenix.tickPlayer(p);
        check(Phoenix.form(p),"All-four mastered Phoenix Form has no duration cap");
        p.setHealth(20);p.damageCooldownTime=0;p.hurtTime=0;
        check(!p.hurtServer(level,p.damageSources().generic(),100)&&p.getHealth()==20&&p.hurtTime==0,"Attacks pass through all-four mastered Phoenix Form without hurt feedback");
        check(!Phoenix.immune(p,p.damageSources().genericKill()),"Even mastered Phoenix does not block kill command");
        Player attacker=player(level);p.damageCooldownTime=0;p.setHealth(20);
        check(!p.hurtServer(level,p.damageSources().playerAttack(attacker),4)&&p.getHealth()==20,"Unmastered player cannot pierce mastered Phoenix immunity");
        for(Element e:Element.values())Mastery.grant(attacker,e,3);
        p.damageCooldownTime=0;
        check(p.hurtServer(level,p.damageSources().playerAttack(attacker),4)&&Math.abs(p.getHealth()-18)<.001,"All-four master melee pierces full Phoenix immunity at half damage");
        var arrow=EntityTypes.ARROW.create(level,EntitySpawnReason.EVENT);arrow.setOwner(attacker);
        p.damageCooldownTime=0;p.hurtServer(level,p.damageSources().arrow(arrow,attacker),4);
        check(Math.abs(p.getHealth()-16)<.001,"Projectile inherits its owner's all-four mastery");
        Mastery.grant(attacker,Element.EARTH,2);p.damageCooldownTime=0;
        check(!p.hurtServer(level,p.damageSources().arrow(arrow,attacker),4)&&Math.abs(p.getHealth()-16)<.001,"Losing a mastery immediately removes projectile bypass");
        arrow.discard();

        Mastery.grant(attacker,Element.EARTH,3);p.damageCooldownTime=0;
        p.hurtServer(level,p.damageSources().playerAttack(attacker),100);
        check(p.isAlive()&&Math.abs(p.getHealth()-10)<.001&&Phoenix.spent(p)&&Phoenix.unlocked(p),"Native fatal hit triggers one resurrection at half health without losing fruit");
        check(!Phoenix.form(p)&&!p.getAbilities().mayfly&&Phoenix.recovery(p)==300&&Phoenix.strength(p)==.5,"Rebirth ends form and starts five active minutes at half strength");
        check(Phoenix.immune(p,p.damageSources().generic()),"Rebirth grants three-second protection");
        checkPhoenixMarker(level,p,false);
        Mastery.grant(p,Element.EARTH,2);
        Phoenix.forget(p);FruitClaims.load(server);
        check(Phoenix.spent(p)&&Phoenix.recovery(p)==300&&Phoenix.strength(p)==.5,"Logout/reload cannot reset revival or skip recovery");
        for(int i=0;i<6;i++){p.snapTo(p.getX()+.3,p.getY(),p.getZ());clock(level,(level.getGameTime()/20+1)*20);Phoenix.tickPlayer(p);}
        check(Phoenix.recovery(p)<300&&Phoenix.recovery(p)>290,"Recovery decreases only with active online play");
        Mastery.set(p,"cb_phoenix_recovery_",0);check(Phoenix.spent(p)&&Phoenix.strength(p)==1,"Recovery restores strength without recharging resurrection");
        clock(level,level.getGameTime()+601);p.getFoodData().setFoodLevel(20);p.setHealth(20);Phoenix.cast(p,1,8);
        p.damageCooldownTime=0;p.hurtServer(level,p.damageSources().generic(),100);
        check(!p.isAlive()&&FruitClaims.phoenixAvailable()&&!Phoenix.unlocked(p),"Second fatal hit causes real death and releases Phoenix");
        check(Progress.unlocked(p,Element.AIR),"Actual fruit death preserves elemental progress");
        Player next=player(level);check(Phoenix.consume(next,Relics.phoenixFruit()),"Phoenix can be eaten by next owner after death");Phoenix.grant(next,3);Phoenix.cast(next,1,8);
        next.damageCooldownTime=0;next.hurtServer(level,next.damageSources().genericKill(),1000);
        check(!next.isAlive()&&FruitClaims.phoenixAvailable(),"Native kill bypasses fresh Phoenix resurrection");
        Phoenix.clear();FruitCreepers.clear();
    }

    private Player player(ServerLevel level) { return player(level,UUID.randomUUID()); }
    private Player player(ServerLevel level,UUID id) {
        Player p = new Player(level, new GameProfile(id, "BenderTest")) {
            @Override public GameType gameMode() { return GameType.SURVIVAL; }
        };
        p.snapTo(.5, y, .5); p.setHealth(20); p.setYRot(0); p.setXRot(0); return p;
    }
    private void checkLightning(ServerLevel level){
        Lightning.clear();FruitCreepers.clear();
        Player p=player(level);p.snapTo(-12.5,y+26,8.5);p.setYRot(0);p.setXRot(0);
        check(Lightning.consume(p,Relics.lightningFruit())&&Lightning.tier(p)==1,"Lightning fruit unlocks tier one");
        check(CombatNetwork.selection(p)==7&&WaterRules.vulnerable(p),"Lightning uses fruit controls and inherits the water curse");
        check(!Phoenix.consume(p,Relics.phoenixFruit())&&!Lightning.consume(player(level),Relics.lightningFruit()),"Lightning obeys personal and world ownership limits");
        FruitClaims.load(server);check(Lightning.unlocked(p),"Lightning claim persists across reload");
        check(Lightning.cast(p,1,0).contains("needs")&&Lightning.cast(p,2,0).contains("needs"),"Movement and Storm are mastery gated");
        Zombie target=EntityTypes.ZOMBIE.create(level,EntitySpawnReason.EVENT),chain=EntityTypes.ZOMBIE.create(level,EntitySpawnReason.EVENT);
        target.snapTo(-12.5,y+26,20.5);chain.snapTo(-10.5,y+26,21.5);
        for(Zombie e:new Zombie[]{target,chain}){e.setNoAi(true);e.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ARMOR).setBaseValue(0);level.addFreshEntity(e);}
        check(Lightning.cast(p,0,0).equals("Lightning Bolt!")&&Math.abs(target.getHealth()-12)<.001,"Aimed bolt reaches twelve blocks and deals eight damage");
        check(Math.abs(chain.getHealth()-16)<.001&&Lightning.uses(p)==1,"Lightning chains at half damage and credits one successful cast");
        check(Lightning.cast(p,0,0).contains("recovering"),"Bolt cooldown is enforced");
        check(LightningBolts.activeCount()==2,"Tap bolt and chained hit create visible lightning arcs");
        var boltDisplays=level.getEntitiesOfClass(net.minecraft.world.entity.Display.BlockDisplay.class,p.getBoundingBox().inflate(32)).stream()
            .filter(d->d.getCustomName()!=null&&LightningBolts.variant(d.getCustomName().getString())>=0).toList();
        check(boltDisplays.size()==2&&boltDisplays.stream().noneMatch(net.minecraft.world.entity.Entity::shouldBeSaved),"Bolt displays are temporary and excluded from world saves");
        check(boltDisplays.stream().anyMatch(d->d.position().distanceToSqr(LightningBolts.hand(p))<.0001),"Primary bolt originates at the casting hand");
        for(int i=0;i<6;i++)LightningBolts.tick();
        check(LightningBolts.activeCount()==0,"Bolt visual expires after six ticks");
        clock(level,level.getGameTime()+9);check(Lightning.cast(p,0,0).contains("recovering"),"Tap Z still recovers before half a second");
        clock(level,level.getGameTime()+1);target.damageCooldownTime=0;
        check(Lightning.cast(p,0,0).equals("Lightning Bolt!"),"Tap Z recovers at half a second");

        target.setHealth(20);chain.setHealth(20);target.damageCooldownTime=chain.damageCooldownTime=0;
        clock(level,level.getGameTime()+21);
        for(int dy=26;dy<=28;dy++)level.setBlockAndUpdate(new BlockPos(-13,(int)y+dy,15),Blocks.STONE.defaultBlockState());
        Lightning.cast(p,0,0);check(target.getHealth()==20&&chain.getHealth()==20,"Walls block initial lightning targeting");
        for(int dy=26;dy<=28;dy++)level.setBlockAndUpdate(new BlockPos(-13,(int)y+dy,15),Blocks.AIR.defaultBlockState());
        clock(level,level.getGameTime()+21);Lightning.press(p,0);clock(level,level.getGameTime()+159);Lightning.release(p,0);
        check(target.getHealth()==20,"Judgment released before eight seconds cancels without damage");
        Lightning.press(p,0);clock(level,level.getGameTime()+160);Lightning.heartbeat(p);Lightning.tickPlayer(p);Lightning.release(p,0);
        check(!target.isAlive()&&!chain.isAlive(),"Eight-second charged strike hits its target and impact area");
        check(!level.getBlockState(new BlockPos(-13,(int)y+26,20)).is(Blocks.FIRE),"Lightning attack does not set terrain on fire");
        target.discard();chain.discard();
        Lightning.grant(p,2);
        for(int dy=26;dy<=28;dy++)level.setBlockAndUpdate(new BlockPos(-13,(int)y+dy,12),Blocks.STONE.defaultBlockState());
        check(Lightning.flashDestination(p,20).z<11.8,"Flash Step checks full body clearance before a wall");
        check(Lightning.cast(p,1,0).startsWith("Flash Step!")&&p.getZ()<11.8&&p.getZ()>8.5,"Flash Step moves to the last clear position");
        check(Lightning.flashCharges(p)==2,"One Flash Step spends one of three normal charges");
        for(int dy=26;dy<=28;dy++)level.setBlockAndUpdate(new BlockPos(-13,(int)y+dy,12),Blocks.AIR.defaultBlockState());
        p.snapTo(-12.5,y+26,8.5);Lightning.press(p,1);clock(level,level.getGameTime()+7);Lightning.heartbeat(p);Lightning.tickPlayer(p);
        check(p.getDeltaMovement().z>1,"Held X starts fast forward Lightning Rush");
        Lightning.release(p,1);check(p.getDeltaMovement().lengthSqr()==0,"Releasing Rush stops its movement immediately");
        check(Lightning.ready(p,"rush")>level.getGameTime(),"Releasing Rush starts recovery");
        Lightning.grant(p,3);p.getFoodData().setFoodLevel(20);
        check(Lightning.cast(p,2,0).startsWith("Storm Form!")&&Lightning.storm(p)&&p.getAbilities().flying,"Storm Form grants flight at tier three");
        check(Lightning.duration(p)==1200&&Lightning.damage(p,p.damageSources().generic(),10)==5,"Unmastered Storm lasts sixty seconds and halves damage");
        target=EntityTypes.ZOMBIE.create(level,EntitySpawnReason.EVENT);target.snapTo(-12.5,y+26,30.5);target.setNoAi(true);
        target.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ARMOR).setBaseValue(0);
        target.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(100);target.setHealth(100);level.addFreshEntity(target);
        clock(level,level.getGameTime()+301);Lightning.cast(p,0,0);
        check(Math.abs(target.getHealth()-86)<.001,"Storm aims powerful fourteen-damage strikes over twenty blocks");
        Lightning.end(p);check(!p.getAbilities().mayfly&&!p.getAbilities().flying,"Ending Storm restores original flight permissions");
        for(Element element:Element.values())Mastery.grant(p,element,3);
        clock(level,level.getGameTime()+901);Lightning.cast(p,2,0);
        check(Lightning.duration(p)==1800&&Lightning.storm(p),"All-four mastery extends Storm to ninety seconds");
        target.setHealth(100);target.damageCooldownTime=0;Lightning.cast(p,0,0);
        check(Math.abs(target.getHealth()-82.5)<.001,"Mastered Storm boosts strike damage by twenty-five percent");
        p.setHealth(20);p.damageCooldownTime=0;
        check(!p.hurtServer(level,p.damageSources().generic(),4)&&p.getHealth()==20,"Ordinary attacks pass through mastered Storm");
        Player attacker=player(level);for(Element element:Element.values())Mastery.grant(attacker,element,3);
        p.damageCooldownTime=0;p.hurtServer(level,p.damageSources().playerAttack(attacker),4);
        check(Math.abs(p.getHealth()-18)<.001,"All-four master can hurt Storm at half damage");
        Lightning.forget(p);FruitClaims.load(server);
        check(Lightning.unlocked(p)&&Lightning.tier(p)==3&&!p.getAbilities().mayfly&&Lightning.ready(p,"storm")>level.getGameTime(),"Logout keeps ownership/mastery and recovery without leaked flight");
        Mastery.grant(p,Element.EARTH,2);clock(level,level.getGameTime()+901);p.getFoodData().setFoodLevel(20);Lightning.cast(p,2,0);
        clock(level,level.getGameTime()+1201);Lightning.tickPlayer(p);
        check(!Lightning.storm(p)&&!p.getAbilities().mayfly,"Unmastered Storm expires at sixty seconds");
        clock(level,level.getGameTime()+901);p.getFoodData().setFoodLevel(20);Lightning.cast(p,2,0);
        level.setBlockAndUpdate(p.blockPosition().above(),Blocks.WATER.defaultBlockState());
        check(WaterRules.suppressed(p),"Lightning water curse returns without all-four mastery");
        WaterRules.apply(p);check(!Lightning.storm(p)&&!p.getAbilities().mayfly,"Submersion ends Storm and restores flight");
        level.setBlockAndUpdate(p.blockPosition().above(),Blocks.AIR.defaultBlockState());
        p.damageCooldownTime=0;p.hurtServer(level,p.damageSources().genericKill(),1000);
        check(!p.isAlive()&&FruitClaims.lightningAvailable()&&!Lightning.unlocked(p),"Actual death releases Lightning and clears its powers");
        check(Progress.unlocked(p,Element.AIR),"Lightning death preserves elemental progression");
        target.discard();Lightning.clear();
    }

    private void holdJudgment(ServerLevel level,Player p,int ticks){
        Lightning.press(p,0);
        for(int i=1;i<=ticks;i++){
            clock(level,level.getGameTime()+1);
            if(i%4==0)Lightning.heartbeat(p);
            Lightning.press(p,0); // Repeated key-downs cannot restart a held charge.
            Lightning.tickPlayer(p);
        }
    }
    private void checkLightningChargeAndFlashes(ServerLevel level){
        Lightning.clear();Player p=player(level);p.snapTo(-12.5,y+28,8.5);p.setYRot(0);p.setXRot(0);
        check(Lightning.consume(p,Relics.lightningFruit()),"Fresh owner obtains Lightning for charge tests");
        p.setHealth(6);p.getFoodData().setFoodLevel(0);
        Zombie target=EntityTypes.ZOMBIE.create(level,EntitySpawnReason.EVENT);target.snapTo(-12.5,y+28,20.5);target.setNoAi(true);
        target.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ARMOR).setBaseValue(0);
        target.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(100);target.setHealth(100);level.addFreshEntity(target);
        holdJudgment(level,p,159);
        check(Lightning.chargeTicks(p)==159,"Low health and empty hunger do not reset normal Judgment charge");
        Lightning.release(p,0);check(target.getHealth()==100,"Judgment still cannot fire one tick early");
        holdJudgment(level,p,160);check(Lightning.chargeTicks(p)==160,"Normal Judgment reaches full charge with repeated key-downs");
        Lightning.release(p,0);check(Math.abs(target.getHealth()-76)<.001,"Low-health hungry caster can fire normal Judgment");
        clock(level,level.getGameTime()+301);Lightning.press(p,0);clock(level,level.getGameTime()+16);Lightning.tickPlayer(p);
        check(Lightning.chargeTicks(p)==0,"Missing heartbeat still cancels stale held input");
        p.getFoodData().setFoodLevel(20);Lightning.grant(p,3);
        check(Lightning.cast(p,2,0).startsWith("Storm Form!"),"Low health does not block Storm Form");
        target.setHealth(100);target.damageCooldownTime=0;
        holdJudgment(level,p,160);Lightning.release(p,0);
        check(Math.abs(target.getHealth()-60)<.001,"Storm Judgment retains its eight-second charge and damage");
        Lightning.end(p);p.getFoodData().setFoodLevel(20);
        check(Lightning.flashCapacity(p)==3&&Lightning.flashCharges(p)==3,"Normal Flash Step starts with three charges");
        for(int i=0;i<3;i++){
            p.snapTo(-12.5,y+28,8.5);check(Lightning.cast(p,1,0).startsWith("Flash Step!"),"Normal player chains three Flash Steps without delay");
        }
        check(Lightning.flashCharges(p)==0&&Lightning.cast(p,1,0).contains("recovering"),"Fourth normal jump waits for recharge");
        clock(level,level.getGameTime()+59);check(Lightning.flashCharges(p)==0,"Flash charge refill takes three full seconds");
        clock(level,level.getGameTime()+1);check(Lightning.flashCharges(p)==3,"All normal charges refill after three seconds without jumping");
        for(Element element:Element.values())Mastery.grant(p,element,3);
        check(Lightning.flashCapacity(p)==5&&Lightning.flashCharges(p)==5,"All-four mastery grants five Flash Step charges");
        for(int i=0;i<5;i++){
            p.snapTo(-12.5,y+28,8.5);check(Lightning.cast(p,1,0).startsWith("Flash Step!"),"Master chains five jumps");
        }
        check(Lightning.flashCharges(p)==0,"Sixth mastered jump is unavailable");
        Lightning.forget(p);FruitClaims.load(server);check(Lightning.flashCharges(p)==0,"Logout cannot reset spent Flash Step charges");
        clock(level,level.getGameTime()+60);check(Lightning.flashCharges(p)==5,"Mastered Flash Step refills five charges");
        Mastery.grant(p,Element.EARTH,2);check(Lightning.flashCapacity(p)==3&&Lightning.flashCharges(p)==3,"Losing all-four mastery restores normal charge capacity");
        check(LightningBolts.variant("cb_lightning_bolt|2|30")==2&&LightningBolts.variant("cb_lightning_bolt|0|NaN")==-1,"Bolt metadata rejects invalid ray lengths");
        target.discard();FruitClaims.releaseOnDeath(p);Lightning.clear();
    }

    private void checkRubberLightningCounter(ServerLevel level){
        Player caster=player(level),rubber=player(level);
        check(Rubber.consume(rubber,Relics.fruit()),"Rubber owner available for Lightning counter test");
        for(float amount:new float[]{8,4,24,14,40,50}){
            rubber.setHealth(20);rubber.damageCooldownTime=0;
            check(Lightning.hit(caster,rubber,amount)&&Math.abs(rubber.getHealth()-(20-amount*.2f))<.001,
                "Rubber resists eighty percent of bolt, chain, Judgment and Storm damage");
        }
        caster.setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND,new ItemStack(Items.DIAMOND_SWORD));
        rubber.setHealth(20);rubber.damageCooldownTime=0;Lightning.hit(caster,rubber,8);
        check(Math.abs(rubber.getHealth()-18.4)<.001,"Holding a sword does not turn an electric attack into Rubber's sharp-weapon weakness");
        check(Math.abs(Rubber.damage(rubber,caster.damageSources().playerAttack(caster),8)-12)<.001,
            "Electrical context is cleared and ordinary sharp-weapon weakness remains");
        Rubber.grant(rubber,3);rubber.getFoodData().setFoodLevel(20);Rubber.press(rubber,2,0);
        clock(level,level.getGameTime()+7);Rubber.heartbeat(rubber);Rubber.tickPlayer(rubber);
        rubber.setHealth(20);rubber.damageCooldownTime=0;Lightning.hit(caster,rubber,8);
        check(Math.abs(rubber.getHealth()-19.68)<.001,"Bounce Guard stacks with Rubber's electric resistance");
        Rubber.cancelInput(rubber);
        check(Rubber.cast(rubber,2,0).contains("Gear 5")&&Rubber.gear(rubber),"Gear 5 is active for Lightning immunity test");
        rubber.setHealth(20);rubber.damageCooldownTime=0;rubber.hurtTime=0;
        check(!Lightning.hit(caster,rubber,50)&&rubber.getHealth()==20&&rubber.hurtTime==0,"Gear 5 blocks Lightning completely without hurt feedback");
        for(Element element:Element.values())Mastery.grant(caster,element,3);
        check(!Lightning.hit(caster,rubber,50)&&rubber.getHealth()==20,"All-four mastery does not remove Rubber's Lightning counter");
        FruitClaims.releaseOnDeath(rubber);Rubber.clear();
    }

    private void checkDarkMoves(ServerLevel level){
        Dark.clear();Player p=player(level);p.snapTo(-12.5,y+30,8.5);p.setYRot(0);p.setXRot(0);
        check(Dark.consume(p,Relics.darkFruit())&&Dark.tier(p)==1&&CombatNetwork.selection(p)==8,"Dark fruit unlocks with authoritative control selection");
        Player blinded=player(level);Dark.blind(p,blinded);
        check(blinded.hasEffect(MobEffects.DARKNESS)&&blinded.getEffect(MobEffects.DARKNESS).getDuration()==60,"Dark pull applies three-second Darkness to an enemy player");
        Dark.blind(p,p);check(!p.hasEffect(MobEffects.DARKNESS),"Caster is not blinded by own pull");
        check(!Dark.consume(player(level),Relics.darkFruit()),"Dark enforces its unique owner limit");
        check(Dark.cast(p,1,0).contains("locked")&&Dark.cast(p,2,0).contains("locked"),"X and C are gated at tiers two and three");
        Zombie target=EntityTypes.ZOMBIE.create(level,EntitySpawnReason.EVENT);target.snapTo(-12.5,y+30,16.5);target.setNoAi(true);
        target.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ARMOR).setBaseValue(0);
        target.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(100);target.setHealth(100);level.addFreshEntity(target);
        check(Dark.cast(p,0,0).equals("Black Vortex!")&&target.getHealth()<100&&target.getDeltaMovement().z<0,"Black Vortex damages and reels target toward caster");
        check(Dark.cast(p,0,0).contains("recovering"),"Dark Vortex cooldown enforced");
        target.snapTo(-12.5,y+30,12.5);target.damageCooldownTime=0;
        check(Dark.cast(p,0,7).equals("Nullifying Grasp!")&&Dark.rooted(target)&&Shackles.rooted(target),"Grasp roots a nearby aimed target using native movement guard");
        check(Dark.graspTicks(p)==60,"Ordinary Grasp lasts three seconds");
        check(Dark.voidRadius(p)==6&&Dark.voidSize(p)==2.5f,"Base Void has six-block radius and enlarged orb");
        clock(level,level.getGameTime()+61);check(!Dark.rooted(target),"Grasp root expires");
        for(Element element:Element.values())Mastery.grant(p,element,3);
        check(Dark.graspTicks(p)==100,"All-four mastery extends Grasp to five seconds");
        check(Dark.voidRadius(p)==9&&Dark.voidSize(p)==4,"All-four mastery expands Void pull radius and visible orb");
        Dark.suppress(p,20,false);check(Dark.cast(p,0,0).contains("suppressed"),"Fruit suppression blocks Dark casts");
        Progress.select(p,Element.EARTH);
        check(!CombatNetwork.press(p,0).contains("suppressed"),"Element controls remain usable while fruit suppressed");
        clock(level,level.getGameTime()+21);Dark.choose(p,"dark");Dark.grant(p,2);p.getFoodData().setFoodLevel(20);
        check(Dark.cast(p,1,0).equals("Dark Mantle!")&&Dark.mantle(p),"Tier-two Mantle activates shadow form");
        Player attacker=player(level);var arrow=EntityTypes.ARROW.create(level,EntitySpawnReason.EVENT);arrow.setOwner(attacker);
        p.setHealth(20);p.damageCooldownTime=0;p.hurtServer(level,p.damageSources().arrow(arrow,attacker),4);
        check(Math.abs(p.getHealth()-19)<.001&&Dark.energy(p)==1,"Mantle reduces projectile damage by seventy-five percent and stores a charge");
        p.setHealth(20);p.damageCooldownTime=0;p.hurtServer(level,p.damageSources().playerAttack(attacker),4);
        check(Math.abs(p.getHealth()-18)<.001,"Mantle halves incoming melee damage");
        check(Dark.damage(p,p.damageSources().generic(),4)==4,"Mantle does not reduce environmental generic damage");
        for(int i=0;i<8;i++)Dark.damage(p,p.damageSources().arrow(arrow,attacker),4);
        check(Dark.energy(p)==6,"Mantle energy caps at six");
        target.setHealth(100);target.damageCooldownTime=0;
        check(Dark.cast(p,1,7).equals("Liberation!")&&Dark.energy(p)==0&&target.getHealth()<100,"Liberation spends energy on damaging outward shockwave");
        Dark.endMantle(p);Dark.grant(p,3);
        check(Dark.cast(p,2,59).contains("cancelled"),"Void Collapse requires a full three-second charge");
        check(Dark.cast(p,2,0).equals("Shadow Surge!"),"Tap C starts grounded Surge");
        Dark.tickPlayer(p);check(p.getDeltaMovement().z>0,"Shadow Surge drives forward motion");
        Dark.endMantle(p);p.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
        target.snapTo(-12.5,y+30,13.5);target.setHealth(100);target.damageCooldownTime=0;
        level.setBlockAndUpdate(new BlockPos(-13,(int)y+29,11),Blocks.STONE.defaultBlockState());
        check(Math.abs(Dark.voidCenter(p).z-p.getZ()-3)<.001&&Math.abs(Dark.voidCenter(p).x-p.getX())<.001,"Void centers three blocks in front of caster");
        check(Dark.cast(p,2,60).equals("Void Collapse!"),"Hold C places a Void");
        var spheres=level.getEntitiesOfClass(net.minecraft.world.entity.Display.BlockDisplay.class,p.getBoundingBox().inflate(40)).stream()
            .filter(d->d.getCustomName()!=null&&d.getCustomName().getString().equals("cb_dark_void")).toList();
        check(spheres.size()==1&&!spheres.getFirst().shouldBeSaved(),"Void sphere is an original temporary display excluded from saves");
        var ground=level.getEntitiesOfClass(net.minecraft.world.entity.Display.BlockDisplay.class,p.getBoundingBox().inflate(40)).stream()
            .filter(d->d.getCustomName()!=null&&d.getCustomName().getString().startsWith("cb_dark_ground|")).toList();
        check(ground.size()==1&&!ground.getFirst().shouldBeSaved()&&ground.getFirst().getCustomName().getString().contains(","),
            "Void ground shadow samples block tops into a temporary mesh");

        for(int i=0;i<81;i++){clock(level,level.getGameTime()+1);Dark.tickEffects();}
        check(spheres.getFirst().isRemoved()&&target.getHealth()<100,"Void pulses damage and its sphere cleans up at collapse");
        check(ground.getFirst().isRemoved()&&level.getBlockState(new BlockPos(-13,(int)y+29,11)).is(Blocks.STONE),"Ground shadow cleans up without changing terrain");
        FruitClaims.releaseOnDeath(p);target.discard();Dark.clear();Creeperborn.ABILITIES.clear();
    }
    private ItemStack capturedItem(Player p,String token){
        for(int i=0;i<p.getInventory().getContainerSize();i++){ItemStack item=p.getInventory().getItem(i);if(Relics.captureToken(item).equals(token))return item;}
        return ItemStack.EMPTY;
    }
    private void checkDarkCaptures(ServerLevel level){
        Dark.clear();Player dark=player(level);dark.snapTo(-12.5,y+30,8.5);
        check(Dark.consume(dark,Relics.darkFruit()),"Dark available for capture tests");Dark.grant(dark,3);
        Player phoenix=player(level);check(Phoenix.consume(phoenix,Relics.phoenixFruit()),"Phoenix victim obtains fruit");Phoenix.grant(phoenix,3);
        Phoenix.cast(phoenix,1,8);
        check(Phoenix.form(phoenix)&&phoenix.getAbilities().mayfly,"Victim enters full Phoenix before Grasp test");
        Dark.suppress(phoenix,100,true);
        check(!Phoenix.form(phoenix)&&!phoenix.getAbilities().mayfly&&Dark.suppressed(phoenix),"Strong nullification ends transformation and restores original flight");
        clock(level,level.getGameTime()+701);phoenix.getFoodData().setFoodLevel(20);Phoenix.cast(phoenix,1,8);
        check(Phoenix.form(phoenix),"Phoenix can transform after suppression and recovery expire");
        phoenix.setHealth(20);phoenix.damageCooldownTime=0;Dark.voidDamage(dark,phoenix,100);
        check(phoenix.isAlive()&&Phoenix.spent(phoenix)&&Dark.captures(dark).isEmpty(),"Phoenix Rebirth is not captured as a death");
        clock(level,level.getGameTime()+61);phoenix.damageCooldownTime=0;Dark.voidDamage(dark,phoenix,100);
        check(!phoenix.isAlive()&&Dark.captures(dark).size()==1,"Actual Void-attributed player death offers a capture");
        var first=Dark.captures(dark).getFirst();
        check(first.fruit().equals("phoenix")&&first.tier()==2&&first.token().equals(FruitClaims.owner("phoenix")),"Capture caps tier at two and reserves unique ownership");
        check(!Phoenix.consume(player(level),Relics.phoenixFruit()),"Pending capture prevents another player claiming a duplicate power");
        check(Dark.decide(dark,first.token().toString(),true).contains("eaten")&&Phoenix.unlocked(dark)&&Phoenix.tier(dark)==2,"Eat grants the captured fruit at its retained tier cap");
        check(Phoenix.selected(dark)&&!Dark.selected(dark)&&CombatNetwork.selection(dark)==6,"Captured power has independent authoritative selection");
        Dark.cycle(dark);check(Dark.selected(dark)&&!Phoenix.selected(dark),"G cycle selects only Dark");
        Dark.cycle(dark);Phoenix.grant(dark,3);Phoenix.cast(dark,1,8);
        check(Phoenix.form(dark),"Captured Phoenix can progress to full form");
        Dark.cycle(dark);check(Dark.selected(dark)&&!Phoenix.form(dark)&&!dark.getAbilities().mayfly,"Switching powers ends prior form without flight leak");
        FruitClaims.load(server);FruitClaims.reconcile(dark);
        check(Dark.unlocked(dark)&&Phoenix.unlocked(dark),"Dark plus captured ownership survives world reload and reconciliation");
        Player rubber=player(level);check(Rubber.consume(rubber,Relics.fruit()),"Second victim obtains Rubber");Rubber.grant(rubber,2);
        Dark.voidDamage(dark,rubber,100);var second=Dark.captures(dark).getFirst();
        check(Dark.decide(dark,second.token().toString(),true).contains("two powers")&&Dark.captures(dark).size()==1,"A full Dark owner cannot eat a third power");
        check(Dark.decide(dark,second.token().toString(),false).contains("materialized"),"Full owner can Materialize instead");
        ItemStack materialized=capturedItem(dark,second.token().toString()),duplicate=materialized.copy();
        check(!materialized.isEmpty()&&Relics.captureTier(materialized)==2,"Materialized item retains captured tier");
        Player friend=player(level);check(Rubber.consume(friend,materialized)&&Rubber.tier(friend)==2,"Friend can eat a materialized captured fruit");
        check(!Rubber.consume(player(level),duplicate),"Copied token cannot create duplicate fruit owners");
        Player lightning=player(level);check(Lightning.consume(lightning,Relics.lightningFruit()),"Third victim obtains Lightning");Dark.voidDamage(dark,lightning,100);
        var third=Dark.captures(dark).getFirst();check(third.tier()==1,"Tier-one captures stay tier one");
        for(int i=0;i<dark.getInventory().getContainerSize();i++)dark.getInventory().setItem(i,new ItemStack(Items.STONE,64));
        check(Dark.decide(dark,third.token().toString(),false).contains("Inventory full")&&Dark.captures(dark).size()==1,"Full inventory keeps pending capture rather than losing it");
        Dark.forget(dark);FruitClaims.load(server);FruitClaims.reconcile(dark);
        check(Dark.captures(dark).size()==1&&third.token().equals(FruitClaims.owner("lightning")),"Pending choice and reservation survive logout/reload");
        dark.getInventory().setItem(0,ItemStack.EMPTY);Dark.decide(dark,third.token().toString(),false);
        ItemStack dropped=capturedItem(dark,third.token().toString()).copy();
        for(int i=0;i<dark.getInventory().getContainerSize();i++)dark.getInventory().setItem(i,ItemStack.EMPTY);
        ItemEntity item=new ItemEntity(level,dark.getX(),dark.getY(),dark.getZ(),dropped);level.addFreshEntity(item);item.discard();
        check(FruitClaims.lightningAvailable(),"Destroying a dropped captured fruit releases its reservation");
        Player door=player(level);check(DoorFruit.consume(door,Relics.doorFruit()),"Fourth victim obtains Door");Dark.voidDamage(dark,door,100);
        var fourth=Dark.captures(dark).getFirst();
        dark.damageCooldownTime=0;dark.hurtServer(level,dark.damageSources().genericKill(),1000);
        check(!dark.isAlive()&&FruitClaims.darkAvailable()&&FruitClaims.phoenixAvailable(),"Actual Dark death releases both equipped powers");
        var drops=level.getEntitiesOfClass(ItemEntity.class,dark.getBoundingBox().inflate(3)).stream().filter(v->Relics.captureToken(v.getItem()).equals(fourth.token().toString())).toList();
        check(drops.size()==1&&Dark.captures(dark).isEmpty(),"Unresolved capture drops as a fruit on owner death");
        ItemStack doorItem=drops.getFirst().getItem().copy();drops.getFirst().setItem(ItemStack.EMPTY);drops.getFirst().discard();
        check(fourth.token().equals(FruitClaims.owner("door")),"Pickup removal preserves the captured fruit reservation");
        Player doorFriend=player(level);check(DoorFruit.consume(doorFriend,doorItem)&&DoorFruit.tier(doorFriend)==1,"Dropped pending capture remains consumable at retained tier");
        FruitClaims.releaseOnDeath(friend);FruitClaims.releaseOnDeath(doorFriend);Dark.clear();Phoenix.clear();Lightning.clear();
    }

    private boolean consumeFound(Player p,String fruit,ItemStack item){
        return switch(fruit){case "rubber"->Rubber.consume(p,item);case "door"->DoorFruit.consume(p,item);case "phoenix"->Phoenix.consume(p,item);case "lightning"->Lightning.consume(p,item);case "dark"->Dark.consume(p,item);default->false;};
    }
    private void checkFoundSecondFruit(ServerLevel level){
        for(String fruit:java.util.List.of("rubber","door","phoenix","lightning")){
            Player p=player(level);check(Dark.consume(p,Relics.darkFruit()),"Dark owner created for found-item test");
            Dark.grant(p,2);p.getFoodData().setFoodLevel(20);Dark.cast(p,1,0);check(Dark.mantle(p),"Mantle active before consuming second fruit");
            ItemStack found=Relics.fruit(fruit);
            check(consumeFound(p,fruit,found)&&found.isEmpty()&&fruit.equals(Dark.extra(p)),"Dark consumes an ordinary found fruit into its empty second slot");
            check(Dark.fruitTier(p,fruit)==1&&!Dark.mantle(p),"Found fruit starts at tier one and ends prior Dark form");
            check(Dark.choice(p).equals(fruit)&&!Dark.selected(p),"Found fruit becomes selected");
            String third=fruit.equals("rubber")?"door":"rubber";ItemStack rejected=Relics.fruit(third);
            check(!consumeFound(p,third,rejected)&&rejected.getCount()==1,"Dark cannot eat a third power and rejection preserves item");
            Dark.cycle(p);check(Dark.selected(p),"G cycles from found second fruit to Dark");
            Dark.cycle(p);check(Dark.choice(p).equals(fruit),"G cycles back to found second fruit");
            FruitClaims.load(server);FruitClaims.reconcile(p);check(Dark.unlocked(p)&&fruit.equals(Dark.extra(p)),"Found two-power pair survives claim reload");
            FruitClaims.releaseOnDeath(p);
        }
        Player ordinary=player(level);check(Rubber.consume(ordinary,Relics.fruit()),"Ordinary player obtains first fruit");
        check(!DoorFruit.consume(ordinary,Relics.doorFruit()),"Ordinary player still cannot eat two non-Dark fruits");
        Rubber.grant(ordinary,3);ordinary.getFoodData().setFoodLevel(20);Rubber.cast(ordinary,2,0);
        check(Rubber.gear(ordinary)&&ordinary.getAbilities().mayfly,"Existing power active before finding Dark");
        check(Dark.consume(ordinary,Relics.darkFruit())&&Dark.unlocked(ordinary)&&Rubber.unlocked(ordinary),"Finding Dark after one ordinary fruit establishes a two-power pair");
        check(!Rubber.gear(ordinary)&&!ordinary.getAbilities().mayfly&&Rubber.tier(ordinary)==3,"Adding Dark restores flight and preserves previously earned mastery");
        check(!Phoenix.consume(ordinary,Relics.phoenixFruit()),"Reverse acquisition order still rejects third power");
        FruitClaims.releaseOnDeath(ordinary);Dark.clear();Rubber.clear();
    }


    private void checkOp(ServerLevel level){
        Op.clear();var p=player(level);p.snapTo(-8,y+15,8);p.setYRot(0);p.setXRot(0);
        check(Op.consume(p,Relics.opFruit())&&Op.tier(p)==1&&CombatNetwork.selection(p)==9,"Op fruit selects authoritative Z/X/C controls");
        check(WaterRules.vulnerable(p)&&!Op.consume(player(level),Relics.opFruit()),"Op water curse and unique owner");
        check(Op.diameter(p)==30&&Op.duration(p)==600,"Base ROOM is 30 diameter and 30 seconds");
        check(Op.cast(p,1,0).contains("locked")&&Op.cast(p,2,0).contains("locked"),"X tier two and C tier three");
        Zombie contact=tremorTarget(level,-8,y+15,10.5),distant=tremorTarget(level,-8,y+15,25);float farHealth=distant.getHealth();
        check(Op.cast(p,0,0).equals("Gamma Knife!")&&contact.getHealth()==190&&Op.coated(p)&&!Op.room(p),"Unarmed Gamma Knife deals damage inside a hand-sized ROOM without opening the large ROOM");
        check(Op.cast(p,0,0).contains("recovering"),"Coated Gamma Knife preserves its cooldown");clock(level,level.getGameTime()+21);Op.tickPlayer(p);check(!Op.coated(p),"Small coating cleans itself up");
        BlockPos coatWall=new BlockPos(-8,(int)y+16,9);level.setBlockAndUpdate(coatWall,Blocks.STONE.defaultBlockState());float contactHealth=contact.getHealth();check(Op.cast(p,0,0).contains("nearby")&&contact.getHealth()==contactHealth,"Local ROOM coating cannot attack through solid walls");level.setBlockAndUpdate(coatWall,Blocks.AIR.defaultBlockState());
        Op.grant(p,2);var sword=new ItemStack(Items.NETHERITE_SWORD);p.setItemInHand(InteractionHand.MAIN_HAND,sword);contact.damageCooldownTime=0;check(Op.cast(p,1,0).equals("Counter Shock!")&&contact.getHealth()<contactHealth&&Op.rooted(contact)&&p.getMainHandItem()==sword,"Weapon-coated Shock damages and stuns without replacing the weapon");check(distant.getHealth()==farHealth&&!Op.rooted(distant),"Local coating cannot shock distant enemies");
        Op.grant(p,3);check(Op.cast(p,2,0).contains("ROOM")&&Op.cast(p,1,60).contains("ROOM"),"Shambles and Heart Extraction still need the large ROOM");p.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);Op.grant(p,1);clock(level,level.getGameTime()+121);p.getFoodData().setFoodLevel(20);Op.tickPlayer(p);contact.discard();distant.discard();
        Op.press(p,0);clock(level,level.getGameTime()+7);Op.heartbeat(p);Op.tickPlayer(p);Op.release(p,0);
        check(Op.room(p),"Tier one held Z opens ROOM through actual press/heartbeat/release route");
        Zombie target=EntityTypes.ZOMBIE.create(level,EntitySpawnReason.EVENT);
        target.snapTo(-8,y+15,12);target.setNoAi(true);target.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(100);target.setHealth(100);level.addFreshEntity(target);
        check(Op.inside(p,target),"Enemy inside spherical ROOM");
        check(Op.cast(p,0,0).equals("Gamma Knife!")&&target.getHealth()<100,"Tier one tap Z damages target with Gamma Knife");
        target.snapTo(-8,y+15,25);check(!Op.inside(p,target),"Targets outside ROOM excluded");
        Op.grant(p,2);check(Op.diameter(p)==70&&Op.duration(p)==1200,"Tier two ROOM intermediate size and time");
        target.snapTo(-8,y+15,12);target.damageCooldownTime=0;
        check(Op.cast(p,1,0).equals("Counter Shock!")&&Op.rooted(target),"Tier two tap X shocks and stuns");
        clock(level,level.getGameTime()+21);check(!Op.rooted(target),"Shock stun expires");
        Op.grant(p,3);check(Op.diameter(p)==120&&Op.duration(p)==1800&&Op.charges(p)==5,"Mastery ROOM 120 diameter 90 seconds and five Shambles");
        p.snapTo(-8,y+15,-15);target.snapTo(-8,y+15,40);
        BlockPos roomWall=new BlockPos(-8,(int)y+16,0);level.setBlockAndUpdate(roomWall,Blocks.STONE.defaultBlockState());
        target.damageCooldownTime=0;
        check(Op.cast(p,0,0).equals("Gamma Knife!"),"Gamma Knife reaches across ROOM through terrain beyond old range");
        check(Op.cast(p,0,0).contains("recovering"),"Gamma cooldown still prevents immediate spam");
        clock(level,level.getGameTime()+20);target.damageCooldownTime=0;
        check(Op.cast(p,0,0).equals("Gamma Knife!"),"Gamma Knife recovers after one second");
        clock(level,level.getGameTime()+120);target.damageCooldownTime=0;
        check(Op.cast(p,1,0).equals("Counter Shock!")&&Op.rooted(target),"Counter Shock reaches far target anywhere inside ROOM");
        level.setBlockAndUpdate(roomWall,Blocks.AIR.defaultBlockState());
        var mount=EntityTypes.PIG.create(level,EntitySpawnReason.EVENT);mount.snapTo(target.getX(),target.getY(),target.getZ());mount.setNoAi(true);level.addFreshEntity(mount);
        check(target.startRiding(mount),"Enemy can start mounted for Shambles regression");
        check(Op.swap(p,target).startsWith("Shambles!")&&!target.isPassenger(),"Shambles moves and dismounts a mounted enemy");
        mount.discard();clock(level,level.getGameTime()+100);p.getFoodData().setFoodLevel(20);
        for(int i=0;i<5;i++)check(Op.swap(p,target).startsWith("Shambles!"),"Five consecutive Shambles at mastery");
        check(Op.swap(p,target).contains("recharging"),"Sixth swap blocked until recharge");
        clock(level,level.getGameTime()+100);check(Op.remaining(p)==5,"Swap charges fully refill after five seconds");
        p.getFoodData().setFoodLevel(20);
        p.snapTo(-8,y+15,8);p.setYRot(0);target.snapTo(-8,y+15,30);p.getFoodData().setFoodLevel(20);
        Op.press(p,1);clock(level,level.getGameTime()+7);Op.heartbeat(p);Op.tickPlayer(p);
        for(int channel=1;channel<=60;channel++){
            clock(level,level.getGameTime()+1);p.snapTo(-8+channel*.08,y+15,8);target.snapTo(-8,y+15,30+channel*.02);
            p.setYRot(90);if(channel==15){p.setHealth(18);p.hurtTime=5;}
            Op.heartbeat(p);Op.tickPlayer(p);
        }
        check(Hearts.count(p)==1,"Held extraction completes while caster moves, looks away and takes damage, and victim moves within ROOM");
        ItemStack channelCube=ItemStack.EMPTY;
        for(int i=0;i<p.getInventory().getContainerSize();i++)if(!Hearts.token(p.getInventory().getItem(i)).isEmpty()){channelCube=p.getInventory().getItem(i);break;}
        check(Hearts.giveBack(p,target,channelCube),"Channel-created heart can be returned");
        p.snapTo(-8,y+15,8);p.setYRot(0);p.hurtTime=0;target.snapTo(-8,y+15,12);
        check(Hearts.extract(p,target),"Heart extraction reserves live target");
        ItemStack cube=ItemStack.EMPTY;
        for(int i=0;i<p.getInventory().getContainerSize();i++)if(!Hearts.token(p.getInventory().getItem(i)).isEmpty()){cube=p.getInventory().getItem(i);break;}
        check(!cube.isEmpty()&&cube.getHoverName().getString().contains("Zombie")&&cube.getHoverName().getString().contains("BenderTest"),"Heart name identifies victim and extractor");
        float health=target.getHealth();check(Hearts.squeeze(p,cube).contains("one heart")&&target.getHealth()==health-2,"Squeeze deals exactly one heart");
        check(Hearts.squeeze(p,cube).contains("Wait"),"Squeeze cooldown");
        clock(level,level.getGameTime()+20);Hearts.squeeze(p,cube);check(target.getHealth()==health-4,"Repeated squeeze damages same linked victim");
        ItemStack duplicate=cube.copy();
        check(Hearts.giveBack(p,target,cube)&&!Hearts.squeeze(p,duplicate).contains("one heart"),"Returning heart invalidates copied token");
        java.util.List<Zombie> victims=new java.util.ArrayList<>();
        for(int i=0;i<6;i++){
            Zombie z=EntityTypes.ZOMBIE.create(level,EntitySpawnReason.EVENT);z.snapTo(-7+i,y+15,11);z.setCustomName(Component.literal("HeartTarget"+i));z.setNoAi(true);level.addFreshEntity(z);victims.add(z);
            check(Hearts.extract(p,z),"Collection allows six distinct named hearts without a cap");
        }
        check(Hearts.count(p)==6,"Heart collection has no five-heart cap");
        ItemStack deadHeart=ItemStack.EMPTY,liveHeart=ItemStack.EMPTY;
        for(int i=0;i<p.getInventory().getContainerSize();i++){
            ItemStack item=p.getInventory().getItem(i);
            if(item.getHoverName().getString().contains("Heart of HeartTarget5"))deadHeart=item.copy();
            if(item.getHoverName().getString().contains("Heart of HeartTarget4"))liveHeart=item.copy();
        }
        check(!deadHeart.isEmpty()&&!liveHeart.isEmpty(),"Death cleanup selects two distinct named hearts");
        Player holder=player(level),offlineHolder=player(level);
        holder.addItem(deadHeart.copy());holder.addItem(liveHeart.copy());holder.addItem(new ItemStack(Items.GLASS,3));
        holder.setItemInHand(net.minecraft.world.InteractionHand.OFF_HAND,deadHeart.copy());holder.containerMenu.setCarried(deadHeart.copy());
        offlineHolder.addItem(deadHeart.copy());
        check(Hearts.cleanup(holder)==0,"Living targets' hearts are retained in inventory, offhand and cursor");
        Zombie doomed=victims.get(5);doomed.damageCooldownTime=0;
        check(doomed.hurtServer(level,doomed.damageSources().genericKill(),Float.MAX_VALUE)&&!doomed.isAlive(),"Unrelated external damage kills extracted target");
        check(Hearts.cleanup(holder)==3&&holder.getOffhandItem().isEmpty()&&holder.containerMenu.getCarried().isEmpty(),"Enemy death removes matching heart from inventory, offhand and cursor");
        check(count(holder,"op:heart")==1,"Another living enemy's heart is preserved");
        int plainGlass=0;for(int i=0;i<holder.getInventory().getContainerSize();i++){ItemStack item=holder.getInventory().getItem(i);if(item.is(Items.GLASS)&&Relics.kind(item).isEmpty())plainGlass+=item.getCount();}
        check(plainGlass==3,"Ordinary glass is preserved");
        Hearts.load(server);Hearts.join(offlineHolder);
        check(count(offlineHolder,"op:heart")==0,"Offline holder's dead heart disappears on join after save reload");
        ItemEntity staleDrop=new ItemEntity(level,p.getX(),p.getY(),p.getZ(),deadHeart.copy());level.addFreshEntity(staleDrop);
        check(Hearts.groundTick(staleDrop)&&staleDrop.isRemoved(),"Dead target's dropped heart disappears instead of leaving plain glass");
        Hearts.cleanup(p);
        var z=victims.getFirst();ItemStack killCube=ItemStack.EMPTY;
        for(int i=0;i<p.getInventory().getContainerSize();i++){var candidate=p.getInventory().getItem(i);if(!Hearts.token(candidate).isEmpty()){killCube=candidate.copy();candidate.shrink(1);break;}}
        ItemEntity dropped=new ItemEntity(level,p.getX(),p.getY(),p.getZ(),killCube);level.addFreshEntity(dropped);
        check(!Hearts.groundTick(dropped),"Heart starts ground return timer");
        clock(level,level.getGameTime()+5999);check(!Hearts.groundTick(dropped),"No premature five-minute return");
        clock(level,level.getGameTime()+1);check(Hearts.groundTick(dropped)&&z.isAlive(),"Five-minute return removes cube without killing victim");
        Hearts.load(server);Hearts.join(p);
        ItemStack returned=ItemStack.EMPTY;for(int i=0;i<p.getInventory().getContainerSize();i++)if(!Hearts.token(p.getInventory().getItem(i)).isEmpty()&&p.getInventory().getItem(i).getHoverName().getString().equals(killCube.getHoverName().getString())){returned=p.getInventory().getItem(i);break;}
        check(!returned.isEmpty(),"Pending return survives registry reload and delivers to owner");
        ItemEntity burn=new ItemEntity(level,p.getX(),p.getY(),p.getZ(),returned.copy());returned.shrink(1);level.addFreshEntity(burn);
        burn.remove(net.minecraft.world.entity.Entity.RemovalReason.KILLED);
        check(!z.isAlive(),"Physical heart destruction kills linked target");
        Hearts.died(p);for(var v:victims)if(v.isAlive())v.discard();target.discard();
        FruitClaims.load(server);check(Op.unlocked(p),"Op ownership survives save reload");
        check(Dark.consume(p,Relics.darkFruit()),"Op can be Dark second fruit");
        Dark.choose(p,"op");check(CombatNetwork.selection(p)==9,"G can select stored Op power");
        Dark.grantFruit(p,"op",2);check(Op.tier(p)==2,"Captured Op mastery dispatch");
        FruitClaims.releaseOnDeath(p);check(!Op.unlocked(p)&&FruitClaims.owner("op")==null&&!Op.room(p),"Actual death clears Op claim and ROOM");
        Op.clear();
    }


    private void checkGravity(ServerLevel level){
        Gravity.clear();Player p=player(level);p.snapTo(-8,y+18,8);p.setYRot(0);p.setXRot(0);
        check(Gravity.consume(p,Relics.gravityFruit())&&Gravity.tier(p)==1&&CombatNetwork.selection(p)==10,"Gravity unlocks with authoritative fruit controls");
        check(WaterRules.vulnerable(p)&&!Gravity.consume(player(level),Relics.gravityFruit())&&!Op.consume(p,Relics.opFruit()),"Gravity inherits water curse and unique/personal ownership limits");
        check(Gravity.cast(p,1,0).contains("locked")&&Gravity.cast(p,2,0).contains("locked"),"Gravity X/C require tiers two and three");
        Zombie t=EntityTypes.ZOMBIE.create(level,EntitySpawnReason.EVENT);t.snapTo(-8,y+18,14);t.setNoAi(true);
        t.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(100);t.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ARMOR).setBaseValue(0);t.setHealth(100);level.addFreshEntity(t);
        Zombie group=EntityTypes.ZOMBIE.create(level,EntitySpawnReason.EVENT);group.snapTo(-5,y+18,18);group.setNoAi(true);group.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ARMOR).setBaseValue(0);level.addFreshEntity(group);
        Zombie outside=EntityTypes.ZOMBIE.create(level,EntitySpawnReason.EVENT);outside.snapTo(-8,y+18,41);outside.setNoAi(true);level.addFreshEntity(outside);
        Zombie hidden=EntityTypes.ZOMBIE.create(level,EntitySpawnReason.EVENT);hidden.snapTo(-12,y+18,8);hidden.setNoAi(true);level.addFreshEntity(hidden);
        BlockPos wall=new BlockPos(-10,(int)y+19,8);level.setBlockAndUpdate(wall,Blocks.STONE.defaultBlockState());
        check(Gravity.range(p)==32&&Gravity.pinTicks(p)==60&&Gravity.waveRange(p)==12&&Gravity.waveDamage(p)==8,"Tier-one Gravity scaling");
        check(Gravity.cast(p,0,0).equals("Gravity Press!")&&t.getHealth()==94&&t.getDeltaMovement().y<0&&t.hasEffect(MobEffects.SLOWNESS),"Gravity Press damages, weighs down and slows targets");
        check(Gravity.rooted(t)&&Gravity.rooted(group)&&Shackles.rooted(group)&&group.getHealth()==14,"Press pins multiple visible enemies within range: root="+Gravity.rooted(t)+"/"+Gravity.rooted(group)+" native="+Shackles.rooted(group)+" health="+group.getHealth());
        check(!Gravity.rooted(outside)&&outside.getHealth()==20&&!Gravity.rooted(hidden)&&hidden.getHealth()==20,"Press excludes out-of-range and terrain-shielded enemies");
        var pinnedPosition=group.position();group.move(net.minecraft.world.entity.MoverType.SELF,new net.minecraft.world.phys.Vec3(1,0,0));
        check(group.position().equals(pinnedPosition),"Gravity pin uses native movement guard");
        level.setBlockAndUpdate(wall,Blocks.AIR.defaultBlockState());
        check(Gravity.cast(p,0,0).contains("recovering"),"Gravity Press cooldown enforced");
        t.damageCooldownTime=0;
        check(Gravity.cast(p,0,7).equals("Repulsion Wave!")&&t.getDeltaMovement().z>0,"Held Z repels enemies in front");
        Gravity.grant(p,2);p.getFoodData().setFoodLevel(20);t.damageCooldownTime=0;
        check(Gravity.range(p)==48&&Gravity.pinTicks(p)==100&&Gravity.waveRange(p)==18&&Gravity.waveDamage(p)==10,"Tier-two Press and Wave grow");
        check(Gravity.liftHeight(p)==16&&Gravity.liftTicks(p)==80&&Gravity.rideTicks(p)==600,"Tier-two levitation and thirty-second ride");
        check(Gravity.cast(p,1,0).equals("Levitate!"),"Tap X starts targeted levitation");
        Gravity.tickEffects();check(t.getDeltaMovement().y>0,"Levitation repeatedly lifts target");
        clock(level,level.getGameTime()+Gravity.liftTicks(p));t.setDeltaMovement(0,0,0);Gravity.tickEffects();
        check(t.getDeltaMovement().lengthSqr()==0,"Levitation stops applying force at its tier duration");
        boolean couldFly=p.getAbilities().mayfly;
        Gravity.press(p,1);clock(level,level.getGameTime()+7);Gravity.heartbeat(p);Gravity.tickPlayer(p);
        check(Gravity.riding(p)&&p.getDeltaMovement().z>0&&p.getAbilities().mayfly==couldFly,"Held X steers a temporary rock without granting vanilla flight");
        check(Gravity.immune(p,p.damageSources().fall()),"Rock ride protects its caster from fall damage");
        Gravity.release(p,1);check(!Gravity.riding(p)&&p.getAbilities().mayfly==couldFly,"Releasing X ends ride and preserves flight permissions");
        Gravity.grant(p,3);p.getFoodData().setFoodLevel(20);
        check(Gravity.range(p)==64&&Gravity.pinTicks(p)==160&&Gravity.waveRange(p)==24&&Gravity.waveDamage(p)==12,"Final-tier Press and Wave scaling");
        check(Gravity.liftHeight(p)==24&&Gravity.liftTicks(p)==120&&Gravity.rideTicks(p)==2400,"Final-tier levitation reaches 24 blocks for six seconds, rock ride lasts two minutes");
        check(Gravity.cast(p,2,0).equals("Orbit Guard!")&&Gravity.guarding(p),"Tier-three tap C starts orbiting defense");
        Player attacker=player(level);var arrow=EntityTypes.ARROW.create(level,EntitySpawnReason.EVENT);arrow.setOwner(attacker);arrow.snapTo(p.getX()+1,p.getY()+1,p.getZ());level.addFreshEntity(arrow);
        p.setHealth(20);p.damageCooldownTime=0;p.hurtServer(level,p.damageSources().arrow(arrow,attacker),4);
        check(Math.abs(p.getHealth()-19.6)<.001,"Final-tier Orbit Guard reduces projectile damage by ninety percent");
        check(Math.abs(Gravity.damage(p,p.damageSources().playerAttack(attacker),4)-1.2)<.001&&Gravity.damage(p,p.damageSources().generic(),4)==4,"Final-tier guard reduces melee by seventy percent while environmental damage remains normal");
        check(Gravity.guardTicks(p)==320&&Gravity.guardRocks(p)==8,"Final-tier guard has eight rocks for sixteen seconds");
        Gravity.tickPlayer(p);check(arrow.isRemoved(),"Orbiting rocks intercept nearby hostile projectiles");
        Dark.suppress(p,20,false);Gravity.tickPlayer(p);
        check(!Gravity.guarding(p)&&Gravity.cast(p,0,0).contains("darkness"),"Darkness suppression ends guard and blocks gravity input");
        clock(level,level.getGameTime()+21);p.getFoodData().setFoodLevel(20);
        check(Gravity.cast(p,2,59).contains("cancelled")&&Gravity.meteorCount()==0,"Meteor requires full three-second charge");
        t.setHealth(100);t.damageCooldownTime=0;t.snapTo(-8,y+18,14);
        BlockPos floor=new BlockPos(-8,(int)y+17,14);level.setBlockAndUpdate(floor,Blocks.STONE.defaultBlockState());
        Zombie blastEdge=EntityTypes.ZOMBIE.create(level,EntitySpawnReason.EVENT);blastEdge.snapTo(1,y+18,14);blastEdge.setNoAi(true);blastEdge.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(100);blastEdge.setHealth(100);level.addFreshEntity(blastEdge);
        Zombie blastOutside=EntityTypes.ZOMBIE.create(level,EntitySpawnReason.EVENT);blastOutside.snapTo(3,y+18,14);blastOutside.setNoAi(true);level.addFreshEntity(blastOutside);
        Gravity.press(p,2);clock(level,level.getGameTime()+60);Gravity.heartbeat(p);Gravity.tickPlayer(p);Gravity.release(p,2);
        check(Gravity.meteorCount()==1,"Held C release launches a charged meteor");
        var meteor=level.getEntitiesOfClass(net.minecraft.world.entity.Display.BlockDisplay.class,p.getBoundingBox().inflate(40)).stream()
            .filter(d->d.getCustomName()!=null&&d.getCustomName().getString().equals("cb_gravity_meteor")).toList();
        check(meteor.size()==1&&!meteor.getFirst().shouldBeSaved(),"Meteor uses a temporary original visual excluded from saves");
        clock(level,level.getGameTime()+30);Gravity.tickEffects();
        check(Gravity.meteorCount()==0&&meteor.getFirst().isRemoved()&&t.getHealth()<100,"Meteor impacts, damages and removes its visual");
        check(level.getBlockState(floor).is(Blocks.STONE),"Meteor leaves terrain intact");
        check(blastEdge.getHealth()<100&&blastOutside.getHealth()==20,"Expanded meteor blast hits nine blocks out and excludes eleven blocks");
        check(Gravity.range(p)==64&&Gravity.meteorRadius(p)==10,"Final-tier Gravity targeting and expanded blast radius");
        clock(level,level.getGameTime()+121);p.getFoodData().setFoodLevel(20);
        Gravity.press(p,1);clock(level,level.getGameTime()+7);Gravity.heartbeat(p);Gravity.tickPlayer(p);
        check(Gravity.riding(p),"Final-tier long ride starts");
        clock(level,level.getGameTime()+2399);Gravity.heartbeat(p);Gravity.tickPlayer(p);check(Gravity.riding(p),"Rock ride remains active just before two-minute limit");
        clock(level,level.getGameTime()+1);Gravity.heartbeat(p);Gravity.tickPlayer(p);check(!Gravity.riding(p),"Rock ride ends at two-minute limit");
        for(Element element:Element.values())Mastery.grant(p,element,3);
        check(Gravity.range(p)==80&&Gravity.meteorRadius(p)==14&&Gravity.rideTicks(p)==3600&&Gravity.guardTicks(p)==400&&Gravity.guardRocks(p)==10,"All-four mastery expands reach, meteor, ride and guard");
        check(Math.abs(Gravity.meleeProtection(p)-.8)<.001&&Math.abs(Gravity.projectileProtection(p)-.95)<.001,"All-four mastery strengthens Orbit Guard defense");
        FruitClaims.load(server);check(Gravity.unlocked(p),"Gravity claim survives ownership reload");
        check(Dark.consume(p,Relics.darkFruit()),"Dark can become Gravity owner's second fruit");
        Dark.choose(p,"gravity");Dark.grantFruit(p,"gravity",2);check(Gravity.selected(p)&&Gravity.tier(p)==2&&CombatNetwork.selection(p)==10,"Dark selection and mastery dispatch support captured Gravity");
        FruitClaims.releaseOnDeath(p);check(FruitClaims.owner("gravity")==null&&!Gravity.unlocked(p)&&!Gravity.riding(p)&&!Gravity.guarding(p),"Death releases Gravity and clears temporary forms");
        var fruitCreeper=EntityTypes.CREEPER.create(level,EntitySpawnReason.EVENT);fruitCreeper.snapTo(-8,y+18,20);FruitCreepers.assignGravity(fruitCreeper);level.addFreshEntity(fruitCreeper);
        fruitCreeper.hurtServer(level,p.damageSources().playerAttack(p),100);
        check(level.getEntitiesOfClass(ItemEntity.class,fruitCreeper.getBoundingBox().inflate(3)).stream().anyMatch(item->Relics.kind(item.getItem()).equals("fruit:gravity")),"Gravity Fruitborn creeper drops its correctly marked fruit");
        t.discard();group.discard();outside.discard();hidden.discard();blastEdge.discard();blastOutside.discard();Gravity.clear();FruitCreepers.clear();
    }


    private void checkFruitArtwork(ServerLevel level){
        for(String type:Dark.FRUITS){
            ItemStack fruit=Relics.fruit(type);
            var expected=net.minecraft.resources.Identifier.fromNamespaceAndPath("creeperborn","fruit_"+type);
            check(expected.equals(fruit.get(DataComponents.ITEM_MODEL)),"New "+type+" fruit has its own item appearance");
            check(!Relics.updateFruitModel(fruit),"Appearance refresh is idempotent");
            String token=UUID.randomUUID().toString();ItemStack captured=Relics.captured(type,token,2);
            captured.remove(DataComponents.ITEM_MODEL);
            check(Relics.updateFruitModel(captured)&&expected.equals(captured.get(DataComponents.ITEM_MODEL)),"Old captured "+type+" fruit appearance migrates");
            check(Relics.captureToken(captured).equals(token)&&Relics.captureTier(captured)==2&&Relics.kind(captured).equals("fruit:"+type),"Artwork migration preserves capture and mastery metadata");
        }
        ItemStack plain=new ItemStack(Items.CHORUS_FRUIT);var vanilla=plain.get(DataComponents.ITEM_MODEL);
        check(!Relics.updateFruitModel(plain)&&java.util.Objects.equals(vanilla,plain.get(DataComponents.ITEM_MODEL)),"Ordinary chorus fruit keeps vanilla appearance");
        Player p=player(level);ItemStack old=Relics.gravityFruit();old.remove(DataComponents.ITEM_MODEL);p.addItem(old);
        ItemStack held=Relics.opFruit();held.remove(DataComponents.ITEM_MODEL);p.setItemInHand(net.minecraft.world.InteractionHand.OFF_HAND,held);
        ItemStack cursor=Relics.darkFruit();cursor.remove(DataComponents.ITEM_MODEL);p.containerMenu.setCarried(cursor);
        Relics.refreshFruitModels(p);
        check(held.get(DataComponents.ITEM_MODEL).toString().equals("creeperborn:fruit_op")&&cursor.get(DataComponents.ITEM_MODEL).toString().equals("creeperborn:fruit_dark"),"Offhand and cursor fruit appearances refresh");
        boolean migrated=false;for(int i=0;i<p.getInventory().getContainerSize();i++){ItemStack item=p.getInventory().getItem(i);if(Relics.kind(item).equals("fruit:gravity"))migrated=net.minecraft.resources.Identifier.fromNamespaceAndPath("creeperborn","fruit_gravity").equals(item.get(DataComponents.ITEM_MODEL));}
        check(migrated,"Existing inventory fruit appearances refresh");
    }


    private void checkStringFruit(ServerLevel level){
        StringFruit.clear();Player p=player(level);p.snapTo(-8,y+18,8);p.setYRot(0);p.setXRot(0);
        check(StringFruit.consume(p,Relics.stringFruit())&&StringFruit.tier(p)==1&&CombatNetwork.selection(p)==11,"String fruit has unique authoritative controls");
        check(WaterRules.vulnerable(p)&&!StringFruit.consume(player(level),Relics.stringFruit()),"String has water curse and unique ownership");
        FruitClaims.load(server);check(StringFruit.unlocked(p),"String claim survives world reload");
        check(StringFruit.cast(p,1,0).contains("tier")&&StringFruit.cast(p,2,0).contains("tier"),"String X/C progression gating");
        Zombie t=EntityTypes.ZOMBIE.create(level,EntitySpawnReason.EVENT);t.snapTo(-8,y+18,14);t.setNoAi(false);
        t.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(100);t.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ARMOR).setBaseValue(0);t.setHealth(100);level.addFreshEntity(t);
        check(StringFruit.cast(p,0,0).equals("Five-Color Slash!")&&t.getHealth()==94,"String slash hits aimed enemy");
        check(StringFruit.cast(p,0,0).contains("recovering"),"String slash cooldown");
        StringFruit.grant(p,2);check(StringFruit.parasiteTicks(p)==100,"Tier two Parasite lasts five seconds");
        check(StringFruit.cast(p,1,19).contains("one second")&&!StringFruit.controlled(t),"Parasite requires one-second attachment");
        check(StringFruit.cast(p,1,20).contains("attached")&&StringFruit.controlling(p)&&StringFruit.controlled(t)&&t.isNoAi(),"Parasite controls hostile mob and stops vanilla AI");
        var before=t.position();t.move(net.minecraft.world.entity.MoverType.SELF,new net.minecraft.world.phys.Vec3(2,0,0));check(t.position().equals(before),"Puppet cannot move itself outside the control driver");
        var body=p.position();p.setPos(p.getX()+2,p.getY(),p.getZ());check(p.position().equals(body),"Possession leaves caster body fixed");
        clock(level,level.getGameTime()+101);StringFruit.tickPlayer(p);check(!StringFruit.controlling(p)&&!StringFruit.controlled(t)&&!t.isNoAi(),"Parasite timeout releases mob and restores AI");
        StringFruit.grant(p,3);check(StringFruit.parasiteTicks(p)==400&&StringFruit.knightTicks(p)==1200,"Mastery grants twenty-second Parasite and one-minute Knight");
        check(StringFruit.controlDirection(0,0,1).x<-.99&&StringFruit.controlDirection(90,0,1).z<-.99,"Puppet right strafe follows Minecraft camera axes");
        check(StringFruit.controlDirection(0,1,0).z>.99&&StringFruit.controlDirection(90,1,0).x<-.99,"Puppet forward follows camera yaw");
        p.getFoodData().setFoodLevel(20);check(StringFruit.cast(p,0,40).equals("Overheat!"),"Mastered held Z remains Overheat without own Birdcage");
        clock(level,level.getGameTime()+200);StringFruit.tickEffects();p.getFoodData().setFoodLevel(20);
        check(StringFruit.cast(p,1,0).contains("Glide"),"String Glide starts at rest");
        var anchor=p.position().add(0,0,8);
        check(StringFruit.glideVelocity(p,anchor,0,true).lengthSqr()==0,"Glide neutral input hovers");
        check(StringFruit.glideVelocity(p,anchor,1,true).z>.6&&StringFruit.glideVelocity(p,anchor,1,false).lengthSqr()==0,"Glide W moves and stale input stops");
        Mastery.grant(p,Element.FIRE,3);Progress.select(p,Element.FIRE);
        StringFruit.tickPlayer(p);check(StringFruit.gliding(p),"Mastered String Glide persists when selecting elements");
        Creeperborn.ABILITIES.clear();Creeperborn.ABILITIES.move(p,1);Creeperborn.ABILITIES.cast(p);StringFruit.tickPlayer(p);
        check(StringFruit.gliding(p),"Casting Flame Ring preserves mastered String Glide");Creeperborn.ABILITIES.clear();
        p.addTag("cb_mode_fruit");StringFruit.cast(p,1,0);check(!StringFruit.gliding(p),"Returning to String and tapping X still ends glide");
        for(int x=-24;x<=8;x++)for(int z=0;z<=30;z++)level.setBlockAndUpdate(BlockPos.containing(x,y+17,z),Blocks.STONE.defaultBlockState());
        check(StringFruit.cast(p,2,7).startsWith("Birdcage!")&&StringFruit.awakened(p),"Own Birdcage enables awakened attacks");
        check(StringFruit.cast(p,0,0).equals("Five-Color Slash!"),"Awakened Five-Color Claw casts inside Birdcage");
        for(int color=3;color<=7;color++){String name="cb_string_beam_"+color;check(level.getEntitiesOfClass(net.minecraft.world.entity.Display.BlockDisplay.class,p.getBoundingBox().inflate(50)).stream().anyMatch(d->d.getCustomName()!=null&&d.getCustomName().getString().equals(name)),"Awakened claw retains colored strand "+color);}
        var surface=StringFruit.terrainOrigin(p,t.getEyePosition(),0,6);
        check(surface!=null&&surface.y<t.getEyeY(),"Awakened attack anchors to real ground");
        check(StringFruit.cast(p,0,39).contains("2-second"),"God Thread requires full charge in Birdcage");
        clock(level,level.getGameTime()+200);t.setHealth(100);t.damageCooldownTime=0;
        StringFruit.press(p,0);
        for(int i=0;i<20;i++){clock(level,level.getGameTime()+1);StringFruit.heartbeat(p);StringFruit.tickPlayer(p);}
        check(StringFruit.chargeThreadCount(p)>=18&&t.getHealth()==100,"God Thread builds many real surface bundles without pre-release damage");
        int charging=StringFruit.chargeThreadCount(p);
        for(int i=0;i<20;i++){clock(level,level.getGameTime()+1);StringFruit.heartbeat(p);StringFruit.tickPlayer(p);}
        check(StringFruit.chargeThreadCount(p)==charging&&charging<=24,"Holding charge reuses a bounded field of threads");
        StringFruit.release(p,0);
        check(StringFruit.activeGodSpears(p)==charging&&StringFruit.chargeThreadCount(p)==0,"God Thread launches the gathered environment bundles and clears charge displays");
        for(int i=0;i<26;i++){clock(level,level.getGameTime()+1);StringFruit.tickEffects();}
        check(t.getHealth()<100&&t.getHealth()>=76,"God Thread is a real bounded damaging spear barrage");
        StringFruit.cast(p,2,7);clock(level,level.getGameTime()+260);StringFruit.tickEffects();
        check(!StringFruit.awakened(p),"Closing cage returns Z to normal attacks");
        Zombie parasiteVictim=EntityTypes.ZOMBIE.create(level,EntitySpawnReason.EVENT);parasiteVictim.snapTo(t.getX(),t.getY(),t.getZ()+2);parasiteVictim.setNoAi(true);parasiteVictim.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ARMOR).setBaseValue(0);level.addFreshEntity(parasiteVictim);
        t.setYHeadRot(180);check(StringFruit.possess(p,t,100),"Parasite attack test takes control");
        StringFruit.input(p,new StringControlPayload(t.getId(),0,0,0));StringFruit.input(p,new StringControlPayload(t.getId(),32,0,0));float hp=parasiteVictim.getHealth();StringFruit.tickPlayer(p);
        check(t.getLookAngle().z>.99&&t.getYHeadRot()==0,"Puppet head, camera and attack aim follow input yaw");check(parasiteVictim.getHealth()<hp,"Parasite attack input damages aimed enemy");StringFruit.cancelInput(p);parasiteVictim.discard();
        p.getFoodData().setFoodLevel(20);check(StringFruit.cast(p,2,0).contains("summoned"),"Black Knight and Spider Web summon together");
        check(StringFruit.damage(p,p.damageSources().playerAttack(player(level)),8)==2,"Spider Web reduces incoming entity damage by seventy-five percent");
        check(StringFruit.cast(p,2,0).contains("control")&&StringFruit.controlling(p),"C pilots the existing Knight");
        Zombie knight=level.getEntitiesOfClass(Zombie.class,p.getBoundingBox().inflate(10)).stream().filter(StringFruit::isKnight).findFirst().orElseThrow();
        Zombie knightVictim=EntityTypes.ZOMBIE.create(level,EntitySpawnReason.EVENT);knightVictim.snapTo(knight.getX(),knight.getY(),knight.getZ()+3);knightVictim.setNoAi(true);knightVictim.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ARMOR).setBaseValue(0);level.addFreshEntity(knightVictim);
        clock(level,level.getGameTime()+20);float knightHp=knightVictim.getHealth();StringFruit.input(p,new StringControlPayload(knight.getId(),32,0,0));StringFruit.tickPlayer(p);
        check(knightVictim.getHealth()<knightHp,"Piloted Black Knight attack damages aimed enemy");knightVictim.discard();
        StringFruit.cancelInput(p);check(!StringFruit.controlling(p),"Cancel restores camera control state");
        clock(level,level.getGameTime()+700);p.getFoodData().setFoodLevel(20);
        check(StringFruit.cast(p,2,7).startsWith("Birdcage!")&&StringFruit.cageCount()==1,"Birdcage creates persistent arena");
        StringFruit.press(p,0);for(int i=0;i<12;i++){clock(level,level.getGameTime()+1);StringFruit.heartbeat(p);StringFruit.tickPlayer(p);}
        check(StringFruit.chargeThreadCount(p)>0,"A later God Thread charge creates fresh environmental threads");
        StringFruit.cancelInput(p);check(StringFruit.chargeThreadCount(p)==0,"Cancel removes all gathered threads");
        var origin=p.position();p.setPos(origin.x+110,origin.y,origin.z);check(p.position().equals(origin),"Caster cannot escape own cage via absolute position");
        Player enemy=player(level);enemy.snapTo(origin.x+5,origin.y,origin.z);var trapped=enemy.position();enemy.setPos(origin.x+110,origin.y,origin.z);check(enemy.position().equals(trapped),"Enemy cannot teleport across cage");
        Player outsider=player(level);outsider.snapTo(origin.x+110,origin.y,origin.z); // initial location may be inside; gate must prevent crossing as well.
        check(StringFruit.crosses(origin.add(-110,0,0),origin.add(110,0,0),origin,100),"Teleport across entire cage cannot bypass boundary");
        check(!StringFruit.canTeleport(enemy,server.getLevel(net.minecraft.world.level.Level.NETHER),enemy.position()),"Cage blocks dimension escape");
        Zombie roofVictim=EntityTypes.ZOMBIE.create(level,EntitySpawnReason.EVENT);
        for(int x=-2;x<=2;x++)for(int z=33;z<=37;z++)level.setBlockAndUpdate(BlockPos.containing(origin.x+x,origin.y+7,origin.z+z),Blocks.STONE.defaultBlockState());
        roofVictim.snapTo(origin.x,origin.y+8,origin.z+35);roofVictim.setNoAi(true);level.addFreshEntity(roofVictim);
        var rooftop=roofVictim.position();float roofHealth=roofVictim.getHealth();
        check(StringFruit.cast(p,2,7).contains("closing"),"Hold C starts physical cage closing");
        for(int i=0;i<199;i++){clock(level,level.getGameTime()+1);StringFruit.tickEffects();}
        check(StringFruit.cageCount()==1&&StringFruit.activeCageRadius(p)>25&&StringFruit.activeCageRadius(p)<26,"Cage contracts to about fifty blocks across");
        clock(level,level.getGameTime()+1);StringFruit.tickEffects();
        check(StringFruit.activeCageRadius(p)==0,"Cage releases its barrier before the merging strings can squish occupants");
        check(roofVictim.position().distanceTo(origin)<25.5&&roofVictim.position().distanceTo(rooftop)>8,"Closing cage drags rooftop mob inward using actual movement; position="+roofVictim.position()+" distance="+roofVictim.position().distanceTo(origin));
        check(roofVictim.getHealth()==roofHealth&&level.getBlockState(BlockPos.containing(origin.x,origin.y+7,origin.z+35)).is(Blocks.STONE),"Cage pull preserves buildings and does not crush targets");
        clock(level,level.getGameTime()+19);StringFruit.tickEffects();check(StringFruit.activeCageRadius(p)==0&&!StringFruit.blocked(roofVictim,origin.add(110,0,0)),"Occupants can escape while cage strings merge");
        clock(level,level.getGameTime()+1);StringFruit.tickEffects();check(StringFruit.cageCount()==1&&StringFruit.activeCageRadius(p)==0,"Merged thread releases occupants and rises");
        clock(level,level.getGameTime()+39);StringFruit.tickEffects();check(StringFruit.cageCount()==1,"Single rising thread has its own exit animation");
        clock(level,level.getGameTime()+1);StringFruit.tickEffects();check(StringFruit.cageCount()==0,"Rising thread cleans up after thirteen seconds total");roofVictim.discard();
        clock(level,level.getGameTime()+700);p.getFoodData().setFoodLevel(20);StringFruit.cast(p,2,7);Dark.suppress(p,60,true);check(StringFruit.cageCount()==0&&!StringFruit.controlling(p),"Nullifying Grasp immediately collapses cage and possession");
        clock(level,level.getGameTime()+61);for(Element e:Element.values())Mastery.grant(p,e,3);
        check(StringFruit.parasiteTicks(p)==460&&StringFruit.knightTicks(p)==1380&&Math.abs(StringFruit.range(p)-46)<.001,"All-four mastery boosts String duration and range fifteen percent");
        Player victim=player(level);victim.snapTo(-8,y+18,13);check(StringFruit.possess(p,victim,20)&&StringFruit.controlled(victim),"Player targets use the same possession state");StringFruit.forget(victim);check(!StringFruit.controlling(p),"Victim disconnect releases controller");
        var claim=FruitClaims.owner("string");FruitClaims.setOwner("gravity",null);check(claim.equals(FruitClaims.owner("string")),"Legacy fruit saves preserve String owner");
        StringFruit.forget(p);check(StringFruit.unlocked(p),"Disconnect cleanup keeps fruit ownership");FruitClaims.releaseOnDeath(p);check(FruitClaims.owner("string")==null&&Mastery.harmony(p),"Death releases String while preserving elemental mastery");
        t.discard();StringFruit.clear();System.out.println("CREEPERBORN_STRING_REGRESSION_PASSED");
    }


    private void checkPaw(ServerLevel level){
        Paw.clear();
        // Earlier Door tests leave a raised floor at eye height; give Paw a clear-sky fixture.
        for(int x=-12;x<=-4;x++)for(int z=4;z<=28;z++)for(int dy=18;dy<=38;dy++)level.setBlockAndUpdate(new BlockPos(x,(int)y+dy,z),Blocks.AIR.defaultBlockState());
        Player p=player(level);p.snapTo(-8,y+18,8);p.setYRot(0);p.setXRot(0);
        check(Paw.consume(p,Relics.pawFruit())&&Paw.tier(p)==1&&CombatNetwork.selection(p)==12,"Paw fruit claims unique server-authoritative controls");
        check(WaterRules.vulnerable(p)&&!Paw.consume(player(level),Relics.pawFruit()),"Paw water curse and unique ownership");
        FruitClaims.load(server);check(Paw.unlocked(p),"Paw ownership persists in legacy-compatible world save");
        for(int i=0;i<2100;i++)check(Paw.launchDistance(p,i)>=20&&Paw.launchDistance(p,i)<=30,"Tier one Paw launch distance");
        Paw.grant(p,2);for(int i=0;i<2100;i++)check(Paw.launchDistance(p,i)>=60&&Paw.launchDistance(p,i)<=80,"Tier two Paw launch distance");
        Paw.grant(p,3);for(int i=0;i<2100;i++){int n=Paw.launchDistance(p,i);check(n>=200&&n<=220||n==300,"Mastered Paw launch range includes rare three hundred");}
        check(Paw.launchDistance(p,0)==300,"Rare mastered launch reaches three hundred");Paw.grant(p,1);
        Zombie t=EntityTypes.ZOMBIE.create(level,EntitySpawnReason.EVENT);t.snapTo(-8,y+18,14);t.setNoAi(true);t.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(100);t.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ARMOR).setBaseValue(0);t.setHealth(100);level.addFreshEntity(t);
        check(Paw.cast(p,0,0).equals("Paw Cannon!"),"Paw Cannon fires");for(int i=0;i<6;i++){clock(level,level.getGameTime()+1);Paw.tickEffects();}
        check(t.getHealth()==96&&Paw.cast(p,0,0).contains("recovering"),"Cannon actually damages aimed target and respects cooldown");
        t.discard();check(Paw.cast(p,0,39).contains("two-second"),"Ursus Shock needs full compression");
        java.util.List<Zombie> shockTargets=new java.util.ArrayList<>();
        for(int[] offset:new int[][]{{0,3},{0,-3},{3,0},{-3,0},{0,6}}){Zombie mob=EntityTypes.ZOMBIE.create(level,EntitySpawnReason.EVENT);mob.snapTo(-8+offset[0],y+18,8+offset[1]);mob.setNoAi(true);mob.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ARMOR).setBaseValue(0);level.addFreshEntity(mob);shockTargets.add(mob);}
        check(Paw.cast(p,0,40).equals("Ursus Shock!"),"Shock releases around caster");
        for(int i=0;i<shockTargets.size();i++){check(shockTargets.get(i).getHealth()==(i<4?12:20),"Shock hits front/back/both sides and respects radius: "+i);shockTargets.get(i).discard();}
        for(int rank:new int[]{2,3}){
            clock(level,level.getGameTime()+220);Paw.grant(p,rank);p.getFoodData().setFoodLevel(20);p.snapTo(-8,y+32,8);int radius=rank==2?10:20;
            java.util.List<Zombie> targets=new java.util.ArrayList<>();
            for(double offset:new double[]{radius-.5,radius+.5}){var mob=EntityTypes.ZOMBIE.create(level,EntitySpawnReason.EVENT);mob.snapTo(-8,y+32,8+offset);mob.setNoAi(true);mob.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(100);mob.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ARMOR).setBaseValue(0);mob.setHealth(100);level.addFreshEntity(mob);targets.add(mob);}
            check(Paw.cast(p,0,40).equals("Ursus Shock!")&&targets.get(0).getHealth()<100&&targets.get(1).getHealth()==100,"Shock tier "+rank+" actual damage boundary has radius "+radius);
            for(var mob:targets)mob.discard();
        }
        Paw.grant(p,1);p.snapTo(-8,y+18,8);p.getFoodData().setFoodLevel(20);
        var orphan=EntityTypes.BLOCK_DISPLAY.create(level,EntitySpawnReason.EVENT);orphan.setCustomName(net.minecraft.network.chat.Component.literal("cb_paw_1"));orphan.snapTo(-8,y+18,12);level.addFreshEntity(orphan);orphan.tickCount=3;orphan.baseTick();check(orphan.isRemoved(),"Native display tick cleans legacy untracked paws without the new tag");
        var ordinary=EntityTypes.BLOCK_DISPLAY.create(level,EntitySpawnReason.EVENT);ordinary.setCustomName(net.minecraft.network.chat.Component.literal("player artwork"));ordinary.tickCount=3;ordinary.baseTick();check(!ordinary.isRemoved(),"Paw watchdog preserves unrelated displays");ordinary.discard();

        Player attacker=player(level);attacker.snapTo(-8,y+18,10);check(Paw.cast(p,2,0).contains("Guard"),"Paw guard opens timed window");
        var arrow=EntityTypes.ARROW.create(level,EntitySpawnReason.EVENT);arrow.snapTo(-8,y+19,10);arrow.setOwner(attacker);arrow.setDeltaMovement(0,0,-1);level.addFreshEntity(arrow);
        check(Paw.reflect(p,arrow)&&arrow.getOwner()==p&&arrow.getDeltaMovement().z>1,"Repel Guard reverses incoming projectile and assigns attacker ownership");
        check(!Paw.reflect(p,arrow),"Guard cannot reflect its own returned projectile");float hp=p.getHealth();p.hurtServer(level,p.damageSources().playerAttack(attacker),5);check(p.getHealth()==hp,"Repel Guard blocks actual nearby melee damage");arrow.discard();
        clock(level,level.getGameTime()+50);Paw.grant(p,3);p.setHealth(7);p.getFoodData().setFoodLevel(2);
        p.setYRot(180);p.setYHeadRot(180);Paw.press(p,2);for(int i=0;i<20;i++){clock(level,level.getGameTime()+1);Paw.heartbeat(p);Paw.tickPlayer(p);}
        check(p.getHealth()==p.getMaxHealth()&&p.getFoodData().getFoodLevel()==20&&p.getAbsorptionAmount()==6,"Pain extraction fills health and hunger and grants three temporary hearts at mastery");
        check(p.hasEffect(MobEffects.ABSORPTION)&&Math.abs(Paw.bubbleDamage(p)-15.6)<.01,"Pain damage derives only from actual health restored");
        var bubble=Paw.bubblePosition(p);check(bubble!=null,"Holding C creates a real pain bubble");p.setYRot(90);p.setYHeadRot(90);
        for(int i=0;i<5;i++){clock(level,level.getGameTime()+1);Paw.heartbeat(p);Paw.tickPlayer(p);}
        check(Paw.bubblePosition(p)!=null&&Paw.bubblePosition(p).x<bubble.x-1,"Holding C steers bubble using player aim while body stays free");
        Zombie painTarget=EntityTypes.ZOMBIE.create(level,EntitySpawnReason.EVENT);var at=p.position().add(p.getLookAngle().scale(14));painTarget.snapTo(at.x,at.y,at.z);painTarget.setNoAi(true);painTarget.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ARMOR).setBaseValue(0);painTarget.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(100);painTarget.setHealth(100);level.addFreshEntity(painTarget);
        painTarget.hurtServer(level,p.damageSources().playerAttack(p),25);check(painTarget.getHealth()==75,"Pain fixture enters vanilla damage immunity");
        Paw.release(p,2);check(Paw.bubblePosition(p)==null&&Paw.extract(p).contains("recovering"),"Release fires bubble and saved heal cooldown prevents immediate refill");
        for(int i=0;i<20;i++){clock(level,level.getGameTime()+1);Paw.tickEffects();}
        check(painTarget.getHealth()==75,"Pain bubble waits through vanilla immunity instead of losing stored damage");painTarget.damageCooldownTime=0;
        for(int i=0;i<3;i++){clock(level,level.getGameTime()+1);Paw.tickEffects();}
        check(Math.abs(painTarget.getHealth()-59.4)<.01,"Off-axis bubble converges on crosshair and retries contact from inside hitbox");painTarget.discard();
        check(Paw.stepCapacity(p)==5&&Paw.stepDistance(p)==50&&Paw.stepCharges(p)==5,"Master has five fifty-block steps");
        var before=p.position();check(Paw.cast(p,1,0).startsWith("Paw Step!"),"Paw Step starts self repulsion");for(int i=0;i<40;i++){clock(level,level.getGameTime()+1);Paw.tickEffects();}
        check(Math.abs(p.position().distanceTo(before)-50)<.1&&!Paw.airborne(p),"Mastered Paw Step moves the real player and ends cleanly");
        p.getFoodData().setFoodLevel(20);
        for(int i=0;i<4;i++)check(Paw.cast(p,1,0).startsWith("Paw Step!"),"Stored steps chain in midair");
        check(Paw.stepCharges(p)==0&&Paw.cast(p,1,0).contains("recovering"),"Five steps exhaust the stored charges");
        Paw.forget(p);check(Paw.stepCharges(p)==0,"Logout cannot refill Paw Step charges");
        clock(level,level.getGameTime()+20);check(Paw.stepCharges(p)==1,"One step recharges after three seconds from first spend");
        clock(level,level.getGameTime()+240);check(Paw.stepCharges(p)==5,"Stored step charges refill to capacity");
        for(Vec3 direction:new Vec3[]{new Vec3(1,.5,1),new Vec3(-1,.7,-1),new Vec3(0,1,0)}){
            var q=Paw.orientation(direction);var right=q.transform(new org.joml.Vector3f(1,0,0));var forward=q.transform(new org.joml.Vector3f(0,0,1));
            check(Math.abs(right.y)<.0001&&forward.distance(new org.joml.Vector3f((float)direction.normalize().x,(float)direction.normalize().y,(float)direction.normalize().z))<.0001,"Paw stays upright and follows aim without roll");
        }
        check(Paw.guardTicks(p)==100,"Mastered Repel Guard lasts five seconds");
        clock(level,level.getGameTime()+200);Paw.tickEffects();check(Paw.visualCount()==0,"All abandoned charge, cannon and impact visuals expire");
        Zombie launched=EntityTypes.ZOMBIE.create(level,EntitySpawnReason.EVENT);launched.snapTo(-8,y+18,18);launched.setNoAi(true);level.addFreshEntity(launched);p.setYRot(0);p.setYHeadRot(0);var origin=launched.position();
        check(Paw.startFlight(p,launched,20,false)&&Paw.locked(launched),"Paw Launch takes bounded flight authority without changing no-gravity state");
        float launchHealth=launched.getHealth();launched.hurtServer(level,p.damageSources().fall(),50);check(launched.getHealth()==launchHealth,"Launched mobs have actual fall damage immunity");double maxY=origin.y;
        for(int i=0;i<160&&Paw.airborne(launched);i++){clock(level,level.getGameTime()+1);Paw.tickEffects();maxY=Math.max(maxY,launched.getY());}
        check(maxY>origin.y+8&&launched.getZ()>origin.z+19&&!Paw.airborne(launched)&&!launched.isNoGravity(),"Real launch ascends, travels in facing direction, descends and restores native motion");launched.discard();
        for(int distance:new int[]{200,300}){
            Zombie far=EntityTypes.ZOMBIE.create(level,EntitySpawnReason.EVENT);far.snapTo(-8,y+18,18);far.setNoAi(true);level.addFreshEntity(far);var start=far.position();
            check(Paw.startFlight(p,far,distance,false),"Mastered long flight begins");
            for(int i=0;i<500&&Paw.airborne(far);i++){clock(level,level.getGameTime()+1);Paw.tickEffects();}
            check(!Paw.airborne(far)&&Math.abs(far.getZ()-start.z-distance)<.1&&far.isAlive(),"Paw carries real mob "+distance+" blocks across chunk boundaries and lands safely; z="+far.getZ()+" flying="+Paw.airborne(far));far.discard();
        }
        clock(level,level.getGameTime()+1300);Paw.extract(p);check(Paw.bubbleDamage(p)==0,"Full-health extraction cannot create free damaging pain");Paw.forget(p);check(Paw.unlocked(p)&&Paw.bubblePosition(p)==null,"Logout cleans temporary Paw effects while retaining fruit claim");
        check(Paw.extract(p).contains("recovering"),"Pain healing cooldown survives logout state cleanup");FruitClaims.releaseOnDeath(p);check(FruitClaims.owner("paw")==null&&!Paw.unlocked(p),"Death releases Paw for natural spawning");
        t.discard();Paw.clear();System.out.println("CREEPERBORN_PAW_REGRESSION_PASSED");
    }

    private Zombie tremorTarget(ServerLevel level,double x,double yy,double z){var e=EntityTypes.ZOMBIE.create(level,EntitySpawnReason.EVENT);e.snapTo(x,yy,z);e.setNoAi(true);e.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(200);e.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ARMOR).setBaseValue(0);e.setHealth(200);level.addFreshEntity(e);return e;}
    private void tremorTicks(ServerLevel level,int ticks){for(int i=0;i<ticks;i++){clock(level,level.getGameTime()+1);Tremor.tickEffects();}}
    private void checkTremor(ServerLevel level){
        Tremor.clear();Player p=player(level);p.snapTo(40,y+12,40);p.setYRot(0);p.setXRot(0);p.setYHeadRot(0);
        for(int x=14;x<=66;x++)for(int z=14;z<=80;z++)for(int dy=11;dy<=30;dy++)level.setBlockAndUpdate(new BlockPos(x,(int)y+dy,z),dy==11?Blocks.STONE.defaultBlockState():Blocks.AIR.defaultBlockState());
        check(Tremor.consume(p,Relics.tremorFruit())&&Tremor.tier(p)==1&&CombatNetwork.selection(p)==13,"Tremor unique consumption and server controls");
        check(!Tremor.consume(player(level),Relics.tremorFruit())&&WaterRules.vulnerable(p),"Tremor ownership and water rules");FruitClaims.load(server);check(Tremor.unlocked(p),"Tremor claim survives world reload");
        Zombie punch=tremorTarget(level,40,y+12,44);check(Tremor.cast(p,0,0).contains("Quake Punch")&&punch.getHealth()==193,"Quake punch damages real aimed target");check(Tremor.cast(p,0,0).contains("recovering"),"Punch cooldown enforced");punch.discard();
        clock(level,level.getGameTime()+25);p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.TRIDENT));Zombie weapon=tremorTarget(level,40,y+12,44);check(Tremor.cast(p,0,0).contains("Kabutowari")&&weapon.getHealth()==193,"Weapon-coated quake strike hits");weapon.discard();p.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);
        check(Tremor.cast(p,0,19).contains("one second"),"Gekishin requires compression");Zombie close=tremorTarget(level,42,y+12,42);check(Tremor.cast(p,0,20).equals("Gekishin!")&&close.getHealth()==188,"Gekishin damages immediate area");check(Tremor.radialCount()==1,"Gekishin schedules outward vibration ripples centered on caster");close.discard();
        Zombie ball=tremorTarget(level,40,y+12,52);check(Tremor.cast(p,1,0).equals("Shock Ball!"),"Shock Ball cast");tremorTicks(level,15);check(ball.getHealth()==193,"Traveling vibration sphere actually hits");ball.discard();
        p.getFoodData().setFoodLevel(20);Zombie sea=tremorTarget(level,40,y+12,46);check(Tremor.cast(p,1,20).contains("ground tremors"),"Tier one Seaquake");tremorTicks(level,45);check(sea.getHealth()<200&&Tremor.pulseCount()==0,"Forward Seaquake hits and ends");sea.discard();
        clock(level,level.getGameTime()+600);Tremor.grant(p,3);p.getFoodData().setFoodLevel(20);Zombie left=tremorTarget(level,30,y+12,52),right=tremorTarget(level,50,y+12,52);BlockPos build=new BlockPos(40,(int)y+12,64),water=new BlockPos(41,(int)y+12,64);level.setBlockAndUpdate(build,Blocks.GOLD_BLOCK.defaultBlockState());level.setBlockAndUpdate(water,Blocks.WATER.defaultBlockState());
        check(Tremor.cast(p,1,20).contains("ground tremors")&&Tremor.pulseCount()==2,"Mastered Seaquake creates exactly two opposing ground fronts");tremorTicks(level,45);check(left.getHealth()<200&&right.getHealth()<200&&left.getX()>38&&right.getX()<42&&Tremor.pulseCount()==0,"Opposing waves sweep and hit enemies on both sides");check(level.getBlockState(build).is(Blocks.GOLD_BLOCK)&&level.getBlockState(water).is(Blocks.WATER),"Tsunamis leave builds and real fluids unchanged");left.discard();right.discard();
        p.getFoodData().setFoodLevel(20);for(int i=0;i<8;i++){double a=i*Math.PI/4;int xx=(int)Math.floor(40+Math.cos(a)*11),zz=(int)Math.floor(40+Math.sin(a)*11);level.setBlockAndUpdate(new BlockPos(xx,(int)y+11,zz),(i%2==0?Blocks.GRASS_BLOCK:Blocks.SAND).defaultBlockState());}
        Zombie island=tremorTarget(level,48,y+12,40);check(Tremor.cast(p,2,39).contains("two seconds"),"Island grab needs full charge");check(Tremor.cast(p,2,40).contains("Island Shaking")&&island.getHealth()==176,"Island Shaking applies real radial quake damage");island.discard();
        check(Tremor.radialCount()==1,"Island burst schedules expanding circular terrain rings");tremorTicks(level,7);
        check(Tremor.groundStates().stream().anyMatch(b->b.is(Blocks.GRASS_BLOCK))&&Tremor.groundStates().stream().anyMatch(b->b.is(Blocks.SAND)),"Island Shaking copies actual grass and sand block states into native display pieces");
        Zombie slam=tremorTarget(level,42,y+12,40);var origin=p.position();check(Tremor.cast(p,2,0).contains("Gurash")&&Tremor.airborne(p),"Gurash takes bounded server flight authority");p.move(net.minecraft.world.entity.MoverType.SELF,new Vec3(4,0,0));check(p.position().equals(origin),"Native movement cannot interrupt quake launch");check(Tremor.immune(p,p.damageSources().fall()),"Gurash grants temporary fall immunity");tremorTicks(level,30);check(!Tremor.airborne(p)&&slam.getHealth()==182&&p.position().distanceTo(origin)<1.2,"Gurash rises and strikes real ground without breaking floor");slam.discard();
        p.getFoodData().setFoodLevel(20);Tremor.press(p,0);for(int i=0;i<10;i++){clock(level,level.getGameTime()+1);Tremor.heartbeat(p);Tremor.tickPlayer(p);}check(Tremor.chargeCount(p)==2&&Tremor.chargePositions(p).get(0).distanceTo(Tremor.chargePositions(p).get(1))>1,"Gekishin creates two separated hand-centered vibration spheres");check(Tremor.chargeCirclePosition(p)!=null&&Tremor.chargeCirclePosition(p).subtract(p.position()).multiply(1,0,1).lengthSqr()<.0001,"Gekishin also charges a full circle around caster");Tremor.cancelInput(p);check(Tremor.chargeCount(p)==0&&Tremor.chargeCirclePosition(p)==null,"Cancel clears hands and surrounding charge circle");
        Tremor.press(p,1);for(int i=0;i<10;i++){clock(level,level.getGameTime()+1);Tremor.heartbeat(p);Tremor.tickPlayer(p);}check(Tremor.chargeCount(p)==2,"Seaquake also charges around both hands");Tremor.cancelInput(p);
        Tremor.press(p,2);for(int i=0;i<40;i++){clock(level,level.getGameTime()+1);Tremor.heartbeat(p);Tremor.tickPlayer(p);}check(Tremor.chargeCount(p)==1&&Tremor.chargePositions(p).get(0).subtract(p.position()).multiply(1,0,1).lengthSqr()<.0001,"Island Shaking charges a circle centered around the player");Tremor.cancelInput(p);
        tremorTicks(level,50);check(Tremor.radialCount()==0&&Tremor.visualCount()==0,"Quake, wave and canceled-charge geometry fully expires");
        Tremor.forget(p);check(Tremor.cast(p,1,20).contains("recovering"),"Logout cannot reset powerful quake cooldowns");
        p.addTag("cb_fruit_suppressed_"+(level.getGameTime()+100));check(Tremor.cast(p,0,0).contains("unavailable"),"Dark nullification disables Tremor");for(String tag:java.util.Set.copyOf(p.entityTags()))if(tag.startsWith("cb_fruit_suppressed_"))p.removeTag(tag);
        check(Dark.fruitTier(p,"tremor")==3&&Relics.kind(Relics.captured("tremor",java.util.UUID.randomUUID().toString(),2)).equals("fruit:tremor"),"Dark capture recognizes Tremor mastery and artwork");
        FruitClaims.releaseOnDeath(p);check(FruitClaims.owner("tremor")==null&&!Tremor.unlocked(p),"Death releases Tremor for natural spawning");
        var creeper=EntityTypes.CREEPER.create(level,EntitySpawnReason.EVENT);creeper.snapTo(40,y+12,45);FruitCreepers.assignTremor(creeper);level.addFreshEntity(creeper);check(FruitCreepers.fruit(creeper)&&FruitCreepers.tremor(creeper),"Tremor Fruitborn variant tracks correctly");FruitCreepers.died(creeper,p.damageSources().playerAttack(p));check(creeper.entityTags().contains("cb_fruit_dropped"),"Unclaimed Tremor Fruitborn drops fruit once");creeper.discard();
        Tremor.clear();System.out.println("CREEPERBORN_TREMOR_REGRESSION_PASSED");
    }

    private void pairTicks(ServerLevel level,int ticks,LivingEntity... targets){for(int i=0;i<ticks;i++){clock(level,level.getGameTime()+1);for(var e:targets)if(e.damageCooldownTime>0)e.damageCooldownTime--;PairPowers.tickEffects();}}
    private void checkLoveSand(ServerLevel level){
        PairPowers.clear();for(int x=80;x<=120;x++)for(int z=80;z<=132;z++)for(int dy=11;dy<=18;dy++)level.setBlockAndUpdate(new BlockPos(x,(int)y+dy,z),dy==11?Blocks.STONE.defaultBlockState():Blocks.AIR.defaultBlockState());
        Player love=player(level),sand=player(level);love.snapTo(100,y+12,100);sand.snapTo(110,y+12,100);love.setYRot(0);love.setXRot(0);sand.setYRot(0);sand.setXRot(0);
        check(PairPowers.consume(love,Relics.loveFruit(),"love")&&PairPowers.consume(sand,Relics.sandFruit(),"sand"),"Both new fruits claim independent owners");check(!PairPowers.consume(love,Relics.sandFruit(),"sand")&&!PairPowers.consume(player(level),Relics.loveFruit(),"love"),"Unique ownership and fruit-slot limits apply to both");
        FruitClaims.load(server);check(PairPowers.unlocked(love,"love")&&PairPowers.unlocked(sand,"sand")&&CombatNetwork.selection(love)==14&&CombatNetwork.selection(sand)==15,"Both claims survive reload and route separate controls");check(WaterRules.vulnerable(love)&&WaterRules.vulnerable(sand)&&PairPowers.sandCapacity(sand)==3,"Both inherit water curse and Sand starts with three drifts");
        Zombie target=tremorTarget(level,100,y+12,104);target.setNoAi(false);check(PairPowers.cast(love,"love",0,0).contains("Pistol Kiss"),"Pistol Kiss fires");pairTicks(level,6,target);check(target.getHealth()==196&&PairPowers.buildup(target)==40,"Heart projectile deals real damage and adds stone buildup");check(PairPowers.cast(love,"love",0,0).contains("recovering"),"Love tap cooldown enforced");
        PairPowers.grant(love,"love",3);clock(level,level.getGameTime()+30);target.damageCooldownTime=0;love.getFoodData().setFoodLevel(20);check(PairPowers.cast(love,"love",0,19).contains("one second"),"Love held move needs charge");check(PairPowers.cast(love,"love",0,20).contains("Beam")&&PairPowers.buildup(target)==100&&PairPowers.frozen(target),"Mastered Love beam fully petrifies its live target");target.damageCooldownTime=0;check(PairPowers.cast(love,"love",1,0).contains("Perfume")&&PairPowers.frozen(target)&&target.isNoAi(),"Perfume kick damages a statue while native mob AI remains suspended");var pos=target.position();target.move(net.minecraft.world.entity.MoverType.SELF,new Vec3(3,0,0));check(target.position().equals(pos),"Petrification blocks actual movement");target.damageCooldownTime=0;check(PairPowers.cast(love,"love",2,20).contains("1 shattered")&&!PairPowers.frozen(target)&&!target.isNoAi(),"Stone Breaker damages and releases statue while restoring AI");
        target.damageCooldownTime=0;check(PairPowers.cast(love,"love",0,0).contains("Kiss"),"Kiss can hit released target");pairTicks(level,6,target);check(PairPowers.buildup(target)==0&&!PairPowers.frozen(target),"Recovery window prevents immediate petrification chaining");target.discard();
        for(int rank=1;rank<=3;rank++){
            clock(level,level.getGameTime()+300);PairPowers.grant(love,"love",rank);love.getFoodData().setFoodLevel(20);Zombie ordinary=EntityTypes.ZOMBIE.create(level,EntitySpawnReason.EVENT);ordinary.snapTo(100,y+12,104);ordinary.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ARMOR).setBaseValue(0);level.addFreshEntity(ordinary);
            for(int hit=0;hit<(rank==3?2:3);hit++){clock(level,level.getGameTime()+20);ordinary.damageCooldownTime=0;check(PairPowers.cast(love,"love",0,0).contains("Kiss"),"Normal zombie petrification fixture casts kiss");pairTicks(level,6,ordinary);}
            check(ordinary.isAlive()&&PairPowers.frozen(ordinary),"Tier "+rank+" petrifies an ordinary 20-health zombie before killing it: hp="+ordinary.getHealth()+" buildup="+PairPowers.buildup(ordinary));
            pairTicks(level,rank==3?105:25,ordinary);check(PairPowers.frozen(ordinary),"Mob statue survives human reaction and one-second finisher charge");ordinary.damageCooldownTime=0;PairPowers.press(love,2);for(int tick=0;tick<20;tick++){pairTicks(level,1,ordinary);PairPowers.heartbeat(love);PairPowers.tickPlayer(love);}PairPowers.release(love,2);check(!PairPowers.frozen(ordinary)&&!ordinary.isAlive(),"Actual charged Stone Breaker finishes ordinary zombie statue at tier "+rank);ordinary.discard();
        }
        PairPowers.grant(love,"love",3);clock(level,level.getGameTime()+300);
        love.getFoodData().setFoodLevel(20);var origin=love.position();check(PairPowers.cast(love,"love",2,0).contains("Heart Step")&&Math.abs(love.position().distanceTo(origin)-8)<.01,"Mastered Heart Step actually dodges eight blocks");level.setBlockAndUpdate(new BlockPos(100,(int)y+12,110),Blocks.STONE.defaultBlockState());level.setBlockAndUpdate(new BlockPos(100,(int)y+13,110),Blocks.STONE.defaultBlockState());clock(level,level.getGameTime()+50);check(PairPowers.cast(love,"love",2,0).contains("Heart Step")&&love.getZ()<110,"Heart Step respects whole-body wall collision");level.setBlockAndUpdate(new BlockPos(100,(int)y+12,110),Blocks.AIR.defaultBlockState());level.setBlockAndUpdate(new BlockPos(100,(int)y+13,110),Blocks.AIR.defaultBlockState());love.snapTo(100,y+12,100);
        Zombie arrows=tremorTarget(level,100,y+12,112);check(PairPowers.cast(love,"love",1,20).contains("Slave Arrow"),"Heart arrow fan releases");pairTicks(level,14,arrows);check(arrows.getHealth()<200&&PairPowers.buildup(arrows)>0,"Arrow fan actually hits and builds petrification");arrows.discard();
        clock(level,level.getGameTime()+200);love.getFoodData().setFoodLevel(20);Zombie blocked=tremorTarget(level,100,y+12,112);for(int dy=12;dy<=14;dy++)level.setBlockAndUpdate(new BlockPos(100,(int)y+dy,106),Blocks.STONE.defaultBlockState());PairPowers.cast(love,"love",0,20);check(blocked.getHealth()==200,"Love beam cannot damage through solid wall");for(int dy=12;dy<=14;dy++)level.setBlockAndUpdate(new BlockPos(100,(int)y+dy,106),Blocks.AIR.defaultBlockState());blocked.discard();
        Player victim=player(level);victim.snapTo(100,y+12,104);victim.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(200);victim.setHealth(200);check(level.addFreshEntity(victim),"PvP petrification fixture joins entity queries");for(int i=0;i<2;i++){clock(level,level.getGameTime()+20);victim.damageCooldownTime=0;love.getFoodData().setFoodLevel(20);PairPowers.cast(love,"love",0,0);pairTicks(level,6,victim);}check(PairPowers.frozen(victim),"Player reaches full petrification through actual heart hits: health="+victim.getHealth()+" buildup="+PairPowers.buildup(victim));pairTicks(level,41,victim);check(PairPowers.frozen(victim),"Mastered player petrification lasts beyond two seconds");pairTicks(level,20,victim);check(!PairPowers.frozen(victim),"Mastered player petrification ends within three seconds");clock(level,level.getGameTime()+20);victim.damageCooldownTime=0;love.getFoodData().setFoodLevel(20);PairPowers.cast(love,"love",0,0);pairTicks(level,6,victim);check(PairPowers.buildup(victim)==0,"Player recovery window rejects new stone buildup");victim.discard();
        clock(level,level.getGameTime()+200);check(level.addFreshEntity(sand),"Sand owner joins PvP query fixture");sand.snapTo(100,y+12,104);sand.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(200);sand.setHealth(200);PairPowers.tickPlayer(sand);love.getFoodData().setFoodLevel(20);sand.damageCooldownTime=0;PairPowers.cast(love,"love",0,20);clock(level,level.getGameTime()+20);sand.damageCooldownTime=0;PairPowers.cast(love,"love",0,0);pairTicks(level,6,sand);sand.damageCooldownTime=0;PairPowers.cast(love,"love",1,0);check(PairPowers.frozen(sand),"Love can petrify an active Sand fruit owner");PairPowers.tickPlayer(sand);check(PairPowers.frozen(sand),"Sand's automatic interruption cleanup cannot erase incoming petrification");pairTicks(level,61,sand);check(!PairPowers.frozen(sand),"Incoming petrification still expires at its real time limit");sand.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(20);sand.setHealth(20);
        PairPowers.grant(sand,"sand",3);sand.snapTo(100,y+12,100);sand.getFoodData().setFoodLevel(20);Zombie blade=tremorTarget(level,100,y+12,106);check(PairPowers.cast(sand,"sand",0,0).contains("Spada"),"Desert Spada casts");pairTicks(level,8,blade);check(blade.getHealth()==193,"Sand blade deals lower real ranged damage");blade.discard();
        clock(level,level.getGameTime()+30);sand.getFoodData().setFoodLevel(20);java.util.List<Zombie> line=new java.util.ArrayList<>();for(int z:new int[]{106,111,116})line.add(tremorTarget(level,100,y+12,z));PairPowers.cast(sand,"sand",0,0);pairTicks(level,20,line.toArray(LivingEntity[]::new));for(var mob:line){check(mob.getHealth()==193,"One Desert Spada pierces each enemy exactly once");mob.discard();}
        Zombie trap=tremorTarget(level,100,y+12,106);sand.setXRot(20);check(PairPowers.cast(sand,"sand",0,20).contains("Girasole"),"Aimed quicksand casts on native ground");sand.setXRot(0);double trapZ=trap.getZ();pairTicks(level,12,trap);check(trap.getZ()<trapZ-.3&&trap.hasEffect(MobEffects.SLOWNESS),"Quicksand actually pulls and slows enemy above unchanged ground");trap.discard();PairPowers.forget(sand);
        Zombie carried=tremorTarget(level,100,y+12,106);double carriedZ=carried.getZ();check(PairPowers.cast(sand,"sand",1,0).equals("Sables!"),"Sables casts");pairTicks(level,30,carried);check(carried.getZ()>carriedZ+12&&carried.getY()>y+12,"Traveling sand tornado carries an actual enemy a short distance");carried.discard();PairPowers.forget(sand);
        sand.getFoodData().setFoodLevel(20);Zombie storm=tremorTarget(level,102,y+12,100);check(PairPowers.cast(sand,"sand",1,39).contains("two seconds"),"Desert Storm requires full two-second charge");check(PairPowers.cast(sand,"sand",1,40).contains("Storm"),"Desert Storm casts");pairTicks(level,25,storm);check(storm.getHealth()<200&&storm.hasEffect(MobEffects.BLINDNESS),"Storm chips health and reduces enemy visibility");storm.discard();PairPowers.forget(sand);check(PairPowers.cast(sand,"sand",1,40).contains("recovering"),"Logout cannot reset Sand Storm cooldown");
        BlockPos build=new BlockPos(105,(int)y+11,106),water=new BlockPos(108,(int)y+12,106);level.setBlockAndUpdate(build,Blocks.OAK_PLANKS.defaultBlockState());level.setBlockAndUpdate(water,Blocks.WATER.defaultBlockState());sand.getFoodData().setFoodLevel(20);sand.setHealth(7);Zombie burial=tremorTarget(level,100,y+12,105);burial.setNoAi(false);check(PairPowers.cast(sand,"sand",2,20).contains("Coffin")&&PairPowers.rooted(burial)&&burial.isNoAi(),"Desert Drain assembles a sand coffin and pauses target AI");var buried=burial.position();burial.move(net.minecraft.world.entity.MoverType.SELF,new Vec3(2,0,0));check(burial.position().equals(buried),"Coffin blocks actual movement");check(!StringFruit.possess(love,burial,100),"Parasite cannot replace a coffin restraint and corrupt original AI state");love.getFoodData().setFoodLevel(20);PairPowers.cast(love,"love",0,20);check(PairPowers.buildup(burial)==0&&PairPowers.coffinCount()==1,"Love buildup cannot stack a second AI restraint over Sand Coffin");pairTicks(level,90,burial);check(!PairPowers.rooted(burial)&&!burial.isNoAi()&&burial.getHealth()<190&&sand.getHealth()>7&&sand.getHealth()<=13,"Burial drains, heals within cap, finishes and restores original mob AI");burial.discard();check(level.getBlockState(build).is(Blocks.OAK_PLANKS)&&level.getBlockState(water).is(Blocks.WATER),"Sand effects preserve builds and actual fluids");
        clock(level,level.getGameTime()+300);sand.getFoodData().setFoodLevel(20);Zombie distant=tremorTarget(level,100,y+12,107.5);check(PairPowers.cast(sand,"sand",2,20).contains("Coffin")&&PairPowers.rooted(distant),"Mastered Sand Coffin reaches a real enemy seven and a half blocks away");PairPowers.forget(sand);distant.discard();
        clock(level,level.getGameTime()+300);sand.getFoodData().setFoodLevel(20);sand.setHealth(7);Zombie immune=tremorTarget(level,100,y+12,105);immune.hurtServer(level,sand.damageSources().playerAttack(sand),100);immune.damageCooldownTime=1000;PairPowers.cast(sand,"sand",2,20);pairTicks(level,90,immune);check(sand.getHealth()==7&&immune.getHealth()==100,"Rejected drain damage cannot create free healing");immune.discard();
        clock(level,level.getGameTime()+300);sand.getFoodData().setFoodLevel(20);Player coffinPlayer=player(level);coffinPlayer.snapTo(100,y+12,104);coffinPlayer.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(200);coffinPlayer.setHealth(200);check(level.addFreshEntity(coffinPlayer),"PvP coffin fixture joins entity queries");PairPowers.cast(sand,"sand",2,20);check(PairPowers.rooted(coffinPlayer),"Player enters short sand coffin");pairTicks(level,41,coffinPlayer);check(!PairPowers.rooted(coffinPlayer),"Player coffin always releases within two seconds");coffinPlayer.discard();
        clock(level,level.getGameTime()+300);sand.getFoodData().setFoodLevel(20);check(level.addFreshEntity(love),"Love owner joins PvP coffin fixture");love.snapTo(100,y+12,104);love.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(200);love.setHealth(200);PairPowers.tickPlayer(love);PairPowers.cast(sand,"sand",2,20);check(PairPowers.rooted(love),"Sand can coffin an active Love fruit owner");PairPowers.tickPlayer(love);check(PairPowers.rooted(love),"Love's automatic interruption cleanup cannot erase an incoming coffin");pairTicks(level,41,love);check(!PairPowers.rooted(love),"Coffin releases the Love owner at its real time limit");love.snapTo(110,y+12,100);love.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(20);love.setHealth(20);
        sand.getFoodData().setFoodLevel(20);check(PairPowers.sandCapacity(sand)==5&&PairPowers.sandCharges(sand)==5,"Mastery grants five Sand Drift charges");for(int i=0;i<5;i++){sand.snapTo(100,y+12,100);check(PairPowers.cast(sand,"sand",2,0).contains("Sand Drift")&&Math.abs(sand.getZ()-116)<.01,"Each Sand Drift actually travels sixteen clear blocks");}check(PairPowers.sandCharges(sand)==0&&PairPowers.cast(sand,"sand",2,0).contains("recovering"),"Exhausted drift charges block movement");PairPowers.forget(sand);check(PairPowers.sandCharges(sand)==0,"Logout preserves spent drift charges");clock(level,level.getGameTime()+60);check(PairPowers.sandCharges(sand)==1,"One drift charge returns after three seconds");sand.snapTo(100,y+12,100);
        clock(level,level.getGameTime()+300);sand.getFoodData().setFoodLevel(20);Zombie interrupted=tremorTarget(level,100,y+12,105);interrupted.setNoAi(false);PairPowers.cast(sand,"sand",2,20);PairPowers.forget(sand);check(!PairPowers.rooted(interrupted)&&!interrupted.isNoAi(),"Interruption releases coffin and restores native AI immediately");interrupted.discard();
        love.getFoodData().setFoodLevel(20);PairPowers.press(love,0);for(int i=0;i<10;i++){clock(level,level.getGameTime()+1);PairPowers.heartbeat(love);PairPowers.tickPlayer(love);}check(PairPowers.visualCount()>0,"Love held input creates real charging heart");PairPowers.cancelInput(love);pairTicks(level,70);PairPowers.forget(love);PairPowers.forget(sand);check(PairPowers.visualCount()==0&&PairPowers.fieldCount()==0&&PairPowers.coffinCount()==0,"Both fruits fully clean up effects and control state");
        for(int keys:new int[]{1,2,4,8,9,5}){Vec3 v=PairPowers.dodgeDirection(0,keys);check(Math.abs(v.length()-1)<.0001,"Diagonal dodge direction is normalized");}
        check(PairPowers.dodgeDirection(0,8).x<-.99&&PairPowers.dodgeDirection(90,8).z<-.99&&PairPowers.dodgeDirection(0,2).z<-.99,"Dodges follow Minecraft right/back axes at different camera yaw");
        clock(level,level.getGameTime()+200);love.snapTo(100,y+12,100);love.setYRot(0);love.getFoodData().setFoodLevel(20);PairPowers.press(love,2,8);PairPowers.release(love,2);check(love.getX()<92.1&&Math.abs(love.getZ()-100)<.01,"Real Heart Step input moves right instead of toward look direction");
        clock(level,level.getGameTime()+200);sand.snapTo(100,y+12,100);sand.setYRot(0);sand.getFoodData().setFoodLevel(20);PairPowers.press(sand,2,2);PairPowers.release(sand,2);check(sand.getZ()<84.1&&Math.abs(sand.getX()-100)<.01,"Real Sand Drift input moves backward");
        PairPowers.forget(love);PairPowers.forget(sand);
        sand.addTag("cb_fruit_suppressed_"+(level.getGameTime()+100));check(PairPowers.cast(sand,"sand",0,0).contains("unavailable"),"Dark suppression disables new powers");for(String t:java.util.Set.copyOf(sand.entityTags()))if(t.startsWith("cb_fruit_suppressed_"))sand.removeTag(t);
        FruitClaims.releaseOnDeath(love);java.util.UUID token=java.util.UUID.randomUUID();FruitClaims.setOwner("love",token);Player captured=player(level);check(PairPowers.consume(captured,Relics.captured("love",token.toString(),2),"love")&&PairPowers.tier(captured,"love")==2,"Captured Love transfers reserved claim and retained tier");FruitClaims.load(server);check(PairPowers.unlocked(sand,"sand")&&PairPowers.unlocked(captured,"love"),"Capturing Love preserves independent Sand owner in world save");FruitClaims.releaseOnDeath(captured);FruitClaims.releaseOnDeath(sand);love.discard();sand.discard();
        for(String f:java.util.List.of("love","sand")){var c=EntityTypes.CREEPER.create(level,EntitySpawnReason.EVENT);c.snapTo(100,y+12,110);if(f.equals("love"))FruitCreepers.assignLove(c);else FruitCreepers.assignSand(c);level.addFreshEntity(c);FruitCreepers.died(c,love.damageSources().playerAttack(love));check(c.entityTags().contains("cb_fruit_dropped"),"New Fruitborn drops unclaimed "+f+" fruit");c.discard();}
        PairPowers.clear();System.out.println("CREEPERBORN_LOVE_SAND_REGRESSION_PASSED");
    }

    private void azureTicks(ServerLevel level,Player owner,int ticks,LivingEntity... targets){for(int i=0;i<ticks;i++){clock(level,level.getGameTime()+1);for(var e:targets)if(e.damageCooldownTime>0)e.damageCooldownTime--;Azure.tickPlayer(owner);Azure.tickEffects();}}
    private void checkAzure(ServerLevel level){
        Player p=player(level);p.snapTo(200,y+12,200);p.setYRot(0);p.setXRot(0);p.getFoodData().setFoodLevel(20);
        for(int x=185;x<=215;x++)for(int z=170;z<=275;z++){level.setBlock(new BlockPos(x,(int)y+11,z),Blocks.STONE.defaultBlockState(),3);for(int dy=12;dy<18;dy++)level.setBlock(new BlockPos(x,(int)y+dy,z),Blocks.AIR.defaultBlockState(),3);}
        var weapon=new ItemStack(Items.DIAMOND_SWORD);p.setItemInHand(InteractionHand.MAIN_HAND,weapon);check(Azure.consume(p,Relics.azureFruit()),"Azure fruit consumes and claims ownership");check(!Azure.consume(player(level),Relics.azureFruit()),"Only one living Azure owner per world");FruitClaims.load(server);check(Azure.unlocked(p)&&CombatNetwork.selection(p)==18&&WaterRules.vulnerable(p),"Azure claim survives reload, controls route and water curse applies");
        check(Azure.form(p)==1,"Eating Azure starts default emerging Hybrid without a transformation key");check(Azure.cast(p,2,20).contains("mastery"),"Full dragon requires mastery");Zombie clap=tremorTarget(level,203,y+12,200);check(Azure.cast(p,2,0).contains("Thunderclap")&&clap.getHealth()==194,"Tier-one C tap is a damaging radial Thunderclap, not a human toggle");clap.discard();check(((Azure.flags(p)>>2)&3)==1,"Emerging model flags carry tier one");azureTicks(level,p,21);int kanabos=0;for(int slot=0;slot<p.getInventory().getContainerSize();slot++){ItemStack item=p.getInventory().getItem(slot);if(net.minecraft.resources.Identifier.fromNamespaceAndPath("creeperborn","azure_kanabo").equals(item.get(net.minecraft.core.component.DataComponents.ITEM_MODEL))){kanabos++;check(item.is(Items.NETHERITE_SWORD)&&net.minecraft.world.item.enchantment.EnchantmentHelper.getItemEnchantmentLevel(p.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT).getOrThrow(net.minecraft.world.item.enchantment.Enchantments.SHARPNESS),item)==9,"Kanabo is a real enchanted netherite sword");}}check(kanabos==1,"Azure grants one usable kanabo without replacing held sword");
        ItemStack club=ItemStack.EMPTY;for(int slot=0;slot<p.getInventory().getContainerSize();slot++)if(Azure.kanabo(p.getInventory().getItem(slot)))club=p.getInventory().getItem(slot);
        p.setItemInHand(InteractionHand.MAIN_HAND,club);Zombie plated=tremorTarget(level,200,y+12,202),plain=tremorTarget(level,202,y+12,202);
        for(Zombie target:new Zombie[]{plated,plain})target.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ARMOR).setBaseValue(20);
        check(Azure.kanaboArmor(p.damageSources().playerAttack(p),10,4)>4,"Kanabo held-item piercing calculation: "+Azure.kanabo(p.getMainHandItem()));plated.hurtServer(level,p.damageSources().playerAttack(p),10);p.setItemInHand(InteractionHand.MAIN_HAND,weapon);plain.hurtServer(level,p.damageSources().playerAttack(p),10);
        check(plated.getHealth()<plain.getHealth(),"Kanabo Piercing VI increases actual damage through armor: "+plated.getHealth()+" vs "+plain.getHealth());
        p.setItemInHand(InteractionHand.MAIN_HAND,club);p.setHealth(20);p.damageCooldownTime=0;float thornsBefore=plain.getHealth();plain.damageCooldownTime=0;p.hurtServer(level,plain.damageSources().mobAttack(plain),4);check(plain.getHealth()<thornsBefore,"Held kanabo Thorns III retaliates against an actual melee attacker");
        p.setItemInHand(InteractionHand.MAIN_HAND,weapon);p.setHealth(20);plated.discard();plain.discard();
Azure.grant(p,2);check(((Azure.flags(p)>>2)&3)==2,"Tier two evolves existing Hybrid appearance");Azure.grant(p,3);check(((Azure.flags(p)>>2)&3)==3,"Mastery evolves existing Hybrid appearance");
        int foodBefore=p.getFoodData().getFoodLevel();clock(level,level.getGameTime()+5000);Azure.tickPlayer(p);check(Azure.form(p)==1&&p.getFoodData().getFoodLevel()==foodBefore,"Default Hybrid has no timer or idle hunger drain");Progress.consume(p,Relics.heart(Element.AIR));Progress.select(p,Element.AIR);Azure.tickPlayer(p);check(Azure.form(p)==1&&CombatNetwork.selection(p)==Element.AIR.ordinal()&&(Azure.flags(p)&128)==0,"Hybrid appearance persists while R routes attacks to elements");check(Azure.cast(p,0,0).contains("unavailable"),"Element selection cannot trigger Azure moves");p.addTag("cb_mode_fruit");
        Player attacker=player(level);p.setHealth(20);p.damageCooldownTime=0;p.hurtServer(level,p.damageSources().playerAttack(attacker),10);check(Math.abs(p.getHealth()-16.5)<.01,"Mastered Hybrid scales reduce ordinary real damage by sixty-five percent");
        for(Element element:Element.values())Mastery.grant(attacker,element,3);p.damageCooldownTime=0;p.setHealth(20);Abilities.strike(level,attacker,p,Element.EARTH,10,0);check(Math.abs(p.getHealth()-8.5)<.01,"Actual Harmony elemental attack bypasses Azure scales");p.setHealth(20);
        clock(level,level.getGameTime()+50);p.getFoodData().setFoodLevel(20);Zombie bagua=tremorTarget(level,200,y+12,206);check(Azure.cast(p,0,0).contains("Thunder Bagua"),"Hybrid Bagua casts");check(bagua.getHealth()==172&&p.getZ()>200,"Bagua rush moves and damages real target");check(Azure.cast(p,0,0).contains("recovering"),"Bagua server cooldown enforced");bagua.discard();p.snapTo(200,y+12,200);
        clock(level,level.getGameTime()+110);p.getFoodData().setFoodLevel(20);Zombie charged=tremorTarget(level,200,y+12,204),farCharged=tremorTarget(level,200,y+12,212);check(Azure.cast(p,0,20).contains("Destroyer"),"Hybrid charged Bagua selected");azureTicks(level,p,7,charged,farCharged);check(farCharged.getHealth()==156,"Destroyer Bagua reaches an enemy twelve blocks ahead");farCharged.discard();check(charged.getHealth()==156,"Heavy overhead smash applies real forty-four damage");charged.discard();
        clock(level,level.getGameTime()+110);p.getFoodData().setFoodLevel(20);Zombie gust=tremorTarget(level,200,y+12,225);check(Azure.cast(p,1,0).contains("Demolition Gust")&&Azure.shotCount()==5,"Hybrid fans five intertwined lightning blades");azureTicks(level,p,18,gust);check(gust.getHealth()<200,"Lightning blades hit distant enemy");gust.discard();Azure.stopOwned(p);check(p.getMainHandItem()==weapon&&p.getMainHandItem().is(Items.DIAMOND_SWORD),"Transforming and attacking preserves actual held weapon");
        clock(level,level.getGameTime()+200);p.getFoodData().setFoodLevel(20);check(Azure.cast(p,2,20).contains("5:00")&&Azure.form(p)==2&&Azure.remaining(p)==6000,"Mastered Full Dragon lasts exactly five minutes");check(Azure.proxyCount()==12,"Long dragon has bounded hittable body segments");check(p.getY()>=y+15,"Dragon transformation takes off clear of the floor");check(!new AzureFlightPayload(128,0,0).valid()&&!new AzureFlightPayload(0,Float.NaN,0).valid()&&new AzureFlightPayload(17,90,-20).valid(),"Per-tick flight validates controls and finite aim");check(Azure.flightMotion(0,0,3).lengthSqr()==0&&Azure.flightMotion(0,0,0).lengthSqr()==0,"Opposing flight keys and no keys hover instead of drifting");azureTicks(level,p,31);Vec3 hover=p.position();azureTicks(level,p,15);check(p.position().distanceTo(hover)<.01,"Dragon hovers with no movement input");for(int i=0;i<8;i++){Azure.input(p,17);azureTicks(level,p,1);}check(p.getZ()>hover.z+4&&p.getY()>hover.y+3,"W and Jump move dragon forward and up");Azure.input(p,0);hover=p.position();azureTicks(level,p,12);check(p.position().distanceTo(hover)<.01,"Releasing flight controls returns to stationary hover");check(Azure.maxBodyGap(p)<=1.051,"Articulated dragon sections remain overlapped with no body gaps");long dragonBefore=Azure.remaining(p);Progress.select(p,Element.AIR);azureTicks(level,p,25);check(Azure.form(p)==2&&Azure.remaining(p)==dragonBefore-25&&CombatNetwork.selection(p)==Element.AIR.ordinal(),"Dragon and its running timer persist while using elements");Azure.flightInput(p,new AzureFlightPayload(17,0,0));Vec3 elementFlight=p.position();azureTicks(level,p,1);check(p.getZ()>elementFlight.z&&p.getY()>elementFlight.y,"Per-tick dragon flight works while elements are selected");Azure.input(p,0);Zombie elementTarget=tremorTarget(level,p.getX(),p.getY(),p.getZ()+10);Creeperborn.ABILITIES.move(p,0);Creeperborn.ABILITIES.cast(p);check(elementTarget.getHealth()<200,"Actual element bolt reaches enemy instead of targeting the dragon owner's hidden hitbox");elementTarget.discard();p.addTag("cb_mode_fruit");check(Azure.remaining(p)<dragonBefore,"Returning to fruit controls does not refresh the dragon timer");
        LivingEntity proxy=level.getEntitiesOfClass(LivingEntity.class,p.getBoundingBox().inflate(40)).stream().filter(Azure::proxy).findFirst().orElseThrow();for(int i=0;i<8;i++)proxy.tick();check(proxy.isInvisible()&&proxy.hasEffect(MobEffects.INVISIBILITY)&&proxy.getCustomName().getString().startsWith("cb_azure_hitbox:"),"Hitbox stays invisible through real native living-entity ticks and carries synced render safeguard");p.setHealth(20);p.damageCooldownTime=0;proxy.hurtServer(level,attacker.damageSources().playerAttack(attacker),10);check(Math.abs(p.getHealth()-17)<.01,"Striking a dragon body proxy damages owner through seventy percent scales");p.setHealth(20);
        p.getFoodData().setFoodLevel(20);clock(level,level.getGameTime()+100);p.setYRot(0);p.setXRot(0);azureTicks(level,p,25);Zombie breath=tremorTarget(level,p.getX(),p.getY(),p.getZ()+35);check(Azure.cast(p,0,0).contains("Bolo Breath"),"Dragon replaces Bagua with Bolo Breath");azureTicks(level,p,22,breath);check(breath.getHealth()==166&&breath.isOnFire(),"Dragon breath hits and burns distant enemy");breath.discard();clock(level,level.getGameTime()+200);p.getFoodData().setFoodLevel(20);Zombie storm=tremorTarget(level,p.getX(),p.getY(),p.getZ()+45);check(Azure.cast(p,1,20).contains("storm projectile"),"Dragon twister launches instead of trapping storm at caster");azureTicks(level,p,30,storm);check(storm.getHealth()<200,"Travelling dragon twister reaches a distant enemy");storm.discard();
        clock(level,level.getGameTime()+200);p.getFoodData().setFoodLevel(20);check(Azure.cast(p,2,0).contains("Torch"),"Dragon C tap activates Flame Dragon Torch");Azure.input(p,64);Azure.input(p,0);check(Azure.cast(p,0,0).contains("Bolo Breath"),"Torch leaves Z assigned to Bolo Breath");Zombie finisher=tremorTarget(level,p.getX(),p.getY(),p.getZ()+5);check(Azure.cast(p,2,0).contains("Flame Bagua"),"Second C tap launches the rising dragon finisher");azureTicks(level,p,14,finisher);check(finisher.getHealth()<200&&Azure.form(p)==2&&Azure.proxyCount()==12,"Ultimate damages enemy and retains dragon body until its normal limit");finisher.discard();
        clock(level,level.getGameTime()+100);p.getFoodData().setFoodLevel(20);Azure.cast(p,2,20);clock(level,level.getGameTime()+50);Azure.cast(p,2,20);azureTicks(level,p,31);check(Azure.cast(p,2,0).contains("recovering"),"Ultimate cooldown persists across form switches");long expiry=level.getGameTime()+Azure.remaining(p);clock(level,expiry);Azure.tickPlayer(p);check(Azure.form(p)==1&&Azure.proxyCount()==0,"Dragon expires and removes hittable proxies at timer boundary");
        clock(level,level.getGameTime()+100);p.getFoodData().setFoodLevel(20);Azure.cast(p,2,20);p.removeTag("cb_mode_fruit");Azure.tickPlayer(p);check(Azure.form(p)==2&&Azure.proxyCount()==12&&Azure.locked(p),"Switching to elements keeps dragon body and flight active");Dark.suppress(p,20,true);Azure.tickPlayer(p);check(Azure.form(p)==0&&Azure.proxyCount()==0&&!Azure.locked(p),"Dark suppression fully removes active dragon helpers");clock(level,level.getGameTime()+21);p.addTag("cb_mode_fruit");Azure.stopOwned(p);Azure.tickEffects();check(Azure.visualCount()==0&&Azure.shotCount()==0,"Azure leaves no orphan body or attack visuals");
        FruitClaims.releaseOnDeath(p);check(FruitClaims.owner("azure")==null&&!Azure.unlocked(p),"Death releases Azure ownership and progression");UUID token=UUID.randomUUID();FruitClaims.setOwner("azure",token);Player captured=player(level);check(Azure.consume(captured,Relics.captured("azure",token.toString(),2))&&Azure.tier(captured)==2,"Dark capture transfers Azure ownership with retained tier");FruitClaims.load(server);check(Azure.unlocked(captured),"Captured Azure claim survives reload");FruitClaims.releaseOnDeath(captured);
        var creeper=EntityTypes.CREEPER.create(level,EntitySpawnReason.EVENT);creeper.snapTo(210,y+12,200);FruitCreepers.assignAzure(creeper);level.addFreshEntity(creeper);FruitCreepers.died(creeper,attacker.damageSources().playerAttack(attacker));check(creeper.entityTags().contains("cb_fruit_dropped"),"Unclaimed Azure creeper drops fruit");creeper.discard();
    }
    private void climateTicks(ServerLevel level,int ticks,Player[] owners,LivingEntity... targets){for(int i=0;i<ticks;i++){clock(level,level.getGameTime()+1);for(var e:targets)if(e.damageCooldownTime>0)e.damageCooldownTime--;for(var owner:owners)ClimatePowers.tickPlayer(owner);ClimatePowers.tickEffects();}}
    private void checkIceMagma(ServerLevel level){
        ClimatePowers.clear();for(int x=190;x<=292;x++)for(int z=194;z<=218;z++)for(int dy=11;dy<=17;dy++)level.setBlockAndUpdate(new BlockPos(x,(int)y+dy,z),dy==11?Blocks.STONE.defaultBlockState():Blocks.AIR.defaultBlockState());
        Player ice=player(level),magma=player(level);ice.snapTo(200,y+12,200);magma.snapTo(220,y+12,200);ice.setYRot(0);ice.setXRot(0);magma.setYRot(0);magma.setXRot(0);Player[] both={ice,magma};
        check(ClimatePowers.consume(ice,Relics.iceFruit(),"ice")&&ClimatePowers.consume(magma,Relics.magmaFruit(),"magma"),"Ice and Magma claim independent persistent owners");check(!ClimatePowers.consume(ice,Relics.magmaFruit(),"magma")&&!ClimatePowers.consume(player(level),Relics.iceFruit(),"ice"),"New fruits preserve personal and world ownership limits");FruitClaims.load(server);check(ClimatePowers.unlocked(ice,"ice")&&ClimatePowers.unlocked(magma,"magma")&&CombatNetwork.selection(ice)==16&&CombatNetwork.selection(magma)==17,"Both new claims survive reload and route correct controls");check(WaterRules.vulnerable(ice)&&WaterRules.vulnerable(magma),"Ice/Magma inherit the fruit water curse");
        check(new ControlPayload(10,127).validRequest()&&!new ControlPayload(10,128).validRequest()&&!new ControlPayload(11,0).validRequest(),"Continuous steering and struggle packet bounds are validated");
        Zombie spear=EntityTypes.ZOMBIE.create(level,EntitySpawnReason.EVENT);spear.snapTo(200,y+12,211);spear.setNoAi(true);spear.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ARMOR).setBaseValue(0);level.addFreshEntity(spear);check(ClimatePowers.cast(ice,"ice",0,0).contains("Ice Spear"),"Ice Spear casts");climateTicks(level,12,both,spear);check(spear.getHealth()==16&&ClimatePowers.buildup(spear)==50,"Spear deals real damage and frost buildup");check(ClimatePowers.cast(ice,"ice",0,0).contains("recovering"),"Spear cooldown enforced");clock(level,level.getGameTime()+8);spear.damageCooldownTime=0;ice.getFoodData().setFoodLevel(20);ClimatePowers.cast(ice,"ice",0,0);climateTicks(level,12,both,spear);check(ClimatePowers.rooted(spear)&&spear.isAlive(),"Repeated spears freeze an ordinary zombie before damage becomes lethal: health="+spear.getHealth()+" frost="+ClimatePowers.buildup(spear));spear.discard();ClimatePowers.stopOwned(ice);
        ClimatePowers.grant(ice,"ice",3);clock(level,level.getGameTime()+200);ice.getFoodData().setFoodLevel(20);Zombie partizan=tremorTarget(level,200,y+12,224);check(ClimatePowers.cast(ice,"ice",0,19).contains("one second"),"Partizan charge gate");check(ClimatePowers.cast(ice,"ice",0,20).contains("Partizan")&&ClimatePowers.shotCount()==7,"Mastered Partizan creates seven converging spears");climateTicks(level,24,both,partizan);check(partizan.getHealth()<200&&ClimatePowers.rooted(partizan),"Mastered spears hit and instantly complete frost: hp="+partizan.getHealth()+" frost="+ClimatePowers.buildup(partizan)+" shots="+ClimatePowers.shotCount());partizan.discard();ClimatePowers.stopOwned(ice);
        ice.getFoodData().setFoodLevel(20);Zombie bird=tremorTarget(level,200,y+12,260);check(ClimatePowers.cast(ice,"ice",1,0).contains("Hawk"),"Ice Hawk casts");climateTicks(level,55,both,bird);check(bird.getHealth()==191&&ClimatePowers.rooted(bird),"Mastered Ice Hawk reaches a real enemy sixty blocks away");bird.discard();ClimatePowers.stopOwned(ice);
        BlockPos build=new BlockPos(230,(int)y+11,202),water=new BlockPos(231,(int)y+11,202);level.setBlockAndUpdate(build,Blocks.GOLD_BLOCK.defaultBlockState());level.setBlockAndUpdate(water,Blocks.WATER.defaultBlockState());
        ice.getFoodData().setFoodLevel(20);Zombie farIce=tremorTarget(level,288,y+12,200);check(ClimatePowers.cast(ice,"ice",1,20).contains("Ice Age")&&ClimatePowers.fieldRadius(ice,"ice")==90&&ClimatePowers.fieldRemaining(ice,"ice")==1200,"Mastered Ice Age has ninety-block radius and sixty-second lifetime");climateTicks(level,65,both,farIce);check(farIce.getHealth()==196&&ClimatePowers.rooted(farIce),"Expanding glacier reaches and freezes an actual enemy eighty-eight blocks away");Player attacker=player(level);ice.setHealth(20);ice.damageCooldownTime=0;ice.hurtServer(level,ice.damageSources().playerAttack(attacker),10);check(ClimatePowers.armored(ice)&&Math.abs(ice.getHealth()-16)<.01,"Ice Age armor reduces actual incoming damage by sixty percent");check(ice.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED).getValue()>=.29,"Ice Age triples owner movement speed");ice.snapTo(291,y+12,200);climateTicks(level,1,both);check(!ClimatePowers.inIceAge(ice)&&ice.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED).getValue()<.11,"Leaving Ice Age removes its speed boost immediately");ice.snapTo(200,y+12,200);climateTicks(level,1,both);check(ClimatePowers.inIceAge(ice)&&ice.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED).getValue()>=.29,"Reentering Ice Age restores its speed boost");Progress.consume(ice,Relics.heart(Element.AIR));Progress.select(ice,Element.AIR);long ageRemaining=ClimatePowers.fieldRemaining(ice,"ice");climateTicks(level,10,both,farIce);check(ClimatePowers.armored(ice)&&ClimatePowers.fieldRemaining(ice,"ice")==ageRemaining-10,"Switching to elements preserves Ice Age armor, speed and timer");ice.addTag("cb_mode_fruit");climateTicks(level,1126,both,farIce);check(!ClimatePowers.armored(ice)&&ClimatePowers.fieldRemaining(ice,"ice")==0,"Ice Age and its armor end together at sixty seconds");farIce.discard();check(ice.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED).getValue()<.11,"Ice Age speed modifier clears with the field");ice.setHealth(20);
        for(int x=203;x<=209;x++)for(int z=203;z<=209;z++){level.setBlockAndUpdate(new BlockPos(x,(int)y+9,z),Blocks.STONE.defaultBlockState());for(int dy=10;dy<=11;dy++)level.setBlockAndUpdate(new BlockPos(x,(int)y+dy,z),Blocks.WATER.defaultBlockState());}
        clock(level,level.getGameTime()+30);ice.snapTo(201.5,y+14.4,205.5);ice.move(net.minecraft.world.entity.MoverType.SELF,new Vec3(4,-4,0));check(ClimatePowers.sliding(ice)&&!WaterRules.submerged(ice),"Fast diagonal jump catches water surface along its full path: pos="+ice.position()+" sliding="+ClimatePowers.sliding(ice)+" suppressed="+WaterRules.suppressed(ice));ClimatePowers.stopOwned(ice);clock(level,level.getGameTime()+21);ice.snapTo(205.5,y+12.4,205.5);double surface=y+11+level.getFluidState(new BlockPos(205,(int)y+11,205)).getHeight(level,new BlockPos(205,(int)y+11,205));ice.move(net.minecraft.world.entity.MoverType.SELF,new Vec3(0,-.8,0));check(ClimatePowers.sliding(ice)&&ice.getY()>=surface&&!WaterRules.submerged(ice),"Native downward movement automatically catches Ice owner above actual fluid surface: sliding="+ClimatePowers.sliding(ice)+" y="+ice.getY()+" surface="+surface);var paused=ice.position();climateTicks(level,350,both);check(ClimatePowers.sliding(ice)&&ice.position().distanceTo(paused)<.01,"Mastered Slide stays active on a frozen platform while movement pauses");Progress.select(ice,Element.AIR);climateTicks(level,5,both);check(ClimatePowers.sliding(ice)&&CombatNetwork.selection(ice)==Element.AIR.ordinal(),"Ice Slide remains active while element controls are selected");for(int i=0;i<4;i++){ClimatePowers.input(ice,1);climateTicks(level,1,both);}check(ice.getZ()>paused.z+4.9&&!WaterRules.submerged(ice),"Slide resumes from movement input and crosses water without submerging");ice.addTag("cb_mode_fruit");ClimatePowers.input(ice,0);climateTicks(level,1300,both);check(ClimatePowers.sliding(ice),"Mastered slide has no duration limit");ClimatePowers.input(ice,32);check(!ClimatePowers.sliding(ice),"Shift intentionally releases the automatic water platform");ice.snapTo(205.5,y+10.1,205.5);check(WaterRules.suppressed(ice),"Actually submerged Ice owner still has normal fruit suppression");WaterRules.apply(ice);ice.damageCooldownTime=0;ice.setHealth(20);ice.hurtServer(level,ice.damageSources().drown(),4);check(ice.getHealth()==16&&!ClimatePowers.sliding(ice),"Drowning damage remains real after entering water");ice.snapTo(200,y+12,200);ice.setHealth(20);ice.getFoodData().setFoodLevel(20);
        Player prisoner=player(level);prisoner.snapTo(200,y+12,207);prisoner.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(200);prisoner.setHealth(200);check(level.addFreshEntity(prisoner),"Ice prison PvP fixture joins entity queries");check(ClimatePowers.cast(ice,"ice",2,20).contains("Coffin")&&ClimatePowers.rooted(prisoner)&&ClimatePowers.coffinIntegrity(prisoner)==200,"Ice Coffin freezes instantly with a real breakable shell");var before=prisoner.position();prisoner.move(net.minecraft.world.entity.MoverType.SELF,new Vec3(2,0,0));check(prisoner.position().equals(before),"Coffin blocks native victim movement");for(int i=0;i<18&&ClimatePowers.rooted(prisoner);i++){climateTicks(level,4,both,prisoner);ClimatePowers.input(prisoner,64);}check(!ClimatePowers.rooted(prisoner)&&prisoner.isAlive(),"Continuous attack input lets a player fight out before the ten-second limit");prisoner.discard();
        clock(level,level.getGameTime()+400);ice.getFoodData().setFoodLevel(20);Zombie fullCoffin=EntityTypes.ZOMBIE.create(level,EntitySpawnReason.EVENT);fullCoffin.snapTo(200,y+12,207);fullCoffin.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ARMOR).setBaseValue(0);level.addFreshEntity(fullCoffin);ClimatePowers.cast(ice,"ice",2,20);climateTicks(level,190,both,fullCoffin);check(ClimatePowers.rooted(fullCoffin),"Ordinary mob can remain inside Ice Coffin for the full ten-second encounter");climateTicks(level,11,both,fullCoffin);check(!ClimatePowers.rooted(fullCoffin)&&!fullCoffin.isAlive(),"Coffin ends with a finishing shatter and releases native state");fullCoffin.discard();
        clock(level,level.getGameTime()+400);ice.getFoodData().setFoodLevel(20);Player rescue=player(level);rescue.snapTo(200,y+12,207);rescue.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(200);rescue.setHealth(200);level.addFreshEntity(rescue);ClimatePowers.cast(ice,"ice",2,20);for(int i=0;i<3;i++)rescue.hurtServer(level,rescue.damageSources().playerAttack(attacker),10);check(!ClimatePowers.rooted(rescue)&&rescue.getHealth()==200,"An ally can break the ice shell without harming the prisoner");rescue.discard();ClimatePowers.stopOwned(ice);
        ClimatePowers.grant(magma,"magma",3);magma.getFoodData().setFoodLevel(20);Zombie fist=tremorTarget(level,220,y+12,212),splash=tremorTarget(level,222,y+12,212);check(ClimatePowers.cast(magma,"magma",0,0).contains("Magma Fist"),"Large Magma Fist casts");climateTicks(level,14,both,fist,splash);check(fist.getHealth()==179&&splash.getHealth()==179&&fist.isOnFire(),"Large fist hits, splashes nearby enemy and leaves real burning effect");fist.discard();splash.discard();ClimatePowers.stopOwned(magma);
        magma.getFoodData().setFoodLevel(20);Zombie eruption=tremorTarget(level,220,y+12,218);check(ClimatePowers.cast(magma,"magma",0,20).contains("Great Eruption"),"Great Eruption casts");climateTicks(level,20,both,eruption);check(eruption.getHealth()==164,"Charged molten fist deals heavy real damage");eruption.discard();ClimatePowers.stopOwned(magma);
        Zombie hound=tremorTarget(level,220,y+12,214);magma.getFoodData().setFoodLevel(20);check(ClimatePowers.cast(magma,"magma",1,0).contains("Hound"),"Magma Hound casts");climateTicks(level,16,both,hound);check(hound.getHealth()==184&&hound.isOnFire(),"Ground-following hound hits and burns enemy");hound.discard();ClimatePowers.stopOwned(magma);
        magma.getFoodData().setFoodLevel(20);Zombie rain=tremorTarget(level,232.1,y+12,200);check(ClimatePowers.cast(magma,"magma",1,39).contains("two seconds"),"Meteor Volcano charge gate");check(ClimatePowers.cast(magma,"magma",1,40).contains("Meteor"),"Meteor Volcano launches around player");climateTicks(level,70,both,rain);check(rain.getHealth()<200&&rain.isOnFire(),"Ascending then descending magma rain damages an enemy around the caster");rain.discard();ClimatePowers.stopOwned(magma);
        magma.getFoodData().setFoodLevel(20);Zombie farMagma=tremorTarget(level,264,y+12,200);check(ClimatePowers.cast(magma,"magma",2,0).contains("Field")&&ClimatePowers.fieldRadius(magma,"magma")==45&&ClimatePowers.fieldRemaining(magma,"magma")==900,"Mastered Magma Field has forty-five-block radius and forty-five-second lifetime");climateTicks(level,45,both,farMagma);check(farMagma.getHealth()<200&&farMagma.isOnFire(),"Expanding magma field actually burns enemy forty-four blocks away");Progress.consume(magma,Relics.heart(Element.FIRE));Progress.select(magma,Element.FIRE);long magmaRemaining=ClimatePowers.fieldRemaining(magma,"magma");climateTicks(level,10,both,farMagma);check(ClimatePowers.fieldRemaining(magma,"magma")==magmaRemaining-10,"Magma Field persists while using elements");magma.addTag("cb_mode_fruit");climateTicks(level,846,both,farMagma);check(ClimatePowers.fieldRemaining(magma,"magma")==0,"Magma Field expires after forty-five seconds");farMagma.discard();check(level.getBlockState(build).is(Blocks.GOLD_BLOCK)&&level.getBlockState(water).is(Blocks.WATER),"Long glacier and magma fields preserve real builds and fluids");
        magma.getFoodData().setFoodLevel(20);magma.setHealth(20);check(ClimatePowers.cast(magma,"magma",2,20).contains("Cloak")&&ClimatePowers.cloaked(magma),"Magma Cloak activates");magma.damageCooldownTime=0;magma.hurtServer(level,magma.damageSources().playerAttack(attacker),10);check(magma.getHealth()==20,"Cloak absorbs ordinary melee without damage");Mastery.grant(attacker,Element.EARTH,3);magma.damageCooldownTime=0;magma.hurtServer(level,magma.damageSources().playerAttack(attacker),10);check(magma.getHealth()==20,"Owning elemental mastery alone does not turn melee into a cloak counter");Abilities.strike(level,attacker,magma,Element.EARTH,10,0);check(Math.abs(magma.getHealth()-15)<.01,"An actual mastered elemental attack pierces cloak at fifty percent damage");
        for(Element e:Element.values())Mastery.grant(attacker,e,3);magma.setHealth(20);magma.damageCooldownTime=0;Abilities.strike(level,attacker,magma,Element.EARTH,10,0);check(Math.abs(magma.getHealth()-8.5)<.01,"Harmony elemental attack pierces cloak at full normal Harmony damage");
        magma.setHealth(20);Progress.select(magma,Element.FIRE);climateTicks(level,5,both);check(ClimatePowers.cloaked(magma),"Magma cloak persists while using elements");for(int i=0;i<110;i++){ClimatePowers.input(magma,16);climateTicks(level,1,both);}check(magma.getY()<=y+32.01&&magma.getY()>=y+31.5,"Ground-fed cloak flight reaches but cannot exceed twenty blocks above launch surface");magma.addTag("cb_mode_fruit");clock(level,level.getGameTime()+250);magma.getFoodData().setFoodLevel(20);Zombie underFlight=tremorTarget(level,220,y+12,205);check(ClimatePowers.cast(magma,"magma",2,0).contains("Field"),"Magma Field can cast from cloak flight");climateTicks(level,25,both,underFlight);check(underFlight.getHealth()<200,"Airborne field projects down onto actual ground instead of floating overhead");underFlight.discard();ClimatePowers.stopOwned(magma);check(!ClimatePowers.locked(magma)&&!ClimatePowers.cloaked(magma),"Ending cloak restores movement and removes immunity");magma.snapTo(220,y+12,200);
        ice.getFoodData().setFoodLevel(20);ClimatePowers.press(ice,0);for(int i=0;i<10;i++){ClimatePowers.heartbeat(ice);climateTicks(level,1,both);}check(ClimatePowers.visualCount()>0,"Held ice input creates charge effect");ClimatePowers.cancelInput(ice);ClimatePowers.stopOwned(ice);ClimatePowers.stopOwned(magma);climateTicks(level,120,both);check(ClimatePowers.visualCount()==0&&ClimatePowers.fieldCount()==0&&ClimatePowers.shotCount()==0,"All new charge, field, projectile, slide and cloak visuals fully clean up");
        check(ClimatePowers.cast(magma,"magma",2,20).contains("recovering"),"Cloak cooldown survives cleanup and reconnect-style state loss");
        FruitClaims.releaseOnDeath(ice);java.util.UUID token=java.util.UUID.randomUUID();FruitClaims.setOwner("ice",token);Player captured=player(level);check(ClimatePowers.consume(captured,Relics.captured("ice",token.toString(),2),"ice")&&ClimatePowers.tier(captured,"ice")==2,"Captured Ice transfers reserved claim and retained mastery");FruitClaims.load(server);check(ClimatePowers.unlocked(captured,"ice")&&ClimatePowers.unlocked(magma,"magma"),"Capture/save preserves both new owners and extended legacy data");FruitClaims.releaseOnDeath(captured);FruitClaims.releaseOnDeath(magma);
        for(String f:java.util.List.of("ice","magma")){var c=EntityTypes.CREEPER.create(level,EntitySpawnReason.EVENT);c.snapTo(220,y+12,210);if(f.equals("ice"))FruitCreepers.assignIce(c);else FruitCreepers.assignMagma(c);level.addFreshEntity(c);FruitCreepers.died(c,attacker.damageSources().playerAttack(attacker));check(c.entityTags().contains("cb_fruit_dropped"),"Unclaimed new Fruitborn drops "+f+" fruit");c.discard();}
        ClimatePowers.clear();System.out.println("CREEPERBORN_ICE_MAGMA_REGRESSION_PASSED");
    }

    private void natureTicks(ServerLevel level,int ticks,Player... owners){for(int i=0;i<ticks;i++){clock(level,level.getGameTime()+1);for(var p:owners)NaturePowers.tickPlayer(p);NaturePowers.tickEffects();}}
    private void checkNature(ServerLevel level){
        NaturePowers.clear();Player tree=player(level),petal=player(level);tree.snapTo(200,y+12,200);petal.snapTo(220,y+12,200);for(Player p:new Player[]{tree,petal}){p.setYRot(0);p.setXRot(0);p.getFoodData().setFoodLevel(20);}
        for(int x=190;x<=235;x++)for(int z=190;z<=235;z++)for(int dy=11;dy<19;dy++)level.setBlock(new BlockPos(x,(int)y+dy,z),dy==11?Blocks.GRASS_BLOCK.defaultBlockState():Blocks.AIR.defaultBlockState(),3);
        check(NaturePowers.consume(tree,Relics.natureFruit("tree"),"tree")&&NaturePowers.consume(petal,Relics.natureFruit("petal"),"petal"),"Both nature fruits consume and claim independently");
        check(!NaturePowers.consume(player(level),Relics.natureFruit("tree"),"tree")&&!NaturePowers.consume(tree,Relics.natureFruit("petal"),"petal"),"Nature fruits preserve world uniqueness and personal fruit slots");
        FruitClaims.load(server);check(NaturePowers.unlocked(tree,"tree")&&NaturePowers.unlocked(petal,"petal")&&CombatNetwork.selection(tree)==19&&CombatNetwork.selection(petal)==20,"Both new claims survive atomic reload and route to distinct controls");
        check(NaturePowers.palette("bamboo_jungle")==8&&NaturePowers.palette("pale_garden")==9&&NaturePowers.palette("cherry_grove")==7&&NaturePowers.palette("mangrove_swamp")==6,"New biome and bamboo wood themes resolve correctly");
        check(NaturePowers.rootedGround(tree)&&NaturePowers.treeScale(tree)==1.6f,"Natural ground enlarges tree form");for(int x=199;x<=201;x++)for(int z=199;z<=201;z++)level.setBlock(new BlockPos(x,(int)y+11,z),Blocks.OAK_PLANKS.defaultBlockState(),3);check(!NaturePowers.rootedGround(tree)&&NaturePowers.treeScale(tree)==1,"Constructed wooden floor cannot count as rooted natural ground");for(int x=199;x<=201;x++)for(int z=199;z<=201;z++)level.setBlock(new BlockPos(x,(int)y+11,z),Blocks.GRASS_BLOCK.defaultBlockState(),3);
        Zombie bolt=tremorTarget(level,200,y+12,210);check(NaturePowers.cast(tree,"tree",0,0).contains("Piercing Root"),"Tier-one root projectile casts");natureTicks(level,8,tree,petal);check(bolt.getHealth()<200,"Root projectile damages an actual enemy ten blocks away");bolt.discard();
        NaturePowers.grant(tree,"tree",3);check(NaturePowers.cast(tree,"tree",2,20).contains("Guardian")&&NaturePowers.giant(tree),"Mastered tree gains giant form");
        check(NaturePowers.damage(tree,tree.damageSources().generic(),10)==3,"Giant tree has strong physical protection");natureTicks(level,1,tree,petal);check(NaturePowers.bodyCount(tree)==3,"Giant tree has three bounded hittable body sections");LivingEntity body=level.getEntitiesOfClass(LivingEntity.class,tree.getBoundingBox().inflate(10)).stream().filter(e->e.getCustomName()!=null&&e.getCustomName().getString().equals("cb_nature_body:"+tree.getUUID())).findFirst().orElseThrow();tree.setHealth(20);tree.damageCooldownTime=0;body.hurtServer(level,tree.damageSources().generic(),10);check(Math.abs(tree.getHealth()-17)<.01,"Striking the visible giant body routes protected damage to its owner");NaturePowers.beginElement(tree,Element.FIRE);float fire=NaturePowers.damage(tree,tree.damageSources().generic(),10);NaturePowers.endElement();NaturePowers.beginDragonFire();float dragon=NaturePowers.damage(tree,tree.damageSources().generic(),10);NaturePowers.endElement();check(fire>3&&dragon>fire,"Elemental fire hurts wood and Dragon fire hurts more");
        Zombie around=tremorTarget(level,196,y+12,200);check(NaturePowers.cast(tree,"tree",1,0).contains("Sweep")&&around.getHealth()<200&&around.getDeltaMovement().x<0,"Giant sweep hits behind or beside owner and pushes outward");around.discard();
        Zombie drained=tremorTarget(level,200,y+12,208);tree.setHealth(12);check(NaturePowers.cast(tree,"tree",0,20).contains("Draining")&&NaturePowers.rooted(drained),"Held tree roots bind real enemy");natureTicks(level,22,tree,petal);check(drained.getHealth()<200&&tree.getHealth()>12,"Draining roots pulse damage and heal owner");NaturePowers.forget(tree);check(!NaturePowers.rooted(drained)&&drained.isNoAi(),"Cleanup releases binds and restores previous AI");drained.discard();
        clock(level,level.getGameTime()+200);tree.getFoodData().setFoodLevel(20);check(NaturePowers.cast(tree,"tree",1,20).contains("Propeller")&&NaturePowers.flying(tree),"Tree flower flight starts");Mastery.grant(tree,Element.AIR,3);Progress.select(tree,Element.AIR);NaturePowers.input(tree,17);Vec3 before=tree.position();natureTicks(level,1,tree);check(NaturePowers.flying(tree)&&tree.getY()>before.y&&tree.getZ()>before.z,"Flower flight moves and persists while elements selected");NaturePowers.forget(tree);tree.addTag("cb_mode_fruit");tree.snapTo(200,y+12,200);
        check(NaturePowers.cast(tree,"tree",2,0).contains("Root Travel")&&NaturePowers.phased(tree),"Natural-ground root travel starts");check(NaturePowers.phaseRange(tree)==120,"Mastered root travel expands to 120 blocks");tree.snapTo(400,y+12,200);natureTicks(level,1,tree);check(tree.getX()<321,"Root travel enforces its range boundary");check(NaturePowers.exitPhase(tree,false)&&!NaturePowers.phased(tree)&&!tree.isInvisible(),"Manual root emergence restores visible body at a safe location");
        for(int tier=1;tier<=3;tier++){NaturePowers.grant(petal,"petal",tier);check(NaturePowers.cloneCapacity(petal)==petal.getMaxHealth()*tier,"Clone health scales from original maximum through 1x/2x/3x mastery");petal.getFoodData().setFoodLevel(20);clock(level,level.getGameTime()+200);List<Zombie> targets=new ArrayList<>();for(int i=0;i<12;i++)targets.add(tremorTarget(level,220+(i%3-1)*.7,y+12,205+i/3*.6));check(NaturePowers.cast(petal,"petal",0,0).contains("Clutch"),"Clutch casts at every tier");check(NaturePowers.boundCount()==NaturePowers.clutchCapacity(tier),"Clutch binds 1/4/12 enemies according to mastery");natureTicks(level,21,petal);check(targets.stream().anyMatch(t->t.getHealth()<200),"Clutch damage lands after its arm-wrapping windup");NaturePowers.forget(petal);for(var t:targets)t.discard();}
        petal.getFoodData().setFoodLevel(20);clock(level,level.getGameTime()+500);check(NaturePowers.cast(petal,"petal",2,0).contains("Cuerpo")&&NaturePowers.cloneHealth(petal)==60,"Mastered controlled clone has its own health pool");float health=petal.getHealth();Player attacker=player(level);petal.hurtServer(level,attacker.damageSources().playerAttack(attacker),80);check(petal.getHealth()==health&&NaturePowers.phased(petal)&&NaturePowers.cloneHealth(petal)==0,"Destroyed clone protects actual health and enters petal travel");check(NaturePowers.phaseRange(petal)==48&&NaturePowers.phaseRemaining(petal)==120,"Mastered petal travel grants six seconds and forty-eight blocks");check(NaturePowers.exitPhase(petal,false),"Petal owner chooses an open place to reappear");for(Element e:Element.values())Mastery.grant(petal,e,3);check(NaturePowers.cloneCapacity(petal)==petal.getMaxHealth()*4,"Harmony grants four-times-health clone capacity");
        clock(level,level.getGameTime()+500);petal.getFoodData().setFoodLevel(20);check(NaturePowers.cast(petal,"petal",2,20).contains("Demonio")&&NaturePowers.giant(petal)&&NaturePowers.flying(petal),"Demonio grants giant form and winged flight");Mastery.grant(petal,Element.AIR,3);Progress.select(petal,Element.AIR);natureTicks(level,1,petal);check(NaturePowers.giant(petal)&&NaturePowers.flying(petal),"Demonio persists while selecting elements");NaturePowers.forget(petal);petal.addTag("cb_mode_fruit");
        var traveler=doorPlayer(level,"NatureTraveler");FruitClaims.releaseOnDeath(tree);check(NaturePowers.consume(traveler,Relics.natureFruit("tree"),"tree"),"Server traveler can claim released tree fruit");traveler.snapTo(200,y+12,200);check(NaturePowers.cast(traveler,"tree",2,0).contains("Root Travel")&&traveler.isSpectator(),"Root travel enters real spectator-backed realm");NaturePowers.recover(traveler);check(!traveler.isSpectator()&&!traveler.isInvisible(),"Persistent recovery snapshot restores survival and visibility after interruption");NaturePowers.forget(traveler);FruitClaims.releaseOnDeath(traveler);FruitClaims.releaseOnDeath(petal);check(FruitClaims.owner("tree")==null&&FruitClaims.owner("petal")==null,"Death releases both nature fruit claims");NaturePowers.clear();check(NaturePowers.boundCount()==0&&NaturePowers.visualCount()==0,"All nature constructs clean up");
        System.out.println("CREEPERBORN_TREE_PETAL_REGRESSION_PASSED");
    }

    private void run(ServerLevel level) {
        try { Class.forName("net.minecraft.server.network.ServerGamePacketListenerImpl"); }
        catch (ClassNotFoundException error) { throw new AssertionError(error); }
        y = level.getMaxY() - 40;
        checkDoorFruit(level);
        checkDangerousDoors(level);
        checkTridentCuring(level);
        checkExplicitFruitCommands(level);
        checkPhoenix(level);
        checkLightning(level);
        checkLightningChargeAndFlashes(level);
        checkRubberLightningCounter(level);
        checkDarkMoves(level);
        checkDarkCaptures(level);
        checkFoundSecondFruit(level);
        checkOp(level);
        checkGravity(level);
        checkStringFruit(level);checkPaw(level);checkTremor(level);checkLoveSand(level);checkIceMagma(level);checkAzure(level);checkNature(level);
        checkFruitArtwork(level);
        checkVortex(level);
        checkQuietMastery(level);
        checkGuardMaterials(level);
        checkAirSteps(level);
        for (int x = -5; x <= 5; x++) for (int z = -5; z <= 12; z++) {
            level.setBlockAndUpdate(new BlockPos(x, (int)y - 1, z), Blocks.STONE.defaultBlockState());
            for (int dy = 0; dy <= 4; dy++)
                level.setBlockAndUpdate(new BlockPos(x, (int)y + dy, z), Blocks.AIR.defaultBlockState());
        }
        Player cycling = player(level);
        check(Progress.cycle(cycling, 1) == null && Progress.cycle(cycling, -1) == null, "No cycling without hearts");
        Progress.consume(cycling, Relics.heart(Element.EARTH));
        check(Progress.cycle(cycling, -1) == Element.EARTH, "Single unlocked element stays selected");
        Progress.consume(cycling, Relics.heart(Element.AIR));
        check(Progress.cycle(cycling, 1) == Element.EARTH && Progress.cycle(cycling, -1) == Element.AIR,
            "Forward and reverse cycling skip locked elements and wrap");
        check(new ControlPayload(0,0).validRequest() && new ControlPayload(1,-1).validRequest()
            && new ControlPayload(1,1).validRequest() && new ControlPayload(2,2).validRequest()
            && !new ControlPayload(1,0).validRequest() && !new ControlPayload(2,3).validRequest()
            && !new ControlPayload(3,0).validRequest(), "Control packets validate actions, direction and move bounds");
        Player p = player(level);
        check(!Progress.all(p) && Progress.selected(p) == null, "Starts without powers");
        ItemStack renamed = new ItemStack(Items.HEART_OF_THE_SEA);
        renamed.set(DataComponents.CUSTOM_NAME, Component.literal("Earth Creeper Heart"));
        check(!Progress.consume(p, renamed), "Renamed vanilla items cannot unlock powers");
        for (Element e : Element.values()) {
            ItemStack heart = Relics.heart(e);
            check(Progress.consume(p, heart) && heart.isEmpty() && Progress.unlocked(p, e), "Each heart unlocks and consumes once");
            ItemStack duplicate = Relics.heart(e);
            check(!Progress.consume(p, duplicate) && duplicate.getCount() == 1, "Duplicates stay available for trading");
        }
        check(Progress.all(p) && count(p, "trident") == 1 && count(p, "focus") == 1, "One focus and one all-elements trident");
        check(Mastery.tier(p, Element.EARTH) == 1 && !Mastery.allowed(p, Element.EARTH, 1), "Heart starts with basic move only");
        check(Mastery.tier(9, 300) == 1 && Mastery.tier(10, 299) == 1 && Mastery.tier(10, 300) == 2,
            "Second move requires both use count and active time");
        check(Mastery.tier(39, 1200) == 2 && Mastery.tier(40, 1199) == 2 && Mastery.tier(40, 1200) == 3,
            "Third move requires both use count and active time");
        Player learner = player(level); Progress.consume(learner, Relics.heart(Element.AIR));
        Creeperborn.ABILITIES.move(learner,1);
        check(Creeperborn.ABILITIES.cast(learner).contains("locked"), "Mastery gate blocks advanced move before unlock");
        Creeperborn.ABILITIES.move(learner,0); Creeperborn.ABILITIES.cast(learner);
        check(Mastery.uses(learner,Element.AIR) == 0, "Empty cast grants no use credit");
        Mastery.credit(learner,Element.AIR); Mastery.credit(learner,Element.AIR);
        check(Mastery.uses(learner,Element.AIR) == 1, "Use credit limited to once per second");
        Mastery.grant(learner,Element.AIR,2); Player restored = player(level);
        for (String tag : learner.entityTags()) restored.addTag(tag);
        check(Mastery.tier(restored,Element.AIR) == 2 && Mastery.seconds(restored,Element.AIR) == 300,
            "Mastery restored from persistent player tags");
        check(!Mastery.harmony(p), "Collecting four hearts alone does not grant Harmony");
        for (Element e : Element.values()) Mastery.grant(p,e,3);
        Mastery.grant(p,Element.AIR,2); // Preserve baseline checks below without all-four mastery bonus.
        check(Progress.cycle(p) == Element.EARTH, "Cycles all unlocked elements");

        Progress.select(p, Element.WATER); Creeperborn.ABILITIES.move(p, 2); p.setHealth(12);
        check(Creeperborn.ABILITIES.cast(p).equals("Mending Flow!") && p.getHealth() > 12 && p.getHealth() < 16,
            "Water still works at reduced strength without a source");
        Creeperborn.ABILITIES.clear(); Creeperborn.ABILITIES.move(p, 2); p.setHealth(12);
        ItemStack bucket = new ItemStack(Items.WATER_BUCKET);
        bucket.set(DataComponents.CUSTOM_NAME, Component.literal("Travel water")); p.addItem(bucket);
        check(Creeperborn.ABILITIES.cast(p).equals("Mending Flow!") && p.getHealth() == 16, "Bucket enables healing");
        check(Creeperborn.ABILITIES.cast(p).startsWith("Cooling down") && p.getHealth() == 16, "Cooldown prevents repeated healing");
        Progress.select(p, Element.EARTH); Creeperborn.ABILITIES.move(p, 1); Creeperborn.ABILITIES.cast(p);
        check(Creeperborn.ABILITIES.rocks.guarded(p), "Stone Guard creates frontal rocks");
        Zombie front = EntityTypes.ZOMBIE.create(level, EntitySpawnReason.COMMAND); front.snapTo(.5, y, 3.5);
        Zombie rear = EntityTypes.ZOMBIE.create(level, EntitySpawnReason.COMMAND); rear.snapTo(.5, y, -3.5);
        check(Creeperborn.ABILITIES.rocks.blocks(p, p.damageSources().mobAttack(front)), "Stone Guard blocks front");
        check(!Creeperborn.ABILITIES.rocks.blocks(p, p.damageSources().mobAttack(rear)), "Stone Guard leaves rear exposed");
        Progress.select(p, Element.FIRE); Creeperborn.ABILITIES.move(p, 2); Creeperborn.ABILITIES.cast(p);
        check(p.hasEffect(MobEffects.FIRE_RESISTANCE), "Heat Shield grants fire protection");
        Progress.select(p, Element.AIR); Creeperborn.ABILITIES.move(p, 1); Creeperborn.ABILITIES.cast(p);
        check(p.getDeltaMovement().y > .6, "Air Step launches player");
        p.setDeltaMovement(0, 0, 0);

        Zombie z = EntityTypes.ZOMBIE.create(level, EntitySpawnReason.COMMAND);
        z.snapTo(.5, y, 6.5); z.setNoAi(true); level.addFreshEntity(z);
        Progress.select(p, Element.EARTH); Creeperborn.ABILITIES.move(p, 0); String boulderCast = Creeperborn.ABILITIES.cast(p);
        StringBuilder path = new StringBuilder();
        for (int i = 0; i < 10; i++) {
            Creeperborn.ABILITIES.rocks.tick(); path.append(Creeperborn.ABILITIES.rocks.activeBoulders()).append(',');
        }
        check(z.getHealth() < 20, "Earth Bolt hits hostile: cast=" + boulderCast + ", active path=" + path
            + ", visible=" + p.hasLineOfSight(z) + ", queried=" + level.getEntitiesOfClass(net.minecraft.world.entity.LivingEntity.class, p.getBoundingBox().inflate(17)).contains(z)
            + ", displays=" + level.getEntitiesOfClass(net.minecraft.world.entity.Display.BlockDisplay.class, p.getBoundingBox().inflate(17)).stream().map(d -> d.position().toString()).toList());
        Zombie blocked = EntityTypes.ZOMBIE.create(level, EntitySpawnReason.COMMAND);
        blocked.snapTo(.5, y, 9.5); blocked.setNoAi(true); level.addFreshEntity(blocked);
        z.discard();
        for (int x = -1; x <= 1; x++) for (int dy = 0; dy <= 2; dy++)
            level.setBlockAndUpdate(new BlockPos(x, (int)y + dy, 3), Blocks.STONE.defaultBlockState());
        Progress.select(p, Element.FIRE); Creeperborn.ABILITIES.move(p, 0); Creeperborn.ABILITIES.cast(p);
        check(blocked.getHealth() == 20, "Solid wall blocks targeted abilities"); blocked.discard();
        for (int x = -1; x <= 1; x++) for (int dy = 0; dy <= 2; dy++)
            level.setBlockAndUpdate(new BlockPos(x, (int)y + dy, 3), Blocks.AIR.defaultBlockState());

        Villager v = EntityTypes.VILLAGER.create(level, EntitySpawnReason.COMMAND);
        v.snapTo(2.5, y, .5); level.addFreshEntity(v); var offers = v.getOffers();
        Player other = player(level);
        check(!Creeperborn.GUARDIANS.assign(other, v, Element.AIR), "Four elements required for villagers");
        check(Creeperborn.GUARDIANS.assign(p, v, Element.WATER), "Trident progression enables guardian assignment");
        for (Element e : Element.values()) other.addTag("cb_unlock_" + e.key());
        check(!Creeperborn.GUARDIANS.assign(other, v, Element.FIRE), "Another player cannot steal a guardian");
        check(v.getOffers() == offers, "Villager offers preserved");
        Zombie invader = EntityTypes.ZOMBIE.create(level, EntitySpawnReason.COMMAND);
        invader.snapTo(5.5, y, .5); invader.setNoAi(true); level.addFreshEntity(invader);
        Creeperborn.GUARDIANS.tick();
        check(invader.getHealth() < 20 && v.getHealth() == 20, "Guardian attacks hostile without harming itself");
        check(Creeperborn.GUARDIANS.release(p, v) && Guardians.element(v) == null, "Owner can release guardian");
        invader.discard(); v.discard();

        Creeper c = EntityTypes.CREEPER.create(level, EntitySpawnReason.COMMAND);
        c.snapTo(.5, y, 4.5); Creeperborn.CREEPERS.assign(c, Element.FIRE); level.addFreshEntity(c);
        c.hurtServer(level, p.damageSources().playerAttack(p), 200);
        var drops = level.getEntitiesOfClass(ItemEntity.class, new AABB(-5, y - 2, -5, 6, y + 3, 12),
            item -> Relics.kind(item.getItem()).equals("heart:fire"));
        check(drops.size() == 1, "Player kill drops one correct elemental heart");
        Creeperborn.CREEPERS.died(c, p.damageSources().playerAttack(p));
        check(level.getEntitiesOfClass(ItemEntity.class, new AABB(-5, y - 2, -5, 6, y + 3, 12),
            item -> Relics.kind(item.getItem()).equals("heart:fire")).size() == 1, "Duplicate callbacks cannot duplicate hearts");
        boolean rejected = false;
        try { server.getCommands().getDispatcher().execute("creeperborn move 4", server.createCommandSourceStack()); }
        catch (Exception expected) { rejected = true; }
        check(rejected, "Invalid move slot rejected");
        for (Element e : Element.values()) for (int slot = 0; slot < 3; slot++) {
            Player caster = player(level); Mastery.grant(caster,e,3); Progress.select(caster, e);
            caster.addItem(new ItemStack(Items.WATER_BUCKET)); caster.setHealth(12);
            Zombie target = EntityTypes.ZOMBIE.create(level, EntitySpawnReason.COMMAND);
            target.snapTo(.5, y, 3.5); target.setNoAi(true); check(level.addFreshEntity(target), "Move target added");
            Creeperborn.ABILITIES.move(caster, slot);
            check(Creeperborn.ABILITIES.cast(caster).equals(e.moves[slot] + "!"), "Every move casts: " + e.moves[slot]);
            if (e == Element.EARTH && slot == 0) for (int i = 0; i < 10; i++) Creeperborn.ABILITIES.rocks.tick();
            if(e==Element.AIR&&slot==2)Creeperborn.ABILITIES.vortices.tick();
            if(slot==1&&e==Element.FIRE)check(FireAnimations.activeCount()>0,"Flame Ring creates an expanding circle model");
            if(slot==0&&e==Element.FIRE)check(FireAnimations.activeCount()>0,"Fire Z creates a fireball and trail model");
            if(slot==0&&e==Element.WATER)check(WaterAnimations.activeCount()>0,"Water Whip creates a flowing model effect");
            if (slot == 0 || (slot == 2 && e == Element.AIR) || (slot == 1 && e == Element.FIRE))
                check(target.getHealth() < 20, "Offensive move damages hostile: " + e.moves[slot]);
            if (slot == 1 && e == Element.WATER)
                check(caster.hasEffect(MobEffects.RESISTANCE), "Defensive move grants resistance");
            if (slot == 1 && e == Element.EARTH) check(Creeperborn.ABILITIES.rocks.guarded(caster), "Frontal rock guard created");
            if (slot == 2 && e == Element.EARTH) check(Shackles.rooted(target)&&Shackles.remaining(target)==80, "Slab Shackles roots hostile for four seconds");
            if (slot == 1 && e == Element.WATER) {
                check(Creeperborn.ABILITIES.pushRemaining(target)==0,"Tidal arms rise before pushing");
                for(int phase=0;phase<WaterAnimations.GUARD_WINDUP;phase++){clock(level,level.getGameTime()+1);Creeperborn.ABILITIES.tick();}
                check(Creeperborn.ABILITIES.pushRemaining(target) == 10, "Tidal Guard requests ten-block push");
                check(target.getHealth()<20,"Tidal Guard damages hostile while pushing");
            }
            if(slot==1&&e==Element.FIRE)check(Math.abs(Creeperborn.ABILITIES.pushRemaining(target)-4*EnvironmentPower.strength(caster,e))<.001,
                "Flame Ring requests sustained four-block push scaled by environment");
            if (slot == 2 && e == Element.WATER) check(caster.getHealth() == 16, "Mending Flow heals four health");
            if (slot == 2 && e == Element.FIRE) check(caster.hasEffect(MobEffects.FIRE_RESISTANCE), "Heat Shield protects");
            if (slot == 1 && e == Element.AIR) check(caster.getDeltaMovement().y > .6, "Air Step has upward impulse");
            if (slot == 2 && e == Element.AIR) check(target.getDeltaMovement().z < 0, "Vortex pulls target inward");
            if (e == Element.AIR && slot == 1)
                check(Creeperborn.ABILITIES.cast(caster).equals("Air Step!") && Mastery.uses(caster,e) == 40,
                    "Air Step repeats without cooldown and grants no use credit");
            else check(Creeperborn.ABILITIES.cast(caster).startsWith("Cooling down"), "Other moves enforce cooldowns");
            target.discard();
        }
        Creeper natural = EntityTypes.CREEPER.create(level, EntitySpawnReason.COMMAND);
        Creeperborn.config().naturalCreepers = true; Creeperborn.config().elementalChance = 1;
        Creeperborn.CREEPERS.track(natural); Element rolled = ElementalCreepers.element(natural);
        check(rolled != null, "Natural conversion uses configured probability");
        natural.removeTag("cb_creeper_" + rolled.key()); Creeperborn.CREEPERS.track(natural);
        check(ElementalCreepers.element(natural) == null, "Already examined creeper cannot reroll");
        Creeper split = EntityTypes.CREEPER.create(level, EntitySpawnReason.COMMAND); split.addTag("ns_creeper");
        Creeperborn.CREEPERS.track(split); check(ElementalCreepers.element(split) == null, "Night Splitters creepers excluded");
        for (Element e : new Element[]{Element.WATER}) {
            Creeperborn.ABILITIES.clear(); Player caster = player(level); Mastery.grant(caster,e,3);
            caster.addItem(new ItemStack(Items.WATER_BUCKET)); Progress.select(caster, e);
            Zombie target = EntityTypes.ZOMBIE.create(level, EntitySpawnReason.COMMAND);
            target.snapTo(.5, y, 2.5); target.setNoAi(true); level.addFreshEntity(target);
            Creeperborn.ABILITIES.move(caster, e == Element.EARTH ? 2 : 1); Creeperborn.ABILITIES.cast(caster);
            double start = target.getZ(), expected = 10;
            for (int tick = 0; tick < 36; tick++) { clock(level,level.getGameTime()+1);target.travel(net.minecraft.world.phys.Vec3.ZERO); Creeperborn.ABILITIES.tick(); }
            double travelled = target.getZ() - start;
            check(travelled >= expected - 1 && travelled <= expected + 1,
                e.title + " actual push distance: " + travelled + ", expected about " + expected);
            target.discard();
        }
        Creeperborn.ABILITIES.clear(); Player tidal = player(level); Mastery.grant(tidal,Element.WATER,3);
        tidal.addItem(new ItemStack(Items.WATER_BUCKET)); Progress.select(tidal, Element.WATER);
        // Keep all four directions inside the same fully loaded entity chunk.
        for (int x = 5; x <= 12; x++) for (int gridZ = 5; gridZ <= 12; gridZ++) {
            level.setBlockAndUpdate(new BlockPos(x, (int)y - 1, gridZ), Blocks.STONE.defaultBlockState());
            for (int dy = 0; dy <= 4; dy++) level.setBlockAndUpdate(new BlockPos(x, (int)y + dy, gridZ), Blocks.AIR.defaultBlockState());
        }
        tidal.snapTo(8.5, y, 8.5);
        java.util.List<Zombie> surrounding = new java.util.ArrayList<>();
        for (int[] offset : new int[][]{{2,0},{-2,0},{0,2},{0,-2}}) {
            Zombie target = EntityTypes.ZOMBIE.create(level, EntitySpawnReason.COMMAND);
            target.snapTo(8.5 + offset[0], y, 8.5 + offset[1]); target.setNoAi(true);
            check(level.addFreshEntity(target), "Tidal target added"); surrounding.add(target);
        }
        Creeperborn.ABILITIES.move(tidal, 1); Creeperborn.ABILITIES.cast(tidal);
        check(WaterAnimations.activeCount()==1,"Tidal Guard uses one bounded animated display");
        for(int phase=0;phase<WaterAnimations.GUARD_WINDUP;phase++){clock(level,level.getGameTime()+1);Creeperborn.ABILITIES.tick();}
        for (Zombie target : surrounding) {
            check(Creeperborn.ABILITIES.pushRemaining(target) == 10, "Tidal Guard covers front, rear and both sides: pos="
                + target.position() + ", remaining=" + Creeperborn.ABILITIES.pushRemaining(target) + ", visible=" + tidal.hasLineOfSight(target)); target.discard();
        }
        WaterAnimations.forget(tidal);check(WaterAnimations.activeCount()==0,"Water effects clean up on owner removal");
        Mastery.grant(tidal,Element.EARTH,3); Progress.select(tidal, Element.EARTH); Creeperborn.ABILITIES.move(tidal, 1);
        Creeperborn.ABILITIES.cast(tidal);
        var rockVisuals = level.getEntitiesOfClass(net.minecraft.world.entity.Display.BlockDisplay.class,
            new AABB(5, y - 2, 5, 12, y + 5, 12));
        check(rockVisuals.size() == 3 && rockVisuals.stream().allMatch(display -> !display.shouldBeSaved()),
            "Temporary rock visuals are excluded from world saves");
        check(new CastPayload(3,2).valid() && !new CastPayload(-1,0).valid() && !new CastPayload(4,0).valid()
            && !new CastPayload(0,3).valid(), "Combat packet bounds validated");
        var buffer = new net.minecraft.network.RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(), level.registryAccess());
        CastPayload.CODEC.encode(buffer, new CastPayload(2,1));
        check(CastPayload.CODEC.decode(buffer).equals(new CastPayload(2,1)), "Combat packet codec round trip"); buffer.release();
        checkHarmony(level);
        checkRubber(level);
        checkFruitWater(level);
        checkMoveRouting(level);
        checkReachAndNourishment(level);
        checkFiftyBlocksAndShield(level);
        checkStretchPunches(level);
        checkFlightCleanup(level);
        Player lavaMaster=player(level);
        for(Element e:Element.values())Mastery.grant(lavaMaster,e,3);
        check(!Rubber.unlocked(lavaMaster)&&Rubber.damage(lavaMaster,lavaMaster.damageSources().lava(),10)==0,
            "All-four mastery grants lava immunity without any fruit");
        check(WaterRules.mastered(lavaMaster)&&lavaMaster.canBreatheUnderwater(),"All-four mastery grants underwater breathing without a fruit");
        check(!lavaMaster.canFreeze(),"All-four mastery prevents native freezing");
        lavaMaster.setTicksFrozen(200);WaterRules.apply(lavaMaster);
        check(lavaMaster.getTicksFrozen()==0,"Mastery clears preexisting frozen buildup");
        check(Rubber.damage(lavaMaster,lavaMaster.damageSources().freeze(),4)==0,"Mastery rejects freezing damage without a fruit");
        lavaMaster.setAirSupply(1);WaterRules.apply(lavaMaster);
        check(lavaMaster.getAirSupply()==lavaMaster.getMaxAirSupply(),"Nonfruit master replenishes air");
        float breathBefore=lavaMaster.getHealth();
        lavaMaster.hurtServer(level,lavaMaster.damageSources().drown(),4);
        check(lavaMaster.getHealth()==breathBefore,"Native drowning damage cannot hurt a nonfruit master");
        lavaMaster.damageCooldownTime=0;
        float lavaBefore=lavaMaster.getHealth();lavaMaster.hurtServer(level,lavaMaster.damageSources().lava(),4);
        check(lavaMaster.getHealth()==lavaBefore,"Native lava damage respects elemental mastery immunity");
        lavaMaster.damageCooldownTime=0;
        check(Rubber.damage(lavaMaster,lavaMaster.damageSources().fall(),100)==0,"All-four mastery negates fall damage without a fruit");
        lavaMaster.hurtServer(level,lavaMaster.damageSources().fall(),100);
        check(lavaMaster.getHealth()==lavaBefore,"Native fall damage is blocked by all-four mastery");
        check(Rubber.damage(lavaMaster,lavaMaster.damageSources().generic(),4)==4,"Mastery leaves ordinary damage unchanged");
        lavaMaster.setRemainingFireTicks(100);WaterRules.apply(lavaMaster);
        check(!lavaMaster.isOnFire(),"Mastery clears residual burning after lava");
        Mastery.grant(lavaMaster,Element.AIR,2);
        check(Rubber.damage(lavaMaster,lavaMaster.damageSources().lava(),10)==10,"Three mastered elements are insufficient for lava immunity");
        check(Rubber.damage(lavaMaster,lavaMaster.damageSources().fall(),4)==4,"Dropping one mastery removes fall immunity");
        lavaMaster.damageCooldownTime=0;float fallBefore=lavaMaster.getHealth();
        lavaMaster.hurtServer(level,lavaMaster.damageSources().fall(),4);
        check(lavaMaster.getHealth()<fallBefore,"Native fall damage returns without full mastery");
        check(!WaterRules.mastered(lavaMaster)&&!lavaMaster.canBreatheUnderwater(),"Losing one mastery removes underwater breathing");
        check(lavaMaster.canFreeze()&&Rubber.damage(lavaMaster,lavaMaster.damageSources().freeze(),4)==4,"Losing one mastery restores normal freezing");
        lavaMaster.setTicksFrozen(180);WaterRules.apply(lavaMaster);
        check(lavaMaster.getTicksFrozen()==180,"Nonmaster keeps ordinary frozen buildup");
        check(Rubber.damage(lavaMaster,lavaMaster.damageSources().drown(),4)==4,"Drowning protection requires all-four mastery");
        Player airOnly=player(level);Mastery.grant(airOnly,Element.AIR,3);
        check(Rubber.damage(airOnly,airOnly.damageSources().fall(),4)==4,"Air-only mastery does not grant the all-four reward");
        Player budget = player(level);
        for (int i = 0; i < 20; i++) check(CombatNetwork.allow(budget), "Normal input packet budget accepted");
        check(!CombatNetwork.allow(budget), "Excess combat packets ignored");
    }
    private void checkStretchPunches(ServerLevel level){
        Player p=player(level);p.snapTo(-12,y+8,-14);
        check(Rubber.consume(p,Relics.fruit()),"Animation fixture acquires Rubber");Rubber.grant(p,3);
        var body=p.getBoundingBox();
        Zombie target=EntityTypes.ZOMBIE.create(level,EntitySpawnReason.COMMAND);target.snapTo(-12,y+8,-2);target.setNoAi(true);target.setNoGravity(true);level.addFreshEntity(target);
        Rubber.cast(p,0,8);
        check(Punches.activeCount()==1&&!Punches.lastLeft(p),"First punch uses right arm");
        check(target.getHealth()==20,"Distant target is not hit before fist reaches it");
        Punches.tick();check(target.getHealth()<20,"Extending fist sweeps into distant hostile");
        float after=target.getHealth();for(int tick=0;tick<5;tick++)Punches.tick();
        check(target.getHealth()==after&&Punches.activeCount()==0,"Retraction does not hit twice and displays expire");
        check(p.getBoundingBox().equals(body),"Stretch punch does not extend player body hitbox");
        var visuals=level.getEntitiesOfClass(net.minecraft.world.entity.Display.BlockDisplay.class,p.getBoundingBox().inflate(22));
        check(visuals.stream().noneMatch(d->d.getCustomName()!=null&&d.getCustomName().getString().startsWith("cb_punch|")),
            "Completed punch removes its temporary display entities");
        Rubber.clear();target.setHealth(20);target.damageCooldownTime=0;
        for(int dy=8;dy<=10;dy++)level.setBlockAndUpdate(new BlockPos(-12,(int)y+dy,-8),Blocks.STONE.defaultBlockState());
        Rubber.cast(p,0,8);
        var activeVisuals=level.getEntitiesOfClass(net.minecraft.world.entity.Display.BlockDisplay.class,p.getBoundingBox().inflate(22));
        check(activeVisuals.stream().filter(d->d.getCustomName()!=null&&d.getCustomName().getString().startsWith("cb_punch|")).allMatch(d->!d.shouldBeSaved()),
            "Punch arm and fist are excluded from world saves");
        for(int tick=0;tick<6;tick++)Punches.tick();
        check(target.getHealth()==20,"Wall blocks stretching fist before the hostile behind it");
        for(int dy=8;dy<=10;dy++)level.setBlockAndUpdate(new BlockPos(-12,(int)y+dy,-8),Blocks.AIR.defaultBlockState());
        target.discard();FruitClaims.releaseOnDeath(p);Rubber.clear();
        check(Punches.visual("cb_punch|"+p.getUUID()+"|L|F").left()&&Punches.visual("bad")==null,"Punch visual metadata validates owner and side");
    }
    private void checkFlightCleanup(ServerLevel level){
        Player p=player(level);check(Rubber.consume(p,Relics.fruit()),"Flight cleanup test acquires Rubber");Rubber.grant(p,3);
        p.getAbilities().mayfly=true;p.getAbilities().flying=true;
        Rubber.cast(p,2,0);Rubber.forget(p);
        check(p.getAbilities().mayfly&&p.getAbilities().flying,"Preexisting flight permission survives Gear ending");
        FruitClaims.releaseOnDeath(p);
        Player interrupted=player(level);interrupted.getAbilities().mayfly=true;interrupted.getAbilities().flying=true;
        interrupted.addTag("cb_rubber_gear_flight");Rubber.recoverFlight(interrupted);
        check(!interrupted.getAbilities().mayfly&&!interrupted.getAbilities().flying&&!interrupted.entityTags().contains("cb_rubber_gear_flight"),
            "Rejoin cleans up saved Gear flight marker after interrupted session");
    }
    private void checkFiftyBlocksAndShield(ServerLevel level){
        for(int distance:new int[]{40,52})for(boolean mastered:new boolean[]{false,true}){
            Player p=player(level);p.snapTo(-12,y+5,-14);p.addItem(new ItemStack(Items.WATER_BUCKET));
            Mastery.grant(p,Element.WATER,3);if(mastered)for(Element e:Element.values())Mastery.grant(p,e,3);
            Progress.select(p,Element.WATER);Creeperborn.ABILITIES.clear();Creeperborn.ABILITIES.move(p,0);
            Zombie target=EntityTypes.ZOMBIE.create(level,EntitySpawnReason.COMMAND);
            target.snapTo(-12,y+5,-14+distance);target.setNoAi(true);target.setNoGravity(true);level.addFreshEntity(target);
            Creeperborn.ABILITIES.cast(p);
            check((target.getHealth()<target.getMaxHealth())==(mastered&&distance<50),"Elemental bolt extends to fifty blocks and stops beyond");
            target.discard();
            Zombie rockTarget=EntityTypes.ZOMBIE.create(level,EntitySpawnReason.COMMAND);
            rockTarget.snapTo(-12,y+5,-14+distance);rockTarget.setNoAi(true);rockTarget.setNoGravity(true);level.addFreshEntity(rockTarget);
            Creeperborn.ABILITIES.rocks.throwBoulder(p,1);
            for(int tick=0;tick<44;tick++)Creeperborn.ABILITIES.rocks.tick();
            check((rockTarget.getHealth()<rockTarget.getMaxHealth())==(mastered&&distance<50),"Earth projectile extends to fifty blocks and stops beyond");
            rockTarget.discard();Creeperborn.ABILITIES.clear();
        }
        Player shield=player(level);Mastery.grant(shield,Element.FIRE,3);Progress.select(shield,Element.FIRE);
        shield.setHealth(8);
        Creeperborn.ABILITIES.move(shield,2);Creeperborn.ABILITIES.cast(shield);
        check(shield.getHealth()==18&&shield.getMaxHealth()==20,"Heat Shield heals five red hearts without changing maximum health");
        check(shield.getEffect(MobEffects.RESISTANCE).getDuration()==(int)Math.round(160*EnvironmentPower.strength(shield,Element.FIRE)),
            "Heat Shield uses doubled eight-second base protection");
        check(shield.getAbsorptionAmount()==shield.getMaxHealth()/2,"Heat Shield gives half maximum health in temporary hearts");
        check(shield.getEffect(MobEffects.ABSORPTION).getDuration()==shield.getEffect(MobEffects.RESISTANCE).getDuration(),
            "Extra hearts share Heat Shield physical-protection duration");
        float health=shield.getHealth(),absorbed=shield.getAbsorptionAmount();shield.hurtServer(level,shield.damageSources().generic(),4);
        check(shield.getHealth()==health&&shield.getAbsorptionAmount()<absorbed,"Heat Shield extra hearts absorb native damage");
        shield.removeEffect(MobEffects.ABSORPTION);check(shield.getAbsorptionAmount()==0,"Removing temporary absorption removes its extra hearts");
        Player stronger=player(level);Mastery.grant(stronger,Element.FIRE,3);Progress.select(stronger,Element.FIRE);
        stronger.addEffect(new net.minecraft.world.effect.MobEffectInstance(MobEffects.ABSORPTION,600,3));stronger.setAbsorptionAmount(16);
        Creeperborn.ABILITIES.move(stronger,2);Creeperborn.ABILITIES.cast(stronger);
        check(stronger.getAbsorptionAmount()==16&&stronger.getEffect(MobEffects.ABSORPTION).getDuration()==600,
            "Heat Shield preserves stronger existing absorption");
        Creeperborn.ABILITIES.clear();
    }
    private void checkReachAndNourishment(ServerLevel level){
        for(boolean mastered:new boolean[]{false,true}){
            Player p=player(level);p.snapTo(-12,y+5,-14);p.addItem(new ItemStack(Items.WATER_BUCKET));
            Mastery.grant(p,Element.WATER,3);if(mastered)for(Element e:Element.values())Mastery.grant(p,e,3);
            Progress.select(p,Element.WATER);Creeperborn.ABILITIES.clear();Creeperborn.ABILITIES.move(p,0);
            Zombie target=EntityTypes.ZOMBIE.create(level,EntitySpawnReason.COMMAND);
            target.snapTo(-12,y+5,26);target.setNoAi(true);target.setNoGravity(true);level.addFreshEntity(target);
            Creeperborn.ABILITIES.cast(p);
            check((target.getHealth()<target.getMaxHealth())==mastered,"Harmony extends elemental Z beyond tier-three range to forty blocks");target.discard();
            p.snapTo(-14,y+5,-14);p.setYRot(-45);
            Creeperborn.ABILITIES.clear();Creeperborn.ABILITIES.rocks.throwBoulder(p,1);
            for(int tick=0;tick<26;tick++)Creeperborn.ABILITIES.rocks.tick();
            check(Creeperborn.ABILITIES.rocks.activeBoulders()==(mastered?1:0),"Mastered Earth boulder travels beyond baseline lifetime");Creeperborn.ABILITIES.clear();
        }
        Player nourished=player(level);Mastery.grant(nourished,Element.WATER,3);Progress.select(nourished,Element.WATER);
        nourished.getFoodData().setFoodLevel(2);nourished.setHealth(nourished.getMaxHealth());
        Creeperborn.ABILITIES.move(nourished,2);check(Creeperborn.ABILITIES.cast(nourished).equals("Mending Flow!"),"Mending Flow casts at full health while hungry");
        check(nourished.getFoodData().getFoodLevel()==20,"Mending Flow fills all hunger");
        check(Creeperborn.ABILITIES.cast(nourished).startsWith("Cooling down"),"Hunger refill preserves Mending cooldown");Creeperborn.ABILITIES.clear();
        Player rubber=player(level);rubber.snapTo(-12,y+5,-14);check(Rubber.consume(rubber,Relics.fruit()),"Reach test acquires Rubber");Rubber.grant(rubber,3);
        Zombie far=EntityTypes.ZOMBIE.create(level,EntitySpawnReason.COMMAND);far.snapTo(-12,y+5,4);far.setNoAi(true);far.setNoGravity(true);level.addFreshEntity(far);
        Rubber.cast(rubber,0,8);check(far.getHealth()==far.getMaxHealth(),"Normal charged punch cannot reach eighteen blocks");
        Rubber.clear();Rubber.cast(rubber,2,0);Rubber.cast(rubber,0,0);Punches.tick();
        check(far.getHealth()<far.getMaxHealth(),"Gear 5 tap punch reaches eighteen blocks");
        Rubber.tickPlayer(rubber);
        check(rubber.getEffect(MobEffects.SPEED).getAmplifier()==2&&rubber.getEffect(MobEffects.JUMP_BOOST).getAmplifier()==3,
            "Gear 5 supplies Speed III and Jump Boost IV");
        far.discard();FruitClaims.releaseOnDeath(rubber);Rubber.clear();
    }
    private void checkMoveRouting(ServerLevel level){
        Player p=player(level);Mastery.grant(p,Element.WATER,3);p.addItem(new ItemStack(Items.WATER_BUCKET));
        Progress.select(p,Element.WATER);p.setHealth(10);
        check(CombatNetwork.press(p,2).equals("Mending Flow!")&&p.getHealth()==14,"C routes to selected Water immediately");
        check(Rubber.consume(p,Relics.fruit()),"Routing test acquires Rubber");Rubber.grant(p,3);
        check(CombatNetwork.press(p,2).isEmpty()&&p.getHealth()==14,"C press routes to newly selected Rubber, not stale Water");
        Rubber.release(p,2);check(Rubber.gear(p),"C release activates Rubber Gear 5 after selection");
        Progress.select(p,Element.WATER);Creeperborn.ABILITIES.clear();p.setHealth(10);
        check(CombatNetwork.press(p,2).equals("Mending Flow!")&&p.getHealth()==14,"C routes back to element without a failed first cast");
        FruitClaims.releaseOnDeath(p);Rubber.clear();Creeperborn.ABILITIES.clear();
    }
    private void checkFruitWater(ServerLevel level) {
        Rubber.clear();
        for(int x=6;x<=10;x++)for(int z=-10;z<=-6;z++) {
            level.setBlockAndUpdate(new BlockPos(x,(int)y-1,z),Blocks.STONE.defaultBlockState());
            for(int dy=0;dy<4;dy++)level.setBlockAndUpdate(new BlockPos(x,(int)y+dy,z),Blocks.WATER.defaultBlockState());
        }
        Player ordinary=player(level);ordinary.snapTo(8.5,y,-8.5);
        check(WaterRules.submerged(ordinary)&&!WaterRules.suppressed(ordinary),"Ordinary submerged players retain swimming");
        check(!ordinary.canBreatheUnderwater(),"Nonfruit ordinary players retain normal breathing");
        Player fruit=player(level);Rubber.consume(fruit,Relics.fruit());Rubber.grant(fruit,3);
        check(Rubber.cast(fruit,2,0).contains("Gear 5!"),"Gear starts on land for drowning test");
        fruit.snapTo(8.5,y+1,-8.5);
        check(WaterRules.suppressed(fruit),"Fruit-only player is weak when eyes are underwater");
        fruit.addEffect(new net.minecraft.world.effect.MobEffectInstance(MobEffects.WATER_BREATHING,600,0));
        fruit.setAirSupply(100);fruit.baseTick();
        check(fruit.getAirSupply()==99,"Fruit curse consumes normal air even with breathing potion");
        fruit.damageCooldownTime=0;fruit.setAirSupply(-19);float before=fruit.getHealth();fruit.baseTick();
        check(fruit.getHealth()<before&&Rubber.gear(fruit),"Native drowning damages an active Gear 5 player");
        WaterRules.apply(fruit);
        check(!Rubber.gear(fruit),"Submersion ends Gear 5");
        check(!fruit.getAbilities().mayfly,"Water suppression revokes Gear flight");
        fruit.setDeltaMovement(0,.8,0);double previousY=fruit.getY();
        fruit.travel(new net.minecraft.world.phys.Vec3(0,1,0));
        check(fruit.getY()<previousY&&fruit.getDeltaMovement().y<0,"Native travel overrides upward swim input with sinking");
        check(Rubber.cast(fruit,0,0).contains("disabled"),"Rubber powers disabled underwater");
        for(Element e:Element.values())Mastery.grant(fruit,e,2);
        Progress.select(fruit,Element.WATER);
        check(WaterRules.suppressed(fruit)&&Creeperborn.ABILITIES.cast(fruit).contains("disabled"),
            "Owning all hearts or selecting Water cannot bypass fruit weakness");
        fruit.snapTo(8.5,y+3,-8.5);
        check(!WaterRules.suppressed(fruit),"Feet in shallow water with head above surface are safe");
        fruit.snapTo(.5,y,.5);check(!WaterRules.suppressed(fruit),"Dry land does not trigger fruit weakness");
        fruit.snapTo(8.5,y+1,-8.5);
        for(Element e:Element.values())Mastery.grant(fruit,e,3);
        check(WaterRules.mastered(fruit)&&!WaterRules.suppressed(fruit)&&fruit.canBreatheUnderwater(),
            "All four fully mastered elements restore swimming and grant underwater breathing");
        fruit.setAirSupply(-19);before=fruit.getHealth();fruit.damageCooldownTime=0;fruit.baseTick();WaterRules.apply(fruit);
        check(fruit.getAirSupply()==fruit.getMaxAirSupply()&&fruit.getHealth()==before,"Mastered fruit user restores air without drowning");
        check(Rubber.damage(fruit,fruit.damageSources().drown(),2)==0,"Harmony protects against drowning damage");
        fruit.setDeltaMovement(0,.3,0);double swimY=fruit.getY();
        fruit.travel(net.minecraft.world.phys.Vec3.ZERO);
        check(fruit.getY()>swimY,"Mastery restores native upward water movement");
        check(!Creeperborn.ABILITIES.cast(fruit).contains("disabled"),"Mastered player can cast underwater");
        Mastery.grant(fruit,Element.AIR,2);
        check(WaterRules.suppressed(fruit)&&!fruit.canBreatheUnderwater(),"Losing one mastery immediately restores the weakness");
        Player restored=player(level,fruit.getUUID());for(String tag:fruit.entityTags())restored.addTag(tag);restored.snapTo(8.5,y,-8.5);
        check(WaterRules.suppressed(restored),"Restored fruit ownership still applies water weakness");
        var buffer=new net.minecraft.network.RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(),level.registryAccess());
        WaterStatePayload.CODEC.encode(buffer,new WaterStatePayload(3));
        check(WaterStatePayload.CODEC.decode(buffer).flags()==3,"Water-state packet round trip");buffer.release();
        FruitClaims.releaseOnDeath(fruit);Rubber.clear();Creeperborn.ABILITIES.clear();
        for(int x=6;x<=10;x++)for(int z=-10;z<=-6;z++)for(int dy=0;dy<4;dy++)
            level.setBlockAndUpdate(new BlockPos(x,(int)y+dy,z),Blocks.AIR.defaultBlockState());
    }
    private void startRubberLive(ServerLevel level) {
        rubberLive=player(level); rubberLive.snapTo(-8,y,-8);
        Rubber.consume(rubberLive,Relics.fruit());Rubber.grant(rubberLive,3);
        rubberTarget=EntityTypes.ZOMBIE.create(level,EntitySpawnReason.COMMAND);
        rubberTarget.snapTo(-8,y,-3);rubberTarget.setNoAi(true);rubberTarget.setNoGravity(true);
        check(level.addFreshEntity(rubberTarget),"Rubber live hostile fixture added");
        Rubber.press(rubberLive,0);
        for(boolean mastered:new boolean[]{false,true}){
            Player caster=player(level);caster.snapTo(mastered?-8.5:8.5,y+5,6.5);
            Mastery.grant(caster,Element.EARTH,3);if(mastered)for(Element e:Element.values())Mastery.grant(caster,e,3);
            Progress.select(caster,Element.EARTH);
            Zombie target=EntityTypes.ZOMBIE.create(level,EntitySpawnReason.COMMAND);target.snapTo(mastered?-8.5:8.5,y+5,9.5);
            target.setNoAi(true);target.setNoGravity(true);level.addFreshEntity(target);
            Creeperborn.ABILITIES.move(caster,2);Creeperborn.ABILITIES.cast(caster);
            check(Shackles.remaining(target)==(mastered?180:80),"Shackles duration follows all-four mastery");
            if(mastered)slabMaster=target;else slabNormal=target;
        }
    }
    private boolean runRubberLive() {
        rubberTicks++;Rubber.tickPlayer(rubberLive);
        if(rubberTicks==8) {
            Rubber.release(rubberLive,0);
            check(rubberTarget.getHealth()<rubberTarget.getMaxHealth(),"Held punch damages a real aimed hostile");
            check(Rubber.uses(rubberLive)==41,"Successful punch credits fruit progression");
            Rubber.press(rubberLive,1);
        }
        if(rubberTicks==16) {
            check(rubberLive.getDeltaMovement().z>0,"Held grab pulls caster toward a nonplayer target");
            Rubber.release(rubberLive,1);
            check(rubberLive.getDeltaMovement().equals(net.minecraft.world.phys.Vec3.ZERO),"Releasing X detaches grapple and stops its imposed motion");
            Rubber.press(rubberLive,2);
        }
        if(rubberTicks==20){
            for(Zombie target:new Zombie[]{slabNormal,slabMaster}){
                var before=target.position();target.setDeltaMovement(.5,.4,0);target.travel(new net.minecraft.world.phys.Vec3(0,0,1));
                check(target.position().equals(before)&&target.getDeltaMovement().lengthSqr()==0,"Root blocks native travel and jump velocity");
                target.move(net.minecraft.world.entity.MoverType.SELF,new net.minecraft.world.phys.Vec3(.5,0,0));
                check(target.position().equals(before),"Root also blocks direct custom mob movement");
            }
        }
        if(rubberTicks==24) {
            float modified=Rubber.damage(rubberLive,rubberTarget.damageSources().mobAttack(rubberTarget),10);
            check(Math.abs(modified-2)<.01,"Held C activates sustained Bounce Guard after threshold");
            check(!Rubber.gear(rubberLive),"Held C does not activate transformation");
            var level=(ServerLevel)rubberLive.level();
            var arrow=EntityTypes.ARROW.create(level,EntitySpawnReason.EVENT);
            var shot=rubberLive.damageSources().arrow(arrow,rubberTarget);
            var ownerless=rubberLive.damageSources().arrow(arrow,null);
            check(Math.abs(Rubber.damage(rubberLive,shot,10)-2)<.001,"Guard blocks eighty percent of ordinary arrow damage without Rubber penalty");
            check(Math.abs(Rubber.damage(rubberLive,ownerless,10)-2)<.001,"Guard protects against ownerless dispenser arrows");
            rubberLive.damageCooldownTime=0;float arrowBefore=rubberLive.getHealth();
            rubberLive.hurtServer(level,ownerless,4);
            check(Math.abs((arrowBefore-rubberLive.getHealth())-.8)<.01,"Native arrow damage applies Bounce Guard reduction");
            arrow.setRemainingFireTicks(100);
            check(Math.abs(Rubber.damage(rubberLive,shot,10)-3)<.001,"Flaming arrows retain heat weakness while guard still reduces damage");
            arrow.clearFire();

            check(Math.abs(rubberLive.getBbWidth()-.9)<.01&&Math.abs(rubberLive.getBbHeight()-2.7)<.01,
                "Bounce Guard scales actual player width and height by 1.5");
            Rubber.tickPlayer(rubberLive);
            check(Math.abs(rubberLive.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.SCALE)-1.5)<.001,
                "Repeated guard ticks do not stack size modifiers");
            rubberLive.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.SCALE).setBaseValue(2);
            rubberLive.refreshDimensions();
            check(Math.abs(rubberLive.getBbHeight()-5.4)<.01,"Guard multiplies existing player scale");

            Rubber.cancelInput(rubberLive);
            check(Rubber.damage(rubberLive,rubberTarget.damageSources().mobAttack(rubberTarget),10)==10,"Cancel immediately clears guard");
            check(Math.abs(Rubber.damage(rubberLive,shot,10)-15)<.001,"Releasing guard restores ordinary Rubber arrow vulnerability");
            arrow.discard();
            check(Math.abs(rubberLive.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.SCALE)-2)<.001,
                "Guard cancel preserves unrelated base scale");
            rubberLive.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.SCALE).setBaseValue(1);
            rubberLive.refreshDimensions();
            check(Math.abs(rubberLive.getBbHeight()-1.8)<.01,"Guard release restores normal hitbox");
            Rubber.press(rubberLive,2);
        }
        if(rubberTicks==25) {
            Rubber.release(rubberLive,2);check(Rubber.gear(rubberLive),"Quick C tap enters Gear 5");
            check(Math.abs(rubberLive.getBbHeight()-1.8)<.01,"Gear 5 keeps normal size");
            rubberLive.damageCooldownTime=0;float before=rubberLive.getHealth();
            rubberLive.hurtServer((ServerLevel)rubberLive.level(),rubberLive.damageSources().generic(),4);
            check(rubberLive.getHealth()==before,"Native damage path applies Gear 5 immunity");
            rubberLive.damageCooldownTime=0;
            rubberLive.hurtServer((ServerLevel)rubberLive.level(),rubberLive.damageSources().lava(),4);
            check(rubberLive.getHealth()<before,"Native lava damage bypasses Gear 5");
            rubberLive.getFoodData().setFoodLevel(20);
        }
        if(rubberTicks==26){
            check(rubberLive.getAbilities().mayfly,"Active Gear 5 permits double-tap flight");
            rubberLive.getAbilities().flying=true;
        }
        if(rubberTicks==31||rubberTicks==37){
            rubberTarget.snapTo(-8,y,-3);rubberTarget.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
            float before=rubberTarget.getHealth();
            check(Rubber.cast(rubberLive,0,0).equals("Stretch Punch!"),"Gear punch recovers after six ticks");
            check(Punches.lastLeft(rubberLive)==(rubberTicks==31),"Successive punches alternate left and right");
            check(rubberTarget.getHealth()<before,"Rapid Gear punches bypass hostile hurt cooldown");
        }
        if(rubberTicks==38)rubberLive.getFoodData().setFoodLevel(7);
        if(rubberTicks==80) {
            check(!rubberLive.getAbilities().mayfly&&!rubberLive.getAbilities().flying,"Hunger ending Gear removes active flight");
            check(!Rubber.gear(rubberLive)&&rubberLive.getFoodData().getFoodLevel()==6,"Food drain ends Gear at three icons");
            rubberLive.getFoodData().setFoodLevel(20);
            check(Rubber.cast(rubberLive,2,0).contains("recovering"),"Food cannot bypass post-Gear cooldown");
            rubberTarget.discard();FruitClaims.releaseOnDeath(rubberLive);Rubber.clear();
            Shackles.tick();check(!Shackles.rooted(slabNormal)&&Shackles.rooted(slabMaster),"Four-second root releases while mastered root remains");
            var before=slabNormal.position();slabNormal.setDeltaMovement(.4,0,0);slabNormal.travel(net.minecraft.world.phys.Vec3.ZERO);
            check(slabNormal.position().distanceToSqr(before)>0,"Native movement resumes after normal root expires");
        }
        if(rubberTicks==181){
            Shackles.tick();check(!Shackles.rooted(slabMaster),"Nine-second mastered root expires");
            var before=slabMaster.position();slabMaster.setDeltaMovement(.4,0,0);slabMaster.travel(net.minecraft.world.phys.Vec3.ZERO);
            check(slabMaster.position().distanceToSqr(before)>0,"Mastered target resumes movement");
            slabNormal.discard();slabMaster.discard();return true;
        }
        return false;
    }
    private void checkRubber(ServerLevel level) {
        Rubber.clear(); FruitCreepers.clear();
        var look=new net.minecraft.world.phys.Vec3(0,0,1);
        check(Rubber.dodgeMotion(look,1).z>1 && Rubber.dodgeMotion(look,2).z< -1,
            "W/S dodge forward/back relative to view");
        check(Rubber.dodgeMotion(look,4).x>1 && Rubber.dodgeMotion(look,8).x< -1,
            "A/D dodge left/right relative to view");
        check(Math.abs(Rubber.dodgeMotion(look,9).horizontalDistance()-1.2)<.001,
            "Diagonal dodge has same speed, not a diagonal boost");
        check(Rubber.dodgeMotion(look,0).z< -1,"No input defaults to backward dodge");
        Player p=player(level);
        ItemStack fake=new ItemStack(Items.CHORUS_FRUIT);
        fake.set(DataComponents.CUSTOM_NAME,Component.literal("Rubber Fruit"));
        check(!Rubber.consume(p,fake),"Renamed vanilla fruit cannot unlock Rubber");
        ItemStack fruit=Relics.fruit();check(Rubber.consume(p,fruit)&&fruit.isEmpty()&&Rubber.selected(p)&&Progress.selected(p)==null,
            "Fruit-only player unlocks Rubber without bending");
        check(CombatNetwork.selection(p)==4,"Server selection reports newly consumed fruit immediately");
        ItemStack duplicate=Relics.fruit();check(!Rubber.consume(p,duplicate)&&duplicate.getCount()==1,"Duplicate fruit is shareable");
        check(Rubber.progressTier(40,3599)==2&&Rubber.progressTier(39,3600)==2&&Rubber.progressTier(40,3600)==3,
            "Gear 5 requires 40 uses AND a full active hour");
        check(Rubber.cast(p,1,0).contains("locked")&&Rubber.cast(p,2,0).contains("locked"),"Fruit mastery gates dodge and Gear 5");
        Rubber.grant(p,2);check(Rubber.seconds(p)==300&&Rubber.tier(p)==2,"Tier 2 grants five-minute progression only");
        check(Rubber.cast(p,1,0).contains("dodge")&&p.getDeltaMovement().lengthSqr()>0,"Dodge moves player");
        check(Rubber.cast(p,1,0).contains("cooling"),"Dodges share a cooldown");
        Rubber.grant(p,3);p.getFoodData().setFoodLevel(6);
        check(Rubber.cast(p,2,0).contains("Eat"),"Gear needs food above three icons");
        p.getFoodData().setFoodLevel(20);
        check(Rubber.cast(p,2,8).isEmpty()&&!Rubber.gear(p),"Holding C never triggers Gear 5 on release");
        check(Rubber.cast(p,2,0).contains("Gear 5!")&&Rubber.gear(p),"Tier 3 can activate Gear 5");
        check(p.getAbilities().mayfly&&!p.getAbilities().invulnerable&&!p.getAbilities().instabuild,"Gear grants flight without Creative privileges");
        check(Rubber.MAX_GEAR_TICKS==12000&&Rubber.GEAR_COOLDOWN==2400,"Ten-minute cap and two-minute recovery");
        check(Rubber.damage(p,p.damageSources().fall(),10)==0,"Gear 5 prevents fall damage");
        check(Rubber.damage(p,p.damageSources().generic(),10)==0,"Gear 5 prevents ordinary damage");
        check(Rubber.damage(p,p.damageSources().lava(),10)==5,"Lava bypasses Gear 5 at reduced damage");
        check(Rubber.damage(p,p.damageSources().fellOutOfWorld(),10)==10,"Void bypasses Gear 5 protection");
        Player attacker=player(level);attacker.setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND,new ItemStack(Items.DIAMOND_SWORD));
        check(!Rubber.sharp(attacker.damageSources().playerAttack(attacker)),"Unenchanted diamond weapon cannot pierce Gear 5");
        attacker.getMainHandItem().enchant(level.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT)
            .getOrThrow(net.minecraft.world.item.enchantment.Enchantments.SHARPNESS),1);
        check(Rubber.sharp(attacker.damageSources().playerAttack(attacker))&&Rubber.damage(p,attacker.damageSources().playerAttack(attacker),10)==5,
            "Sharpness diamond melee weapon bypasses Gear 5");
        Rubber.fireBending(true);
        check(Rubber.damage(p,attacker.damageSources().playerAttack(attacker),10)==5,"Fire bending direct strike bypasses Gear 5");
        Rubber.fireBending(false);
        Rubber.forget(p);check(!Rubber.gear(p)&&Rubber.cast(p,2,0).contains("recovering"),"Logout ends Gear and preserves recovery");
        check(!p.getAbilities().mayfly&&!p.getAbilities().flying,"Logout removes Gear-granted flight");
        check(Rubber.damage(p,p.damageSources().lava(),10)==15,"Normal Rubber remains vulnerable to heat");
        Progress.consume(p,Relics.heart(Element.EARTH));check(!Rubber.selected(p)&&Rubber.unlocked(p),"Element selection preserves fruit unlock");
        check(CombatNetwork.selection(p)==Element.EARTH.ordinal(),"Server selection reports element switch after fruit");
        Player restored=player(level,p.getUUID());for(String tag:p.entityTags())restored.addTag(tag);
        check(Rubber.unlocked(restored)&&Rubber.tier(restored)==3,"Rubber mastery survives persistent-tag restoration");
        Player challenger=player(level);ItemStack spare=Relics.fruit();
        check(!FruitClaims.available()&&FruitClaims.owns(p)&&!Rubber.consume(challenger,spare)&&spare.getCount()==1,
            "A second player cannot consume the same fruit while owner lives");
        FruitClaims.load(level.getServer());
        check(FruitClaims.owns(p)&&!FruitClaims.available(),"Fruit claim survives world-file reload, including offline owner");
        Creeperborn.config().naturalCreepers=true;Creeperborn.config().fruitChance=1;
        Creeper blocked=EntityTypes.CREEPER.create(level,EntitySpawnReason.COMMAND);FruitCreepers.track(blocked);
        check(!blocked.entityTags().contains("cb_fruitborn_rubber")&&FruitCreepers.fruit(blocked),"Claimed Rubber is suppressed while another unclaimed fruit may spawn");
        FruitCreepers.unload(blocked);
        Mastery.grant(p,Element.EARTH,3);Progress.select(p,Element.EARTH);
        p.damageCooldownTime=0;p.hurtServer(level,p.damageSources().genericKill(),1000);
        check(!p.isAlive(),"Native player death triggered");
        check(FruitClaims.available()&&!Rubber.unlocked(p)&&Rubber.uses(p)==0&&Rubber.seconds(p)==0,
            "Owner death releases fruit and clears fruit mastery");
        check(Progress.unlocked(p,Element.EARTH)&&Mastery.tier(p,Element.EARTH)==3&&Progress.selected(p)==Element.EARTH,
            "Death preserves elemental unlock, mastery and selection");
        check(Rubber.consume(challenger,spare)&&Rubber.tier(challenger)==1&&spare.isEmpty(),
            "New owner can eat after death and starts at tier one");
        FruitClaims.releaseOnDeath(challenger);
        Creeperborn.config().fruitChance=1;Creeperborn.config().naturalCreepers=true;
        Creeper first=EntityTypes.CREEPER.create(level,EntitySpawnReason.COMMAND);first.snapTo(-8,y,-8);FruitCreepers.track(first);
        check(FruitCreepers.fruit(first),"Configured fruit conversion");
        Creeper second=EntityTypes.CREEPER.create(level,EntitySpawnReason.COMMAND);second.snapTo(-6,y,-8);FruitCreepers.track(second);
        check(!FruitCreepers.fruit(second),"Nearby Fruitborn suppresses clusters");
        FruitCreepers.unload(first);FruitCreepers.track(second);check(!FruitCreepers.fruit(second),"Unloading does not reroll checked creepers");
        Creeper night=EntityTypes.CREEPER.create(level,EntitySpawnReason.COMMAND);night.addTag("ns_creeper");FruitCreepers.track(night);
        check(!FruitCreepers.fruit(night),"Night Splitters creepers excluded from fruit conversion");
        Creeperborn.config().fruitChance=.002;Creeperborn.config().naturalCreepers=false;Rubber.clear();FruitCreepers.clear();
    }
    private void checkHarmony(ServerLevel level) {
        Creeperborn.ABILITIES.clear();
        Player basic = player(level), master = player(level);
        for (Element e : Element.values()) { Mastery.grant(basic,e,3); Mastery.grant(master,e,3); }
        Mastery.grant(basic,Element.AIR,2);
        check(!Mastery.harmony(basic) && Mastery.harmony(master), "Harmony requires tier three in all four elements");
        check(Mastery.multiplier(basic) == 1 && Mastery.multiplier(master) == 1.15, "Harmony is a single fifteen-percent bonus");
        basic.addItem(new ItemStack(Items.WATER_BUCKET)); master.addItem(new ItemStack(Items.WATER_BUCKET));
        Zombie normalTarget = EntityTypes.ZOMBIE.create(level, EntitySpawnReason.COMMAND);
        Zombie boostedTarget = EntityTypes.ZOMBIE.create(level, EntitySpawnReason.COMMAND);
        for (Zombie target : new Zombie[]{normalTarget,boostedTarget}) {
            target.snapTo(.5,y,3.5); target.setNoAi(true);
            target.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ARMOR).setBaseValue(0);
            level.addFreshEntity(target);
        }
        Abilities.strike(level,basic,normalTarget,Element.EARTH,8,.35);
        Abilities.strike(level,master,boostedTarget,Element.EARTH,8,.35);
        check(Math.abs(normalTarget.getHealth()-12) < .001 && Math.abs(boostedTarget.getHealth()-10.8) < .001,
            "Harmony increases actual ability damage by fifteen percent");
        check(normalTarget.getDeltaMovement().equals(boostedTarget.getDeltaMovement()), "Harmony leaves strike knockback unchanged");
        normalTarget.discard(); boostedTarget.discard();
        for (Player caster : new Player[]{basic,master}) {
            Progress.select(caster,Element.WATER); Creeperborn.ABILITIES.move(caster,2);
            caster.setHealth(10); Creeperborn.ABILITIES.cast(caster);
        }
        check(Math.abs(basic.getHealth()-14) < .001 && Math.abs(master.getHealth()-14.6) < .001,
            "Harmony increases real healing from four to 4.6 health");
        for (Player caster : new Player[]{basic,master}) {
            Creeperborn.ABILITIES.move(caster,1); Creeperborn.ABILITIES.cast(caster);
        }
        check(basic.getEffect(MobEffects.RESISTANCE).getDuration() == 80
            && master.getEffect(MobEffects.RESISTANCE).getDuration() == 92, "Tidal Guard duration gets fifteen percent");
        check(Abilities.elementRadius(basic,Element.WATER,5)==10&&Abilities.elementRadius(master,Element.WATER,5)==12.5,"Mastered tidal diameter is twenty blocks, Harmony twenty-five");
        check(Abilities.elementRadius(basic,Element.FIRE,4)==10&&Abilities.elementRadius(master,Element.FIRE,4)==12.5,"Mastered flame ring shares the expanded damage radius");
        for (Player caster : new Player[]{basic,master}) {
            Progress.select(caster,Element.EARTH); Creeperborn.ABILITIES.move(caster,1); Creeperborn.ABILITIES.cast(caster);
        }
        check(Creeperborn.ABILITIES.rocks.guardTicks(basic) == 100 && Creeperborn.ABILITIES.rocks.guardTicks(master) == 160,
            "Mastered brick guard lasts eight seconds at full environmental strength");
        for (Player caster : new Player[]{basic,master}) {
            Progress.select(caster,Element.FIRE); Creeperborn.ABILITIES.move(caster,2); Creeperborn.ABILITIES.cast(caster);
            int expected = (int)Math.round(160 * EnvironmentPower.strength(caster,Element.FIRE) * Mastery.multiplier(caster));
            check(caster.getEffect(MobEffects.FIRE_RESISTANCE).getDuration() == expected,
                "Harmony multiplies environmental strength rather than removing weaknesses");
            Progress.select(caster,Element.AIR); Creeperborn.ABILITIES.move(caster,1); Creeperborn.ABILITIES.cast(caster);
            check(Creeperborn.ABILITIES.cast(caster).equals("Air Step!"), "Harmony keeps Air Step unlimited");
        }
        check(basic.getDeltaMovement().equals(master.getDeltaMovement()), "Harmony leaves Air Step movement unchanged");
        for (boolean boosted : new boolean[]{false,true}) {
            Player fireShield = player(level);
            for (Element e : Element.values()) Mastery.grant(fireShield,e,3);
            if (!boosted) Mastery.grant(fireShield,Element.AIR,2);
            Progress.select(fireShield,Element.FIRE); Creeperborn.ABILITIES.move(fireShield,2); Creeperborn.ABILITIES.cast(fireShield);
            int expectedResistance = (int)Math.round(160 * EnvironmentPower.strength(fireShield,Element.FIRE) * Mastery.multiplier(fireShield));
            check(fireShield.getEffect(MobEffects.RESISTANCE).getDuration() == expectedResistance,
                "Heat Shield physical protection receives the same Harmony duration bonus");
        }
        Mastery.grant(master,Element.AIR,2);
        check(!Mastery.harmony(master) && Mastery.multiplier(master) == 1, "Dropping a mastery tier removes Harmony immediately");
        Creeperborn.ABILITIES.clear();
    }
    private static int count(Player p, String kind) {
        int count = 0;
        for (int i = 0; i < p.getInventory().getContainerSize(); i++) {
            ItemStack item = p.getInventory().getItem(i); if (Relics.kind(item).equals(kind)) count += item.getCount();
        }
        return count;
    }
    private static void check(boolean ok, String message) { if (!ok) throw new AssertionError(message); }
}





