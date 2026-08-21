package com.pierce.skinrestorer.network;

import com.mojang.authlib.GameProfile;
import com.pierce.skinrestorer.PierceSkinRestorer;
import com.pierce.skinrestorer.skin.SkinManager;
import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S0CPacketSpawnPlayer;
import net.minecraft.network.play.server.S38PacketPlayerListItem;
import net.minecraft.server.MinecraftServer;
import java.lang.reflect.Field;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * Intercepts outgoing packets to inject skin data into GameProfiles.
 * This allows vanilla clients to see custom skins without any client mod.
 *
 * Compatible with Minecraft 1.7.10 / Forge 10.13.4.1614
 */
public class SkinPacketHandler {

    // Reflection fields - initialized once
    private static Field networkManagerField;      // NetHandlerPlayServer.networkManager
    private static Field channelField;             // NetworkManager.channel
    private static Field spawnPlayerProfileField;  // S0CPacketSpawnPlayer.gameProfile

    private static boolean reflectionFailed = false;
    private static boolean initialized = false;

    public static void init() {
        if (initialized) {
            return;
        }
        initialized = true;

        try {
            // Find NetworkManager field in NetHandlerPlayServer
            // SRG: field_147371_a, MCP: networkManager
            networkManagerField = findField(
                net.minecraft.network.NetHandlerPlayServer.class,
                "field_147371_a", "networkManager"
            );

            // Find channel field in NetworkManager
            // SRG: field_150746_k, MCP: channel
            channelField = findField(
                NetworkManager.class,
                "field_150746_k", "channel"
            );

            // Find GameProfile field in S0CPacketSpawnPlayer
            // In 1.7.10, S0CPacketSpawnPlayer stores player data differently
            // We need to find the field that holds GameProfile or construct one
            // SRG: field_148955_a might not exist - find by type instead
            spawnPlayerProfileField = findFieldByType(
                S0CPacketSpawnPlayer.class,
                GameProfile.class
            );

            if (spawnPlayerProfileField == null) {
                // 1.7.10 might not have a direct GameProfile field
                // We need a different approach - explained below
                PierceSkinRestorer.LOGGER.warn("No GameProfile field found in S0CPacketSpawnPlayer - using alternative approach");
            }

            PierceSkinRestorer.LOGGER.info("Packet handler initialized successfully");

        } catch (Exception e) {
            PierceSkinRestorer.LOGGER.error("Failed to initialize packet reflection", e);
            reflectionFailed = true;
        }
    }

    private static Field findField(Class<?> clazz, String... names) {
        for (String name : names) {
            try {
                Field field = clazz.getDeclaredField(name);
                field.setAccessible(true);
                PierceSkinRestorer.LOGGER.debug("Found field " + name + " in " + clazz.getSimpleName());
                return field;
            } catch (NoSuchFieldException ignored) {
            }
        }
        throw new RuntimeException("Could not find field in " + clazz.getName() + " (tried: " + String.join(", ", names) + ")");
    }

    private static Field findFieldByType(Class<?> clazz, Class<?> fieldType) {
        for (Field field : clazz.getDeclaredFields()) {
            if (field.getType() == fieldType) {
                field.setAccessible(true);
                PierceSkinRestorer.LOGGER.debug("Found " + fieldType.getSimpleName() + " field by type: " + field.getName());
                return field;
            }
        }
        return null;
    }

    /**
     * Get the Netty channel from a player's network handler.
     */
    private static Channel getChannel(EntityPlayerMP player) {
        try {
            NetworkManager networkManager = (NetworkManager) networkManagerField.get(player.playerNetServerHandler);
            return (Channel) channelField.get(networkManager);
        } catch (Exception e) {
            PierceSkinRestorer.LOGGER.error("Failed to get channel for " + player.getCommandSenderName(), e);
            return null;
        }
    }

    /**
     * Inject our channel handler into a player's network channel.
     * Called when a player joins the server.
     */
    public static void injectPlayer(EntityPlayerMP player) {
        if (reflectionFailed) {
            return;
        }

        if (player == null || player.playerNetServerHandler == null) {
            return; // Player not fully connected or already disconnected
        }

        try {
            Channel channel = getChannel(player);
            if (channel == null) {
                return;
            }

            // Remove old handler if exists
            try {
                if (channel.pipeline().get("pierceskin_handler") != null) {
                    channel.pipeline().remove("pierceskin_handler");
                }
            } catch (NoSuchElementException ignored) {
            }

            // Add our handler before the packet_handler
            channel.pipeline().addBefore("packet_handler", "pierceskin_handler",
                new SkinChannelHandler(player));

            PierceSkinRestorer.LOGGER.debug("Injected skin handler for " + player.getCommandSenderName());

        } catch (Exception e) {
            PierceSkinRestorer.LOGGER.error("Failed to inject channel handler for " + player.getCommandSenderName(), e);
        }
    }

    /**
     * Remove our channel handler when a player disconnects.
     */
    public static void removePlayer(EntityPlayerMP player) {
        if (player == null || player.playerNetServerHandler == null) {
            return; // Player not fully connected
        }
        try {
            Channel channel = getChannel(player);
            if (channel != null && channel.pipeline().get("pierceskin_handler") != null) {
                channel.pipeline().remove("pierceskin_handler");
            }
        } catch (Exception e) {
            // Ignore - player already disconnected
        }
    }

