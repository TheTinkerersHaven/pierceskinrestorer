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
                @Override public void println(String x) { if (shouldSuppress(x)) return; origErr.println(x); }
                @Override public void println(Object x) { if (shouldSuppress(String.valueOf(x))) return; origErr.println(x); }
                @Override public void print(String s) { if (shouldSuppress(s)) return; origErr.print(s); }
                @Override public void print(Object obj) { if (shouldSuppress(String.valueOf(obj))) return; origErr.print(obj); }
                private boolean shouldSuppress(String s) {
                    if (s == null) return false;
                    String thread = Thread.currentThread().getName();
                    boolean isEarsThread = thread != null && thread.contains("Ears lookup thread");
                    if (isEarsThread) {
                        if (s.contains("com.unascribed.ears") || s.contains("Profile lookup failed") || s.contains("NullPointerException") || s.contains("Cannot invoke") || s.contains("HTTP.performGetRequest") || s.contains("HTTP.makeRequest") || s.contains("LegacyHelper") || s.contains("SessionService")) return true;
                    }
                    // Fallback: even if thread check fails, drop Ears legacy spam
                    if (s.contains("com.unascribed.ears.legacy") && (s.contains("NullPointerException") || s.contains("at Launch//"))) return true;
                    if (s.contains("[Ears] Profile lookup failed")) return true;
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
                    boolean isEarsThread = thread != null && thread.contains("Ears lookup thread");
                    if (isEarsThread && x != null && (x.contains("com.unascribed.ears") || x.contains("Profile lookup failed"))) return;
                    origOut.println(x);
                }
                @Override public void print(String s) {
                    String thread = Thread.currentThread().getName();
                    boolean isEarsThread = thread != null && thread.contains("Ears lookup thread");
                    if (isEarsThread && s != null && s.contains("Profile lookup failed")) return;
                    origOut.print(s);
                }
            };
            System.setOut(filteredOut);
        } catch (Exception ignored) {}
    }
}
