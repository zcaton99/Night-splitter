# Lightning Creeper Fruit — alpha.33

Fourth rare fruit; one fruit per player and one living owner of each fruit per world. Ownership and mastery persist across logout/restart. Actual death releases the fruit and resets its progression, retaining elemental progression. Existing fruit saves without a Lightning owner remain compatible. Total Fruitborn spawn chance stays unchanged; available types share it.

Select Lightning with G; B toggles combat controls. Keys remain rebindable.

| Input | Ability | First balance |
| --- | --- | --- |
| Tap Z | Lightning Bolt | 8 damage; 30-block aimed reach; chains to at most two hostiles within 5 blocks for 4 damage; 0.5s cooldown |
| Hold Z 8s, release | Thunder Judgment | Aimed strike up to 50 blocks away, 5-block impact radius, 24 damage; 15s cooldown. Release between .35s and 8s cancels. Charging shows progress. |
| Tap X | Flash Step | Instant travel up to 20 blocks in aimed direction; body collision, loaded chunks, height and world border limit travel; 3 charges (5 with all-four mastery); full refill after 3s without a jump |
| Hold X .35s | Lightning Rush | Up to 3s of fast aimed movement while held; release ends it; 8s recovery; half a food icon per 2 active seconds |
| Tap C | Storm Form toggle | Animated storm cloud with flight; 60s duration, 45s recovery, half a food icon per 4 active seconds |
| Z in Storm | Directed Lightning | Tap strikes deal 14 damage within 2 blocks, aimed up to 50 blocks; charged strikes deal 40 damage within 5 blocks, aimed up to 64 blocks |

Tier 1 grants the Z moves. Tier 2 needs 10 uses and 300 active seconds, unlocking movement. Tier 3 needs 40 uses and 3600 active seconds, unlocking Storm Form. A use requires a successful hit, Flash Step or transformation; active movement/recent actions progress time while Lightning is selected. Waiting AFK does not earn mastery.

All-four elemental mastery extends Storm to 90s, adds 25% storm attack damage, and lets ordinary attacks pass through. Another player with all four mastered can pierce that immunity at half damage. Nonmastered Storm halves ordinary damage. Lightning owners resist native lightning; Storm prevents fall damage, but void and kill bypass protection. Unlike Phoenix, Lightning has no Rebirth.

Powers end when food reaches three icons, submerged without all-four mastery, on death, logout, or dimension change. Flight permissions/speed are restored and recovery survives logout. Rush/Storm fuel progress is saved so toggling does not reset fuel. Changing selection or opening a menu cancels held inputs; Storm can stay active while selecting an element.

Lightning visuals do not start fires or transform nearby mobs. Attacks target hostile mobs or non-allied players, never the caster, villagers or pets; walls block initial targeting, chains and strike splash. Normal body collision stays human-sized under the cloud.

## Testing

Eat the marked item from `/creeperborn testfruit lightning`. Use `/creeperborn fruitmastery 1|2|3` (or lightningmastery) after eating. Only one fruit can be eaten; use a fresh player/world or lose the prior fruit by actual death.

- Tier 1: aim tap Z at a hostile and check two nearby chains. Check walls and misses.
- Hold Z: early release cancels; hold until 100% then release. Check the impact area without terrain damage.
- Tier 2: tap X toward clear air, then toward a wall. Hold X to steer Rush; release to stop.
- Tier 3: tap C, use F5 to inspect the cloud, fly and aim Z at mobs or blocks. C again ends form.
- Compare normal 60s Storm against all-four mastered 90s Storm; check attack damage and PvP immunity bypass.
- Verify swimming/drowning curse before elemental mastery, and ordinary swimming after mastering all four.
- Logout/rejoin during Storm and verify no leaked flight permission. Actual death releases Lightning for another player.

## Alpha.32 playtest changes

Tap Z has a half-second recovery and a six-tick branching white/blue bolt from the casting hand to the target, with visible chain arcs. Bolt displays never save to the world and are removed after their animation or owner disconnect.

Judgment requires neither full health nor a minimum hunger level. Low hunger now ends only Storm/Rush; it no longer clears a normal Z charge. Repeated held-key events are ignored by the client, and duplicate server presses preserve the original charge time. The eight-second requirement, water curse and heartbeat cancellation remain.

Storm visuals are three times larger in both horizontal axes; height, duration, movement, damage area and player collision remain unchanged.

Flash Step now has three charges normally and five with all-four elemental mastery. Each accepted jump spends one. All charges refill three seconds after the latest successful jump. Failed placement spends none, and logout cannot reset spent charges.

## Alpha.33 Rubber counter

Rubber fruit owners take 80% less damage from all Lightning fruit attacks: normal bolts, chain arcs, Judgment and both Storm strikes. Bounce Guard stacks with this resistance, leaving 4% of the original electric damage before other native defenses. Gear 5 blocks Lightning fruit attacks completely, including from all-four masters, without hurt feedback.

Lightning damage is classified only during its own damage call. A sword held by the caster does not make the lightning a sharp-weapon attack; ordinary melee/heat weaknesses and unrelated damage remain unchanged.
