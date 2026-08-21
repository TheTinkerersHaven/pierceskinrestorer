package com.pierce.skinrestorer.skin;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.pierce.skinrestorer.PierceSkinRestorer;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Fetches skin data from Mojang API.
 * Returns texture property data that can be injected into GameProfiles.
 */
public class SkinFetcher {

    private static final String MOJANG_UUID_API = "https://api.mojang.com/users/profiles/minecraft/";
    private static final String MOJANG_PROFILE_API = "https://sessionserver.mojang.com/session/minecraft/profile/";

    private static final int TIMEOUT = 10000; // 10 seconds

    /**
     * Fetch skin texture property for a Minecraft username.
     *
     * @param username The Minecraft username
     * @return SkinData containing texture value and signature, or null if failed
     */
    public static SkinData fetchSkinData(String username) {
        // First, get UUID from Mojang
        String uuid = getUUIDFromUsername(username);

        if (uuid == null) {
            PierceSkinRestorer.LOGGER.warn("Could not find UUID for username: " + username);
            return null;
        }

        // Then get the full profile with textures
        return fetchProfileTextures(uuid);
    }

    /**
     * Get Mojang UUID from username.
     */
    public static String getUUIDFromUsername(String username) {
        for (int attempt = 0; attempt < 3; attempt++) {
            HttpURLConnection conn = null;
            BufferedReader reader = null;
            try {
                URL url = new URL(MOJANG_UUID_API + username);
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(TIMEOUT);
                conn.setReadTimeout(TIMEOUT);
                conn.setRequestProperty("User-Agent", "PierceSkinRestorer/1.0");

                int responseCode = conn.getResponseCode();
                if (responseCode == 429) {
                    String retry = conn.getHeaderField("Retry-After");
                    long wait = retry != null ? (Long.parseLong(retry) * 1000L) : 1500L * (attempt + 1);
                    PierceSkinRestorer.LOGGER.warn("Mojang UUID API rate limited (429) for " + username + ", retry " + (attempt+1) + " in " + wait + "ms");
                    Thread.sleep(wait);
                    continue;
                }
                if (responseCode != 200) {
                    PierceSkinRestorer.LOGGER.debug("Mojang UUID API returned " + responseCode + " for " + username);
                    return null;
                }

            reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }

            JsonParser parser = new JsonParser();
            JsonObject json = parser.parse(response.toString()).getAsJsonObject();

            if (json.has("id")) {
                String uuid = json.get("id").getAsString();
                PierceSkinRestorer.LOGGER.debug("Found UUID for " + username + ": " + uuid);
                return uuid;
            }

        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            return null;
        } catch (Exception e) {
            PierceSkinRestorer.LOGGER.warn("Error getting UUID for username: " + username + " attempt " + (attempt+1) + ": " + e.getMessage());
            if (attempt < 2) { try { Thread.sleep(1500L*(attempt+1)); } catch (InterruptedException ie2) { Thread.currentThread().interrupt(); return null; } continue; }
            PierceSkinRestorer.LOGGER.error("Error getting UUID for username: " + username, e);
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (Exception ignored) {
                }
            }
            if (conn != null) {
                conn.disconnect();
            }
        }
        }
        return null;
    }

    /**
     * Fetch profile textures from Mojang session server.
     * This returns the signed texture property that can be used in GameProfiles.
     */
    private static SkinData fetchProfileTextures(String uuid) {
        for (int attempt = 0; attempt < 3; attempt++) {
        HttpURLConnection conn = null;
        BufferedReader reader = null;
        try {
            // Request unsigned=false to get the signature
            URL url = new URL(MOJANG_PROFILE_API + uuid + "?unsigned=false");
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(TIMEOUT);
            conn.setReadTimeout(TIMEOUT);
            conn.setRequestProperty("User-Agent", "PierceSkinRestorer/1.0");

            int responseCode = conn.getResponseCode();
            if (responseCode == 429) {
                String retry = conn.getHeaderField("Retry-After");
                long wait = retry != null ? (Long.parseLong(retry) * 1000L) : 1500L * (attempt + 1);
                PierceSkinRestorer.LOGGER.warn("Mojang profile API rate limited (429) for UUID " + uuid + ", retry " + (attempt+1) + " in " + wait + "ms");
                Thread.sleep(wait);
                continue;
            }
            if (responseCode != 200) {
                PierceSkinRestorer.LOGGER.warn("Mojang profile API returned " + responseCode + " for UUID " + uuid);
                return null;
            }

            reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }

            JsonParser parser = new JsonParser();
            JsonObject json = parser.parse(response.toString()).getAsJsonObject();

            if (!json.has("properties")) {
                PierceSkinRestorer.LOGGER.warn("No properties in profile for UUID " + uuid);
                return null;
            }

            // Find textures property
            JsonArray properties = json.getAsJsonArray("properties");
            for (JsonElement prop : properties) {
                JsonObject propObj = prop.getAsJsonObject();
                if (propObj.get("name").getAsString().equals("textures")) {
                    String value = propObj.get("value").getAsString();
                    String signature = propObj.has("signature") ? propObj.get("signature").getAsString() : null;

                    SkinData data = new SkinData();
                    data.uuid = uuid;
                    data.textureValue = value;
                    data.textureSignature = signature;

                    PierceSkinRestorer.LOGGER.info("Fetched skin data for UUID " + uuid);
                    return data;
                }
            }

        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            return null;
        } catch (Exception e) {
            PierceSkinRestorer.LOGGER.warn("Error fetching profile for UUID: " + uuid + " attempt " + (attempt+1) + ": " + e.getMessage());
            if (attempt < 2) { try { Thread.sleep(1500L*(attempt+1)); } catch (InterruptedException ie2) { Thread.currentThread().interrupt(); return null; } continue; }
            PierceSkinRestorer.LOGGER.error("Error fetching profile for UUID: " + uuid, e);
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (Exception ignored) {
                }
            }
            if (conn != null) {
                conn.disconnect();
            }
        }
        }
        return null;
    }

    /**
     * Convert UUID without dashes to UUID with dashes.
     */
    public static String formatUUID(String uuid) {
        if (uuid == null || uuid.length() != 32) {
            return uuid;
        }
        return uuid.substring(0, 8) + "-" +
               uuid.substring(8, 12) + "-" +
               uuid.substring(12, 16) + "-" +
               uuid.substring(16, 20) + "-" +
               uuid.substring(20);
    }

    /**
     * Container for skin texture data.
     */
    public static class SkinData {
        public String uuid;
        public String textureValue;      // Base64 encoded texture data
        public String textureSignature;  // Mojang's signature (required for secure servers)
    }
}
