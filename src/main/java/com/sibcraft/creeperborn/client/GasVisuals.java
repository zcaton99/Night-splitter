package com.sibcraft.creeperborn.client;
import java.util.*;
public final class GasVisuals {
 private static final Map<Integer,Integer> flags=new HashMap<>();
 public static void update(int entity,int value){if((value&3)==0)flags.remove(entity);else flags.put(entity,value);}
 public static int flags(int entity){return flags.getOrDefault(entity,0);}
 public static void clear(){flags.clear();}
}
