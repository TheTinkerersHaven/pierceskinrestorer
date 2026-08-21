package com.pierce.skinrestorer.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import com.mojang.authlib.properties.Property;
import com.pierce.skinrestorer.PierceSkinRestorer;
import com.pierce.skinrestorer.network.SkinUpdatePacket;
import cpw.mods.fml.common.Loader;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.renderer.ImageBufferDownload;
import net.minecraft.client.renderer.IImageBuffer;
import net.minecraft.client.renderer.ThreadDownloadImageData;
import net.minecraft.util.ResourceLocation;

@SideOnly(Side.CLIENT)
public class ClientSkinHandler {

    public static void handle(final SkinUpdatePacket pkt) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null) return;
        mc.func_152344_a(new Runnable() {
            @Override
            public void run() {
                try {
                    Minecraft mc2 = Minecraft.getMinecraft();
                    if (mc2 == null || mc2.thePlayer == null) return;

                    com.mojang.authlib.GameProfile entityProfile = mc2.thePlayer.getGameProfile();
                    com.mojang.authlib.GameProfile sessionProfile = mc2.getSession().func_148256_e();

                    entityProfile.getProperties().removeAll("textures");
                    sessionProfile.getProperties().removeAll("textures");

                    if (pkt.clear) {
                        if (mc2.thePlayer instanceof AbstractClientPlayer) {
                            ((AbstractClientPlayer) mc2.thePlayer).func_152121_a(MinecraftProfileTexture.Type.SKIN, AbstractClientPlayer.locationStevePng);
                        }
                        PierceSkinRestorer.LOGGER.info("Client skin cleared");
                        return;
                    }

                    Property prop = pkt.textureSignature != null
                            ? new Property("textures", pkt.textureValue, pkt.textureSignature)
                            : new Property("textures", pkt.textureValue);
                    entityProfile.getProperties().put("textures", prop);
                    sessionProfile.getProperties().put("textures", prop);

                    try {
                        String jsonStr = new String(Base64.getDecoder().decode(pkt.textureValue), StandardCharsets.UTF_8);
                        JsonObject root = new JsonParser().parse(jsonStr).getAsJsonObject();
                        JsonObject textures = root.getAsJsonObject("textures");
                        if (textures != null && textures.has("SKIN")) {
                            JsonObject skin = textures.getAsJsonObject("SKIN");
                            String url = skin.get("url").getAsString();
                            String model = null;
                            if (skin.has("metadata")) {
                                JsonObject meta = skin.getAsJsonObject("metadata");
                                if (meta.has("model")) model = meta.get("model").getAsString();
                            }
                            Map<String, String> metaMap = new HashMap<String, String>();
                            if ("slim".equals(model)) metaMap.put("model", "slim");
                            MinecraftProfileTexture tex = new MinecraftProfileTexture(url, metaMap);
                            ResourceLocation rl = new ResourceLocation("skins/" + tex.getHash());
                            File skinCache = new File(mc2.mcDataDir, "assets/skins/" + tex.getHash().substring(0, 2));
                            File file2 = new File(skinCache, tex.getHash());
                            // Use Ears-patched ImageBufferDownload when Ears present, otherwise our 64x64-aware fallback (Ears-inspired)
                            IImageBuffer buffer;
                            if (Loader.isModLoaded("ears")) {
                                buffer = new ImageBufferDownload();
                            } else {
                                buffer = new SkinImageBuffer();
                            }
                            ThreadDownloadImageData data = new ThreadDownloadImageData(file2, url, AbstractClientPlayer.locationStevePng, buffer);
                            mc2.getTextureManager().loadTexture(rl, data);
                            if (mc2.thePlayer instanceof AbstractClientPlayer) {
                                ((AbstractClientPlayer) mc2.thePlayer).func_152121_a(MinecraftProfileTexture.Type.SKIN, rl);
                            }
                            PierceSkinRestorer.LOGGER.info("Client skin direct-loaded url=" + url + " model=" + model + " ears=" + Loader.isModLoaded("ears"));
                            return;
                        }
                    } catch (Exception ex) {
                        PierceSkinRestorer.LOGGER.debug("Direct skin load failed, fallback: " + ex.getMessage());
                    }

                    if (mc2.thePlayer instanceof AbstractClientPlayer) {
                        AbstractClientPlayer acp = (AbstractClientPlayer) mc2.thePlayer;
                        mc2.func_152342_ad().func_152790_a(entityProfile, acp, true);
                    }
                } catch (Exception e) {
                    PierceSkinRestorer.LOGGER.error("Failed to apply client skin", e);
                }
            }
        });
    }
}
