package com.pierce.skinrestorer.client;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.awt.image.ImageObserver;
import net.minecraft.client.renderer.IImageBuffer;

/**
 * 64x64-aware ImageBuffer for 1.7.10 - standalone fallback when Ears not present.
 * Logic taken from Ears by unascribed (Ampflower) https://github.com/unascribed/Ears
 * platform-forge-1.7 Ears.interceptParseUserSkin - MIT, re-implemented.
 * When Ears is present, ClientSkinHandler delegates to ImageBufferDownload (patched by Ears) instead.
 */
@SideOnly(Side.CLIENT)
public class SkinImageBuffer implements IImageBuffer {
    private int[] imageData;
    private int imageWidth;
    private int imageHeight;

    @Override
    public BufferedImage parseUserSkin(BufferedImage src) {
        if (src == null) return null;
        this.imageWidth = 64;
        this.imageHeight = 64;
        BufferedImage newImg = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
        Graphics g = newImg.getGraphics();
        g.drawImage(src, 0, 0, (ImageObserver) null);
        if (src.getHeight() == 32) {
            // Upgrade legacy 64x32 to 64x64 like Ears does
            g.drawImage(newImg, 24, 48, 20, 52, 4, 16, 8, 20, null);
            g.drawImage(newImg, 28, 48, 24, 52, 8, 16, 12, 20, null);
            g.drawImage(newImg, 20, 52, 16, 64, 8, 20, 12, 32, null);
            g.drawImage(newImg, 24, 52, 20, 64, 4, 20, 8, 32, null);
            g.drawImage(newImg, 28, 52, 24, 64, 0, 20, 4, 32, null);
            g.drawImage(newImg, 32, 52, 28, 64, 12, 20, 16, 32, null);
            g.drawImage(newImg, 40, 48, 36, 52, 44, 16, 48, 20, null);
            g.drawImage(newImg, 44, 48, 40, 52, 48, 16, 52, 20, null);
            g.drawImage(newImg, 36, 52, 32, 64, 48, 20, 52, 32, null);
            g.drawImage(newImg, 40, 52, 36, 64, 44, 20, 48, 32, null);
            g.drawImage(newImg, 44, 52, 40, 64, 40, 20, 44, 32, null);
            g.drawImage(newImg, 48, 52, 44, 64, 52, 20, 56, 32, null);
        }
        g.dispose();
        this.imageData = ((DataBufferInt) newImg.getRaster().getDataBuffer()).getData();
        // Ears: carefullyStripAlpha -> make base areas opaque (0,0-32,16 and others)
        // Approximate by making main base opaque
        setAreaOpaque(0, 0, 32, 16);
        setAreaOpaque(0, 16, 64, 32);
        // Ears transparent overlays (6 areas)
        setAreaTransparent(32, 0, 64, 32);
        setAreaTransparent(0, 32, 16, 48);
        setAreaTransparent(16, 32, 40, 48);
        setAreaTransparent(40, 32, 56, 48);
        setAreaTransparent(0, 48, 16, 64);
        setAreaTransparent(48, 48, 64, 64);
        return newImg;
    }

    @Override public void func_152634_a() {}

    private void setAreaTransparent(int x1, int y1, int x2, int y2) {
        if (!hasTransparency(x1, y1, x2, y2)) {
            for (int x = x1; x < x2; ++x) for (int y = y1; y < y2; ++y) this.imageData[x + y * this.imageWidth] &= 0x00FFFFFF;
        }
    }
    private void setAreaOpaque(int x1, int y1, int x2, int y2) {
        for (int x = x1; x < x2; ++x) for (int y = y1; y < y2; ++y) this.imageData[x + y * this.imageWidth] |= 0xFF000000;
    }
    private boolean hasTransparency(int x1, int y1, int x2, int y2) {
        for (int x = x1; x < x2; ++x) for (int y = y1; y < y2; ++y) if ((this.imageData[x + y * this.imageWidth] >> 24 & 255) < 128) return true;
        return false;
    }
}
