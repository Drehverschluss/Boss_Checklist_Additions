<p align="center">
  <img src="src/main/resources/boss_checklist_additions.png" alt="Boss Checklist Additions" width="160">
</p>

<h1 align="center">Boss Checklist Additions</h1>

<p align="center">
  <img alt="Minecraft 1.21.1" src="https://img.shields.io/badge/Minecraft-1.21.1-5b8731">
  <img alt="NeoForge" src="https://img.shields.io/badge/NeoForge-21.1-e68c37">
  <img alt="License MIT" src="https://img.shields.io/badge/License-MIT-blue">
</p>

Unofficial addon for [Boss Checklist](https://www.curseforge.com/minecraft/mc-mods/boss-checklist) by Yori3o. It is not affiliated with Boss Checklist.

Boss Checklist shows the base values of the boss mods. In a modpack with [Dynamic Difficulty](https://www.curseforge.com/minecraft/mc-mods/dynamic-difficulty-mod)
and [Dungeon Difficulty](https://www.curseforge.com/minecraft/mc-mods/dungeon-difficulty) those numbers are wrong: a boss that is listed with 450 health can have 8,550 in your pack.
This addon replaces them with the real **solo** values of your pack, adds a **Pack Info** tab with strength, location and zone of each boss,
and brings bosses back into the list that Boss Checklist hides because of a mod id mismatch.

The addon contains no data of any specific modpack: everything comes from the mods and from your datapack or KubeJS files.

_Screenshot: health tooltip with base values (placeholder)_

_Screenshot: Pack Info tab of a boss with a fixed level (placeholder)_

_Screenshot: Pack Info tab of a boss with a level range (placeholder)_

## Installation

1. Install NeoForge 1.21.1 and [Boss Checklist](https://www.curseforge.com/minecraft/mc-mods/boss-checklist) 5.3.x.
2. Put `boss_checklist_additions-0.1.0.jar` into the `mods` folder of the **server and of every client**.
3. Optional: install Dynamic Difficulty and/or Dungeon Difficulty to get their values and zones.

| Mod | Version | Needed |
|---|---|---|
| Boss Checklist | 5.3.x (`[5.3.2,5.4)`) | required |
| Dynamic Difficulty | 1.3.x | optional |
| Dungeon Difficulty | 3.9.x | optional |

- The server calculates the values and sends them to the clients, so the addon is needed on both sides to show the real values.
- Without the addon on the server, Boss Checklist stays original and the Pack Info tab only shows a notice. A client without the addon can still join a server that has it.
- Without Dynamic Difficulty and Dungeon Difficulty only the base values are shown.

## What it does

- **Real stats:** The health and armor tooltip of the boss info screen shows the solo values, e.g. `Health: 8550 (Base 450)` or
  `Health: 2975–5600 (Base 350)` for a boss with a level range, followed by `Lvl 120 · Solo`.
- **Pack Info tab:** A green bookmark at the bottom of the right book page opens a panel with the strength of the boss
  (level, health, armor, attack, each with its base value) and its location (dimension, structure or biome, Dungeon Difficulty zone, loot level).
  The panel scrolls with the mouse wheel if it is too tall.
- **Dungeon Difficulty:** The zone of a boss is resolved from its dimension and location in the pack file (no world position needed), and the
  attribute modifiers of that zone are included in the values. Random modifiers are not rolled, their mean is used and the value is marked with `≈`.
- **Missing bosses:** Boss Checklist drops entries whose entity namespace is not a loaded mod id (Remnant Bosses uses `remnants:*` for its entities).
  The addon keeps entries whose entity exists and can hide entries whose entity does not exist.
## Configuration

Client (`boss_checklist_additions-client.toml`):

| Option | Default | |
|---|---|---|
| `hideMissingEntities` | `true` | Remove Boss Checklist entries whose entity is not registered. |
| `replaceStats` | `true` | Show the pack values instead of the base values. |
| `showPackInfoTab` | `true` | Show the Pack Info bookmark. The tooltip replacement stays. |

Server (`boss_checklist_additions-server.toml`, in the world's `serverconfig` folder):

| Option | Default | |
|---|---|---|
| `observeBaseValues` | `true` | Remember the base attributes of spawned bosses. Values that were already observed stay in use. |
| `enableDungeonDifficulty` | `true` | Include the zone and the attribute modifiers of Dungeon Difficulty. |
| `debugLogging` | `false` | Write additional information about the calculation to the log. |

Changing a server option while the game is running recalculates the stats.

## For modpack developers

### Pack files

`data/<namespace>/boss_checklist_additions/bosses/<name>.json`, in a datapack or in KubeJS (`kubejs/data/...`). One file per boss.
Only `entity` is required. If several files describe the same entity, the last one (sorted by file id) wins. Invalid files are logged with their file name and skipped.

| Field | Meaning |
|---|---|
| `entity` | Entity id, e.g. `cataclysm:ignis`. |
| `dimension` | Dimension id. Shown in the tab and needed for the Dungeon Difficulty zone. |
| `location.type` | `structure`, `biome`, `summoned` or `natural`. |
| `location.id` | Structure or biome id, for the types `structure` and `biome`. |
| `base_attributes` | Base values `max_health`, `armor`, `attack_damage`, each optional. |
| `hidden` | `true`: no data is sent for this boss, Boss Checklist stays original and there is no tab. |
| `quests` | Reserved. Accepted, but ignored. |

A boss in a structure:

```json
{
  "entity": "cataclysm:ignis",
  "dimension": "minecraft:the_nether",
  "location": { "type": "structure", "id": "cataclysm:burning_arena" }
}
```

A summoned boss whose real base values are known (they replace the values of the temporary entity):

```json
{
  "entity": "remnants:ossukage",
  "dimension": "minecraft:overworld",
  "location": { "type": "summoned" },
  "base_attributes": { "max_health": 800, "armor": 6, "attack_damage": 7 }
}
```

A boss that roams freely, and a boss without any location:

```json
{ "entity": "mowziesmobs:naga", "dimension": "minecraft:overworld", "location": { "type": "natural" } }
```

```json
{ "entity": "minecraft:wither" }
```

A boss without a pack file still gets its strength values, only the location and zone are missing.

### Base values

A boss may only set its real base attributes when it is spawned (Remnant Bosses reads them from its own config), so the temporary entity that
the addon uses for the calculation can show placeholder values. The base values are taken from, in this order:

1. `base_attributes` of the pack file
2. the values observed on a real boss: when a boss spawns, its base attributes are read one tick later and stored in the world
   (`data/boss_checklist_additions_observed.dat`). Bosses that are loaded from a chunk are ignored.
3. the temporary entity

The source is shown in the tab (`Base: Pack`, `Base: Observed`) and in `/bcadditions dump`.

### Names

- Structures: `structure.<namespace>.<path>`, the same key Explorer's Compass uses, e.g. `"structure.cataclysm.burning_arena": "Burning Arena"`.
- Dimensions: `boss_checklist_additions.dimension.<namespace>.<path>`. Overworld, Nether and End are included.
- Biomes: the vanilla key `biome.<namespace>.<path>`.
- The zone uses the translation of Dungeon Difficulty (`difficulty.type.<name>`), which a pack can override.

Without a translation the id is made readable (`the_nether` becomes `The Nether`). Add the names in `kubejs/assets/<namespace>/lang/en_us.json`.
A line that is too wide is cut with `…`, the full text is shown as a tooltip.

### Missing bosses

Entries and kill tracking stay in the files of Boss Checklist, the addon only makes sure they are not filtered out wrongly:

- `assets/boss_checklist/bosses.json`: the entries of the list, e.g. `{ "id": "remnants:ossukage", "scale": 20, "y_offset": 10, "position": 55 }`
- `assets/boss_checklist/lang/en_us.json`: `boss_checklist.boss.remnants_ossukage`, `boss_checklist.summon.remnants_ossukage`, `boss_checklist.mod.remnants`
- `data/boss_checklist/server_bosses_ids.json`: the ids for the kill tracking, e.g. `[{ "id": "remnants:ossukage" }]`

Boss Checklist only keeps an entry if the namespace of its entity id is the id of a loaded mod. The addon also keeps it if an entity type with that namespace exists.
With `hideMissingEntities`, entries whose entity does not exist (like the default entry `remnant_bosses:ossukage`) are removed.

### Commands

Permission level 2.

- `/bcadditions dump`: all calculated bosses with level range, health, armor, attack, base values, source of the base values, location and zone.
- `/bcadditions observed`: the observed base values.
- `/bcadditions forget <entity>`: deletes the observed base values of an entity type.
- `/bcadditions compare`: compares the prediction with the real values of the living entity in the crosshair (32 blocks).

### Known limits

- Bosses without their own Dynamic Difficulty settings get their level from the place where they spawn. The tab shows "depends on location" and the values of
  such a boss do not include the Dynamic Difficulty bonus of its location. `/bcadditions compare` points this out.
- Zones come from a structure or biome in the pack file. A summoned or free-roaming boss gets no structure or biome zone, only the entity entries (for example Wither or Ender Dragon) and the dimension-wide difficulty of Dungeon Difficulty apply.
- Dungeon Difficulty has random modifiers. The addon uses their mean and marks the values with `≈`.
- Dungeon Difficulty has no API. The addon uses its classes directly and rebuilds the zone resolution for a boss without a world position,
  so the supported versions are limited (3.9.x). A single player gets no per-player bonus.
- All values are solo values.

## Development

Requires Java 21. Boss Checklist, Dynamic Difficulty and Dungeon Difficulty are resolved via Maven (Modrinth, maven.muon.rip) as `compileOnly` and `localRuntime`.
Put test-only mods (Cataclysm, Remnant Bosses, Bosses of Mass Destruction, Yung's Better Ocean Monuments and their dependencies) as jars into `libs/test/`.
They are added to the dev runs only.

- `./gradlew build` (includes `checkLang`, which checks that `en_us.json` and `de_de.json` have the same keys and that every key used in the code exists)
- `./gradlew runClient`, `./gradlew runServer`

### Testing

1. Fill `libs/test/`. For the Dungeon Difficulty zones copy its config to `run/config/dungeon_difficulty/` and its structure tags into a datapack.
2. `./gradlew runServer` (accept the EULA in `run/eula.txt`), then `./gradlew runClient` and join the server.
3. Open Boss Checklist, a boss, and hover over the boss to see the tooltip. Click the green bookmark for the Pack Info tab.
4. **Observed values:** `/summon remnants:ossukage`, `/bcadditions observed` shows 800 health a second later, `/bcadditions dump` lists the source `OBSERVED`.
   `/bcadditions forget remnants:ossukage` reverts to the values of the temporary entity.
5. **Pack file:** a datapack with `data/test/boss_checklist_additions/bosses/umbrakar.json` (`base_attributes`), then `/reload`: the source is `PACK`.
   `"hidden": true` removes the boss from the dump and removes its tab.
6. **Location and zone:** pack files with `dimension` and `location` for Ignis, Elder Guardian, Wither and Ender Dragon.
   Summon a boss inside its structure, look at it and run `/bcadditions compare`.
7. **Options:** `showPackInfoTab = false` hides the bookmark, `enableDungeonDifficulty = false` removes the zones and the Dungeon Difficulty part of the values.
8. **No server data:** join a server without this addon, the tab shows a notice and Boss Checklist shows its original values.

## License and credits

MIT, see [LICENSE](LICENSE).

- [Boss Checklist](https://www.curseforge.com/minecraft/mc-mods/boss-checklist) by Yori3o is the mod this addon is built for.
- [Dynamic Difficulty](https://www.curseforge.com/minecraft/mc-mods/dynamic-difficulty-mod) by MuonR and
  [Dungeon Difficulty](https://www.curseforge.com/minecraft/mc-mods/dungeon-difficulty) by ZsoltMolnarrr provide the values and zones that are shown.
- Bug reports and suggestions: [issue tracker](https://github.com/Drehverschluss/Boss_Checklist_Additions/issues).