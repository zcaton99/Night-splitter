package com.sibcraft.creeperborn;
import java.util.*;
import com.mojang.authlib.GameProfile;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
final class HotfixRegression {
    private static void check(boolean b,String text){if(!b)throw new AssertionError(text);System.out.println("HOTFIX_CHECK: "+text);}
    static void run(MinecraftServer server){
        ServerLevel l=server.overworld();Player p=new Player(l,new GameProfile(UUID.randomUUID(),"FixTest")){@Override public GameType gameMode(){return GameType.SURVIVAL;}};
        p.snapTo(-8,130.5,24);check(ZoanPowers.consume(p,Relics.zoanFruit("fox"),"fox"),"Fox water fixture consumes fruit");
        l.setBlock(new BlockPos(-8,130,24),Blocks.WATER.defaultBlockState(),3);
        check(!WaterRules.submerged(p)&&WaterRules.suppressed(p),"Surface water suppresses Fox even with eyes above water");
        WaterRules.apply(p);check(p.getDeltaMovement().y<0&&ZoanPowers.flags(p)==0,"Fox sinks and transformation ends when body enters water");
        l.setBlock(new BlockPos(-8,131,24),Blocks.WATER.defaultBlockState(),3);l.setBlock(new BlockPos(-8,132,24),Blocks.WATER.defaultBlockState(),3);
        check(WaterRules.submerged(p)&&!p.canBreatheUnderwater(),"Unmastered Fox has no underwater-breathing exemption");
        p.baseTick();p.setAirSupply(-19);p.damageCooldownTime=0;float beforeDrowning=p.getHealth();p.baseTick();check(p.getHealth()<beforeDrowning,"Unmastered Fox takes real drowning damage");
        for(Element e:Element.values())Mastery.grant(p,e,3);
        check(WaterRules.protectedWater(p)&&!WaterRules.suppressed(p),"All four mastered elements protect Fox at the water surface");
        FruitClaims.releaseOnDeath(p);for(int y=130;y<=132;y++)l.setBlock(new BlockPos(-8,y,24),Blocks.AIR.defaultBlockState(),3);
        ThreeHeadedDragon.begin(l,new Vec3(-8,140,24),1);var f=ThreeHeadedDragon.state();
        var body=(LivingEntity)l.getEntity(UUID.fromString(f.body));check(body.getName().getString().equals("Three-Headed Dragon"),"Dragon damage source has the correct name");
        p.snapTo(-8,140,24);p.setHealth(20);p.damageCooldownTime=0;boolean hurt=p.hurtServer(l,l.damageSources().mobAttack(body),100);
        check(!hurt&&p.getHealth()==20,"Hidden dragon proxies cannot inflict vanilla slime contact damage");
        int visuals=0;for(Entity e:l.getAllEntities())if(e.entityTags().stream().anyMatch(t->t.startsWith("cb_dragon_visual_"))){visuals++;check(!e.shouldBeSaved(),"Dragon visual is not serialized as a frozen duplicate");}
        check(visuals>30,"Full multipart model is present");
        var ghost=EntityTypes.BLOCK_DISPLAY.create(l,EntitySpawnReason.COMMAND);l.addFreshEntity(ghost);ghost.addTag("cb_dragon_visual_legacy");
        var attack=EntityTypes.DRAGON_FIREBALL.create(l,EntitySpawnReason.COMMAND);attack.setOwner(body);attack.addTag("cb_dragon_attack_"+f.id);l.addFreshEntity(attack);
        ThreeHeadedDragon.dismiss(server);check(ghost.isRemoved()&&attack.isRemoved(),"Cancel removes orphaned poses and in-flight attacks");
        for(Entity e:l.getAllEntities())check(e.entityTags().stream().noneMatch(t->t.startsWith("cb_dragon_visual_")||t.startsWith("cb_dragon_part_")||t.startsWith("cb_dragon_attack_")),"No dragon actor survives cancellation");
        var late=EntityTypes.BLOCK_DISPLAY.create(l,EntitySpawnReason.COMMAND);l.addFreshEntity(late);late.addTag("cb_dragon_visual_legacy");ThreeHeadedDragon.tickEntity(late);check(late.isRemoved(),"An old pose loaded after cancel is also removed");
        ThreeHeadedDragon.dismiss(server);check(!ThreeHeadedDragon.active(),"Cancel also repairs already-inactive encounters");
        var bag=Relics.holyBag();check(bag.get(net.minecraft.core.component.DataComponents.ITEM_MODEL).getPath().equals("holy_bag")&&Boolean.TRUE.equals(bag.get(net.minecraft.core.component.DataComponents.ENCHANTMENT_GLINT_OVERRIDE)),"Holy Bag uses its white model and enchanted glow");
        p.snapTo(-8,145,24);check(HolyBag.consume(p,Relics.holyBag()),"Form carry fixture unlocks Holy Bag");
        check(Rubber.consume(p,Relics.fruit()),"Form carry fixture collects Rubber");Rubber.grant(p,3);
        check(ZoanPowers.consume(p,Relics.zoanFruit("fox"),"fox"),"Form carry fixture collects Fox");ZoanPowers.grant(p,"fox",3);int mode=ZoanPowers.mode(p);
        HolyBag.select(p,"rubber");check(Rubber.selected(p)&&ZoanPowers.mode(p)==mode&&mode>0&&FormCarry.carries(p,"fox"),"Switching selected powers preserves the current Fox form");
        Rubber.cast(p,2,0);check(Rubber.gear(p)&&ZoanPowers.mode(p)==0,"Entering Gear 5 replaces the carried Fox form");
        HolyBag.select(p,"fox");check(Rubber.gear(p)&&!Rubber.selected(p),"G can also carry Gear 5 while selecting Fox attacks");
        ZoanPowers.cast(p,"fox",1,20);check(ZoanPowers.mode(p)>0&&!Rubber.gear(p)&&p.getAbilities().flying,"A new Fox cloak replaces Gear 5 and restores its own flight");
        FormCarry.cancel(p);check(ZoanPowers.mode(p)==0&&!Rubber.gear(p),"Explicit form cancellation clears carried transformations");
        for(String fruit:Dark.FRUITS)if(p.getUUID().equals(FruitClaims.owner(fruit)))FruitClaims.setOwner(fruit,null);
        System.out.println("CREEPERBORN_HOTFIX_REGRESSION_PASSED");
    }
}
