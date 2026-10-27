# Dark Creeper Fruit — alpha.34

Dark is the fifth rare fruit. The total Fruitborn chance is unchanged. One living owner per fruit remains; Dark has a special allowance for one additional captured power.

| Input | Move | Initial balance |
| --- | --- | --- |
| Tap Z | Black Vortex | 24-block aimed pull, 5 damage, 2s recovery |
| Hold Z .35s, release | Nullifying Grasp | 6-block aim; ends fruit transformations, roots target and suppresses fruit powers for 3s / 5s with all-four mastery; 5 damage; 8s recovery |
| Tap X | Dark Mantle | 8s shadow-player model; incoming projectiles deal 55% damage and charge up to six darkness charges; half a food icon activation cost; 12s recovery |
| Hold X .35s, release | Liberation | Spend stored charges on an 8-block shockwave; 4 + 2 damage per charge, pushback; 8s recovery |
| Tap C | Shadow Surge | Roughly 16-block grounded shadow rush, stops at obstacles; 6s recovery |
| Hold C 3s, release | Void Collapse | 32-block placement; pulls enemies within 6 blocks for 4s, 4-damage pulses and 12-damage collapse; 20s recovery; actual player kills capture fruits |

Tier 1 unlocks Z, tier 2 X (10 uses + 300 active seconds), tier 3 C (40 uses + 3600 active seconds). Ordinary offensive hits suppress fruit moves for one second, refreshing without stacking. Element casts stay available. Grasp ends Rubber Gear/Guard, Phoenix/Lightning forms, Door sessions and Dark Mantle; temporary flight is restored correctly. All-four mastery strengthens Dark damage by the normal Harmony multiplier and extends Grasp to five seconds.

## Captures

Only an actual Void Collapse death captures the victim's selected fruit (last selected fruit when using elements). Phoenix Rebirth is not death. The cap is tier 2: tier 1 stays 1; tiers 2/3 become 2 with progress at the starting threshold.

A persistent clickable chat choice offers Eat / Materialize. Eat grants the power as Dark's second fruit; G rotates between the two. Switching fruits ends the previous transformation to avoid stacked forms. Each fruit progresses separately. When Dark already has a second fruit, only Materialize is available.

Use /creeperborn dark captures to reopen pending choices. /creeperborn dark eat or materialize acts on a pending capture, or append its token. Full inventory leaves Materialize pending. Pending captures persist in player tags, with their unique ownership reservation in the existing world fruit file.

Materialized captured items keep their capped tier, can be traded/stored in chests, and can be consumed by another eligible player. The token is consumed by the ownership transfer, so duplicate item copies cannot create duplicate owners. Despawned or destroyed dropped capture items release their reservation; chunk unload and pickup do not. Administrative deletion directly from containers/inventories may require restoring the item or repairing the world reservation.

Actual death clears both equipped fruits while retaining elemental progression. Unresolved captures drop as physical fruit items. Logout preserves ownership/progress/pending choices but ends temporary effects.

## Test commands

/creeperborn testfruit dark; /creeperborn darkmastery 1|2|3; /creeperborn fruitmastery applies to the current Dark/captured selection. /creeperborn testcreeper dark spawns a test Fruitborn.

Use a fresh test world/player to preserve existing powers. PvP capture needs a second fruit-owning player. Test Grasp against forms, normal one-second suppression while elements still work, Mantle projectile absorption and six-charge cap, Liberation spending, Surge collision, Void kills/Rebirth, Eat tier cap/G switching, Materialize/full inventory/chest trade, and logout/death restoration.

All models are original code-built meshes. Dark Mantle replaces the visible avatar with a black shadow and violet eyes, Surge flattens into a shadow pool, and Void Collapse has a temporary black sphere with a rotating violet rim. Effects do not remove terrain blocks.
