# Creeperborn — 0.1.0-alpha.2

An original Sibcraft mod: hunt rare elemental creepers, consume their hearts, master four elements, and empower villagers to defend your settlements. This is a separate mod from Night Splitters. It does not contain, download, or require an Avatar/One Piece mod, copied abilities, or third-party assets.

## First alpha scope

Minecraft **26.3**, Java **25**, Fabric Loader **0.19.5+**, Fabric API **0.161.0+26.3**. Install the JAR on the server or in singleplayer. Install the same JAR on a Fabric client to get the optional combat keys. Vanilla clients can still connect and use focus/commands. No resource pack is required. Test in a copied world. This prototype is not yet published to Modrinth.

- **2%** of unexamined vanilla creepers become a random Earth, Water, Fire, or Air creeper when loaded. Each creeper is checked only once; unloading/reloading does not reroll it. Existing unexamined creepers can also convert. Night Splitters' marked creepers are excluded.
- Elemental creepers have visible names/particles and a short elemental attack every six seconds against their vanilla target within ten blocks. They retain normal creeper explosions. Hearts drop once only when a player kills one before it explodes.
- Right-click a marked heart to consume it and permanently unlock that element's basic move. Duplicate hearts are not consumed, so they can be shared. Unlocks and mastery counters survive logout and player respawn.
- The first heart grants an **Elemental Focus** (named stick). Collecting all four grants one **Elemental Trident**. Vanilla items with matching names do not grant powers.
- Right-click the focus to cast. Sneak-right-click it or the trident to cycle unlocked elements. `/creeperborn element earth|water|fire|air` selects directly. `/creeperborn move 1|2|3` selects a move. `/creeperborn cast` is an alternative to focus use.
- All player attacks target hostile mobs only. No PvP, friendly fire, terrain removal, placed fire, or permanent barriers in this alpha.

## Combat controls and mastery

Press **B** to toggle elemental combat (starts OFF). **Z/X/C/V** select Earth/Water/Fire/Air; **R/G/H** cast moves 1/2/3. Select an element then press an ability key, or hold a selector while pressing the ability. Normal movement, jumping, sprinting, weapon attacks, and offhand swap remain available. Hold **G** with Air selected to repeat Air Step. All eight bindings can be changed in Options → Controls → Key Binds → Creeperborn Combat; resolve conflicts with other mods there. Keys are ignored while menus/chat are open. The mode resets OFF after disconnecting.

Combat keys send a small custom packet to the server, not chat commands. The server validates element unlocks, mastery, cooldowns, and packet bounds. You do not need to hold a focus when using combat keys, so you can keep a sword/shield equipped. Focus/command activation remains available without the client component.

Each element progresses separately. A heart grants **tier 1 / move 1**. **Move 2** unlocks at **10 meaningful uses AND 5 active minutes** with that element selected. **Move 3** unlocks at **40 uses AND 20 active minutes**. Active time counts while moving or shortly after a meaningful cast; standing AFK does not accumulate it. Hostile hits, successful rock blocks, and actual healing count as meaningful use, at most once per second per element. Empty casts and Air Step do not grant use credit. Repeated Air Step can count as active movement, but cannot satisfy the combat-use requirement on its own. `/creeperborn status` shows selected-element uses, active seconds, mastery tier, and current environmental power.

For testing only, operators can run `/creeperborn mastery earth 3` (or water/fire/air) after consuming that element's heart. This bypasses the waiting period for that element; use tier `1` to reset its mastery. `/creeperborn use earth 1` selects and casts a specific unlocked move in one command.

## Elemental Harmony

Reach tier 3 in **all four** elements to automatically gain **+15% direct ability damage, healing, and shield duration**. Collecting four hearts alone does not qualify. The bonus multiplies the existing environmental power, so biome strengths and weaknesses remain. It does not stack per element, boost villager/creeper damage, extend burn duration, reduce cooldowns, change knockback/range, or alter Air Step. Shield durations round to whole game ticks.

Use `/creeperborn status` to see whether Harmony is ON. For a quick comparison, consume all four hearts and set all four masteries to 3 with the operator testing commands. Set one element back to tier 2 to remove the bonus, then repeat the same move in the same environment. At full Water power, Mending Flow heals 4 health normally and 4.6 with Harmony; Stone Guard lasts 5 seconds normally and 5.75 with Harmony.

## Move list

Cooldowns are independent per player, element and move; switching elements does not reset them. **Air Step has no cooldown or use limit.** One damage point is half a heart. Listed damage/distances/durations are at full environmental strength.

