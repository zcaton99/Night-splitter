# Time-Time — alpha 87

A counter-fighting Creeper Fruit. Select with G; ordinary Z/X/C tap and hold controls.

| Input | Move |
|---|---|
| Z tap | Time Dodge; tier 3 activates Awakened Time |
| Z hold | Borrowed Second: aimed strike, then a damaging echo 0.6 seconds later |
| X tap | Stolen Moment: nearby enemies move 65% slower temporarily |
| X hold | Place Rewind Anchor; hold again within 30 seconds to return |
| C tap | Controls/status |
| C hold | Homeward Hour: stay still for five seconds to return to the valid respawn point, or world spawn |

## Progression

- T1: 15-second window, three dodges, two-second gap. Basic melee and ordinary projectiles only.
- T2: 25-second window, six dodges, one-second gap, stronger Borrowed Second.
- T3: 45-second window, nine dodges, one-second gap, skin-matched mirage trail and orbiting clock fragments. Also dodges scoped fruit powers from T1/T2 users and nonmastered individual elements. Fruit T3 and mastered elements pierce it without consuming a charge.
- Z recovery: 60 seconds after the nominal dodge window. Disconnect/water suppression never reset the persistent cooldown. Exhausting charges ends the form early.
- Incoming powers are classified by their explicit damage-origin scope, not vanilla's generic playerAttack source. Fruit rank is the origin user's progression tier at impact. Ordinary weapon attacks remain basic melee, even if that player owns a fruit. Mob elemental attacks have an explicit element scope too.

Dodge needs clear line of travel, body clearance, loaded terrain, world-border clearance, dry feet/head and solid ground. Restrained, sleeping, mounted, suppressed or dead players cannot dodge. No safe position means no evade and no spent charge. Environmental damage and explosions are not eligible. Successful dodges leave a skin-matched, articulated visual copy which breaks into clock fragments over 1.2 seconds; they are displays, not extra mobs. At most 18 Time displays per owner.

A successful dodge empowers Borrowed Second for three seconds (+50% first hit and its echo). Echo damage is 45% / 70% / 100% of the first strike. Target must remain in the same world, nearby and visible. Echoes finish when selecting elements for combos, but end on death, suppression, disconnect or fruit loss.

Stolen Moment radius is 6/8/10 blocks. Duration is 3.5/4.5/5.5 seconds for normal mobs; 2/2.5/3 seconds for players and mobs over 100 maximum health. Uses a temporary, synchronized movement-speed modifier; does not alter global tick speed or replace terrain. Rewind returns position and facing, clears momentum/fall distance, and requires a safe destination. While Awakened Time is active, it restores up to three hearts of health lost since the anchor; never lowers health, restores hunger or duplicates inventory. Anchor expires or clears on death/disconnect/world change.

Homeward Hour is interrupted by actual damage, moving more than 0.8 blocks, restraints, suppression or selection changes. Successful return uses Minecraft's native safe respawn resolution (bed/respawn anchor/world spawn), with a two-minute cooldown. Interrupted channels have a ten-second recovery. It teleports the living player and preserves their inventory.

Testing commands: `/creeperborn testfruit time`, eat, `/creeperborn fruitmastery 3`. Standard one-owner, Dark capture/second-fruit, water weakness, natural Fruitborn Creeper, death release and item model migration rules apply.
