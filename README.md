# Pierce Skin Restorer

A **server-side only** Minecraft 1.7.10 Forge mod that restores player skins on offline mode servers.

**No client mod required!** Works with vanilla clients and is compatible with GregTech: New Horizons (GTNH) 2.8.4.

## Download

**[Download Latest Release](https://github.com/williampierce-hue/pierceskinrestorer/releases/latest)**

## Features

- **Server-side only** - Players don't need to install anything
- **Works with vanilla clients** - No client mod required
- **Instant skin updates** - Other players see your new skin immediately
- **Command-based**: `/skin set <username>` to use any Minecraft account's skin
- **Persistent storage** - Skin preferences survive server restarts
- **GTNH compatible** - Tested with GregTech: New Horizons 2.8.4

## How It Works

When a player sets their skin, the server:
1. Fetches the skin texture data from Mojang's API
2. Stores the preference in a JSON file
3. Injects the skin data into player spawn packets sent to other clients
4. Other players' vanilla clients automatically display the correct skin

**Note:** You will NOT see your own skin change - only other players see your updated skin. This is a Minecraft limitation (your local client renders your own player model using local data, not server packets).

## Commands

| Command | Description |
|---------|-------------|
| `/skin set <username>` | Set your skin to any Minecraft account's skin |
| `/skin clear` | Remove your custom skin |
| `/skin reload` | Refresh your skin from Mojang |
| `/skin <player> set <username>` | (Admin) Set another player's skin |

### Examples

```
/skin set Notch          - Use Notch's skin
/skin set jeb_           - Use Jeb's skin
/skin clear              - Reset to default
```

## Installation

### Requirements
- Minecraft 1.7.10
- Minecraft Forge 10.13.4.1614 or compatible
- Java 8+ (Java 17+ recommended for GTNH 2.8.4)

### Server Installation
1. Download the latest `PierceSkinRestorer-x.x.x.jar` from [Releases](https://github.com/williampierce-hue/pierceskinrestorer/releases/latest)
2. Place in your server's `mods/` folder
3. Restart the server
4. Done! Players can now use `/skin` command

**Players do NOT need to install anything** - other players will see their skins automatically.

## Building from Source

```bash
# Clone the repository
git clone https://github.com/williampierce-hue/pierceskinrestorer.git
cd pierceskinrestorer

# Set up ForgeGradle (first time only)
./gradlew setupDecompWorkspace

# Build the mod
./gradlew build
```

The JAR will be at `build/libs/PierceSkinRestorer-x.x.x.jar`

## Configuration

Config file: `config/pierceskinrestorer.cfg`

```properties
# Timeout for fetching skins from Mojang API (seconds)
I:fetchTimeoutSeconds=10

# Require OP permission to use /skin command
B:requirePermission=false

# Enable debug logging
B:logDebug=false
```

## Data Storage

Skin preferences are stored in `skinrestorer/skins.json`:

```json
{
  "player-uuid-here": {
    "playerName": "Steve",
    "skinSource": "Notch",
    "skinType": "MOJANG_USERNAME",
    "lastUpdated": 1705766400000
  }
}
```

## GTNH Compatibility

This mod is designed to be fully compatible with GTNH 2.8.4:

- **No core mod** - Uses standard Forge events only
- **No client mod** - Server-side packet manipulation
- **No conflicts** - Doesn't touch rendering code
- **Lightweight** - Minimal memory footprint

## Limitations

- **Mojang usernames only** - The username must exist on Mojang's servers
- **No custom URL skins** - Vanilla clients only download from Mojang CDN
- **You cannot see your own skin** - Other players see your skin, but you will always see your default skin (Minecraft limitation - your client renders your own model using local data, not server packets)

## Troubleshooting

### "Failed to fetch skin" error
- The username doesn't exist or has no skin set
- Mojang API may be temporarily unavailable
- Check server logs for details

### Skin not showing for other players
- Wait for the player to respawn or rejoin
- The server may still be fetching the skin data
- Try `/skin reload`

### API Rate Limits
Mojang's API has rate limits (~200 requests/minute). If you have many players setting skins simultaneously, some requests may fail temporarily.

## Technical Details

The mod works by:
1. Intercepting `S0CPacketSpawnPlayer` packets before they're sent to other players
2. Modifying the `GameProfile` to include skin texture properties
3. Other players' vanilla clients receive the modified profile and download the skin

This approach requires no client-side code because the vanilla Minecraft client already knows how to:
- Read texture properties from GameProfiles
- Download skin textures from Mojang's CDN
- Apply skins to player models

**Why you can't see your own skin:** The spawn packet is only sent when one player appears in another player's view. Your own client never receives a spawn packet for yourself - it uses your local GameProfile directly.

## License

MIT License - Feel free to use, modify, and distribute.

## Credits

- Mojang for the skin/profile APIs
- GTNH team for the amazing modpack
- [Ears](https://github.com/unascribed/Ears) by `unascribed` (Ampflower) - MIT Licensed - inspiration for `1.8` outer layers (`bipedBodyWear`/`ArmWear`/`LegWear`) backport to `1.7.10` (`WearLayerHandler`, `SkinImageBuffer` `64x64` handling). This mod re-implements wear boxes (`16,32`/`40,32`/`48,48`/`0,32`/`0,48` `+0.25F`) based on Ears' approach, not copied verbatim.
