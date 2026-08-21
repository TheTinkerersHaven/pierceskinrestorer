package com.pierce.skinrestorer;

import com.pierce.skinrestorer.command.SkinCommand;
import com.pierce.skinrestorer.config.ModConfig;
import com.pierce.skinrestorer.handler.PlayerEventHandler;
import com.pierce.skinrestorer.network.NetworkHandler;
import com.pierce.skinrestorer.network.SkinPacketHandler;
import com.pierce.skinrestorer.skin.SkinStorage;
import cpw.mods.fml.common.FMLCommonHandler;
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
 * Server restores others via S0C injection. Client companion restores self via custom packet.
 * GTNH 2.8.4 Compatible.
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
    public static final String VERSION = "1.0.7";

    @Instance(MODID)
    public static PierceSkinRestorer instance;

    public static final Logger LOGGER = LogManager.getLogger(MODID);

    private File dataDir;

    @EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        LOGGER.info("Pierce Skin Restorer Pre-Initialization (Universal 1.0.7)");
        ModConfig.init(new File(event.getModConfigurationDirectory(), MODID + ".cfg"));
        dataDir = new File(event.getModConfigurationDirectory().getParentFile(), "skinrestorer");
        SkinStorage.init(dataDir);
        NetworkHandler.init();
    }

    @EventHandler
    public void init(FMLInitializationEvent event) {
        LOGGER.info("Pierce Skin Restorer Initialization");
        PlayerEventHandler handler = new PlayerEventHandler();
        MinecraftForge.EVENT_BUS.register(handler);
        FMLCommonHandler.instance().bus().register(handler);
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
