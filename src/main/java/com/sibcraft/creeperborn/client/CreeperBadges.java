package com.sibcraft.creeperborn.client;

import java.util.*;
import com.sibcraft.creeperborn.Element;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import org.joml.Quaternionf;

/** Uses existing item artwork, attached to the creeper's torso rather than separate display entities. */
public final class CreeperBadges {
    private static final Map<String, String> LABELS = labels();
    private static Map<String, String> labels() {
        Map<String, String> result = new HashMap<>();
        for (Element e : Element.values()) result.put(e.title + " Creeper", "element_" + e.key());
        String[][] fruits = {{"rubber","Rubber"},{"door","Door"},{"phoenix","Phoenix"},{"lightning","Lightning"},
            {"dark","Dark"},{"op","Op-Op"},{"gravity","Gravity"},{"string","String"},{"paw","Paw-Paw"},
            {"tremor","Tremor-Tremor"},{"love","Love-Love"},{"sand","Sand-Sand"},{"ice","Ice-Ice"},
            {"magma","Magma-Magma"},{"azure","Azure Dragon"},{"tree","Tree-Tree"},{"petal","Petal-Petal"},
            {"glint","Glint-Glint"},{"straw","Straw-Straw"},{"flame","Flame-Flame"},{"buddha","Golden Buddha"},
            {"fox","Nine-Tailed Fox"},{"axolotl","Axolotl"},{"time","Time-Time"}};
        for (String[] f : fruits) result.put(f[1] + " Fruitborn Creeper", "fruit_" + f[0]);
        return Map.copyOf(result);
    }
    public static String key(String name) { return LABELS.getOrDefault(name, ""); }
    public static net.minecraft.world.item.ItemStack stack(String key) {
        if (key.startsWith("element_")) return com.sibcraft.creeperborn.Relics.heart(Element.parse(key.substring(8)));
        return key.startsWith("fruit_") ? com.sibcraft.creeperborn.Relics.fruit(key.substring(6)) : net.minecraft.world.item.ItemStack.EMPTY;
    }
    public static void extract(net.minecraft.world.entity.monster.Creeper creeper, net.minecraft.client.renderer.entity.state.EntityRenderState state) {
        String key = key(creeper.getCustomName() == null ? "" : creeper.getCustomName().getString());
        var badge = (CreeperBadgeState)state;
        badge.creeperborn$badge(key);
        badge.creeperborn$badgeItem().clear();
        if (!key.isEmpty()) {
            state.nameTag = null;
            var client = net.minecraft.client.Minecraft.getInstance();
            client.getItemModelResolver().updateForTopItem(badge.creeperborn$badgeItem(), stack(key),
                net.minecraft.world.item.ItemDisplayContext.FIXED, client.level, null, creeper.getId());
        }
    }
    public static void submit(LivingEntityRenderState state, PoseStack poses, SubmitNodeCollector nodes) {
        String key = ((CreeperBadgeState)state).creeperborn$badge();
        if (key.isEmpty() || state.isInvisible) return;
        poses.pushPose();
        poses.rotate(new Quaternionf().rotationY((float)Math.toRadians(-state.bodyRot)));
        // Creeper torso spans y=.375..1.125 and its forward face is z=.125.
        poses.translate(0,.79,.132);
        poses.scale(.44f,.44f,.025f);
        ((CreeperBadgeState)state).creeperborn$badgeItem().submit(poses, nodes, 0xF000F0, OverlayTexture.NO_OVERLAY, 0);
        poses.popPose();
    }
    public static void validate() {
        if (LABELS.size() != 28) throw new AssertionError("Every special creeper needs a chest icon");
        for (String key : LABELS.values()) if (stack(key).isEmpty()) throw new AssertionError("Missing chest icon: " + key);
    }
}
