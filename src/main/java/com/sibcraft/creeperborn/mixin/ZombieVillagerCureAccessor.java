package com.sibcraft.creeperborn.mixin;
import java.util.UUID;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.monster.zombie.ZombieVillager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
@Mixin(ZombieVillager.class)
public interface ZombieVillagerCureAccessor {
    @Invoker("startConverting") void creeperborn$start(UUID player,int ticks);
    @Invoker("finishConversion") void creeperborn$finish(ServerLevel level);
}
