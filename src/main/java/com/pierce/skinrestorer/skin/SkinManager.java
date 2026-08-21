package com.pierce.skinrestorer.skin;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.pierce.skinrestorer.PierceSkinRestorer;
import com.pierce.skinrestorer.network.NetworkHandler;
import com.pierce.skinrestorer.network.SkinPacketHandler;
import com.pierce.skinrestorer.network.SkinUpdatePacket;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Main skin management class for server-side operation.
 * Coordinates between fetching, storage, and GameProfile modification.
 */
public class SkinManager {

    // Cache of modified GameProfiles (player UUID -> modified profile with skin)
    private static final Map<String, GameProfile> profileCache = new ConcurrentHashMap<String, GameProfile>();

    // Cache of fetched skin data (player UUID -> skin data)
    private static final Map<String, SkinFetcher.SkinData> skinDataCache = new ConcurrentHashMap<String, SkinFetcher.SkinData>();

    /**
     * Set a player's skin by Minecraft username.
     *
     * @param player The player whose skin to set
     * @param skinUsername The Minecraft username to fetch skin from
     * @return true if successful
     */
    public static boolean setSkinByUsername(EntityPlayerMP player, String skinUsername) {
        String playerUUID = player.getUniqueID().toString();
        String playerName = player.getCommandSenderName();

        PierceSkinRestorer.LOGGER.info("Setting skin for " + playerName + " to " + skinUsername);

        // Fetch skin data from Mojang
        SkinFetcher.SkinData skinData = SkinFetcher.fetchSkinData(skinUsername);

        if (skinData == null) {
            PierceSkinRestorer.LOGGER.warn("Failed to fetch skin data for " + skinUsername);
            return false;
        }

        // Store in persistent storage
        SkinStorage.setSkin(playerUUID, playerName, skinUsername, SkinStorage.SkinType.MOJANG_USERNAME);

        // Cache the skin data
        skinDataCache.put(playerUUID, skinData);

        // Apply skin to player's actual GameProfile (so they see their own skin)
        applySkinToProfile(player.getGameProfile(), skinData);

        // Create and cache modified profile for packet interception
        GameProfile modifiedProfile = createModifiedProfile(player.getGameProfile(), skinData);
        profileCache.put(playerUUID, modifiedProfile);

        // Refresh skin for all viewers
        SkinPacketHandler.refreshPlayerSkin(player);
        // Companion: push texture to self client for self-view (offline name != premium)
        try { NetworkHandler.INSTANCE.sendTo(new SkinUpdatePacket(skinData.textureValue, skinData.textureSignature, false), player); } catch (Exception e) { PierceSkinRestorer.LOGGER.debug("Failed to send self skin packet: "+e.getMessage()); }

        PierceSkinRestorer.LOGGER.info("Successfully set skin for " + playerName + " to " + skinUsername);
        return true;
    }

    /**
     * Clear a player's custom skin.
     *
     * @param player The player whose skin to clear
     */
    public static void clearSkin(EntityPlayerMP player) {
        String playerUUID = player.getUniqueID().toString();

        PierceSkinRestorer.LOGGER.info("Clearing skin for " + player.getCommandSenderName());

        // Remove from storage and caches
        SkinStorage.removeSkin(playerUUID);
        skinDataCache.remove(playerUUID);
        profileCache.remove(playerUUID);

        // Clear skin from player's actual GameProfile
        clearSkinFromProfile(player.getGameProfile());

        // Refresh skin for all viewers (will now show default)
        SkinPacketHandler.refreshPlayerSkin(player);
        try { NetworkHandler.INSTANCE.sendTo(new SkinUpdatePacket(null, null, true), player); } catch (Exception e) { PierceSkinRestorer.LOGGER.debug("Failed to send clear packet: "+e.getMessage()); }
    }

    /**
     * Reload a player's skin from stored data.
     *
     * @param player The player whose skin to reload
     * @return true if a skin was found and reloaded
     */
    public static boolean reloadSkin(EntityPlayerMP player) {
        String playerUUID = player.getUniqueID().toString();
        SkinStorage.SkinData storedData = SkinStorage.getSkin(playerUUID);

        if (storedData == null) {
            return false;
        }

        // Re-fetch the skin
        SkinFetcher.SkinData skinData = SkinFetcher.fetchSkinData(storedData.skinSource);

        if (skinData == null) {
            return false;
        }

        // Update caches
        skinDataCache.put(playerUUID, skinData);

        // Apply skin to player's actual GameProfile
        applySkinToProfile(player.getGameProfile(), skinData);

        GameProfile modifiedProfile = createModifiedProfile(player.getGameProfile(), skinData);
        profileCache.put(playerUUID, modifiedProfile);

        // Refresh for viewers
        SkinPacketHandler.refreshPlayerSkin(player);
        try { NetworkHandler.INSTANCE.sendTo(new SkinUpdatePacket(skinData.textureValue, skinData.textureSignature, false), player); } catch (Exception e) { PierceSkinRestorer.LOGGER.debug("Failed to send reload packet: "+e.getMessage()); }

        return true;
    }

