package com.pierce.skinrestorer.client;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.model.ModelRenderer;
import net.minecraftforge.client.event.RenderPlayerEvent;

@SideOnly(Side.CLIENT)
public class WearLayerHandler {

    private ModelRenderer bodyWear;
    private ModelRenderer leftArmWear;
    private ModelRenderer rightArmWear;
    private ModelRenderer leftLegWear;
    private ModelRenderer rightLegWear;
    private boolean init = false;

    private void ensureInit() {
        if (init) return;
        ModelBase wearBase = new ModelBase() {
            @Override public void render(net.minecraft.entity.Entity p_78088_1_, float p_78088_2_, float p_78088_3_, float p_78088_4_, float p_78088_5_, float p_78088_6_, float p_78088_7_) {}
        };
        wearBase.textureWidth = 64;
        wearBase.textureHeight = 64;
        bodyWear = new ModelRenderer(wearBase, 16, 32);
        bodyWear.addBox(-4.0F, 0.0F, -2.0F, 8, 12, 4, 0.25F);
        rightArmWear = new ModelRenderer(wearBase, 40, 32);
        rightArmWear.addBox(-3.0F, -2.0F, -2.0F, 4, 12, 4, 0.25F);
        leftArmWear = new ModelRenderer(wearBase, 48, 48);
        leftArmWear.addBox(-1.0F, -2.0F, -2.0F, 4, 12, 4, 0.25F);
        rightLegWear = new ModelRenderer(wearBase, 0, 32);
        rightLegWear.addBox(-2.0F, 0.0F, -2.0F, 4, 12, 4, 0.25F);
        leftLegWear = new ModelRenderer(wearBase, 0, 48);
        leftLegWear.addBox(-2.0F, 0.0F, -2.0F, 4, 12, 4, 0.25F);
        init = true;
    }

    @SubscribeEvent
    public void onRenderSpecialsPost(RenderPlayerEvent.Specials.Post event) {
        ModelBiped model = event.renderer.modelBipedMain;
        if (model == null) return;
        ensureInit();
        copyModel(model.bipedBody, bodyWear);
        copyModel(model.bipedRightArm, rightArmWear);
        copyModel(model.bipedLeftArm, leftArmWear);
        copyModel(model.bipedRightLeg, rightLegWear);
        copyModel(model.bipedLeftLeg, leftLegWear);
        float scale = 0.0625F;
        bodyWear.render(scale);
        rightArmWear.render(scale);
        leftArmWear.render(scale);
        rightLegWear.render(scale);
        leftLegWear.render(scale);
    }

    private void copyModel(ModelRenderer src, ModelRenderer dst) {
        dst.rotateAngleX = src.rotateAngleX;
        dst.rotateAngleY = src.rotateAngleY;
        dst.rotateAngleZ = src.rotateAngleZ;
        dst.rotationPointX = src.rotationPointX;
        dst.rotationPointY = src.rotationPointY;
        dst.rotationPointZ = src.rotationPointZ;
        dst.mirror = src.mirror;
    }
}
