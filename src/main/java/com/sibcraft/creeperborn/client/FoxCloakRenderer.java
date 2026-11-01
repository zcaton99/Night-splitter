package com.sibcraft.creeperborn.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.*;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;

/** Drawn inside vanilla's player transform, with exact copies of the animated bones. */
public final class FoxCloakRenderer {
    private static final Identifier RED=texture("red_concrete"),GOLD=texture("yellow_concrete"),BLACK=texture("black_concrete");
    private static final String[] BONES={"head","body","right_arm","left_arm","right_leg","left_leg"};
    private static final ModelPart SHELL=mesh(false),MARKS=mesh(true);
    private static Identifier texture(String n){return Identifier.withDefaultNamespace("textures/block/"+n+".png");}
    private static void box(PartDefinition r,String n,float x,float y,float z,float w,float h,float d){r.addOrReplaceChild(n,CubeListBuilder.create().texOffs(0,0).addBox(x,y,z,w,h,d),PartPose.ZERO);}
    private static ModelPart mesh(boolean markings){var mesh=new MeshDefinition();var root=mesh.getRoot();for(String name:BONES)root.addOrReplaceChild(name,CubeListBuilder.create(),PartPose.ZERO);
        var body=root.getChild("body");if(markings){box(body,"zipper",-.35f,0,-2.6f,.7f,12,.15f);box(body,"belt",-4.4f,10.5f,-2.6f,8.8f,.8f,.15f);for(int side:new int[]{-1,1})for(int i=0;i<3;i++)box(body,"seal"+side+i,side*2.7f-.45f,2+i*1.7f,-2.65f,.9f,1,.15f);}else{
            box(body,"chest",-4.35f,-.2f,-2.45f,8.7f,12.4f,4.9f);box(body,"collar",-4.8f,-.35f,-2.8f,9.6f,2,5.6f);
            var head=root.getChild("head");for(int side:new int[]{-1,1}){box(head,"ear"+side,side*3-1.5f,-11.5f,-1.5f,3,4,3);box(head,"tip"+side,side*3-1,-13,-1,2,2,2);
                var arm=root.getChild(side<0?"right_arm":"left_arm");float x=side<0?-3.3f:-1.3f;box(arm,"sleeve",x,-2.3f,-2.3f,4.6f,12.6f,4.6f);box(arm,"hand",x-.2f,8.5f,-2.5f,5,4,5);for(int i=0;i<3;i++)box(arm,"claw"+i,x+.3f+i*1.5f,11.5f,-2.5f,1,3,1.5f);
                var leg=root.getChild(side<0?"right_leg":"left_leg");box(leg,"pant",-2.25f,-.1f,-2.25f,4.5f,12.3f,4.5f);box(leg,"coat",-2.5f,-.1f,-2.5f,5,4.5f,5);
            }
        }return LayerDefinition.create(mesh,16,16).bakeRoot();
    }
    private static ModelPart[] bones(HumanoidModel<?> m){return new ModelPart[]{m.head,m.body,m.rightArm,m.leftArm,m.rightLeg,m.leftLeg};}
    private static void match(HumanoidModel<?> parent){var parts=bones(parent);for(int i=0;i<BONES.length;i++){SHELL.getChild(BONES[i]).loadPose(parts[i].storePose());MARKS.getChild(BONES[i]).loadPose(parts[i].storePose());}}
    public static boolean matches(HumanoidModel<?> parent){var parts=bones(parent);for(int i=0;i<BONES.length;i++)if(!SHELL.getChild(BONES[i]).storePose().equals(parts[i].storePose()))return false;return true;}
    private static void draw(ModelPart part,Identifier texture,PoseStack p,SubmitNodeCollector n,boolean glow){n.submitModelPart(part,p,glow?RenderTypes.eyes(texture):RenderTypes.entityTranslucent(texture),0xF000F0,OverlayTexture.NO_OVERLAY,null);}
    public static void submit(AvatarRenderState state,PoseStack p,SubmitNodeCollector n,HumanoidModel<?> parent,int flags){int mode=(flags>>2)&3;if((flags&3)!=1||mode<1||mode>2||state.isSpectator||state.isInvisibleToPlayer)return;match(parent);draw(SHELL,mode==1?RED:GOLD,p,n,true);if(mode==2)draw(MARKS,BLACK,p,n,false);
        p.pushPose();parent.body.translateAndRotate(p);p.translate(0,10/16.0,2.5/16.0);p.scale(1,-1,-1);p.translate(0,-.65,.15);p.rotate(new org.joml.Quaternionf().rotationX((float)Math.sin(state.walkAnimationPos*.45)*Math.min(.12f,state.walkAnimationSpeed*.12f)));ZoanRenderer.tails(p,n,mode,state.ageInTicks);p.popPose();
    }
    public static void validate(){var parent=new HumanoidModel<AvatarRenderState>(LayerDefinition.create(HumanoidModel.createMesh(CubeDeformation.NONE,0),64,64).bakeRoot());parent.body.xRot=.5f;parent.body.y=3.2f;parent.rightArm.xRot=-1.4f;parent.leftArm.zRot=.7f;parent.rightLeg.xRot=.9f;parent.leftLeg.z=4;parent.head.yRot=.8f;match(parent);if(!matches(parent))throw new AssertionError("Fox cloak must exactly copy every animated player bone");parent.rightArm.xRot=1.2f;parent.leftLeg.xRot=-.9f;match(parent);if(!matches(parent))throw new AssertionError("Fox cloak pose must update every frame");}
}
