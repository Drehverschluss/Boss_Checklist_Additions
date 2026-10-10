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
2. Put `boss_checklist_additions-0.2.0.jar` into the `mods` folder of the **server and of every client**.
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
- **Real drops:** The Drops tab of the boss info screen shows the real drops of the boss's actual loot table instead of Boss Checklist's static,
  hand-maintained list, each with the chance it was seen in a simulated kill (solo, no Looting), e.g. `73%` or `100%x2`. If the simulation finds no
  drops at all, Boss Checklist's own list is kept, since that can also mean the simulation could not run for this boss.

## Configuration

Client (`boss_checklist_additions-client.toml`):

| Option | Default | |
|---|---|---|
| `hideMissingEntities` | `true` | Remove Boss Checklist entries whose entity is not registered. |
| `replaceStats` | `true` | Show the pack values instead of the base values. |
| `showPackInfoTab` | `true` | Show the Pack Info bookmark. The tooltip replacement stays. |
| `replaceDrops` | `true` | Show the real, simulated loot table drops instead of Boss Checklist's own drop list. |

Server (`boss_checklist_additions-server.toml`, in the world's `serverconfig` folder):

| Option | Default | |
|---|---|---|
| `observeBaseValues` | `true` | Remember the base attributes of spawned bosses. Values that were already observed stay in use. |
| `enableDungeonDifficulty` | `true` | Include the zone and the attribute modifiers of Dungeon Difficulty. |
| `debugLogging` | `false` | Write additional information about the calculation to the log. |
| `cacheDrops` | `true` | Save simulated drops to the world and reuse them while mods, datapacks and loot tables stay the same. `false` always simulates again. |

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
| `loot_tables` | Simulate exactly these loot table ids instead of the entity's own one (see [Real drops](#real-drops)). A single id or a list. |
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

A boss that fills a reward chest on death instead of dropping items itself, so its own entity has no loot table at all
(Bosses of Mass Destruction's Obsidilith hands out `bosses_of_mass_destruction:chests/obsidilith`):

```json
{ "entity": "bosses_of_mass_destruction:obsidilith", "loot_tables": ["bosses_of_mass_destruction:chests/obsidilith"] }
```

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

### Real drops

The Drops tab of Boss Checklist shows a static list that has to be maintained by hand (`assets/boss_checklist/bosses.json`, or the in-game editor's
`config/boss_checklist/override/bosses.json`) and is not connected to the actual loot tables, so it can be wrong or outdated for a modpack.

Instead, the addon resolves the loot table of the entity instance (`entity.getLootTable()`, falling back to a pack file's `loot_tables` override if
set) and runs it against a temporary, never-spawned entity, as if it were killed by a plain player without Looting (the same "solo, no bonus"
baseline the rest of the addon uses). No items are ever created in the world, and the simulation uses its own random source, so the world's loot
`random_sequence` state (used e.g. for "once per reset" rewards) is never touched or advanced by it. The chance shown for each item is the share of
those runs that dropped it at least once; `x2` etc. is shown if the amount is always the same and more than 1. This also picks up loot added by
datapacks or KubeJS, Global Loot Modifiers, and conditions such as "only if killed by a player". If Dynamic Difficulty gives the boss a level range,
the runs are spread across it (covering the low and high end early, not just linearly), so drops that only unlock from a certain level onward (e.g.
Dynamic Difficulty's own level-based loot) show up with a realistic, level-weighted chance instead of never appearing at all.

It runs 100 to 300 times: once 100 runs are done, it stops early as soon as the result looks stable (no new item and no chance shifted by more than
2 percentage points over the last 50 runs), otherwise it keeps going up to 300. The result is then saved to the world and reused on the next start or
`/reload` as long as the loaded mods, the selected datapacks and the boss's actual loot table(s) did not change, so a modpack normally only pays the
simulation cost once. A config file of some *other* mod (e.g. an RPG class mod's own loot pools) cannot be detected reliably; after changing one of
those, run `/bcadditions drops refresh` to force everything to be simulated again. Setting `cacheDrops = false` disables saving/reusing results
entirely. How much time the simulation spends per tick adjusts itself to how busy the server currently is (more budget when the server is idle,
less under load), so it should not make an already laggy server noticeably laggier; in singleplayer it also pauses while the game is paused.

This is a simulation, not an exact probability: conditions that only make sense for a real kill (e.g. the exact damage type) fall back to a plain
player attack, drops that depend on the actual player (e.g. Apotheosis's gems scaling with a world tier) are simulated with a default player and are
only a rough estimate, and a few modded bosses may build their loot table differently per instance instead of from a fixed loot table id. Some drops
are handed out directly in a mod's code rather than through a loot table at all (e.g. the Wither's Nether Star) - the simulation can never see those,
so Boss Checklist's own entries for them are kept (merged in after the simulated ones) with a `?` label instead of being lost. If the simulation
finds no loot table at all for a boss, Boss Checklist's own list is left alone instead of being replaced with an empty one; a pack file's
`loot_tables` field can point the simulation at the right table(s) in that case (see [Pack files](#pack-files)).

The simulation runs one trial at a time, spread across server ticks within a small time budget per tick, and never all bosses or all runs of one
boss at once. This means the world always loads and plays normally right away, even in a huge modpack with many bosses or a boss whose loot table
happens to be unusually expensive to evaluate; the Drops tab of bosses farther back in the list just keeps Boss Checklist's own list a little longer
until their simulation finishes in the background (seconds, rarely longer, and normally nothing at all once results are cached). The tab updates
live once it does, without needing to reopen it.

### Commands

Permission level 2.

- `/bcadditions dump`: all calculated bosses with level range, health, armor, attack, base values, source of the base values, location and zone.
- `/bcadditions drops`: all simulated boss drops with their chance and amount.
- `/bcadditions drops refresh`: discards the saved drops and simulates every boss again.
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
- The simulated drop chances are an approximation from 100–300 runs, not calculated probabilities, and assume a solo kill without Looting.
- Drops a mod hands out from its own code instead of through a loot table (e.g. the Wither's Nether Star, or the `endrem:blossom_eye` Endrem Additions
  adds to `bosses_of_mass_destruction:void_blossom`) cannot be simulated; Boss Checklist's own entry for them is kept, with a `?` label.
- Drops that depend on the player (e.g. Apotheosis gems scaling with a world tier) are simulated with a default player and are only a rough estimate.
- Very rare drops (below about 0.3%) can be missing purely by chance.
- A boss with no loot table at all (only code-given drops) is not simulated; Boss Checklist's own list is used, unless a pack file's `loot_tables`
  points the simulation at the table(s) it actually fills.
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
9. **Drops:** click the Drop tab of a boss, `/bcadditions drops` lists the simulated chances in the log/chat. `replaceDrops = false` shows Boss Checklist's
   original list again.
10. **Code-given drops:** open the Wither's Drop tab - the simulated drops are shown together with the Nether Star, labeled `?`.
11. **Dynamic Difficulty drops:** for a boss with a level range (e.g. Ignis), `/bcadditions drops` should also list its level-gated items (small chance).
12. **Live update:** load the world, open the boss book right away and leave a boss further down the Drops tab open; the list updates by itself,
    without reopening it, once that boss's simulation finishes.
13. **Reload:** `/reload` should not error, and the drops come back after a short while.
14. **No addon / old addon client:** a dedicated server on 0.2.0 with a client that has no addon, or still has 0.1.0 (different protocol version),
    joins and stays connected through a `/reload`: no error, no crash, that client just sees Boss Checklist's original values.
15. **No loot table:** Ossukage (a sword that is only given in code, no loot table) keeps Boss Checklist's original list unchanged.
16. **Pack file override:** a pack file for Obsidilith with `"loot_tables": ["bosses_of_mass_destruction:chests/obsidilith"]`, then `/reload`:
    `/bcadditions drops` lists Obsidian Heart at 100% plus the End city chest loot, and the tab shows them together with any of Boss Checklist's own
    entries that are missing from the simulation, labeled `?`. A typo in the id instead logs one warning and leaves Obsidilith's original list alone.
17. **Caching:** join the world, let the simulation finish (`debugLogging = true` shows a `ms/trial` line per boss in the log), stop the server and
    start it again: the log should show bosses coming from the cache and the whole simulation finishing almost instantly.
18. **Cache invalidation:** change a file under `data/lootintegrations/` or a loot table in a datapack, `/reload`: only the affected bosses are
    simulated again, the rest stay cached. `/bcadditions drops refresh` forces every boss to be simulated again regardless.
19. **No cache:** with `cacheDrops = false`, no file is written and every start/reload simulates everything again.
20. **Load under pressure:** with many entities loaded, the simulation should not noticeably raise the server's MSPT.

## License and credits

MIT, see [LICENSE](LICENSE).

- [Boss Checklist](https://www.curseforge.com/minecraft/mc-mods/boss-checklist) by Yori3o is the mod this addon is built for.
- [Dynamic Difficulty](https://www.curseforge.com/minecraft/mc-mods/dynamic-difficulty-mod) by MuonR and
  [Dungeon Difficulty](https://www.curseforge.com/minecraft/mc-mods/dungeon-difficulty) by ZsoltMolnarrr provide the values and zones that are shown.
- Bug reports and suggestions: [issue tracker](https://github.com/Drehverschluss/Boss_Checklist_Additions/issues).