<img src="art/icon-512.png" alt="Mod Hider icon" width="128">

# Mod Hider

A client-side Fabric mod for Minecraft 1.21.5 to 1.21.11, 26.1 and 26.2 that hides your installed mods from multiplayer servers.

## Download

Pick the file that matches your Minecraft version:

| Minecraft | Download |
|---|---|
| 26.2 | [modhider-1.1.0+mc26.2.jar](https://github.com/Heroethmc/modhider/raw/main/download/modhider-1.1.0%2Bmc26.2.jar) |
| 26.1 (26.1, 26.1.1, 26.1.2) | [modhider-1.1.0+mc26.1.jar](https://github.com/Heroethmc/modhider/raw/main/download/modhider-1.1.0%2Bmc26.1.jar) |
| 1.21.11 | [modhider-1.1.0+mc1.21.11.jar](https://github.com/Heroethmc/modhider/raw/main/download/modhider-1.1.0%2Bmc1.21.11.jar) |
| 1.21.10 | [modhider-1.1.0+mc1.21.10.jar](https://github.com/Heroethmc/modhider/raw/main/download/modhider-1.1.0%2Bmc1.21.10.jar) |
| 1.21.9 | [modhider-1.1.0+mc1.21.9.jar](https://github.com/Heroethmc/modhider/raw/main/download/modhider-1.1.0%2Bmc1.21.9.jar) |
| 1.21.8 | [modhider-1.1.0+mc1.21.8.jar](https://github.com/Heroethmc/modhider/raw/main/download/modhider-1.1.0%2Bmc1.21.8.jar) |
| 1.21.7 | [modhider-1.1.0+mc1.21.7.jar](https://github.com/Heroethmc/modhider/raw/main/download/modhider-1.1.0%2Bmc1.21.7.jar) |
| 1.21.6 | [modhider-1.1.0+mc1.21.6.jar](https://github.com/Heroethmc/modhider/raw/main/download/modhider-1.1.0%2Bmc1.21.6.jar) |
| 1.21.5 | [modhider-1.1.0+mc1.21.5.jar](https://github.com/Heroethmc/modhider/raw/main/download/modhider-1.1.0%2Bmc1.21.5.jar) |
| 1.21.1 | [modhider-1.0.0+mc1.21.1.jar](https://github.com/Heroethmc/modhider/raw/main/download/modhider-1.0.0%2Bmc1.21.1.jar) |

The 1.21.1 file is an older version without the `visibleMods` setting.

## Install

1. Install [Fabric Loader](https://fabricmc.net/use/installer/) for your Minecraft version.
2. Put the jar in your mods folder:
   - Windows: `%appdata%\.minecraft\mods`
   - macOS: `~/Library/Application Support/minecraft/mods`
   - Linux: `~/.minecraft/mods`
3. Start the game with the Fabric profile. Fabric API is not required. Minecraft 26.x needs Java 25, which the official launcher provides.

## What it does on multiplayer servers

- Reports the client brand as `vanilla` instead of `fabric`.
- Stops mods from announcing their network channels, and drops mod-only packets to the server.
- Answers server login queries the way a vanilla client does.
- Resolves server-supplied sign and anvil text using only vanilla and resource-pack translations, so servers can't probe for mod translation keys or mod key bindings.

Singleplayer is never affected.

## Config

`.minecraft/config/modhider.json` is created on first launch:

```json
{
  "spoofBrand": true,
  "hideChannels": true,
  "blockTranslationProbes": true,
  "visibleMods": ["voicechat"],
  "allowedServers": []
}
```

`visibleMods` lists mods every server may see and talk to. It contains [Simple Voice Chat](https://modrinth.com/plugin/simple-voice-chat) (`voicechat`) by default, so voice chat keeps working and the server can tell you have it. Add other mod ids to show those mods too.

Add a server to `allowedServers` (for example `"play.example.com"`) to let it see your mods normally. Use this for modded servers that need your mods to talk to them.

## Limits

This hides which mods are installed. It does not hide what they do: anti-cheat that watches gameplay still works, and servers can still ban players for breaking their rules.

## Build

Minecraft 1.21.x (Yarn names, Java 21):

```
./gradlew build -Pmc=1.21.8
```

Minecraft 26.x (Mojang names, needs a JDK 25) lives in `mc26/`:

```
cd mc26
./gradlew build -Pmc=26.2
```

Jars are written to `build/libs/`. Version-specific settings are in `versions/*.properties`.
