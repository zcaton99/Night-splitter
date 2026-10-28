# Ice-Ice and Magma-Magma — alpha.58

Minecraft 26.3 / Fabric / Java 25. Fruits 13 and 14; preserves alpha.57 Love/Sand and earlier fruit behavior. Original icons and native model effects are included. No attack places blocks, ice, water, lava or fire blocks; builds remain intact.

## Ice

| Input | Move | Tiers 1 / 2 / 3 |
|---|---|---|
| Tap Z | Ice Spear | Piercing spear; size 1/1.8/2.6, damage 4/6/8, range 20/36/56; 0.9s recovery |
| Hold Z 1s | Ice Partizan | 3/5/7 converging spears; size 1.2/2/3, damage 4/6/8 each, range 32/52/80; 7s recovery |
| Tap X | Ice Pheasant | Frost bird; damage 5/7/9, range 24/48/72; 5s recovery |
| Hold X 1s | Ice Age | Expanding glacier radius 18/40/90; duration 12/25/60s; 40% armor while active; recovery 20/40/90s |
| Tap C | Ice Slide | Steer with WASD; pauses on a frozen platform with no input; lasts 8/15s, unlimited at mastery |
| Hold C 1s | Ice Coffin | Instant prison; range 4/6/9; maximum 10s, with escape and rescue; 15s recovery |

Frost builds toward a freeze; ordinary spear freezes last 2/4/6s on mobs and 1/1.5/2s on players. Pheasant freezes last 3/5/8s on mobs and 1.5/2/3s on players. Ice Age freezes last 4/6/10s on mobs and 2/3/4s on players. Released targets resist refreezing for four seconds. Frozen mob AI returns to its original state.

Ice Age armor ends when the glacier ends, is toggled off, or is interrupted. Its radius expands gradually; only loaded entities are queried, without forcing remote chunks. Field meshes have a fixed geometry budget regardless of radius.

Ice Slide automatically catches downward movement just above a water surface while Ice is selected. With movement released it holds a temporary platform; movement resumes the slide. Mastery has no duration limit or ongoing food drain. Shift intentionally releases the platform and suppresses auto-catch while held. Actual submersion retains the normal fruit sinking/drowning rules (including existing Harmony exceptions). No real blocks are placed. Collision, build height, borders and Birdcage still constrain movement.

Ice Coffin has integrity 120/160/200. A player can hold attack to struggle, removing 12 integrity per four server ticks; allies can hit the prisoner to break the shell without damaging them. Ordinary mobs struggle periodically; stronger mobs break it faster. If it remains intact, frost pulses and a final shatter can kill the victim. It never lasts beyond ten seconds. It cannot overlap other hard restraints.

## Magma

| Input | Move | Tiers 1 / 2 / 3 |
|---|---|---|
| Tap Z | Magma Fist | Fixed large size 3; damage 10/15/21, range 20/32/48, splash radius 3/4/5; 1.75s recovery |
| Hold Z 1s | Great Eruption | Large size 4; damage 18/26/36, range 12/20/30, splash 5/6/7; 8s recovery |
| Tap X | Magma Hound | Ground-following hound, piercing targets; damage 8/12/16, range 24/36/48; 6s recovery |
| Hold X 2s | Meteor Volcano | 5/8/12 ascending and falling fists around caster, spread radius 8/14/22; damage 10/15/20; 25s recovery |
| Tap C | Magma Field | Lava-textured pools, radius 10/25/45, duration 10/25/45s, 4/6/8 damage per second; recovery 20/40/60s |
| Hold C 1s | Magma Cloak | Absorbs ordinary hits; actual mastered elemental attacks pierce at 50%, Harmony at 100%; controlled ground-fed flight capped 20 blocks above launch ground |

Magma hits leave enemies burning. The fruit resists fire; cloak does not bypass normal fruit drowning, void or administrative kill behavior. Cloak lasts up to 10/20/30s and consumes food while active. Jump starts/rises in flight; Shift descends, WASD steers and releasing inputs hovers. The molten column is a visual effect. Ending flight gives temporary landing protection without permanent gravity or flight-ability changes.

Cloak counters use an explicit context from real elemental attack damage. A player merely owning mastery, or using a sword while mastered, does not bypass it. Existing elemental projectiles that call the shared strike path inherit this counter. Harmony uses full normally scaled elemental damage.

Tap moves cost one food point, held moves two; activation needs food above six. Harmony boosts damage and shortens cooldowns. Cooldowns persist through logout. Repeat the relevant active form/field input to end it early. Selection changes, water suppression, darkness, death and logout clean up outgoing abilities. Incoming prisons keep their own timer unless the target departs or dies.

## Testing

Replace the old JAR. `/creeperborn testfruit ice` or `testfruit magma` gives the fruit; release the previous normal fruit first. Use G/B for selection/combat and Z/X/C tap/hold. `/creeperborn icemastery 1|2|3` and `magmamastery 1|2|3` choose tiers. `/creeperborn test ice` or `test magma` spawns the Fruitborn.

Server regression exercises all twelve inputs, actual long-distance hits, 90-block/60s glacier and armor expiry, water auto-catch/pause/resume/submersion, prison struggle/rescue/timeout, splash/burning/hound/meteor hits, 45-block/45s field, native incoming damage through cloak, real elemental/Harmony counter damage, 20-block ceiling, claim/save/capture/drop behavior, cooldown persistence and cleanup. Client smoke loads all fourteen fruit icons and submits every new effect with pose-isolation checks. Appearance, multiplayer movement feel and balance still need in-game testing.
