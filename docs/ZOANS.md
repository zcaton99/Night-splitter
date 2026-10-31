# Nine-Tailed Fox and Axolotl — alpha 85

R selects elements, G selects your fruit, Z/X/C are the moves. Tap and release for tap moves; hold for at least one second and release for hold moves. The charged Beast Bomb grows for up to 2.25 seconds in a cloak or 3.5 seconds as the full fox. No extra click is needed.

## Nine-Tailed Fox

| Key | Tap | Hold |
| --- | --- | --- |
| Z | Chakra Claw | Three Fox Fire projectiles |
| X | Fox Flash / full-fox jump dash | Change form |
| C | Mini Beast Bomb | Charged Tailed Beast Bomb |

Tier 1 starts with the red three-tail cloak. Tier 2 unlocks a six-tail golden cloak with native flight, increased movement speed, and nine linked Fox Flashes. At tier 3, hold X switches Golden Cloak and the full nine-tailed fox. The full fox stands five blocks tall, with articulated tails reaching approximately nine blocks. Its collision and combat hitbox grows with its body. The tails are visual extensions.

Golden Cloak flies using normal flight controls, with Space ascending and Shift descending. Its nine dash charges refill together after four seconds without a dash. Each dash has a short input interval; holding a key cannot skip the charge limit. Full fox cannot grant flight, but jumps and lunges up to 25 blocks, stopping on collision or a cage boundary. It has wider, heavier claws, stronger projectiles, larger Beast Bombs, greater damage resistance, and more knockback resistance. Creative flight permissions are restored rather than stripped when leaving a cloak.

Forms persist when selecting elements. Death, disconnect, dimension mismatch, strong suppression, or low hunger ends a form. Hunger drains slowly while transformed, and moves cost additional exhaustion. Mastery follows the existing 10-use/300-active-second and 40-use/3,600-active-second fruit progression. Test commands can grant tiers immediately.

## Axolotl

| Key | Tap | Hold |
| --- | --- | --- |
| Z | Gill Jet | Five-second Regrowth heal |
| X | Power Imprint from an aimed powered target | Absorption Guard for one incoming power |
| C | Use the stored borrowed move once | Change size/form |

Tier 1 is a small axolotl, with very fast normal underwater movement, a low collision box, and variant-preserving fish collection. Tier 2 also permits a human/axolotl hybrid. Tier 3 switches small and giant combat forms. The giant axolotl gains reach through its larger body, stronger Gill Jet, damage resistance, stronger Regrowth and stronger absorption. Entering a large form requires enough open space.

Owning Axolotl allows normal swimming and water breathing even while using elements or outside its transformation. It does not grant all-four-element mastery or any other mastery immunity.

Power Imprint aims at fruit users, elemental players, bender villagers, and elemental or fruitborn creepers. Absorption Guard intercepts a qualifying fruit or element damage event, not ordinary melee or environmental damage. It catches one power and then recovers; a held key cannot provide indefinite immunity. Targeting and projectiles respect walls.

One borrowed slot lasts 30/60/90 seconds by tier. The next successful copy replaces it. Tap C consumes it for a bounded signature move from that set: for example Fire Bolt, Ice Spear, Stretch Punch, Healing Flame, Door Step, Gravity Press, or Mini Beast Bomb. These are temporary Axolotl versions, not ownership grants or access to the source's entire move set. Source owners retain their fruit; copies cannot capture fruit claims, take hearts, create another permanent form, or revive the copier. The slot survives selecting elements. Expiry is based on world time and cannot be refreshed by logging out.

While small, sneak-right-click a fish with a water bucket to collect its actual bucket item. Native fish data, including tropical-fish pattern and colors, is retained. Normal attacks remain available for food.

## Testing

- `/creeperborn testfruit fox` or `/creeperborn testfruit axolotl`
- Eat the fruit, then `/creeperborn foxmastery 1|2|3` or `/creeperborn axolotlmastery 1|2|3`.
- `/creeperborn fruitmastery 1|2|3` also supports both fruits.
- `/creeperborn testcreeper fox` and `/creeperborn testcreeper axolotl` create their drop-bearing creepers.

One living owner per fruit and the existing Dark second-slot rules apply. Use separate players or release an existing fruit through the established reset/death workflow to test another.

Automated regression covers claims/reload, water breathing, forms/flight cleanup, dash limits, 25-block travel, physical dimensions, copy targeting and one-use consumption, incoming elemental absorption, ordinary-melee exclusion, fish bucket data, healing, and projectiles finishing after an element switch. Client smoke checks bake every form and effect and verify textures and balanced render poses. Visual feel and camera framing still require a live playtest.
