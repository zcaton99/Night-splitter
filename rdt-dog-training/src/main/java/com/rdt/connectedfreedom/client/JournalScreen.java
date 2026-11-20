package com.rdt.connectedfreedom.client;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.*;
import net.minecraft.network.chat.Component;
public final class JournalScreen extends Screen {
    private int page,lesson;private EditBox name;
    private static final String[] LESSONS={
        "BEGIN TOGETHER\nSay Yes, then let the pup explore. This pairs the release with an enjoyable opportunity. A hesitant pup does not have to play with a toy straight away.",
        "NAME + END OF THE LINE\nWhen the pup reaches the line's limit, pause the walk. Say the name once. Give a brief opportunity to connect, then use familiar help. Once the line is loose, walk on. The name asks for connection; it does not mean Here or Sit.",
        "OUR CYCLE OF HELP\nA cue comes before help. In this first lesson, Help encourages a return along a clear path. The pup gradually gets an opportunity to respond before help begins. Repeating the word louder does not teach its meaning.",
        "CELEBRATE TOGETHER\nAfter a connection, choose Celebrate and move together in a small loop. Keep it comfortable for this pup. Movement and shared enjoyment can make the next interaction worthwhile. You do not need maximum excitement.",
        "LINE PROBLEMS\nWalk around a tree or post while the pup explores. If the line crosses it, pause. Retrace your path, then use Help to encourage a clear route. Be the pup's cheat sheet. Do not drag or leave them stuck to guess.",
        "FIRST CURIOSITY\nAfter shared walking, check-ins, celebrations and a line problem, try Ready, then Yes. Watch the pup investigate. A sniff or approach is enough. Full tug and fetch arrive in later chapters.",
        "DONE + THE FOUR WINDOWS\nWork gives a taught job; Enough is settling after Done; Play begins with Ready, then Yes; Free is exploration released with Yes. In this prototype, Done starts a quiet pause. Settling is learned, not an instant emotional switch.",
        "UNDERSTANDING LEARNING\nAcquisition builds the behavior and cue. Fluency makes it smoother. Generalization adds places and situations. Discrimination clarifies the exact cue. Relationship meters here are game estimates, not a way to measure a real dog's feelings.",
        "REAL DOGS\nPractice with a trusted adult and a suitable dog. Give unfamiliar dogs space. Look at the whole body and situation; one wag does not tell the whole story. A game cannot tell you that any real dog is safe to approach."
    };
    public JournalScreen(){super(Component.literal("RDT - Connected Freedom"));}
    public boolean isPauseScreen(){return false;}
    private void button(String text,int x,int y,int w,Runnable run){addRenderableWidget(Button.builder(Component.literal(text),b->run.run()).bounds(x,y,w,20).build());}
    private void cue(String action){RdtClient.send(action,"");onClose();}
    protected void init(){
        int left=(width-Math.min(300,width-16))/2,w=Math.min(300,width-16),half=(w-6)/2;
        var v=RdtClient.view;
        if(v==null||v.dogId()==-1&&v.name().isEmpty()){
            name=new EditBox(font,left,100,w,20,Component.literal("Your pup's name"));name.setMaxLength(24);name.setValue("Pup");addRenderableWidget(name);
            button("Meet my pup",left,128,w,()->RdtClient.send("adopt",name.getValue()));
            button("Close",left,height-26,w,this::onClose);return;
        }
        button("Walk",left,32,(w-8)/3,()->{page=0;rebuildWidgets();});button("Learn",left+(w-8)/3+4,32,(w-8)/3,()->{page=1;rebuildWidgets();});button("Progress",left+2*((w-8)/3+4),32,(w-8)/3,()->{page=2;rebuildWidgets();});
        if(page==0){String[] labels={"Yes - release",v.name()+" - connect","Help","Celebrate","Ready - invite play","Done - settle","Change line length","Good - continue"};String[] actions={"yes","name","help","celebrate","ready","done","line","good"};for(int i=0;i<labels.length;i++){final String action=actions[i];button(labels[i],left+(i%2)*(half+6),116+(i/2)*23,half,()->cue(action));}}
        if(page==1){button("Previous lesson",left,height-52,half,()->{lesson=Math.floorMod(lesson-1,LESSONS.length);});button("Next lesson",left+half+6,height-52,half,()->{lesson=(lesson+1)%LESSONS.length;});}
        button("Back to my pup",left,height-26,w,this::onClose);
    }
    public void extractRenderState(GuiGraphicsExtractor g,int mouseX,int mouseY,float partial){
        g.fill(0,0,width,height,0xF018302D);int w=Math.min(300,width-16),left=(width-w)/2;
        g.text(font,"RDT: Connected Freedom",left,12,0xFFFFE7AD);var v=RdtClient.view;
        if(v==null||v.name().isEmpty()){
            g.textWithWordWrap(font,Component.literal("CHAPTER 1: A FIRST CONNECTION\nName a hesitant, curious pup. Begin on clear ground in the Overworld. We will walk, reconnect, celebrate, and discover the first curiosity about play."),left,40,w,0xFFF0F2DF);
            g.textWithWordWrap(font,Component.literal("Craft the field guide with a Book + Bone. You can also open it with J or /rdt."),left,158,w,0xFFD0DBCE);
        }else if(page==0){
            g.text(font,v.name()+" | Line "+(int)v.line()+" blocks | "+v.mode(),left,59,0xFFFFE7AD);
            var lines=font.split(Component.literal(v.notice()),w);int y=74;for(int i=0;i<Math.min(4,lines.size());i++){g.text(font,lines.get(i),left,y,0xFFF0F2DF);y+=9;}
        }else if(page==1){
            g.textWithWordWrap(font,Component.literal(LESSONS[lesson]),left,65,w,0xFFF0F2DF);
        }else{
            String text="CHAPTER 1 - "+(v.complete()?"COMPLETE":"IN PROGRESS")+"\n\nComfort: "+v.comfort()+" / 100\nEngagement: "+v.engagement()+" / 100\nName check-ins: "+v.connections()+" / 3\nMoving celebrations: "+v.celebrations()+" / 2\nLine problems solved: "+v.obstacles()+" / 1\nShared walking: "+v.distance()+" blocks\n\nNext: "+v.next();
            g.textWithWordWrap(font,Component.literal(text),left,62,w,0xFFF0F2DF);
        }
        super.extractRenderState(g,mouseX,mouseY,partial);
    }
}
