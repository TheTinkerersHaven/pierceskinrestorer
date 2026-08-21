package com.pierce.skinrestorer;

import com.pierce.skinrestorer.command.SkinCommand;
import com.pierce.skinrestorer.config.ModConfig;
import com.pierce.skinrestorer.handler.PlayerEventHandler;
import com.pierce.skinrestorer.network.NetworkHandler;
import com.pierce.skinrestorer.network.SkinPacketHandler;
import com.pierce.skinrestorer.skin.SkinStorage;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.Mod.EventHandler;
import cpw.mods.fml.common.Mod.Instance;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;
import cpw.mods.fml.common.event.FMLServerStoppingEvent;
import net.minecraftforge.common.MinecraftForge;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;

/**
 * Pierce Skin Restorer - Universal (server + client companion for self-view)
 * GTNH 2.8.4 Compatible. 1.0.15: use Ears for layers, suppress Ears offline lookup spam via filter (drop, not move)
 */
@Mod(
    modid = PierceSkinRestorer.MODID,
    name = PierceSkinRestorer.NAME,
    version = PierceSkinRestorer.VERSION,
    acceptedMinecraftVersions = "[1.7.10]",
    dependencies = "required-after:Forge@[10.13.4.1614,)",
    acceptableRemoteVersions = "*"
)
public class PierceSkinRestorer {

    public static final String MODID = "pierceskinrestorer";
    public static final String NAME = "Pierce Skin Restorer";
    public static final String VERSION = "1.0.15";

    @Instance(MODID)
    public static PierceSkinRestorer instance;

    public static final Logger LOGGER = LogManager.getLogger(MODID);

    private File dataDir;

    @EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        LOGGER.info("Pierce Skin Restorer Pre-Initialization (Universal 1.0.15)");
        ModConfig.init(new File(event.getModConfigurationDirectory(), MODID + ".cfg"));
        dataDir = new File(event.getModConfigurationDirectory().getParentFile(), "skinrestorer");
        SkinStorage.init(dataDir);
        NetworkHandler.init();
        // Use Ears for layers, suppress its offline lookup spam (drop, not move)
        try { if (event.getSide().isClient() && Loader.isModLoaded("ears")) com.pierce.skinrestorer.client.EarsLogFilter.install(); } catch (Exception ignored) {}
    }

    @EventHandler
    public void init(FMLInitializationEvent event) {
        LOGGER.info("Pierce Skin Restorer Initialization");
        PlayerEventHandler handler = new PlayerEventHandler();
        MinecraftForge.EVENT_BUS.register(handler);
        FMLCommonHandler.instance().bus().register(handler);
        // Wear backport only if Ears not present (avoid double)
        try {
            if (event.getSide().isClient() && !Loader.isModLoaded("ears")) {
                MinecraftForge.EVENT_BUS.register(new com.pierce.skinrestorer.client.WearLayerHandler());
                LOGGER.info("Wear layer handler registered (Ears not detected)");
            } else if (event.getSide().isClient()) {
                LOGGER.info("Ears detected - using Ears for wear layers, skipping built-in");
            }
        } catch (Exception e) {
            LOGGER.debug("Wear layer register failed: " + e.getMessage());
        }
        LOGGER.info("Pierce Skin Restorer loaded - self-view via companion when installed on client!");
    }

    @EventHandler
    public void serverStarting(FMLServerStartingEvent event) {
        LOGGER.info("Registering /skin command");
        event.registerServerCommand(new SkinCommand());
        SkinPacketHandler.init();
    }

    @EventHandler
    public void serverStopping(FMLServerStoppingEvent event) {
        LOGGER.info("Server stopping - saving skin data");
        SkinStorage.save();
    }

    public File getDataDir() {
        return dataDir;
    }
}
