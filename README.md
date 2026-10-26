# Creeperborn — 0.1.0-alpha.6

An original Sibcraft mod: hunt rare elemental creepers, consume their hearts, master four elements, and empower villagers to defend your settlements. This is a separate mod from Night Splitters. It does not contain, download, or require an Avatar/One Piece mod, copied abilities, or third-party assets.

## First alpha scope

Minecraft **26.3**, Java **25**, Fabric Loader **0.19.5+**, Fabric API **0.161.0+26.3**. Install the JAR on the server or in singleplayer. Install the same JAR on a Fabric client to get the optional combat keys. Vanilla clients can still connect and use focus/commands. No resource pack is required. Test in a copied world. This prototype is not yet published to Modrinth.

- **2%** of unexamined vanilla creepers become a random Earth, Water, Fire, or Air creeper when loaded. Each creeper is checked only once; unloading/reloading does not reroll it. Existing unexamined creepers can also convert. Night Splitters' marked creepers are excluded.
- Elemental creepers have visible names/particles and a short elemental attack every six seconds against their vanilla target within ten blocks. They retain normal creeper explosions. Hearts drop once only when a player kills one before it explodes.
- Right-click a marked heart to consume it and permanently unlock that element's basic move. Duplicate hearts are not consumed, so they can be shared. Element unlocks and mastery counters survive logout and player respawn.
- The first heart grants an **Elemental Focus** (named stick). Collecting all four grants one **Elemental Trident**. Vanilla items with matching names do not grant powers.
- Right-click the focus to cast. Sneak-right-click it or the trident to cycle unlocked elements. `/creeperborn element earth|water|fire|air` selects directly. `/creeperborn move 1|2|3` selects a move. `/creeperborn cast` is an alternative to focus use.
- All player attacks target hostile mobs only. No PvP, friendly fire, terrain removal, placed fire, or permanent barriers in this alpha.

## Combat controls and mastery

Press **B** to toggle elemental combat (starts OFF). **R** cycles through unlocked elements in Earth → Water → Fire → Air order; **Shift + R** cycles backward. **Z/X/C** cast moves 1/2/3 for the selected element. The server selects the element and shows its name and move list onscreen when cycling. Normal movement, jumping, sprinting, weapon attacks, and offhand swap remain available. Hold **X** with Air selected to repeat Air Step. **V** is free for voice chat. All five bindings can be changed in Options → Controls → Key Binds → Creeperborn Combat; resolve conflicts with other mods there. Keys are ignored while menus/chat are open. The mode resets OFF after disconnecting.

Combat keys send a small custom packet to the server, not chat commands. The server validates element unlocks, mastery, cooldowns, and packet bounds. You do not need to hold a focus when using combat keys, so you can keep a sword/shield equipped. Focus/command activation remains available without the client component.

Each element progresses separately. A heart grants **tier 1 / move 1**. **Move 2** unlocks at **10 meaningful uses AND 5 active minutes** with that element selected. **Move 3** unlocks at **40 uses AND 20 active minutes**. Active time counts while moving or shortly after a meaningful cast; standing AFK does not accumulate it. Hostile hits, successful rock blocks, and actual healing count as meaningful use, at most once per second per element. Empty casts and Air Step do not grant use credit. Repeated Air Step can count as active movement, but cannot satisfy the combat-use requirement on its own. `/creeperborn status` shows selected-element uses, active seconds, mastery tier, and current environmental power.

For testing only, operators can run `/creeperborn mastery earth 3` (or water/fire/air) after consuming that element's heart. This bypasses the waiting period for that element; use tier `1` to reset its mastery. `/creeperborn use earth 1` selects and casts a specific unlocked move in one command.

This updated alpha.2 replaces the earlier alpha.2 JAR and includes both the revised controls and Elemental Harmony. Existing saved move bindings may retain their old keys: reset the Creeperborn bindings in Controls to apply the new defaults.

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

## Rubber core — alpha.3

Rubber is independent of bending. Rare Rubber Fruitborn creepers roll at **0.2% (1 in 500)** before the elemental roll, with at most one naturally converted loaded Fruitborn within 128 blocks. Each eligible creeper is checked once. Existing elemental/checked creepers and Night Splitters creepers are excluded. Player kills before explosion drop a marked Rubber Fruit; right-click to consume. Only one living owner can claim Rubber per world. Fruit ownership and progression persist through saves/logout, but reset on death; element progression survives death. Spare fruit items cannot be eaten while the fruit is claimed.

