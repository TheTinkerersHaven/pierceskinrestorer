package com.pierce.skinrestorer.client;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.awt.image.ImageObserver;
import net.minecraft.client.renderer.IImageBuffer;

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
            // 1.8-style transparency handling for 64x64
            setAreaTransparent(32, 0, 64, 16);   // hat
            setAreaTransparent(0, 32, 16, 48);   // left leg overlay
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

    @Override
    public void func_152634_a() {}

    private void setAreaTransparent(int x1, int y1, int x2, int y2) {
        if (!hasTransparency(x1, y1, x2, y2)) {
            for (int x = x1; x < x2; ++x) {
                for (int y = y1; y < y2; ++y) {
                    this.imageData[x + y * this.imageWidth] &= 0x00FFFFFF;
                }
            }
        }
    }

    private void setAreaOpaque(int x1, int y1, int x2, int y2) {
        for (int x = x1; x < x2; ++x) {
            for (int y = y1; y < y2; ++y) {
                this.imageData[x + y * this.imageWidth] |= 0xFF000000;
            }
        }
    }

    private boolean hasTransparency(int x1, int y1, int x2, int y2) {
        for (int x = x1; x < x2; ++x) {
            for (int y = y1; y < y2; ++y) {
                int k = this.imageData[x + y * this.imageWidth];
                if ((k >> 24 & 255) < 128) return true;
            }
        }
        return false;
    }
}
