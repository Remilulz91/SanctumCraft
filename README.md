# SanctumCraft

> A Fabric mod for Minecraft 1.21.1 inspired by the egg hunt mode of **Aniimo**.
> Build a rune portal, raid an ancient sanctum floating in the void, find a rare egg and get out before it collapses.

[![Minecraft](https://img.shields.io/badge/Minecraft-1.21.1-brightgreen)](https://www.minecraft.net/)
[![Fabric](https://img.shields.io/badge/Loader-Fabric-orange)](https://fabricmc.net/)
[![License](https://img.shields.io/badge/License-MIT-blue)](LICENSE)
[![Status](https://img.shields.io/badge/Status-Alpha-red)](#)

---

## ✦ Concept

SanctumCraft works on a **regular survival world**. Craft Rune Stones, build a frame like a Nether portal and light it with a **Sanctum Shard**. Step in: a brand-new sanctum is generated just for you (and up to 2 friends). Rooms are hand-made templates assembled in a different order every run.

You have **30 minutes**. Leave whenever you want through the exit portal, but anyone still inside when the timer hits zero **dies**, and everything they carried is lost with the sanctum. The portal then needs to recharge before opening a new sanctum. Every portal in the world is independent: if one is recharging, go use another.

## ⚒ Crafting

| Item | Recipe |
|------|--------|
| Rune Stone ×8 | 8 Deepslate Bricks around 1 Amethyst Shard |
| Sanctum Shard | 4 Amethyst Shards around 1 Ender Pearl (plus shape) |

Portal frame: a Rune Stone rectangle, interior from 2×3 to 21×21, corners optional. Right-click the frame with a Sanctum Shard (consumed).

## 🎮 Commands

| Command | Description | Permission |
|---------|-------------|------------|
| `/sanctum status` | Time left (inside) or state of the nearest portal | Everyone |
| `/sanctum leave` | Leave the sanctum (same as the exit portal) | Everyone |
| `/sanctum list` | List portals and sanctums | OP |
| `/sanctum debug generate [normal\|nightmare\|chaos]` | Open a sanctum without portal | OP, debug build |
| `/sanctum debug close` / `closeall` | Collapse your sanctum / all sanctums | OP, debug build |
| `/sanctum debug time <seconds>` | Set the time left of your sanctum | OP, debug build |
| `/sanctum debug cooldown reset` | Reset every portal cooldown | OP, debug build |

## ⚙ Configuration

`config/sanctumcraft.properties` (restart after editing):

| Key | Default | |
|-----|---------|-|
| `raidDurationSeconds` | 1800 | Sanctum timer, Normal |
| `raidDurationNightmareSeconds` | 1500 | Sanctum timer, Nightmare |
| `raidDurationChaosSeconds` | 1200 | Sanctum timer, Chaos |
| `sanctumKeepInventory` | false | Keep inventory and XP when dying inside a sanctum only |
| `portalCooldownSeconds` | 1800 | Portal recharge after a collapse |
| `maxPlayersPerSanctum` | 3 | Different players per sanctum |
| `roomsPerSanctum` | 12 | Rooms per sanctum |
| `nestRooms` | 2 | Nest rooms |
| `roomsPlacedPerTick` | 1 | Generation speed |
| `blocksClearedPerTick` | 12000 | Cleanup speed |
| `minGenerationTicks` | 40 | Min time of the "Generating…" screen |
| `maxSimultaneousSanctums` | 16 | Sanctums open at the same time |
| `loopChancePercent` | 15 | Extra doors between rooms |
| `forceSurvival` | true | Survival forced and game mode locked inside the Sanctum (not in dev environment) |

## 🧱 Editing rooms

Rooms live in `src/main/resources/data/sanctumcraft/structure/room/` (25×10×25, a doorway in the middle of each side: x/z 11–13, y 1–4).
To retouch one in game (debug build, creative): place a structure block in **Load** mode, name `sanctumcraft:room/hall_pillars`, load it, edit, switch to **Save** mode with the same name and size, save, then copy `saves/<world>/generated/sanctumcraft/structures/room/hall_pillars.nbt` back into the mod.
`tools/gen_rooms.py` regenerates the placeholder rooms (`pip install nbtlib`).

## ⚒ Build

```bash
./gradlew build                       # public build
./gradlew build -PbuildType=debug     # debug build (debug commands)
```

Requires JDK 21. See MurderCraft's SETUP.md for the IntelliJ steps, they are identical.

## 🎯 Roadmap

- [x] 0.1: dimension, rune portal, sanctum lifecycle (generation, timer, collapse, cooldown), room assembler
- [ ] 0.2: raid loop: nests, eggs (item), extraction through the destination room
- [ ] 0.3: rarity-colored chests with progressive reveal, loot tables, keys and locked rooms, natural portal ruins
- [ ] 0.4: mobs, rift events, traps, rolling egg, difficulties (Normal / Nightmare / Chaos)
- [ ] 0.5: Sanctum Altar: levels, shells currency, shop, selling, Chaos ticket with daily reset
- [ ] 0.6: talents, incubator, companions, showcase with collector relics
- [ ] 0.7: HUD, Cloth Config screen, Modrinth CI

## 📜 License

[MIT](LICENSE).

## 🙌 Credits

- **Inspiration**: the egg hunt (Operation: Egg Heist) mode of *Aniimo* by Pawprint Studio. SanctumCraft reuses the gameplay idea only: rooms, names, creatures and assets are original.
- **Mod design & development**: Remilulz_91
