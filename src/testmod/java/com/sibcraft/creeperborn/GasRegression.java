package com.sibcraft.creeperborn;
import java.util.*;
import com.mojang.authlib.GameProfile;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.effect.*;
import net.minecraft.world.phys.Vec3;
public final class GasRegression {
 private static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);System.out.println("GAS_CHECK "+message);}
 private static Player player(ServerLevel l){return new Player(l,new GameProfile(UUID.randomUUID(),"GasTest")){public GameType gameMode(){return GameType.SURVIVAL;}};}
 private static void tick(ServerLevel l,int n,Player p){for(int i=0;i<n;i++){((net.minecraft.world.level.storage.ServerLevelData)l.getLevelData()).setGameTime(l.getGameTime()+1);GasPowers.tickPlayer(p);GasPowers.effects();}}
 static void run(ServerLevel l,double y){
  GasPowers.clear();Player p=player(l),victim=player(l),other=player(l);int floor=(int)y+11;
  for(int x=-14;x<=0;x++)for(int z=0;z<=29;z++){l.setBlock(new BlockPos(x,floor,z),Blocks.STONE.defaultBlockState(),3);for(int h=1;h<=4;h++)l.setBlock(new BlockPos(x,floor+h,z),Blocks.AIR.defaultBlockState(),3);}
  p.snapTo(-8,floor+1,2);victim.snapTo(-8,floor+1,8);other.snapTo(-3,floor+1,2);p.setYRot(0);p.setXRot(0);l.addFreshEntity(p);l.addFreshEntity(victim);l.addFreshEntity(other);
  check(GasPowers.consume(p,Relics.gasFruit())&&!GasPowers.consume(other,Relics.gasFruit()),"Gas has one persistent owner");
  check(CombatNetwork.selection(p)==28&&WaterRules.vulnerable(p),"Gas routes through fruit controls and inherits water weakness");
  check(GasPowers.cast(p,1,0).contains("tier 2")&&GasPowers.cast(p,2,20).contains("mastery"),"Higher tier moves are gated");
  GasPowers.press(p,0);tick(l,1,p);GasPowers.release(p,0);check(GasPowers.shotCount()==1,"Tap Z fires a server-owned shot");tick(l,8,p);check(victim.getHealth()<20&&victim.hasEffect(MobEffects.POISON)&&GasPowers.cloudCount(p)>0,"Gas Shot hits an actual entity, poisons it and leaves gas");
  GasPowers.forget(p);tick(l,200,p);p.getFoodData().setFoodLevel(20);victim.setHealth(20);victim.damageCooldownTime=0;
  check(GasPowers.cast(p,0,20).contains("Choking Cloud"),"Hold Z creates choking clouds");tick(l,21,p);check(victim.hasEffect(MobEffects.NAUSEA)&&victim.hasEffect(MobEffects.SLOWNESS),"Concentrated gas applies nausea and slowness to a player");
  victim.removeEffect(MobEffects.NAUSEA);tick(l,20,p);check(!victim.hasEffect(MobEffects.NAUSEA),"Overlapping gas does not refresh nausea during its pulse cooldown");
  p.addEffect(new MobEffectInstance(MobEffects.POISON,200,1));GasPowers.tickPlayer(p);check(!p.hasEffect(MobEffects.POISON),"Active Gas removes poison");GasPowers.forget(p);tick(l,300,p);GasPowers.grant(p,2);p.getFoodData().setFoodLevel(20);
  for(int h=1;h<=3;h++)l.setBlock(new BlockPos(-8,floor+h,11),Blocks.STONE.defaultBlockState(),3);Vec3 from=p.position();check(GasPowers.cast(p,1,0).contains("Dash")&&p.getZ()>from.z&&p.getZ()<11,"Gas Dash moves and respects a solid wall");
  p.getFoodData().setFoodLevel(20);check(GasPowers.cast(p,1,20).contains("Poison Veil")&&GasPowers.veil(p),"Hold X activates Poison Veil");check(Math.abs(GasPowers.damage(p,victim.damageSources().playerAttack(victim),10)-8)<.01,"Poison Veil reduces physical damage");
  GasPowers.forget(p);tick(l,400,p);GasPowers.grant(p,3);p.getFoodData().setFoodLevel(20);p.snapTo(-8,floor+1,2);p.setYRot(0);victim.snapTo(-8,floor+1,6);victim.setHealth(20);victim.damageCooldownTime=0;
  check(GasPowers.cast(p,0,20).contains("Choking Cloud"),"Mastered choking cloud casts");var block=new BlockPos(-8,floor,6);var before=l.getBlockState(block);check(GasPowers.cast(p,2,0).contains("Combustion")&&GasPowers.cloudCount(p)>0,"C tap warns before consuming clouds");tick(l,21,p);check(GasPowers.cloudCount(p)==0&&l.getBlockState(block).equals(before),"Combustion consumes clouds and preserves terrain");
  p.getFoodData().setFoodLevel(20);check(GasPowers.cast(p,2,20).contains("Gas Body")&&GasPowers.body(p)&&p.getAbilities().mayfly&&p.getAbilities().flying,"Hold C starts Gas Body with native flight");
  check(Math.abs(GasPowers.damage(p,other.damageSources().playerAttack(other),8)-2)<.01,"Gas Body resists ordinary melee");for(Element e:Element.values())Mastery.grant(other,e,3);check(GasPowers.damage(p,other.damageSources().playerAttack(other),8)==8,"Harmony attacker pierces Gas Body resistance");check(GasPowers.damage(p,p.damageSources().drown(),4)==4,"Gas Body does not block drowning");
  check(HolyBag.consume(p,Relics.holyBag()),"Gas owner can unlock Holy Bag");check(Rubber.consume(p,Relics.fruit()),"Holy Bag can store Gas beside Rubber");HolyBag.select(p,"gas");GasPowers.grant(p,3);tick(l,3100,p);p.getFoodData().setFoodLevel(20);check(GasPowers.cast(p,2,20).contains("Gas Body"),"Stored Gas mastery and cooldowns restore");HolyBag.select(p,"rubber");check(GasPowers.body(p)&&FormCarry.carries(p,"gas"),"G preview carries the gas form");FormCarry.useSelected(p);check(!GasPowers.body(p)&&!p.getAbilities().mayfly&&GasPowers.cloudCount(p)==0,"Using another fruit removes gas form, clouds and granted flight");
  HolyBag.select(p,"gas");FruitClaims.load(l.getServer());check(p.getUUID().equals(FruitClaims.owner("gas")),"Gas claim survives ledger reload");PlayerReset.apply(p);check(!GasPowers.unlocked(p)&&FruitClaims.owner("gas")==null&&!p.getAbilities().mayfly,"Testing reset releases Gas and restores flight");
  p.discard();victim.discard();other.discard();GasPowers.clear();System.out.println("CREEPERBORN_GAS_REGRESSION_PASSED");
 }
}