**B** toggles combat, **R / Shift+R** select elements, **G** selects Rubber (the first fruit; future fruits will cycle here). **Z/X/C** operate the selected category. Only one fruit input can be held at a time. Releasing or switching categories, disabling combat, opening a menu, death, disconnect, or losing input heartbeat ends held defense. Keys remain rebindable; V and F are unused.

| Rubber key | Tap | Hold |
| --- | --- | --- |
| Z | Stretch Punch: 8-block aimed hostile-only hit, 5 damage, 1.5s cooldown | Release after 0.35s for a 14-block, 8-damage charged punch |
| X | Dodge using held movement keys: W forward, A left, D right, S backward; diagonals supported; no input defaults backward; 0.8s shared cooldown | Hold for 0.35s to latch up to 14 blocks away: pull a targeted player toward you, or pull yourself toward a mob/solid block. Release to detach; 4s cooldown after detaching. Player grabs respect PvP/team protections. |
| C | Activate Gear 5 | Bounce Guard outside Gear 5: 65% reduction against entity-sourced hits, repels melee attackers; drains food |

A fruit grants punch immediately. Dodge/grab require **10 meaningful uses AND 5 active minutes**. Gear 5/guard require **40 uses AND 1 active hour**. Only selected Rubber gains active time while moving or shortly after meaningful action; idle time does not count. Successful punches, grabs, and actual blocks grant at most one use per second. Empty punches/dodges grant no usage. Elemental Harmony does not boost Rubber.

Gear 5 supplies Speed II, Jump Boost III, Strength II, a 75% Stretch Punch bonus, and no fall damage. Food drops by one point (half an icon) every two seconds; eating normally replenishes it, including in Creative during testing. It ends at three remaining food icons, at ten minutes, on death, disconnect, or dimension change. Recovery is two minutes and survives logout. No permanent attribute changes are applied; short movement effects expire shortly after transformation ends. B controls input, not transformation duration; selecting an element does not remove an active transformation.

In Gear 5, ordinary damage is blocked. Diamond/netherite swords or axes with Sharpness, fire/lava, flaming arrows, and direct Fire bending strikes can hurt the player at 50% incoming damage. The actual direct melee attacker's held weapon is checked; a shooter holding a Sharpness sword does not upgrade ordinary arrows. Void and `/kill` bypass protection. Outside Gear 5, sharp melee weapons, arrows and heat deal 50% extra damage. Bounce Guard is not available during Gear 5. Rubber punches target hostile mobs only. Grapples do no damage; player pulls require PvP enabled and respect teams, excluding Creative/spectator players. Other living mobs (including villagers/pets) serve as anchors that pull the caster, without moving or damaging them. No terrain deformation, rubber ground, custom stretching models, or copied assets in this core build; effects use vanilla particles and motion.

Operator testing:
```
/creeperborn testfruit
/creeperborn fruitmastery 2
/creeperborn fruitmastery 3
/creeperborn testcreeper rubber
/creeperborn status
```
Consume the test fruit before setting mastery. Tier 3 is a testing shortcut; use tier 1 to reset. `/creeperborn fruit` selects Rubber without the client shortcut. Optional client component is required for fruit tap/hold controls. Set `fruitChance` in `config/creeperborn.json` (default 0.002), then restart. This alpha needs in-game client/multiplayer testing, particularly held controls, hunger pacing, grab/directional-dodge feel, and Gear 5 balance.

## Fruit water weakness — alpha.4

Owning a fruit creates the weakness even when an element is selected or combat is OFF. **Eyes below a water surface** (including flowing water) suppress new element/fruit casts, end Gear 5, release held grapples/guard, prevent upward swimming, and make the player sink. Rain, splashes, shallow water with the head above the surface, and carried water buckets do not trigger it. Boats are safe while your head stays above water. Ordinary nonfruit players retain vanilla swimming and breathing.

Air depletes through the normal drowning system. Water Breathing effects do not bypass the fruit curse; potion effects are not removed. Gear 5 cannot block drowning damage. Creative/spectator invulnerability still follows vanilla rules; use Survival for drowning tests.

**All four elements at mastery tier 3** cancel the weakness, allow normal swimming and powers underwater, replenish air, and prevent drowning. Owning four hearts alone is insufficient. This is the same all-four mastery condition as Elemental Harmony; Rubber mastery does not qualify. Dropping one element below tier 3 restores the weakness immediately. Ownership/mastery are read from saved progression, with server-to-client state synchronization for local swimming physics. Install the matching alpha.4 JAR on the client and server for the intended movement behavior.

