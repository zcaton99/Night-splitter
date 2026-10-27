# Phoenix — alpha.27

Phoenix is the third rare fruit in Creeperborn. It follows the existing one-fruit-per-player and one-living-owner-per-fruit rules, preserves ownership across logout/restart, and loses powers/progression on actual death. Elemental unlocks/mastery remain.

## Controls

Select Phoenix with G; B enables combat controls. Z/X/C are rebindable in Minecraft Controls.

| Input | Ability | Starting balance |
| --- | --- | --- |
| Tap Z | Phoenix Talon | 6 damage, 5-block forward reach, 1s cooldown |
| Hold Z at least .35s, release | Phoenix Dive | Up to about 16 blocks; swept hostile hit, 10 damage plus small 4-damage impact area; 5s cooldown; walls stop travel |
| Tap X | Wing Flight | Partial animated wings, 30s duration; tap again to end; 5s cooldown after ending |
| Hold X at least .35s, release | Phoenix Form | Full bird, 90s maximum, half incoming damage; 30s cooldown after ending |
| Tap C | Healing Flames toggle | Self plus nearby players/villagers within 10 blocks and line of sight; tap again to stop |
| Hold C | Phoenix Blessing | Nearby teammates/villagers receive 10s of healing that follows them out of the circle; 75% strength; 10s cooldown and half a food icon per cast |

Healing Flames restores 2 health (one heart) every 2 seconds. Full form passively restores 1 health (half a heart) every 2 seconds; channel healing replaces passive self-healing rather than stacking. Blessing heals 1.5 health every 2 seconds before mastery/recovery scaling and does not stack with circle healing. With no scoreboard team, nearby players count as friendly; with a team, player recipients must be allied. Healing blocks Phoenix attacks until stopped. Changing selection or ending form stops healing.

Flight/form costs one food point (half a hunger icon) per cumulative active minute. Healing costs an additional food point per 10 active seconds. Fuel accumulation persists across flight activations/logout. Food at three icons ends active powers. Eating refuels but does not reset normal form duration. Existing vanilla hunger/exhaustion still applies where applicable.

## Progression and protection

- Eat fruit: Z attacks.
- Tier 2: 10 meaningful uses and 300 active seconds: Wing Flight.
- Tier 3: 40 uses and 3600 active seconds: full form, healing and one revival.
- All four elements mastered: 45s partial flight; unlimited full-form duration; full-form attacks do zero damage; healing increased 25%.
- Phoenix ownership grants fire/lava and fall immunity. The existing fruit water curse still applies without all-four mastery; submerged powers stop and drowning is not a resurrection escape.
- Void and kill-command damage bypass Phoenix protection and resurrection.

## Rebirth

One revival per ownership, only during full Phoenix Form. A fatal hit restores half maximum health, extinguishes the player, ends form/flight and grants 3 seconds of ordinary protection. Phoenix attacks and healing then run at half strength for 300 active online seconds. Movement or meaningful activity advances recovery; logout does not skip it. Recovery finishing restores power but never recharges the spent revival. Actual death releases the fruit.

## Visuals

Original articulated bird mesh uses blue/cyan feathers, gold beak/talons, luminous accents, split tail feathers, articulated wing tips, flight flapping, attack/dive poses and healing poses. Partial wings attach to the normal avatar; full form replaces avatar/armor/held-item rendering and hides human first-person hands. The player's gameplay hitbox stays unchanged. Blue flame trails, transformation/rebirth bursts and a visible healing boundary support the model. Press F5 to inspect it.

## Test commands

- /creeperborn testfruit phoenix
- /creeperborn testcreeper phoenix
- /creeperborn phoenixmastery 1|2|3
- /creeperborn status

Use a fresh test player/world or actually die to release an existing Rubber/Door fruit before eating Phoenix. Test ordinary form before granting all-four mastery, then compare damage, timers and healing. Test revival before granting all-four protection, because ordinary fatal attacks cannot reach a fully mastered active form. Test wings, C on/off, allied healing, hunger, expiry, double-tap Space, walls during Dive, water curse, logout/rejoin and F5 visuals.

Automated server tests cover gameplay and native damage/death paths. The client smoke check covers startup, model construction/submission and client mixin application. Visual appearance, animation alignment and live multiplayer remain in-game playtest work.

## Alpha.26 changes

Wing leading feathers now glow orange over red wings with gold flame tips. Wing Flight and Phoenix Form surround the torso and legs with a dense orange flame mantle and a blue healing-flame core; the partial form keeps the player's head visible. Fire particles trace the flapping wing edges. These are cosmetic flames and do not damage the owner or nearby blocks.

`/creeperborn fruitmastery 1|2|3` now changes the fruit the player actually owns (Rubber, Door or Phoenix), including while an element is selected. It never grants an uneaten fruit. Feedback stays in chat. The fruit-specific mastery commands continue to work.

## Alpha.27 full-bird appearance

The full Phoenix now matches the fiery wings: red head/body, glowing orange crown and animated tail, layered orange cheek/shoulder/body plumage, and gold chest embers, beak and talons. The flame mantle from alpha.26 remains. Tap X is still human Wing Flight; hold X and release is the full bird. This update changes appearance only.
