package com.rdt.connectedfreedom;
/** Progress records completed movement, never button presses. Familiar practice is not generalization. */
public final class RecallLearning {
    public int letsGo,here;
    public int successes(String cue){return cue.equals("here")?here:letsGo;}
    public boolean familiar(String cue){return successes(cue)>=3;}
    public void completed(String cue){if(cue.equals("here"))here++;else letsGo++;}
}
