package com.rdt.connectedfreedom;
import java.nio.file.*;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
public final class Regression implements ModInitializer {
    private static int checks;
    private static void check(boolean v,String text){if(!v)throw new AssertionError(text);checks++;System.out.println("RDT_CHECK "+text);}
    public void onInitialize(){if(Boolean.getBoolean("rdt.multiplayer"))return;ServerLifecycleEvents.SERVER_STARTED.register(s->{try{
        Learning l=new Learning();check(!l.readyForPlay()&&!l.invite(),"A new hesitant pup is not ready on demand");
        l.yes();check(l.released&&l.mode.equals("explore")&&l.chapterStep()==1,"Yes opens exploration before any known obedience");
        l.connection();l.obstacle();check(l.chapterStep()==1,"An early line problem cannot skip name recognition");
        l.connection();l.connection();check(l.chapterStep()==2,"Three check-ins lead to shared celebrations");
        check(!l.readyForPlay(),"Check-ins alone do not buy play readiness");l.celebrate();l.celebrate();
        check(!l.readyForPlay(),"A walk is required as well as menu choices");l.walkBlocks=20;l.walked(1);
        check(l.readyForPlay()&&l.chapterStep()==5,"Readiness combines experience, connection and comfort");
        check(l.invite()&&l.mode.equals("ready"),"Ready precedes engagement");l.yes();check(!l.curious&&l.mode.equals("investigate"),"Yes alone does not award a curiosity response");
        l.done();check(!l.complete,"The chapter cannot complete without actual curiosity");
        l.curious=true;l.done();check(l.complete&&l.mode.equals("settle"),"Observed curiosity followed by Done completes the first chapter");
        for(int i=0;i<100;i++){l.connection();l.celebrate();}check(l.comfort<=100&&l.engagement<=100,"Relationship estimates have bounded values");
        check(DogTraining.cleanName("  Sprout  ").equals("Sprout"),"Pup names are trimmed");check(!DogTraining.cleanName("<script>Bad\nName</script>").contains("<"),"Pup names cannot introduce formatting or control characters");check(DogTraining.cleanName(" ").equals("Pup"),"Empty names have a readable fallback");
        check(!new JournalPayload("open","x".repeat(49)).valid(),"Oversized client values are rejected");
        RecallLearning recall=new RecallLearning();check(!recall.familiar("here")&&!recall.familiar("letsgo"),"New recall words begin without learned responses");
        recall.completed("here");recall.completed("here");check(!recall.familiar("here")&&recall.letsGo==0,"Learning one recall does not teach the other picture");recall.completed("here");check(recall.familiar("here")&&!recall.familiar("letsgo"),"Completed returns build familiarity separately");
        Path p=Path.of("rdt-save-test.json");PupState state=new PupState();state.name="Sprout";state.learning=l;state.recalls=recall;state.holding=true;state.recall="here";state.atLimit=true;state.pendingName=true;WorldStore.write(p,state);PupState loaded=WorldStore.read(p,PupState.class,new PupState());
        check(loaded.name.equals("Sprout")&&loaded.learning.complete,"Learning and pup identity survive JSON round trips");check(!loaded.pendingName&&!loaded.atLimit,"Temporary movement state is not restored as stale work");check(loaded.recalls.here==3&&!loaded.holding&&loaded.recall==null,"Recall learning saves while temporary work is cleared");Files.delete(p);
        var holder=s.getRecipeManager().byKey(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.RECIPE,net.minecraft.resources.Identifier.fromNamespaceAndPath("rdt","field_guide"))).orElseThrow();
        var input=net.minecraft.world.item.crafting.CraftingInput.of(2,1,java.util.List.of(new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.BOOK),new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.BONE)));
        @SuppressWarnings("unchecked")var recipe=(net.minecraft.world.item.crafting.CraftingRecipe)holder.value();
        check(recipe.matches(input,s.overworld())&&recipe.assemble(input).is(DogTraining.JOURNAL),"A book and bone craft the real field guide");
        System.out.println("RDT_REGRESSION_PASSED checks="+checks);s.halt(false);
    }catch(Throwable e){e.printStackTrace();s.halt(false);throw new RuntimeException(e);}});}
}
