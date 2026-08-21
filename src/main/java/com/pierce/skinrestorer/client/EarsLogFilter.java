package com.pierce.skinrestorer.client;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import java.io.OutputStream;
import java.io.PrintStream;

@SideOnly(Side.CLIENT)
public class EarsLogFilter {
    private static boolean installed = false;
    public static void install() {
        if (installed) return;
        installed = true;
        try {
            PrintStream origErr = System.err;
            PrintStream filtered = new PrintStream(new OutputStream() {
                @Override public void write(int b) { origErr.write(b); }
                @Override public void write(byte[] b, int off, int len) { origErr.write(b, off, len); }
            }, true) {
                @Override public void println(String x) {
                    if (shouldSuppress(x)) return;
                    origErr.println(x);
                }
                @Override public void println(Object x) {
                    if (shouldSuppress(String.valueOf(x))) return;
                    origErr.println(x);
                }
                private boolean shouldSuppress(String s) {
                    if (s == null) return false;
                    if (s.contains("Ears lookup thread") && (s.contains("Profile lookup failed") || s.contains("Cannot invoke \"java.io.InputStream.close()\"") || s.contains("LegacyHelper"))) return true;
                    String thread = Thread.currentThread().getName();
                    if (thread != null && thread.contains("Ears lookup thread")) {
                        if (s.contains("NullPointerException") || s.contains("HTTP.processResponse") || s.contains("SessionService.fillProfileProperties") || s.contains("LegacyHelper.getSkinUrl")) return true;
                    }
                    return false;
                }
            };
            System.setErr(filtered);
            PrintStream origOut = System.out;
            PrintStream filteredOut = new PrintStream(new OutputStream() {
                @Override public void write(int b) { origOut.write(b); }
                @Override public void write(byte[] b, int off, int len) { origOut.write(b, off, len); }
            }, true) {
                @Override public void println(String x) {
                    String thread = Thread.currentThread().getName();
                    if (thread != null && thread.contains("Ears lookup thread") && x != null && x.contains("Profile lookup failed")) return;
                    origOut.println(x);
                }
            };
            System.setOut(filteredOut);
        } catch (Exception ignored) {}
    }
}
