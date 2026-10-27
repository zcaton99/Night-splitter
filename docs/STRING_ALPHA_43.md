# String Creeper Fruit — alpha.43

Minecraft 26.3 / Fabric; Java 25. Existing fruit and elemental bindings remain unchanged.

| Input | Ability | Unlock |
|---|---|---|
| Tap Z | Five-Color Slash: 6 health damage, 0.75s recovery | Tier 1 |
| Hold Z | Overheat: 12 health, 8s recovery; at tier 3, charge 2s for six God Thread spears (24 health total if all connect) | Tier 1 / Awakening at 3 |
| Tap X | Toggle String Glide: aim to steer connected strings; Space transfers to a fresh string | Tier 2 |
| Hold X for 1s, release | Parasite: aimed enemy, third-person camera and WASD/attack control | Tier 2 |
| Tap C | Summon Black Knight plus 4s web guard; subsequent taps enter/leave knight control | Tier 3 |
| Hold C | Create Birdcage; repeat to start 10s closing animation | Tier 3 |

Parasite lasts 5s at tier 2, **20s at fruit mastery**. Black Knight lasts **60s at fruit mastery**. One controlled target and one knight per user. X or C returns from control; opening a menu, switching powers, water suppression, death, disconnect, target loss, or input timeout restores control and AI. The owner's body remains in place and vulnerable. Puppet attacks use a bounded ordinary attack, never the victim's inventory, items, powers, or commands. Native server player targets share this state, but actual two-client PvP remains unverified until played.

Glide lasts 30s at tier 2 / 120s at tier 3 and costs one hunger point every 10s. It follows visible straight thread segments and chooses a new segment when aiming away, reaching an anchor, or jumping. It never grants creative flight permissions. A short fall-safety grace follows gliding. Awakened glide temporarily widens FOV by 8, restoring the prior value when it ends; existing FOV settings can be changed normally. Awakening uses temporary visible terrain threads; no blocks are replaced.

Stitching starts after **7s without actual damage**, restoring 1 health every 3s with hunger exhaustion. All-four elemental mastery gives String damage, range, durations, stitching rate and glide speed +15%; cooldown recovery is 15% faster. This makes Parasite 23s and Knight 69s with Harmony. Clone count stays one.

Birdcage is fixed at creation, **200-block diameter** at the tier-3 unlock (230 with Harmony), and traps caster, enemies, and allies. Its full spherical boundary extends underground to stop tunneling, alongside movement and teleport guards. Death, logout, restart, water suppression, or strong Dark nullification removes it. Mild Dark suppression interrupts input rather than permanently removing the cage. Hold C begins a ten-second visual shrink to twenty blocks across, then dissolution. **During this first prototype, collision remains at the original boundary until dissolution; closing does not push or crush anyone.** No terrain destruction. Cage collision also applies to Door/Spectator travel; it does not rely only on vanilla block collision.

Visual language: white emissive strings; purple Parasite outlines; white woven knight with black silhouette; white web and cage. String fruit inventory artwork follows the source credited in FRUIT_ART_CREDITS.md.

## Solo test

1. Replace the previous Creeperborn JAR (do not keep both), launch a backed-up test world.
2. `/creeperborn testfruit string`, eat it, press G/B as needed. `/creeperborn fruitmastery 1`, then test Z tap and hold against zombies/skeletons.
3. `/creeperborn stringmastery 2`: X tap glide, turn/aim/jump, X tap stop. Aim at a skeleton, hold X for 1s and release; test WASD, mouse, attack, X return, 5s expiry.
4. `/creeperborn stringmastery 3`: repeat Parasite (20s). Hold Z 2s and release God Thread. C tap summons knight/web; C taps enter and leave clone control. Knight expires at 60s.
5. Hold C opens cage. Try walking/flying/tunneling and fruit teleport escapes from both sides. Hold C again: closing visual lasts 10s, then boundary releases. Test caster death/logout cleanup.
6. Take damage, wait 7s and observe stitching; hit again to reset delay. Check each all-four bonus separately.
7. Two players later: possession, camera return, inventory protection, allied cage trapping, and Nullifying Grasp collapse. These are not claimed as network-play-tested by the detached server regressions.
