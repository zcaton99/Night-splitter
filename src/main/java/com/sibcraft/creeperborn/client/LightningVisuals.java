package com.sibcraft.creeperborn.client;
import java.util.*;
public final class LightningVisuals{
    private static final Map<Integer,Integer> flags=new HashMap<>();
    public static void update(int entity,int value){if((value&7)==0)flags.remove(entity);else flags.put(entity,value&7);}
    public static int flags(int entity){return flags.getOrDefault(entity,0);}
    public static void clear(){flags.clear();}
}
