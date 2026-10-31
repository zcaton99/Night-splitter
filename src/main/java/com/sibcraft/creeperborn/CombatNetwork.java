package com.sibcraft.creeperborn;

import java.util.*;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.world.entity.player.Player;

public final class CombatNetwork {
    private record Window(long second, int used) {}
    private static final Map<UUID, Window> limits = new HashMap<>();
    private static final Map<UUID,Integer> sentSelection=new HashMap<>();
    private CombatNetwork() {}
    public static boolean allow(Player p) {
        long second = p.level().getGameTime() / 20;
        Window w = limits.get(p.getUUID());
        if (w == null || w.second() != second) { limits.put(p.getUUID(), new Window(second, 1)); return true; }
        if (w.used() >= 20) return false;
        limits.put(p.getUUID(), new Window(second, w.used() + 1)); return true;
    }
    public static void clear() { limits.clear();sentSelection.clear(); }
    public static int selection(Player p){
        Element e=Progress.selected(p);return ZoanPowers.current(p).equals("fox")?25:ZoanPowers.current(p).equals("axolotl")?26:CelestialPowers.selected(p,"flame")?23:CelestialPowers.selected(p,"buddha")?24:CelestialPowers.selected(p,"glint")?21:CelestialPowers.selected(p,"straw")?22:NaturePowers.selected(p,"tree")?19:NaturePowers.selected(p,"petal")?20:Azure.selected(p)?18:ClimatePowers.selected(p,"ice")?16:ClimatePowers.selected(p,"magma")?17:PairPowers.selected(p,"love")?14:PairPowers.selected(p,"sand")?15:Tremor.selected(p)?13:Paw.selected(p)?12:StringFruit.selected(p)?11:Gravity.selected(p)?10:Op.selected(p)?9:Dark.selected(p)?8:Lightning.selected(p)?7:Phoenix.selected(p)?6:DoorFruit.selected(p)?5:Rubber.selected(p)?4:e==null?-1:e.ordinal();
    }
    public static void forget(Player p){limits.remove(p.getUUID());sentSelection.remove(p.getUUID());}
    public static void tick(net.minecraft.server.MinecraftServer server){
        for(var p:server.getPlayerList().getPlayers()){
            int value=selection(p);
            if(!Objects.equals(sentSelection.get(p.getUUID()),value)&&ServerPlayNetworking.canSend(p,ControlPayload.TYPE)){
                ServerPlayNetworking.send(p,new ControlPayload(3,value));sentSelection.put(p.getUUID(),value);
            }
        }
    }
    /** Move presses always use server selection, even before a client selection reply arrives. */
    public static String press(Player p,int packed){
        int move=packed&3;
        if(CelestialPowers.rooted(p)||NaturePowers.rooted(p)||StringFruit.controlled(p)||PairPowers.rooted(p)||ClimatePowers.rooted(p))return "You are restrained.";
        if(move>2)return "";
        if(Dark.suppressed(p)&&selection(p)>=4)return "Fruit powers suppressed by darkness.";
        if(!ZoanPowers.current(p).isEmpty()){ZoanPowers.press(p,move);return "";}
        if(!CelestialPowers.current(p).isEmpty()){CelestialPowers.press(p,move);return "";}
        if(!NaturePowers.current(p).isEmpty()){NaturePowers.press(p,move);return "";}
        if(Azure.selected(p)){Azure.press(p,move);return "";}
        if(!ClimatePowers.current(p).isEmpty()){ClimatePowers.press(p,move);return "";}
        if(!PairPowers.current(p).isEmpty()){PairPowers.press(p,move,packed>>>2);return "";}
        if(Tremor.selected(p)){Tremor.press(p,move);return "";}
        if(Paw.selected(p)){Paw.press(p,move);return "";}
        if(StringFruit.selected(p)){StringFruit.press(p,move);return "";}
        if(Gravity.selected(p)){Gravity.press(p,move);return "";}
        if(Op.selected(p)){Op.press(p,move);return "";}
        if(Dark.selected(p)){Dark.press(p,move);return "";}
        if(DoorSpace.active(p)||DoorFruit.selected(p))return DoorFruit.cast(p,move);
        if(Lightning.selected(p)){Lightning.press(p,move);return "";}
        if(Phoenix.selected(p)){Phoenix.press(p,move);return "";}
        if(Rubber.selected(p)){Rubber.press(p,move,packed>>>2);return "";}
        if(Progress.selected(p)==null)return "Select an unlocked element with R or Rubber with G.";
        Creeperborn.ABILITIES.move(p,move);return Creeperborn.ABILITIES.cast(p);
    }
    public static void init() {
        PayloadTypeRegistry.clientboundPlay().register(ZoanPayload.TYPE,ZoanPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(CelestialPayload.TYPE,CelestialPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(NatureVisualPayload.TYPE,NatureVisualPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(AzureFlightPayload.TYPE,AzureFlightPayload.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(AzureFlightPayload.TYPE,(payload,context)->Azure.flightInput(context.player(),payload));
        PayloadTypeRegistry.serverboundPlay().register(StringControlPayload.TYPE,StringControlPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(StringControlPayload.TYPE,StringControlPayload.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(StringControlPayload.TYPE,(payload,context)->{if(payload.valid()&&context.player().isAlive())StringFruit.input(context.player(),payload);});
        PayloadTypeRegistry.serverboundPlay().register(CastPayload.TYPE, CastPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(ControlPayload.TYPE, ControlPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(ControlPayload.TYPE, ControlPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(WaterStatePayload.TYPE,WaterStatePayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(AzureVisualPayload.TYPE,AzureVisualPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(PhoenixVisualPayload.TYPE,PhoenixVisualPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(LightningVisualPayload.TYPE,LightningVisualPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(DarkVisualPayload.TYPE,DarkVisualPayload.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(ControlPayload.TYPE, (payload, context) -> {
            Player p = context.player();
            if(CelestialPowers.rooted(p)||StringFruit.controlled(p)||PairPowers.rooted(p))return;
            if (!payload.validRequest() || !p.isAlive() || p.isSpectator()&&!DoorSpace.active(p)&&!NaturePowers.phased(p) || !(payload.action()==10&&(CelestialPowers.flying(p)||NaturePowers.flying(p))||payload.action()==12&&CelestialPowers.customMelee(p))&&!allow(p)) return;
            if(NaturePowers.phased(p)){if(payload.action()==5&&(payload.value()&3)==2)Creeperborn.say(p,NaturePowers.exitPhase(p,false)?"Reappeared!":"Find an open place to emerge.");return;}
            if(payload.action()==12){CelestialPowers.basicButtons(p,payload.value());return;}
            if(payload.action()==10){ZoanPowers.input(p,payload.value());CelestialPowers.input(p,payload.value());NaturePowers.input(p,payload.value());ClimatePowers.input(p,payload.value());Azure.input(p,payload.value());return;}
            if(ClimatePowers.rooted(p))return;
            if(DoorSpace.active(p)){
                if(payload.action()==5){String result=DoorSpace.exit(p);Creeperborn.say(p,result);}
                return;
            }
            if(payload.action()==9){Hearts.heartbeat(p,payload.value()==1);return;}
            Element selected = Progress.selected(p);
            if (payload.action() == 1) {
                selected = Progress.cycle(p, payload.value());
                Creeperborn.say(p, selected == null ? "Consume an elemental heart first."
                    : selected.title + " — Z/X/C: " + String.join(" / ", selected.moves));
            }
            if (payload.action() == 2 && ZoanPowers.current(p).isEmpty()&&CelestialPowers.current(p).isEmpty()&&NaturePowers.current(p).isEmpty()&&!Azure.selected(p)&& !Rubber.selected(p)&&!DoorFruit.selected(p)&&!Phoenix.selected(p)&&!Lightning.selected(p)&&!Dark.selected(p)&&!Op.selected(p)&&!Gravity.selected(p)&&!StringFruit.selected(p)&&!Paw.selected(p)&&!Tremor.selected(p)&&PairPowers.current(p).isEmpty()&&ClimatePowers.current(p).isEmpty()) {
                if (selected == null) Creeperborn.say(p, "Consume an elemental heart first.");
                else { Creeperborn.ABILITIES.move(p, payload.value()); Creeperborn.say(p, Creeperborn.ABILITIES.cast(p)); }
            }
            if (payload.action() == 4) {
                ZoanPowers.cancelInput(p);CelestialPowers.cancelInput(p);NaturePowers.cancelInput(p);Rubber.cancelInput(p);Phoenix.cancelInput(p);Lightning.cancelInput(p);Dark.cancelInput(p);Op.cancelInput(p);Gravity.cancelInput(p);StringFruit.cancelInput(p);Paw.cancelInput(p);Tremor.cancelInput(p);PairPowers.cancelInput(p);ClimatePowers.cancelInput(p);Azure.cancelInput(p);
                if(Dark.unlocked(p)){Creeperborn.say(p,Dark.cycle(p));}
                else if(ZoanPowers.owns(p)){p.addTag("cb_mode_fruit");Creeperborn.say(p,ZoanPowers.status(p,ZoanPowers.owned(p)));}
                else if(CelestialPowers.unlocked(p,"flame")||CelestialPowers.unlocked(p,"buddha")){String f=CelestialPowers.unlocked(p,"flame")?"flame":"buddha";p.addTag("cb_mode_fruit");Creeperborn.say(p,CelestialPowers.status(p,f));}
                else if(CelestialPowers.unlocked(p,"glint")||CelestialPowers.unlocked(p,"straw")){String f=CelestialPowers.unlocked(p,"glint")?"glint":"straw";p.addTag("cb_mode_fruit");Creeperborn.say(p,CelestialPowers.status(p,f));}
                else if(NaturePowers.unlocked(p,"tree")||NaturePowers.unlocked(p,"petal")){String f=NaturePowers.unlocked(p,"tree")?"tree":"petal";p.addTag("cb_mode_fruit");Creeperborn.say(p,NaturePowers.status(p,f));}
                else if(Azure.unlocked(p)){p.addTag("cb_mode_fruit");Creeperborn.say(p,Azure.status(p));}
                else if(ClimatePowers.unlocked(p,"ice")){p.addTag("cb_mode_fruit");Creeperborn.say(p,ClimatePowers.status(p,"ice"));}
                else if(ClimatePowers.unlocked(p,"magma")){p.addTag("cb_mode_fruit");Creeperborn.say(p,ClimatePowers.status(p,"magma"));}
                else if(PairPowers.unlocked(p,"love")){p.addTag("cb_mode_fruit");Creeperborn.say(p,PairPowers.status(p,"love"));}
                else if(PairPowers.unlocked(p,"sand")){p.addTag("cb_mode_fruit");Creeperborn.say(p,PairPowers.status(p,"sand"));}
                else if(Tremor.unlocked(p)){p.addTag("cb_mode_fruit");Creeperborn.say(p,Tremor.status(p));}
                else if(Paw.unlocked(p)){p.addTag("cb_mode_fruit");Creeperborn.say(p,Paw.status(p));}
                else if(StringFruit.unlocked(p)){p.addTag("cb_mode_fruit");Creeperborn.say(p,"String — Z Slash / hold Whip | X Glide / hold Parasite | C Knight / hold Birdcage");}
                else if(Gravity.unlocked(p)){p.addTag("cb_mode_fruit");Creeperborn.say(p,"Gravity — Z Press / hold Wave | X Levitate / hold Rock Ride | C Orbit Guard / hold 3s Meteor");}
                else if(Op.unlocked(p)){p.addTag("cb_mode_fruit");Creeperborn.say(p,"Op-Op — Z Gamma Knife / hold ROOM | X Shock / hold Heart | C Shambles / hold Takt");}
                else if(Lightning.unlocked(p)){p.addTag("cb_mode_fruit");Creeperborn.say(p,"Lightning — Z: Bolt / hold 8s Judgment | X: Flash / hold Rush | C: Storm Form");}
                else if(Phoenix.unlocked(p)){p.addTag("cb_mode_fruit");Creeperborn.say(p,"Phoenix — Z: Talon / hold Dive | X: Wings / hold Form | C: Healing Circle / hold Ally Blessing");}
                else if(Rubber.unlocked(p)) { p.addTag("cb_mode_fruit"); Creeperborn.say(p,"Rubber — Z: Stretch Punch | X: Directional dodge / hold Grapple | C: Gear 5 / hold Guard"); }
                else if(DoorFruit.unlocked(p)){p.addTag("cb_mode_fruit");Creeperborn.say(p,"Door-Door — Z: Passage | X: Rescue | C: Shared Passage");}
                else Creeperborn.say(p,"Consume a Devil Fruit first.");
            }
            if(payload.action()==5){String result=press(p,payload.value());if(!result.isEmpty())Creeperborn.say(p,result);}
            if(payload.action()==6){if(!ZoanPowers.current(p).isEmpty())ZoanPowers.release(p,payload.value());else if(!CelestialPowers.current(p).isEmpty())CelestialPowers.release(p,payload.value());else if(!NaturePowers.current(p).isEmpty())NaturePowers.release(p,payload.value());else if(Azure.selected(p))Azure.release(p,payload.value());else if(!ClimatePowers.current(p).isEmpty())ClimatePowers.release(p,payload.value());else if(!PairPowers.current(p).isEmpty())PairPowers.release(p,payload.value());else if(Tremor.selected(p))Tremor.release(p,payload.value());else if(Paw.selected(p))Paw.release(p,payload.value());else if(StringFruit.selected(p))StringFruit.release(p,payload.value());else if(Gravity.selected(p))Gravity.release(p,payload.value());else if(Op.selected(p))Op.release(p,payload.value());else if(Dark.selected(p))Dark.release(p,payload.value());else if(Lightning.selected(p))Lightning.release(p,payload.value());else if(Phoenix.selected(p))Phoenix.release(p,payload.value());else Rubber.release(p,payload.value());}
            if(payload.action()==7){ZoanPowers.heartbeat(p);CelestialPowers.heartbeat(p);NaturePowers.heartbeat(p);Rubber.heartbeat(p);Phoenix.heartbeat(p);Lightning.heartbeat(p);Dark.heartbeat(p);Op.heartbeat(p);Gravity.heartbeat(p);StringFruit.heartbeat(p);Paw.heartbeat(p);Tremor.heartbeat(p);PairPowers.heartbeat(p);ClimatePowers.heartbeat(p);Azure.heartbeat(p);}
            if(payload.action()==8){ZoanPowers.cancelInput(p);CelestialPowers.cancelInput(p);NaturePowers.cancelInput(p);Rubber.cancelInput(p);Phoenix.cancelInput(p);Lightning.cancelInput(p);Dark.cancelInput(p);Op.cancelInput(p);Gravity.cancelInput(p);StringFruit.cancelInput(p);Paw.cancelInput(p);Tremor.cancelInput(p);PairPowers.cancelInput(p);ClimatePowers.cancelInput(p);Azure.cancelInput(p);}
            ServerPlayNetworking.send(context.player(), new ControlPayload(3, selection(p)));
        });
        ServerPlayNetworking.registerGlobalReceiver(CastPayload.TYPE, (payload, context) -> {
            Player p = context.player();
            if(CelestialPowers.rooted(p)||StringFruit.controlled(p)||StringFruit.controlling(p)||NaturePowers.rooted(p)||PairPowers.rooted(p)||ClimatePowers.rooted(p))return;
            if (!payload.valid() || !p.isAlive() || p.isSpectator() || !allow(p)) return;
            Element e = Element.values()[payload.element()];
            if (!Progress.select(p,e)) { Creeperborn.say(p, "Consume the " + e.title + " heart first."); return; }
            Creeperborn.ABILITIES.move(p, payload.move()); Creeperborn.say(p, Creeperborn.ABILITIES.cast(p));
        });
    }
}



