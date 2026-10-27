# Gravity alpha.39

Minecraft 26.3 / Fabric / Java 25. Same JAR on client and server.

| Tier | Key | Tap | Hold |
| --- | --- | --- | --- |
| 1 | Z | Gravity Press: 6 damage, downward slam, Slowness III for 2s, 32-block aim, 1s recovery | Repulsion Wave: forward cone within 12 blocks, 8 damage and knockback, 6s recovery |
| 2 | X | Levitate: aimed enemy floats upward for 3s, 2 initial damage, 6s recovery | Rock Ride: steer by looking, release to stop; max 10s, 6s recovery after stopping |
| 3 | C | Orbit Guard: 8s, 50% less melee / 80% less projectile damage, nearby hostile projectiles intercepted; 12s recovery | Meteor: hold 3s and release, 1.5s descent, 24 damage in radius 6, 20s recovery |

One food point per ability; Meteor costs three, Rock Ride costs one to start and one per five active seconds. Eat above three food icons. Mastery uses the existing 10 uses + 5 active minutes / 40 uses + 1 active hour thresholds. All-four elemental mastery raises targeting to 50 blocks, meteor radius to 8 and Rock Ride duration to 20 seconds; normal Harmony damage multiplier applies.

No native creative flight granted. Rock flight uses checked loaded movement and a temporary stone platform. Temporary visuals are not saved. Meteor never explodes terrain, creates fire, or edits blocks. Terrain shields enemies from the meteor's damage; initial overhead obstruction clips the impact point to the first surface.

Natural Gravity Fruitborn joins the existing rare fruit pool without increasing the total rare spawn chance. One living owner per fruit; Dark can hold Gravity as its second power. Captured mastery is capped at tier 2. Death clears fruit powers; elements survive. Water and Dark suppression interrupt powers.

Dark update: Black Vortex applies three seconds of Darkness to an enemy player. Void Collapse refreshes that brief Darkness while pulling an enemy player inside its field. Caster/allies are excluded and the effect expires naturally after escape.

Testing:
- /creeperborn testfruit gravity
- /creeperborn gravitymastery 1|2|3
- /creeperborn testcreeper gravity
- G selects fruit; B toggles combat.
- Test all six inputs, early Meteor release, feeding during Rock Ride, collision, projectile guard, water, logout and death.
- Real two-player tests remain necessary for enemy-player Darkness, Gravity PvP and Dark capture.
