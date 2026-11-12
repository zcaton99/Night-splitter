package com.sibcraft.creeperborn;
import net.minecraft.world.entity.player.Player;

public final class FruitHelp {
    private FruitHelp(){}
    public static String describe(Player p){
        String f=HolyBag.unlocked(p)?HolyBag.active(p):Dark.unlocked(p)?Dark.choice(p):Dark.ownedFruit(p);
        if(f==null||f.isEmpty())return "Find and consume a Creeper Fruit first.";
        String help=switch(f){
            case "gas"->GasPowers.status(p);
            case "rubber"->LuffyPowers.status(p);
            case "fox","axolotl"->ZoanPowers.status(p,f);
            case "glint","straw","flame","buddha"->CelestialPowers.status(p,f);
            case "tree","petal"->NaturePowers.status(p,f);
            case "azure"->Azure.status(p);
            case "ice","magma"->ClimatePowers.status(p,f);
            case "love","sand"->PairPowers.status(p,f);
            case "time"->TimePowers.status(p);
            case "paw"->Paw.status(p);case "tremor"->Tremor.status(p);
            case "dark"->Dark.status(p)+" | Z Vortex / hold Grasp | X Mantle / hold Liberation | C Surge / hold Void Collapse";
            case "op"->Op.status(p)+" | Z Gamma Knife / hold ROOM | X Shock / hold Heart | C Shambles / hold Takt";
            case "gravity"->Gravity.status(p)+" | Z Press / hold Wave | X Levitate / hold Rock Ride | C Orbit Guard / hold Meteor";
            case "string"->StringFruit.status(p)+" | Z Slash / hold Whip | X Glide / hold Parasite | C Knight / hold Birdcage";
            case "lightning"->Lightning.status(p)+" | Z Bolt / hold Judgment | X Flash / hold Rush | C Storm Form";
            case "phoenix"->Phoenix.status(p)+" | Z Talon / hold Dive | X Wings / hold Form | C Heal / hold Blessing";
            case "door"->DoorFruit.status(p)+" | Z Passage | X Rescue | C Shared Passage";
            default->f;
        };
        help=help.replace(" | Z ","\nZ ").replace(" | X ","\nX ").replace(" | C ","\nC ");
        return (HolyBag.unlocked(p)?"Holy Bag — G cycles fruits\n":"")+help;
    }
}
