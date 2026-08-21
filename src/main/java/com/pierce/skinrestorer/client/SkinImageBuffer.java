package com.pierce.skinrestorer.client;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.awt.image.ImageObserver;
import net.minecraft.client.renderer.IImageBuffer;

/**
 * 64x64-aware ImageBuffer for 1.7.10.
 * Vanilla 1.7.10 ImageBufferDownload only handles 64x32 (clips y=32..64).
 * 1.8+ / Ears (https://github.com/unascribed/Ears) correctly handles 64x64 outer layers.
 * This buffer ports the 1.8 logic: keep full 64x64 and handle hat + body/arm/leg overlay transparency.
 * Inspired by Ears / vanilla 1.8, MIT.
 */
@SideOnly(Side.CLIENT)
public class SkinImageBuffer implements IImageBuffer {
    private int[] imageData;
    private int imageWidth;
    private int imageHeight;

    @Override
    public BufferedImage parseUserSkin(BufferedImage src) {
        if (src == null) return null;
        int w = src.getWidth();
        int h = src.getHeight();
        if (w == 64 && h == 64) {
            BufferedImage img = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
            Graphics g = img.getGraphics();
            g.drawImage(src, 0, 0, (ImageObserver) null);
            g.dispose();
            this.imageData = ((DataBufferInt) img.getRaster().getDataBuffer()).getData();
            this.imageWidth = 64;
            this.imageHeight = 64;
            setAreaTransparent(32, 0, 64, 16);
            setAreaTransparent(0, 32, 16, 48);
            setAreaTransparent(16, 32, 32, 48);
            setAreaTransparent(32, 32, 48, 48);
            setAreaTransparent(48, 32, 64, 48);
            setAreaTransparent(0, 48, 16, 64);
            setAreaTransparent(16, 48, 32, 64);
            setAreaTransparent(32, 48, 48, 64);
            setAreaTransparent(48, 48, 64, 64);
            setAreaOpaque(0, 0, 32, 16);
            setAreaOpaque(0, 16, 32, 32);
            return img;
        }
        this.imageWidth = 64;
        this.imageHeight = 32;
        BufferedImage img = new BufferedImage(this.imageWidth, this.imageHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics g = img.getGraphics();
        g.drawImage(src, 0, 0, (ImageObserver) null);
        g.dispose();
        this.imageData = ((DataBufferInt) img.getRaster().getDataBuffer()).getData();
        setAreaOpaque(0, 0, 32, 16);
        setAreaTransparent(32, 0, 64, 32);
        setAreaOpaque(0, 16, 64, 32);
        return img;
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
