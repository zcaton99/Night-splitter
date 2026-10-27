package com.sibcraft.creeperborn;

import com.mojang.authlib.GameProfile;
import java.util.UUID;
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
        }catch(com.mojang.brigadier.exceptions.CommandSyntaxException error){throw new AssertionError(error);}
    }


    private void clock(ServerLevel level,long time){((net.minecraft.world.level.storage.ServerLevelData)level.getLevelData()).setGameTime(time);}
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
        clock(level,level.getGameTime()+601);Phoenix.tickPlayer(p);
        check(!Phoenix.wing(p)&&!p.getAbilities().mayfly&&!p.getAbilities().flying,"Thirty-second flight expires and restores permissions");
        check(Phoenix.cast(p,1,0).contains("recovering"),"Wing Flight has five-second recovery");
        Phoenix.grant(p,3);p.getFoodData().setFoodLevel(20);
        check(Phoenix.cast(p,1,8).startsWith("Phoenix Form!")&&Phoenix.form(p),"Hold X activates full Phoenix form at tier three");
        check(Phoenix.damage(p,p.damageSources().generic(),10)==5,"Normal form reduces ordinary damage by half");
        p.damageCooldownTime=0;p.setHealth(20);p.hurtServer(level,p.damageSources().generic(),4);
        check(Math.abs(p.getHealth()-18)<.001,"Native damage path applies normal-form mitigation");
        check(Phoenix.immune(p,p.damageSources().lava())&&Phoenix.immune(p,p.damageSources().fall()),"Phoenix grants fire and fall immunity without elemental mastery");
        check(!Phoenix.revive(p,p.damageSources().fellOutOfWorld())&&!Phoenix.spent(p),"Void bypasses revival without consuming it");
        p.setHealth(10);clock(level,(level.getGameTime()/40+1)*40);Phoenix.tickPlayer(p);
        check(Math.abs(p.getHealth()-11)<.001,"Phoenix Form passively heals half a heart every two seconds");
        Villager near=EntityTypes.VILLAGER.create(level,EntitySpawnReason.EVENT),far=EntityTypes.VILLAGER.create(level,EntitySpawnReason.EVENT);
        near.snapTo(-12.5,y+20,29);far.snapTo(-12.5,y+20,32);near.setNoAi(true);far.setNoAi(true);near.setHealth(10);far.setHealth(10);level.addFreshEntity(near);level.addFreshEntity(far);
        p.setHealth(10);Phoenix.press(p,2);clock(level,level.getGameTime()+8);Phoenix.heartbeat(p);Phoenix.tickPlayer(p);
        clock(level,(level.getGameTime()/40+1)*40);Phoenix.heartbeat(p);Phoenix.tickPlayer(p);
        check(Phoenix.healing(p)&&Math.abs(p.getHealth()-12)<.001,"Channel healing replaces rather than stacks passive self healing");
        check(Math.abs(near.getHealth()-12)<.001&&far.getHealth()==10,"Healing Flames reaches ten-block radius only");
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
        Mastery.grant(p,Element.EARTH,2);p.damageCooldownTime=0;
        p.hurtServer(level,p.damageSources().generic(),100);
        check(p.isAlive()&&Math.abs(p.getHealth()-10)<.001&&Phoenix.spent(p)&&Phoenix.unlocked(p),"Native fatal hit triggers one resurrection at half health without losing fruit");
        check(!Phoenix.form(p)&&!p.getAbilities().mayfly&&Phoenix.recovery(p)==300&&Phoenix.strength(p)==.5,"Rebirth ends form and starts five active minutes at half strength");
        check(Phoenix.immune(p,p.damageSources().generic()),"Rebirth grants three-second protection");
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
    private void run(ServerLevel level) {
        try { Class.forName("net.minecraft.server.network.ServerGamePacketListenerImpl"); }
        catch (ClassNotFoundException error) { throw new AssertionError(error); }
        y = level.getMaxY() - 40;
        checkDoorFruit(level);
        checkDangerousDoors(level);
        checkTridentCuring(level);
        checkExplicitFruitCommands(level);
        checkPhoenix(level);
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
            if (slot == 0 || (slot == 2 && e == Element.AIR) || (slot == 1 && e == Element.FIRE))
                check(target.getHealth() < 20, "Offensive move damages hostile: " + e.moves[slot]);
            if (slot == 1 && e == Element.WATER)
                check(caster.hasEffect(MobEffects.RESISTANCE), "Defensive move grants resistance");
            if (slot == 1 && e == Element.EARTH) check(Creeperborn.ABILITIES.rocks.guarded(caster), "Frontal rock guard created");
            if (slot == 2 && e == Element.EARTH) check(Shackles.rooted(target)&&Shackles.remaining(target)==80, "Slab Shackles roots hostile for four seconds");
            if (slot == 1 && e == Element.WATER) {
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
            for (int tick = 0; tick < 30; tick++) { target.travel(net.minecraft.world.phys.Vec3.ZERO); Creeperborn.ABILITIES.tick(); }
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
        for (Zombie target : surrounding) {
            check(Creeperborn.ABILITIES.pushRemaining(target) == 10, "Tidal Guard covers front, rear and both sides: pos="
                + target.position() + ", remaining=" + Creeperborn.ABILITIES.pushRemaining(target) + ", visible=" + tidal.hasLineOfSight(target)); target.discard();
        }
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
            target.snapTo(-12,y+5,6);target.setNoAi(true);target.setNoGravity(true);level.addFreshEntity(target);
            Creeperborn.ABILITIES.cast(p);
            check((target.getHealth()<target.getMaxHealth())==mastered,"Only all-four mastery reaches a target twenty blocks away with elemental Z");target.discard();
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




