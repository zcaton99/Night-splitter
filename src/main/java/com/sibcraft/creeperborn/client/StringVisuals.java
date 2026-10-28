package com.sibcraft.creeperborn.client;
import com.sibcraft.creeperborn.StringControlPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.CameraType;
/** Local camera only: never switches the real player to spectator or transfers their body. */
public final class StringVisuals {
    private static int target=-1;private static CameraType previous;private static Integer fov,appliedFov;private static boolean boost,gliding;
    public static void attack(){var c=Minecraft.getInstance();if(target>=0&&c.player!=null&&c.gui.screen()==null&&ClientPlayNetworking.canSend(StringControlPayload.TYPE))ClientPlayNetworking.send(new StringControlPayload(target,32,c.player.getYRot(),c.player.getXRot()));}
    public static boolean gliding(){return gliding;}
    public static boolean controlling(){return target>=0;}
    public static void update(StringControlPayload p){target=p.entity();boost=(p.flags()&64)!=0;gliding=(p.flags()&128)!=0;if(target<0)restoreCamera();}
    private static void restoreCamera(){var c=Minecraft.getInstance();if(previous!=null){c.setCameraEntity(c.player);c.options.setCameraType(previous);previous=null;}}
    public static void clear(){target=-1;boost=false;gliding=false;restoreCamera();restoreFov();}
    private static void restoreFov(){var c=Minecraft.getInstance();if(fov!=null){if(c.options.fov().get().equals(appliedFov))c.options.fov().set(fov);fov=null;appliedFov=null;}}
    public static void tick(Minecraft c,int ticks){
        if(boost){if(fov==null){fov=c.options.fov().get();appliedFov=Math.min(110,fov+8);c.options.fov().set(appliedFov);}}else restoreFov();
        if(target<0){if(gliding&&ticks%2==0&&ClientPlayNetworking.canSend(StringControlPayload.TYPE))ClientPlayNetworking.send(new StringControlPayload(-1,(c.gui.screen()!=null||c.isPaused()?0:(c.options.keyUp.isDown()?1:0)|(c.options.keyJump.isDown()?16:0)),c.player.getYRot(),c.player.getXRot()));return;}var e=c.level==null?null:c.level.getEntity(target);if(e==null||!e.isAlive()){restoreCamera();return;}
        if(previous==null){previous=c.options.getCameraType();c.player.setYRot(e.getYRot());c.player.setXRot(e.getXRot());c.options.setCameraType(CameraType.THIRD_PERSON_BACK);}c.setCameraEntity(e);
        if(ticks%2==0&&ClientPlayNetworking.canSend(StringControlPayload.TYPE)){
            int flags=c.gui.screen()!=null||c.isPaused()?0:(c.options.keyUp.isDown()?1:0)|(c.options.keyDown.isDown()?2:0)|(c.options.keyLeft.isDown()?4:0)|(c.options.keyRight.isDown()?8:0)|(c.options.keyJump.isDown()?16:0)|(c.options.keyAttack.isDown()?32:0);
            ClientPlayNetworking.send(new StringControlPayload(target,flags,c.player.getYRot(),c.player.getXRot()));
        }
    }
}
