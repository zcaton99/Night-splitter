package com.rdt.connectedfreedom;
import java.util.*;
import net.minecraft.world.phys.Vec3;
public final class PupState {
    public String owner,dog,name="Pup",dimension="minecraft:overworld";
    public double x,y,z,line=5;
    public Learning learning=new Learning();
    public boolean leashed=true;
    public transient boolean installed,atLimit,tangled,helping,pendingName,returning;
    public transient int stillTicks,nameAt,lastCue=-1000,celebrateTicks,celebrateAt=-1,blockedTicks,clearTicks;
    public transient long lastPacket;
    public transient double travelRemainder;
    public transient Vec3 playerLast,dogLast,celebrateLast,exploreTarget;
    public transient java.util.ArrayDeque<Vec3> route=new java.util.ArrayDeque<>();
    public void runtime(){if(route==null)route=new java.util.ArrayDeque<>();}
}