    /**
     * Channel handler that intercepts outgoing packets.
     */
    private static class SkinChannelHandler extends ChannelDuplexHandler {

        private final EntityPlayerMP viewer;

        public SkinChannelHandler(EntityPlayerMP viewer) {
            this.viewer = viewer;
        }

        @Override
        public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
            if (msg instanceof Packet) {
                msg = processPacket((Packet) msg);
            }
            super.write(ctx, msg, promise);
        }

        private Packet processPacket(Packet packet) {
            try {
                if (packet instanceof S0CPacketSpawnPlayer) {
                    return processSpawnPacket((S0CPacketSpawnPlayer) packet);
                }
                if (packet instanceof S38PacketPlayerListItem) {
                    return processListItemPacket((S38PacketPlayerListItem) packet);
                }
            } catch (Exception e) {
                PierceSkinRestorer.LOGGER.debug("Error processing packet: " + e.getMessage());
            }
            return packet;
        }

        private S0CPacketSpawnPlayer processSpawnPacket(S0CPacketSpawnPlayer packet) {
            if (spawnPlayerProfileField == null) {
                return packet;
            }
            try {
                GameProfile originalProfile = (GameProfile) spawnPlayerProfileField.get(packet);
                if (originalProfile == null) {
                    return packet;
                }
                GameProfile modifiedProfile = SkinManager.getModifiedProfile(originalProfile);
                if (modifiedProfile != null && modifiedProfile != originalProfile) {
                    spawnPlayerProfileField.set(packet, modifiedProfile);
                    PierceSkinRestorer.LOGGER.debug("Injected skin into spawn packet for " + originalProfile.getName());
                }
            } catch (Exception e) {
                PierceSkinRestorer.LOGGER.debug("Error processing spawn packet: " + e.getMessage());
            }
            return packet;
        }

        private S38PacketPlayerListItem processListItemPacket(S38PacketPlayerListItem packet) {
            try {
                Field gpField = findFieldByType(packet.getClass(), GameProfile.class);
                if (gpField != null) {
                    GameProfile original = (GameProfile) gpField.get(packet);
                    if (original != null) {
                        GameProfile modified = SkinManager.getModifiedProfile(original);
                        if (modified != null && modified != original) {
                            gpField.set(packet, modified);
                            PierceSkinRestorer.LOGGER.debug("Injected skin into S38 for " + original.getName());
                        }
                    }
                }
            } catch (Exception e) {
                PierceSkinRestorer.LOGGER.debug("Error processing S38 packet: " + e.getMessage());
            }
            return packet;
        }
    }

    /**
     * Force refresh a player's skin for all online players, INCLUDING self (self-view).
     * For others: destroy+spawn. For self: tab-list REMOVE/ADD + respawn trick.
     */
    public static void refreshPlayerSkin(final EntityPlayerMP targetPlayer) {
        if (targetPlayer == null || targetPlayer.playerNetServerHandler == null) {
            return;
        }
        MinecraftServer server = MinecraftServer.getServer();
        if (server == null) {
            return;
        }
        List<?> playerList;
        synchronized (server.getConfigurationManager().playerEntityList) {
            playerList = new java.util.ArrayList<Object>(server.getConfigurationManager().playerEntityList);
        }
        for (Object obj : playerList) {
            EntityPlayerMP viewer = (EntityPlayerMP) obj;
            if (viewer == targetPlayer) {
                continue;
            }
            if (viewer.worldObj == targetPlayer.worldObj) {
                double distance = viewer.getDistanceToEntity(targetPlayer);
                if (distance < 512) {
                    viewer.playerNetServerHandler.sendPacket(
                        new net.minecraft.network.play.server.S13PacketDestroyEntities(targetPlayer.getEntityId())
                    );
                    viewer.playerNetServerHandler.sendPacket(
                        new S0CPacketSpawnPlayer(targetPlayer)
                    );
                }
            }
        }
        try {
            int ping = 0;
            try { ping = targetPlayer.ping; } catch (Exception ignored) {}
            String name = targetPlayer.getCommandSenderName();
            S38PacketPlayerListItem removePacket = new S38PacketPlayerListItem(name, false, 9999);
            S38PacketPlayerListItem addPacket = new S38PacketPlayerListItem(name, true, ping);
            server.getConfigurationManager().sendPacketToAllPlayers(removePacket);
            server.getConfigurationManager().sendPacketToAllPlayers(addPacket);
            try {
                targetPlayer.playerNetServerHandler.sendPacket(
                    new net.minecraft.network.play.server.S07PacketRespawn(
                        targetPlayer.dimension,
                        targetPlayer.worldObj.difficultySetting,
                        targetPlayer.worldObj.getWorldInfo().getTerrainType(),
                        targetPlayer.theItemInWorldManager.getGameType()
                    )
                );
                targetPlayer.playerNetServerHandler.setPlayerLocation(
                    targetPlayer.posX, targetPlayer.posY, targetPlayer.posZ,
                    targetPlayer.rotationYaw, targetPlayer.rotationPitch
                );
            } catch (Exception e) {
                PierceSkinRestorer.LOGGER.debug("Self respawn trick failed (non-fatal): " + e.getMessage());
            }
        } catch (Exception e) {
            PierceSkinRestorer.LOGGER.debug("Self-view tab update failed: " + e.getMessage());
        }
        PierceSkinRestorer.LOGGER.info("Refreshed skin display for " + targetPlayer.getCommandSenderName() + " (including self)");
    }
}