### Alpha.4 test route

1. Consume `/creeperborn testfruit`, stay without mastered elements, and test **Z** (hold to charge), **X + WASD** dodges, hold-X grapple, and hold-C guard. Use `/creeperborn fruitmastery 3` to bypass the one-hour Gear 5 unlock for testing.
2. In Survival, enter deep water. Confirm sinking, no upward swimming, blocked powers even after selecting Water with R, and air bubbles running out. Enter with Gear 5 active and verify it ends; drowning remains dangerous.
3. Check that rain, carrying a water bucket, and wading with your head above water do not trigger the curse.
4. Consume all four hearts. Set each to tier 2 first; the weakness must remain. Then run `/creeperborn mastery earth 3`, `/creeperborn mastery water 3`, `/creeperborn mastery fire 3`, and `/creeperborn mastery air 3`.
5. Reenter deep water: swim normally, keep full air, and cast abilities. `/creeperborn status` reports the current water rule. Set Air to tier 2 underwater to verify the weakness returns.
6. Save/rejoin and respawn to check persistent ownership/mastery. Test grapples with a second player on a PvP-enabled test server, and check PvP/team protections.

GitHub Actions runs the existing element/Rubber tests plus a real water pool, native upward travel, native air depletion (including a breathing potion), drowning during Gear 5, mastery transitions, shallow-water exclusions, and water-state packet encoding. Client movement feel and multiplayer synchronization still need in-game testing.

## Ownership and lava immunity — alpha.5

**Mastery tier 3 in all four elements** grants lava/fire damage immunity and extinguishes residual burning, with or without a fruit. Three mastered elements or merely owning all four hearts are insufficient. This does not grant immunity to physical arrow hits or the direct impact of Fire bending. Downgrading an element immediately removes the lava immunity.

**One living Rubber owner per world.** Consuming a genuine fruit claims it and consumes the item. No further natural Rubber Fruitborn conversions or fruit drops occur while claimed. Spare/debug fruit items may still exist, but no other player can eat them. Offline owners retain their claim through logout and server restart. Claims are atomically persisted in the world's `creeperborn-fruits.json` file. Claim release requires the owner's actual death, including with `keepInventory` enabled; dimension travel/logout are not death.

Death removes the fruit power, its mastery/use/time counters, active transformation and fruit cooldown state. It releases the world claim so Rubber can naturally appear again; it does not drop a guaranteed fruit at the death location. The next eater starts fruit mastery at tier 1. **Element unlocks, mastery and selected element remain unchanged through respawn.** Original vanilla inventory loss follows the world's own settings. This system governs the Rubber fruit currently implemented; future fruits will each need an independent claim.

On upgrading older alphas, a joining legacy Rubber owner claims the fruit if available. If an older save has multiple owners, the first reconciled owner keeps it and other legacy fruit progress is cleared, preserving their bending progress.

Test lava in Survival before/after mastering all four, then test residual burning after climbing out. On a two-player test server, have one player eat Rubber and verify that the other cannot eat a spare. Log out/restart and verify it stays claimed. Kill the owner, respawn, and confirm that bending progress remains, Rubber is lost, and the second player can eat their spare at fruit tier 1. `/creeperborn status` reports availability and lava immunity. Existing alpha.4 water and Rubber control tests still apply.

## All-four fall immunity — alpha.6

Mastering all four elements to tier 3 now also prevents fall damage, alongside lava/fire immunity. It works without any fruit, with combat OFF, and with any element selected. Air mastery alone does not grant this all-four reward. Ordinary damage and void damage remain unchanged. Dropping any element below tier 3 removes the immunity immediately; elemental progression remains preserved on death.

Test in Survival with all four masteries at tier 3: fall from a safe test tower and verify no health loss. Then set Air to tier 2 and repeat a short, nonlethal fall to verify damage returns. Existing Gear 5 fall protection and Air Step's scoped jump protection remain separate. Automated tests exercise native fall damage before and after downgrading mastery.

## Later milestones

Custom models/UI, richer villager patrol behavior, faction banners, terrain-based earth walls, water projectiles, and original fruit-inspired abilities are later work. Avatar and One Piece are inspirations only; Creeperborn is not affiliated with their owners. No external mod code/assets are bundled.



