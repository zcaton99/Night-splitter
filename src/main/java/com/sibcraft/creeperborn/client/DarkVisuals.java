package com.sibcraft.creeperborn.client;
import java.util.*;
public final class DarkVisuals{
    private static final Map<Integer,Integer> flags=new HashMap<>();
    public static void update(int entity,int value){if((value&15)==0)flags.remove(entity);else flags.put(entity,value&15);}
    public static int flags(int entity){return flags.getOrDefault(entity,0);}
    public static void clear(){flags.clear();}
}