    /**
     * Called when a player joins the server.
     * Loads their stored skin if available.
     *
     * @param player The player who joined
     */
    public static void onPlayerJoin(final EntityPlayerMP player) {
        final String playerUUID = player.getUniqueID().toString();
        final String playerName = player.getCommandSenderName();

        PierceSkinRestorer.LOGGER.info("Player joined: " + playerName);

        // Inject packet handler for this player first
        SkinPacketHandler.injectPlayer(player);

        // Check if this player has a stored skin
        SkinStorage.SkinData storedData = SkinStorage.getSkin(playerUUID);

        if (storedData != null) {
            PierceSkinRestorer.LOGGER.info("Loading stored skin for " + playerName + ": " + storedData.skinSource);

            // Check if we have cached skin data
            SkinFetcher.SkinData skinData = skinDataCache.get(playerUUID);

            if (skinData == null) {
                // Need to fetch in background
                final String skinSource = storedData.skinSource;
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        SkinFetcher.SkinData fetchedData = SkinFetcher.fetchSkinData(skinSource);
                        if (fetchedData != null) {
                            skinDataCache.put(playerUUID, fetchedData);

                            // Apply skin to player's actual GameProfile
                            applySkinToProfile(player.getGameProfile(), fetchedData);

                            GameProfile modifiedProfile = createModifiedProfile(player.getGameProfile(), fetchedData);
                            profileCache.put(playerUUID, modifiedProfile);

                            // Refresh skin - this sends packets which is safe from any thread
                            SkinPacketHandler.refreshPlayerSkin(player);
                            try { NetworkHandler.INSTANCE.sendTo(new SkinUpdatePacket(fetchedData.textureValue, fetchedData.textureSignature, false), player); } catch (Exception e) { PierceSkinRestorer.LOGGER.debug("Failed to send join skin packet: "+e.getMessage()); }
                        }
                    }
                }, "SkinFetch-Join-" + playerName).start();
            } else {
                // Already have cached data - apply to player's actual GameProfile
                applySkinToProfile(player.getGameProfile(), skinData);

                GameProfile modifiedProfile = createModifiedProfile(player.getGameProfile(), skinData);
                profileCache.put(playerUUID, modifiedProfile);
            }
        }
    }

    /**
     * Called when a player leaves the server.
     */
    public static void onPlayerLeave(EntityPlayerMP player) {
        SkinPacketHandler.removePlayer(player);
    }

    /**
     * Get a modified GameProfile with skin data injected.
     * Called by the packet handler when sending spawn packets.
     *
     * @param originalProfile The original profile
     * @return Modified profile with skin, or original if no custom skin
     */
    public static GameProfile getModifiedProfile(GameProfile originalProfile) {
        if (originalProfile == null) {
            return null;
        }

        String uuid = originalProfile.getId().toString();

        // Check if we have a cached modified profile
        GameProfile cachedProfile = profileCache.get(uuid);
        if (cachedProfile != null) {
            return cachedProfile;
        }

        // Check if we have skin data but no profile yet
        SkinFetcher.SkinData skinData = skinDataCache.get(uuid);
        if (skinData != null) {
            GameProfile modified = createModifiedProfile(originalProfile, skinData);
            profileCache.put(uuid, modified);
            return modified;
        }

        // No custom skin for this player
        return originalProfile;
    }

    /**
     * Apply skin data directly to a player's GameProfile.
     * This modifies the profile in-place so the player sees their own skin.
     */
    private static void applySkinToProfile(GameProfile profile, SkinFetcher.SkinData skinData) {
        // Remove existing textures property
        profile.getProperties().removeAll("textures");

        // Add new skin texture property
        if (skinData.textureSignature != null) {
            profile.getProperties().put("textures",
                new Property("textures", skinData.textureValue, skinData.textureSignature));
        } else {
            profile.getProperties().put("textures",
                new Property("textures", skinData.textureValue));
        }

        PierceSkinRestorer.LOGGER.debug("Applied skin to GameProfile for " + profile.getName());
    }

    /**
     * Clear skin data from a player's GameProfile.
     * This removes the custom texture property.
     */
    private static void clearSkinFromProfile(GameProfile profile) {
        profile.getProperties().removeAll("textures");
        PierceSkinRestorer.LOGGER.debug("Cleared skin from GameProfile for " + profile.getName());
    }

    /**
     * Create a new GameProfile with skin texture injected.
     */
    private static GameProfile createModifiedProfile(GameProfile original, SkinFetcher.SkinData skinData) {
        // Create new profile with same ID and name
        GameProfile newProfile = new GameProfile(original.getId(), original.getName());

        // Add our skin texture property
        // Note: We only set textures property - this is all that's needed for skin display
        // Avoiding putAll() due to Guava classloader conflicts in GTNH
        if (skinData.textureSignature != null) {
            newProfile.getProperties().put("textures",
                new Property("textures", skinData.textureValue, skinData.textureSignature));
        } else {
            newProfile.getProperties().put("textures",
                new Property("textures", skinData.textureValue));
        }

        return newProfile;
    }

    /**
     * Check if a player has a custom skin set.
     */
    public static boolean hasCustomSkin(String playerUUID) {
        return SkinStorage.hasSkin(playerUUID);
    }
}
