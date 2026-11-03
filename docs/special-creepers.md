# Special creepers: chest icons and farm limits

Alpha.95 uses the existing edible item model on each special creeper's chest.
Elemental creepers show the matching elemental symbol; Fruitborn show the exact
fruit. The icon follows body rotation without spawning display entities. The
original identifying custom name remains saved internally, but its floating
nameplate is hidden. Existing special creepers receive icons too.

The original conversion odds remain 2% for elements and 0.2% for fruit, with the
existing fruit availability, rarity and nearby-Fruitborn rules still applied.
Each eligible creeper is checked once. Conversion limits do not alter ordinary
creeper spawning, damage attribution or loot eligibility of an existing special
creeper. Admin test-spawn commands bypass natural conversion limits.

Defaults in the server's `config/creeperborn.json`:

| Setting | Default |
| --- | --- |
| `limitSpecialSpawns` | `true` |
| `specialSpawnWindowSeconds` | `600` |
| `specialSpawnRadius` | `128` horizontal blocks |
| `elementSpawnsPerArea` | `4` |
| `fruitSpawnsPerArea` | `1` |
| `elementSpawnsPerPlayer` | `8` |
| `fruitSpawnsPerPlayer` | `2` |

A conversion checks all recent same-type conversions within the radius, without
chunk-grid boundaries. It is also attributed to the nearest living,
non-spectator player within 128 blocks if one exists. Personal limits span
dimensions; area limits are dimension-specific. Separate explorers have their
own personal allowance and independent areas. These are maximum successful
conversions, not guaranteed spawns, and are not a perfect farm detector.

The rolling history is written atomically to the world's
`creeperborn-spawn-budget.json` after successful conversions. Killing, unloading
or restarting does not replenish an area's allowance. Expired entries cease
counting after the window. Edit config while the server is stopped and restart.
An existing special creeper is never stripped of its identity or loot because
it was moved into a trap or its area later reached the limit.
