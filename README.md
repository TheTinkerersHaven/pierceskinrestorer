# Pierce Skin Restorer - Universal (GTNH 1.7.10)

Universal fork of [williampierce-hue/pierceskinrestorer](https://github.com/williampierce-hue/pierceskinrestorer) for GregTech: New Horizons 2.8.4 (Forge 10.13.4.1614, Java 17+). Restores skins on offline-mode servers - **others** see your skin server-only, **self** sees it with the same jar on client (+ optional [Ears](https://git.sleeping.town/exa.mods/Ears-Fifth-Edition) Fifth-Edition for full 1.8 layers).

> **Download:** https://github.com/TheTinkerersHaven/pierceskinrestorer-universal/releases
> * `server-latest` = stable server (`PierceSkinRestorer-server-latest.jar` `50K`) - only moves on server logic. Use for server auto-update without restarts.
> * `client-latest` = latest client (`PierceSkinRestorer-client-latest.jar` `54K`) - moves on every visual fix.
> * `v1.0.x` = frozen snapshots. `Latest` badge = `client-latest`.

## Features

- **Others see you** server-only via `S0CPacketSpawnPlayer` `GameProfile` injection (vanilla client, no mod)
- **You see yourself** with same jar on client: `SkinUpdatePacket` (`textureValue`+`signature`) -> injects into `thePlayer` + `Session` `GameProfile` -> `ThreadDownloadImageData` `64x64` aware
- **64x64 modern skins** fixed: `SkinImageBuffer` replicates `Ears` `6` overlay areas (`32,0-64,32` etc) + `10x` legacy `32->64` upgrade (vanilla 1.7.10 clipped to `64x32` -> rainbow half-grey/holes)
- **Outer layers** (`jacket/sleeves/pants`): `WearLayerHandler` `bipedBodyWear`/`ArmWear`/`LegWear` `+0.25F` when `Ears` not present; if [Ears Fifth-Edition](https://git.sleeping.town/exa.mods/Ears-Fifth-Edition) is present, skips built-in and lets Ears render (avoids double)
- **Ears offline spam dropped** (not moved): `NullPointerException` `Cannot invoke InputStream.close()` `Profile lookup failed` on `Ears lookup thread` for `offline` `UUID`s (`809b...`) is filtered via `System.err` `PrintStream` - logs stay in `fml-client-latest.log`, just not spammed
- **Mojang 429 retry**: `SkinFetcher` retries `3x` with `Retry-After` backoff for `api.mojang.com` + `sessionserver.mojang.com` `OVER_LIMIT`
- **Persistent** `skinrestorer/skins.json` + `GTNH` compatible, `acceptableRemoteVersions="*"` (client `1.0.16` vs server `1.0.10` skew ok)

## Installation

### Server (stable)
```bash
curl -L -o mods/PierceSkinRestorer-server-latest.jar https://github.com/TheTinkerersHaven/pierceskinrestorer-universal/releases/download/server-latest/PierceSkinRestorer-server-latest.jar
```
Restart once. Auto-update on restart with same `curl` line. No need to restart on client-only fixes.

### Client (GTNH 1.7.10 Prism)
*With Ears (recommended for full layers):* `Prism > Instance > Mods > Add` `Ears` `forge-1.7` `1.4.7+` Fifth-Edition + `PierceSkinRestorer-client-latest.jar`
*Without Ears:* just `PierceSkinRestorer-client-latest.jar` (layers via built-in `WearLayerHandler`, hat always)

Prism auto-update: `Edit > Settings > Custom Commands > Pre-launch command`:
```bash
bash -c "curl -sL -o mods/PierceSkinRestorer-client-latest.jar https://github.com/TheTinkerersHaven/pierceskinrestorer-universal/releases/download/client-latest/PierceSkinRestorer-client-latest.jar"
```

## Commands

| Command | Description |
|---------|-------------|
| `/skin set <username>` | Use any Mojang account's skin |
| `/skin clear` | Reset to Steve |
| `/skin reload` | Refresh from Mojang |
| `/skin <player> set <username>` | Admin |

## Building

```bash
git clone https://github.com/TheTinkerersHaven/pierceskinrestorer-universal.git
cd pierceskinrestorer-universal
# Requires JDK8 (for patched MC) + JDK17 toolchain, RetroFuturaGradle 1.3.33
export JAVA_HOME=/usr/lib/jvm/java-8-openjdk
./gradlew clean build
# -> build/libs/PierceSkinRestorer-1.0.x.jar (universal, 50K)
```

## Credits

- Mojang skin/profile APIs, GTNH team
- [Ears](https://git.sleeping.town/exa.mods/Ears-Fifth-Edition) / [Ears](https://github.com/unascribed/Ears) by `unascribed` (Ampflower) - MIT - inspiration for `64x64` `6` areas + legacy upgrade and wear boxes `16,32/40,32/48,48/0,32/0,48 +0.25F` (re-implemented, not verbatim)
- Original [williampierce-hue/pierceskinrestorer](https://github.com/williampierce-hue/pierceskinrestorer) MIT

## License

MIT
