package com.rdt.connectedfreedom;

/** Observable learning progress for the first walking chapter; never an obedience timer. */
public final class Learning {
    public int connections,celebrations,obstacles,walkBlocks;
    public java.util.Set<String> socialDogs=new java.util.HashSet<>();
    public float comfort=20,engagement=10;
    public boolean released,ready,curious,complete;
    public String mode="settle",notice="Meet your pup. We will learn together, one small step at a time.";
    public int chapterStep(){return curious?6:!released?0:connections<3?1:celebrations<2?2:obstacles<1?3:readyForPlay()?5:4;}
    public boolean readyForPlay(){return connections>=3&&celebrations>=2&&obstacles>=1&&walkBlocks>=20&&comfort>=40;}
    public String next(){return switch(chapterStep()){
        case 0->"Say Yes to begin a short walk and explore together.";
        case 1->"At the end of the line: pause, say your pup's name, then help if needed. Build 3 check-ins.";
        case 2->"After a success, celebrate together. Try 2 short moving celebrations.";
        case 3->"Walk around a tree or post. If the line crosses it, pause and help your pup find a clear path.";
        case 4->"Explore together a little more. Notice the pup's responses.";
        case 5->"Try Ready, then Yes with the toy. Look for curiosity, rather than demanding a tug.";
        default->"First curiosity! Celebrate, then Done and a quiet pause. More play lessons will follow.";
    };}
    public void walked(double distance){if(!released||mode.equals("settle")||distance<.05||distance>2)return;comfort=Math.min(100,comfort+(float)distance*.1f);}
    public void connection(){connections++;comfort=Math.min(100,comfort+5);engagement=Math.min(100,engagement+4);notice="You reconnected and the line is loose. Continue walking together.";}
    public void celebrate(){celebrations++;comfort=Math.min(100,comfort+4);engagement=Math.min(100,engagement+5);notice="A moving celebration! Give your pup room and enjoy this success together.";}
    public void obstacle(){obstacles++;comfort=Math.min(100,comfort+5);notice="A clear path again. You helped your pup solve the line problem. Celebrate together.";}
    public boolean invite(){if(!readyForPlay()){notice="Not ready for the toy yet? That is useful information. Try the next walking step instead.";return false;}ready=true;mode="ready";notice="Ready opens the play window. Say Yes to invite engagement.";return true;}
    public void yes(){released=true;if(ready){ready=false;mode="investigate";notice="Yes! Let the pup approach the toy. A sniff or small investigation is enough today.";}else{mode="explore";notice="Yes - go explore. Keep some slack in the line and move together.";}}
    public void investigated(){curious=true;mode="curious";notice="Your pup approached and investigated the toy. That first curiosity is enough. Celebrate, then Done and settle.";}
    public void done(){mode="settle";ready=false;if(curious)complete=true;notice=complete?"Chapter 1 complete. You built connection, helped your pup learn, and invited curiosity. Let the pup settle.":"Done. End the activity and give your pup time to settle. Settling is learned gradually.";}
}