| Element | Move 1 | Move 2 | Move 3 |
| --- | --- | --- | --- |
| Earth | **Earth Bolt:** throws a visible 1.5-block stone boulder, up to roughly 30 blocks, 8 damage and brief Slowness; 2s cooldown. | **Stone Guard:** three floating rocks block entity-sourced hits in your forward 120-degree arc for 5s; sides/rear remain exposed; 10s cooldown. | **Quake:** 5-block hostile-only pulse, 5 damage, Slowness and up to 10 blocks of outward knockback; 8s cooldown. |
| Water | **Water Whip:** 16-block aimed strike, 6 damage and brief Slowness; 2s cooldown. | **Tidal Guard:** a full 360-degree pulse within 5 blocks, pushes enemies up to 5 blocks away, slows them, and grants Resistance I for 4s; 8s cooldown. | **Mending Flow:** restores 4 health and extinguishes the caster; 12s cooldown. |
| Fire | **Fire Lance:** 16-block aimed strike, 6 damage and 2s burning; 2s cooldown. | **Flame Ring:** 4-block hostile-only pulse, 5 damage and 2s burning; 8s cooldown. | **Heat Shield:** Fire Resistance for 8s and Resistance I for 4s; 10s cooldown. |
| Air | **Air Gust:** 16-block aimed strike, 2 damage and strong knockback; 2s cooldown. | **Air Step:** repeatable forward/upward impulse for sky-walking, native protection for the added jump height; no cooldown, charge or use limit. | **Vortex:** 6-block hostile-only pulse, 2 damage and inward pull; 8s cooldown. |

## Environmental strength

Every unlocked element remains usable outside its preferred environment, with reduced power rather than a hard lock. Earth is full strength on solid ground and 55% while airborne/over water. Water is full strength in/near water or with a carried water bucket, otherwise 55%. Nearby water/lava checks cover four blocks horizontally and two vertically, and include flowing fluids. Fire is full strength in the Nether, in hot biomes (base temperature at least 1), or near lava; otherwise 70%, 45% in rain, and 35% in water. Air is full strength while airborne, 75% on the ground, and 55% in water. Damage, powered knockback, and defensive/healing duration or amount scale accordingly; cooldowns remain fixed.

The bucket is not consumed and fluid blocks are not removed. Boulders are temporary block-display projectiles, not placed terrain. Stone Guard is a directional damage shield, not a permanent wall or global resistance effect. Quake/Tidal pushes use normal entity movement, stop at obstacles, and can travel less than their maximum distance. Targeted moves stop at solid blocks; pulses require line of sight. Air Step does not make the player immune to later falls below their launch elevation.

## Villager factions and guard posts

With all four elements unlocked, use the Elemental Trident on an **adult villager** to assign the selected Earth/Water/Fire/Air faction. A villager's trades, profession, home, and work memories remain intact. Different factions are allies. The first player to empower a villager owns it; another player cannot reassign it.

Guardians use a ranged elemental strike (4 damage, 3s cooldown, line of sight, 12-block detection). Earth/Water slow, Fire burns, Air pushes. Their colored effects identify their element. They defend while keeping vanilla home/work/bed routines. When idle and over 16 blocks from their post, they periodically navigate back toward it. Brain schedules still run: this is an initial guard-post tether, not a guaranteed patrol or a replacement for beds/workstations. Put homes and jobs near the area you want defended.

- `/creeperborn guardpost`: stand within six blocks of your guardian to set its post at your current position; the nearest visible owned guardian is selected.
- `/creeperborn release`: release the nearest visible owned guardian within six blocks.
- Reuse the trident to change your guardian's element. Reassignment resets its post to its current position.
- Ownership, element, and post are stored on the villager and survive normal saves/reloads. No chunk forcing or offline simulation.

## Commands and testing

Ordinary players can use status, element, move, cast, focus, guardpost and release. Test item/mob commands require operator permission.

```
/creeperborn status
/creeperborn testcreeper earth
/creeperborn testcreeper water
/creeperborn testcreeper fire
/creeperborn testcreeper air
/creeperborn testheart earth
/creeperborn testheart water
/creeperborn testheart fire
/creeperborn testheart air
/creeperborn move 1
/creeperborn cast
/creeperborn focus
```

Test creepers spawn five blocks east if there is collision-free space. Their explosions can damage terrain: use a disposable test area. `/focus` consumes one plain stick to replace a lost focus if an element is unlocked and no focus is already in your inventory. The progression trident is granted once; protect it like other valuable gear.

Configuration: `config/creeperborn.json`, `naturalCreepers` (true/false) and `elementalChance` (0–1, default 0.02). Restart after editing. Names/particle visuals use vanilla assets. The public Minecraft display is English for this first prototype.

## Validation plan

GitHub Actions builds with Java 25, runs a real Fabric dedicated-server test mod, and uploads a JAR only if the regression marker passes. The test mod is excluded from the distributed JAR. Automated checks cover genuine/duplicate hearts, all-four progression, water-source requirements, all twelve moves and their cooldowns, wall blocking, villager ownership/trades/defense, one-time heart drops, natural conversion, reroll prevention, and Night Splitters creeper exclusion. In-game testing is still needed for every move's feel, first-person input, particles, save/rejoin/respawn, villager schedules, and multiplayer performance.

## Later milestones

Custom models/UI, richer villager patrol behavior, faction banners, terrain-based earth walls, water projectiles, and original fruit-inspired abilities are later work. Avatar and One Piece are inspirations only; Creeperborn is not affiliated with their owners. No external mod code/assets are bundled.
