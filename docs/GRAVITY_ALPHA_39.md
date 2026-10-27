# Gravity alpha.40

Minecraft 26.3 / Fabric / Java 25. Same JAR on client and server.

Controls: Z Press / held Repulsion Wave; X Levitate / held Rock Ride; C Orbit Guard / held Meteor. Z unlocks at tier 1, X at tier 2 and C at tier 3.

| Stat | Gravity tier 1 | Gravity tier 2 | Gravity tier 3 |
| --- | --- | --- | --- |
| Press / aimed ability range | 32 blocks | 48 blocks | 64 blocks |
| Press pin duration | 3s | 5s | 8s |
| Press damage | 3 hearts | 3.5 hearts | 4 hearts |
| Wave reach | 12 blocks | 18 blocks | 24 blocks |
| Wave damage | 4 hearts | 5 hearts | 6 hearts |
| Levitate | locked | 16 blocks / 4s | 24 blocks / 6s |
| Rock Ride | locked | 30s | 120s |
| Orbit Guard | locked | locked | 16s, 8 rocks, 70% less melee / 90% less projectile damage |

Press is a multi-target radial attack against every visible, nonallied enemy within its range. It uses the native movement-root framework and Slowness III; Wave and Levitate release a target from its gravity pin so abilities can be combined. Targeting/Press range gets a further 25% with all-four elemental mastery. Wave also gets +25% reach and the standard Harmony damage multiplier. Gravity Press recovers in 1s, Wave and Levitate in 6s.

All-four mastery extends Rock Ride to 180s, Levitate by 2s, and Orbit Guard to 20s / 10 rocks / 80% less melee / 95% less projectile damage. Guard intercepts hostile projectiles within its enlarged orbit and has a four-second gap after a full-duration use. It still unlocks only at Gravity tier 3. Environmental damage remains normal.

Meteor: charge 3s and release, then a 1.5s descent. Fiery orange/yellow underside, flame trail, explosion sound, particle explosion and expanding purple shockwave. Damage is 12 hearts before Harmony; radius 10 blocks, or 14 with all-four mastery. 20s recovery. Terrain shields targets. The spell never invokes a real terrain explosion, edits blocks or places fire.

One food point per ability; Meteor costs three. Rock Ride costs one to start and one per ten active seconds, and ends at low food; feed yourself to sustain longer rides. Release X to stop, six-second recovery after stopping. Flight uses checked native motion and a temporary stone platform, not creative flight permissions. Height limits and solid terrain constrain flight and levitation.

Mastery: 10 uses + five active minutes for tier 2; 40 uses + one active hour for tier 3. Natural Gravity Fruitborn joins the existing rare-fruit pool without increasing total spawn chance. One living owner per fruit; Dark can keep Gravity as its second power. Captured mastery is capped at tier 2. Death clears fruit powers, preserving elements. Water and Dark suppression interrupt powers; transient visuals are excluded from saves and cleaned up on death/disconnect.

Dark's Black Vortex applies three seconds of Darkness to enemy players; Void Collapse refreshes it while pulling them inside. Caster/allies are excluded.

Test commands:
- /creeperborn testfruit gravity
- /creeperborn gravitymastery 1|2|3
- /creeperborn testcreeper gravity
- G selects fruit; B toggles combat.

Real two-player testing is still needed for enemy-player Darkness, PvP pins, and Dark captures. Fruit appearance artwork remains pending separately.
