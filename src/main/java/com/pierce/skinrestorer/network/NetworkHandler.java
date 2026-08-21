package com.pierce.skinrestorer.network;

import com.pierce.skinrestorer.PierceSkinRestorer;
import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import cpw.mods.fml.relauncher.Side;

public class NetworkHandler {
    public static SimpleNetworkWrapper INSTANCE;

    public static void init() {
        INSTANCE = NetworkRegistry.INSTANCE.newSimpleChannel(PierceSkinRestorer.MODID);
        INSTANCE.registerMessage(SkinUpdatePacket.Handler.class, SkinUpdatePacket.class, 0, Side.CLIENT);
        PierceSkinRestorer.LOGGER.info("Network handler registered (client self-view channel)");
    }
}
