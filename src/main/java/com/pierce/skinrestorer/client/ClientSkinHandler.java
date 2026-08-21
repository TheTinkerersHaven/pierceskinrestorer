package com.pierce.skinrestorer.client;

import com.mojang.authlib.properties.Property;
import com.pierce.skinrestorer.PierceSkinRestorer;
import com.pierce.skinrestorer.network.SkinUpdatePacket;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.util.ResourceLocation;

@SideOnly(Side.CLIENT)
public class ClientSkinHandler {

    public static void handle(final SkinUpdatePacket pkt) {
        // Ensure we run on main client thread
        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null) return;
        // 1.7.10: func_152344_a = addScheduledTask
        mc.func_152344_a(new Runnable() {
            @Override
            public void run() {
                try {
                    Minecraft mc2 = Minecraft.getMinecraft();
                    if (mc2 == null || mc2.thePlayer == null) {
                        // Player not yet spawned, retry shortly
                        PierceSkinRestorer.LOGGER.debug("ClientSkinHandler: player not ready, deferring");
                        return;
                    }
                    // Update both the session profile and the entity's GameProfile
                    // EntityPlayerSP is constructed from Session profile, but both share same concept
                    com.mojang.authlib.GameProfile entityProfile = mc2.thePlayer.getGameProfile();
                    com.mojang.authlib.GameProfile sessionProfile = mc2.getSession().func_148256_e();

                    entityProfile.getProperties().removeAll("textures");
                    sessionProfile.getProperties().removeAll("textures");

                    if (pkt.clear) {
                        // Reset to Steve - force reload with empty properties
                        // Trigger skin manager to clear
                        if (mc2.thePlayer instanceof AbstractClientPlayer) {
                            ((AbstractClientPlayer) mc2.thePlayer).func_152121_a(com.mojang.authlib.minecraft.MinecraftProfileTexture.Type.SKIN, AbstractClientPlayer.locationStevePng);
                        }
                        PierceSkinRestorer.LOGGER.info("Client skin cleared (self-view)");
                        return;
                    }

                    Property prop;
                    if (pkt.textureSignature != null) {
                        prop = new Property("textures", pkt.textureValue, pkt.textureSignature);
                    } else {
                        prop = new Property("textures", pkt.textureValue);
                    }
                    entityProfile.getProperties().put("textures", prop);
                    sessionProfile.getProperties().put("textures", prop);

                    // Force SkinManager to re-download and apply
                    if (mc2.thePlayer instanceof AbstractClientPlayer) {
                        AbstractClientPlayer acp = (AbstractClientPlayer) mc2.thePlayer;
                        // This will call sessionService.getTextures(profile) which reads the injected property
                        mc2.func_152342_ad().func_152790_a(entityProfile, acp, true);
                        PierceSkinRestorer.LOGGER.info("Client skin updated for self-view: " + entityProfile.getName());
                    }
                } catch (Exception e) {
                    PierceSkinRestorer.LOGGER.error("Failed to apply client skin", e);
                }
            }
        });
    }
}
