# Mod Hider

A client-side Fabric mod for Minecraft 1.21.1 that hides your installed mods from multiplayer servers.

## Download

Get `modhider-1.0.0.jar` from the [Releases](../../releases) page.

## Install

1. Install [Fabric Loader](https://fabricmc.net/use/installer/) for Minecraft 1.21.1.
2. Put `modhider-1.0.0.jar` in your mods folder:
   - Windows: `%appdata%\.minecraft\mods`
   - macOS: `~/Library/Application Support/minecraft/mods`
   - Linux: `~/.minecraft/mods`
3. Start the game with the Fabric profile. Fabric API is not required.

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
  "allowedServers": []
}
```

Add a server to `allowedServers` (for example `"play.example.com"`) to let it see your mods normally. Use this for modded servers that need your mods to talk to them.

## Limits

This hides which mods are installed. It does not hide what they do: anti-cheat that watches gameplay still works, and servers can still ban players for breaking their rules.

## Build

```
./gradlew build
```

The jar is written to `build/libs/`.
