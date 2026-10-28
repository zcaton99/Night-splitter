# Paw-Paw prototype — alpha.47

Minecraft 26.3 / Fabric / Java 25. Includes colored awakened Five-Color Claw and taller layered Lightning Storm clouds.

| Input | Ability |
|---|---|
| Tap Z | Paw Cannon: traveling paw-shaped compressed air, 4/6/8 health damage |
| Hold Z 2s, release | Ursus Shock: visibly compressing air paw, radius 4/7/10; 8/12/18 damage |
| Tap X | Paw Step: self repulsion, 8/14/20 blocks in aim direction |
| Hold X, release | Paw Launch: nearby aimed mob/player, high ascent then travel in caster facing direction |
| Tap C, no pain bubble | Repel Guard: brief projectile reflection and nearby melee repulsion |
| Hold C 1s | Full health/hunger extraction; keep holding to guide pain bubble using aim, release to fire |
| Tap C with a bubble | Fire the existing bubble |

Launch: tier 1 random 20–30 blocks; tier 2 60–80; mastered 200–220, with 1% reaching 300. Normal launches rise toward the upper world before horizontal travel and collision-respecting descent. World borders, obstructed launch columns, build-height structures and Birdcage can interrupt travel. Native short-lived chunk tickets keep flight terrain available; no persistent forced chunks or no-gravity changes. Landing protection applies to mobs and players. One flight per target, at most two per caster and 32 total.

Pain extraction has a 60s cooldown (Harmony speeds recovery 15%), persisted in player data through logout. Mastery gives six absorption health (three hearts) for 30s. Bubble damage is 1.2 times health actually restored, capped at 24 before Harmony; hunger and temporary hearts add no damage. A full-health extraction makes a harmless bubble. One bubble per caster, 12s lifetime; cancel/menu stops steering without firing. Water, death, logout, fruit switching and Dark nullification clear temporary effects. Launch victims retain fall protection during cleanup.

Unique world ownership, natural fruit Creeper/drop, Dark capture/second fruit support, water curse, mastery, new inventory artwork and renderer effects are included. Two-client network feel remains a human play test.

## Solo testing

Replace the previous JAR, then `/creeperborn testfruit paw`, eat, G/B as needed, `/creeperborn pawmastery 1` (then 2 or 3). Test cannon and guard against skeleton arrows, charge Shock, step in several directions, hold X near a mob with clear sky. At mastery, take damage and lower hunger; hold C for one second, keep holding and turn to steer the red bubble, then release at an enemy. Verify immediate re-healing is blocked and full-health bubbles do not damage. Test water/death/logout cleanup. Check String claw colors inside Birdcage and Lightning's tall Storm form.
