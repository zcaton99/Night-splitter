# Luffy / Rubber — alpha 86

Rubber uses the existing fruit, unique living ownership, mastery progression and Gear 5 defenses. R selects elements; G selects Rubber. The selected fruit now uses these controls:

| Input | Move |
| --- | --- |
| Left-click | Single stretch punch |
| Hold left-click | Alternating Gatling barrage with roughly 20–50 visible fist afterimages |
| Tap Z | Red Rocket: pull back, dark armored flaming fist, then rapid forward strike |
| Hold Z and release | Kong Gun: long backward stretch, then two heavy fists |
| Tap X | Hammer: high rubber leg followed by impact and five seconds of stun |
| Hold X | Octopus: wrap a target, attach behind it, and steer it; repeat to release |
| Tap C | Balloon: visibly inflated body, larger collision box and strong protection |
| Hold C | Enter Gear 5 with white smoke, white hair and a smoke scarf |
| Gear 5, empty hand, hold right-click | Catch one incoming fruit or elemental projectile attack |
| Release right-click with a catch | One full body spin, then throw toward the aim chosen on release |

Hold moves require one second. Balloon and Hammer unlock at tier 2; Octopus and Gear 5 require tier 3. Gatling lasts up to three seconds initially and five seconds at mastery, followed by recovery. Its extra fist images are cosmetic; actual alternating hits have a bounded cadence and stop on release. Punches and new attacks support hostile mobs and players, respecting teams and terrain.

Gear 5 lasts up to ten minutes while food supports it and retains its existing sharp/heat weaknesses, fall protection, native flight permission and recovery. It stays active when selecting elements. Landings and wall impacts rebound; sneak suppresses the owner's automatic rebound. At mastery, a moving rubberized area ripples around nearby terrain and bounces nearby enemies in the direction the owner faces. The actual blocks and fluids are preserved.

Catches cover incoming Lightning, Earth, Fire/Flame, Water, Ice, Magma, Air and Dragon power hits. The stored attack stretches and retains a recognizable visual. Releasing locks the throw direction, animates a twelve-tick spin, and only then launches the attack; camera aim stays under player control. One catch can be held for up to ten seconds. Return damage and range are capped. Natural weather, entire area fields and permanent transformations are not copied. This mechanic redirects an incoming attack; Axolotl's separate temporary borrowed-move slot remains unchanged. Opening a menu cancels a catch instead of throwing unexpectedly.

Octopus lasts up to twelve seconds against mobs and eight against players. WASD steers, Space jumps, and attack makes the captive strike another nearby enemy. The rider remains a hittable player; actual damage interrupts the attachment. Death, logout, suppression, world changes and expiry release the captive. Hammer's stun expires after five seconds and restores movement without permanently changing AI.

Balloon lasts four seconds initially and six at mastery. Inflation requires open space. Catching, control, attacks, temporary visuals, collision changes and Gear flight are cleaned up when ownership ends.

Testing: `/creeperborn testfruit rubber`, eat it, then `/creeperborn fruitmastery 3`. Existing fruit ownership must be released before eating another fruit. Use F5 to inspect body spinning, Balloon and Gear 5 smoke. Automated checks cover actual attack damage, stun timing, steering, damage interruption, catching, spin timing, locked throw aim, Gatling limits, terrain preservation, collision cleanup and two native multiplayer clients under network delay. Final appearance and movement feel still need a player playtest.
