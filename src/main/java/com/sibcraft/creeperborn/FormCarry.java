package com.sibcraft.creeperborn;
import java.util.*;
import net.minecraft.world.entity.player.Player;

/** One carried transformation is independent of the selected attack kit. Timers remain live. */
public final class FormCarry {
    private static final Map<UUID,String> held=new HashMap<>();
    public static String held(Player p){return held.getOrDefault(p.getUUID(),"");}
    public static boolean carries(Player p,String fruit){return held(p).equals(fruit);}
    private static String attacks(Player p){return HolyBag.unlocked(p)?HolyBag.active(p):Dark.unlocked(p)?Dark.choice(p):java.util.Objects.requireNonNullElse(Dark.ownedFruit(p),"");}
    private static List<String> bodies(Player p){
        List<String> forms=new ArrayList<>();
        String zoan=ZoanPowers.formFruit(p);if(!zoan.isEmpty())forms.add(zoan);
        if(Azure.form(p)>0)forms.add("azure");
        String nature=NaturePowers.formFruit(p);if(!nature.isEmpty())forms.add(nature);
        String celestial=CelestialPowers.formFruit(p);if(!celestial.isEmpty())forms.add(celestial);
        if(Rubber.gear(p))forms.add("rubber");if(Phoenix.form(p)||Phoenix.wing(p))forms.add("phoenix");
        if(Lightning.storm(p))forms.add("lightning");if(ClimatePowers.cloaked(p))forms.add("magma");if(Dark.mantle(p))forms.add("dark");
        return forms;
    }
    private static void stop(Player p,String f){switch(f){
        case "fox","axolotl"->{if(ZoanPowers.formFruit(p).equals(f))ZoanPowers.forget(p);}
        case "tree","petal"->{if(NaturePowers.formFruit(p).equals(f))NaturePowers.forget(p);}
        case "glint","straw","flame","buddha"->{if(CelestialPowers.formFruit(p).equals(f))CelestialPowers.forget(p);}
        case "azure"->Azure.forget(p);case "rubber"->Rubber.forget(p);case "phoenix"->Phoenix.forget(p);
        case "lightning"->Lightning.forget(p);case "magma"->ClimatePowers.stopOwned(p);case "dark"->Dark.endMantle(p);
    }}
    public static void prepare(Player p){
        List<String> forms=bodies(p);String pin=held(p),active=attacks(p);
        if(forms.contains(active))pin=active;else if(!forms.contains(pin))pin=forms.isEmpty()?"":forms.getFirst();
        if(pin.isEmpty()){held.remove(p.getUUID());Dark.stopForms(p);return;}
        held.put(p.getUUID(),pin);for(String f:forms)if(!f.equals(pin))stop(p,f);
        TimePowers.cancelInput(p);ZoanPowers.cancelInput(p);CelestialPowers.cancelInput(p);NaturePowers.cancelInput(p);LuffyPowers.cancelInput(p);Rubber.cancelInput(p);Phoenix.cancelInput(p);Lightning.cancelInput(p);Dark.cancelInput(p);Azure.cancelInput(p);ClimatePowers.cancelInput(p);PairPowers.cancelInput(p);Paw.cancelInput(p);Tremor.cancelInput(p);StringFruit.cancelInput(p);Gravity.cancelInput(p);Op.cancelInput(p);
    }
    public static void tick(Player p){
        String pin=held(p);if(pin.isEmpty())return;
        List<String> forms=bodies(p);String active=attacks(p);
        if(!forms.contains(pin)||!active.equals(pin)&&forms.contains(active)){
            HolyBag.snapshot(p);if(forms.contains(pin))stop(p,pin);held.remove(p.getUUID());
            if(HolyBag.unlocked(p)&&!pin.equals(active))HolyBag.removeTags(p,pin);
        }
    }
    public static void replacing(Player p,String next){
        String pin=held(p);if(pin.isEmpty()||pin.equals(next))return;
        HolyBag.snapshot(p);stop(p,pin);held.remove(p.getUUID());
        if(HolyBag.unlocked(p)&&!pin.equals(HolyBag.active(p)))HolyBag.removeTags(p,pin);
    }
    public static void cancel(Player p){String pin=held(p);HolyBag.snapshot(p);Dark.stopForms(p);held.remove(p.getUUID());if(HolyBag.unlocked(p)&&!pin.isEmpty()&&!pin.equals(HolyBag.active(p)))HolyBag.removeTags(p,pin);}
    public static void drop(Player p){held.remove(p.getUUID());}
    public static void clear(){held.clear();}
}
