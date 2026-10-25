# Night Splitters — 0.2.0-alpha.3

Private next-update build on `development/0.2.0`. The submitted Modrinth 0.1.5 build and `main` branch are unchanged.

Requires Minecraft 26.3, Java 25, Fabric Loader 0.19.5+, and Fabric API 0.161.0+26.3. Install just one Night Splitters JAR at a time. For this alpha, use a separate test world or a copy of your server world.

## New settings

All new modes are OFF by default. Existing configuration files keep the previous behavior. Configuration is saved in `config/night-splitters.json`.

| Command | Behavior |
| --- | --- |
| `/nightsplitters wave multiplier 1` | Normal 2–3-parent waves. |
| `/nightsplitters wave multiplier 4` | 8–12 parents. Valid range 1–16; global/local slot budgets scale too, with an absolute 1,536-slot ceiling. |
| `/nightsplitters darkness on` / `off` | About 20% of spawned swarm spiders can add 4 seconds of Darkness to successful melee hits. |
| `/nightsplitters darknesschance 0.20` | Set the chance for newly spawned spiders (0–1). Existing configs keep their saved value; run this command to double the previous 10% setting, then clear the wave. |
| `/nightsplitters darknessonly on` / `off` | Every swarm spider hit applies Darkness instead of direct damage or poison, regardless of the 20% selection. Turning it off restores normal spider damage; the separate Darkness toggle still controls the 20% effect. |
| `/nightsplitters babyhorde on` / `off` | 15% chance for a spider wave to be exactly six small offspring instead of parents. These never split. The multiplier does not enlarge baby-only hordes. |
| `/nightsplitters creeper on` / `off` | Enable rare intermission creepers: one encounter after a randomly chosen 3–5 completed spider waves. |
| `/nightsplitters creeperblockdamage on` / `off` | Parent explosion terrain damage. Default ON, still subject to vanilla mob-griefing rules. |
| `/nightsplitters babycreeperblockdamage on` / `off` | Baby explosion terrain damage. Default OFF. |
| `/nightsplitters testcreeper` | Replace the current loaded wave with a creeper encounter for testing. Requires a player, open loaded outdoor terrain, nighttime, non-Peaceful difficulty, enabled mob spawning, and both swarms and creepers enabled. |
| `/nightsplitters clear` | Remove tracked loaded spiders/creepers and cancel pending births. |
| `/nightsplitters disable` | Disable all encounters, remove tracked loaded swarm entities, and cancel pending births. |
| `/nightsplitters status` | Show modes, multiplier, budgets, active creepers, and pending broods. |

Commands require operator permissions. The global `enable`, `disable`, `clear`, and `reload` controls remain available.

## Creeper encounter

The rare parent uses a normal 3-block explosion radius. If it explodes, two half-size, faster baby creepers emerge **40 game ticks (2 seconds at 20 TPS)** later. Babies have 10 health and weaker radius-1 explosions. Killing a parent before it explodes does not produce babies. Babies never split. The next wave waits for the parent, delayed births, and babies to finish, then uses the existing 5–30 second cooldown. Each interval randomly selects 3, 4, or 5 completed spider waves (including baby hordes). Creeper completion never counts as a spider wave. If no valid spawn is found, the encounter stays due for the next completed wave. Clear, restart, dawn cleanup, toggling creepers, and testcreeper reset the interval. The old creeperChance config field is ignored.

Both parent and babies hunt nearby non-Spectator players, including Creative players, but use vanilla explosion damage/immunity. The existing Creative half-damage exception applies only to swarm **spider melee**. Darkness-only mode also applies only to spiders; creepers still explode normally.

Creeper-off, global disable/clear, Peaceful difficulty, configured dawn cleanup, and lifetime expiry clean up the encounter. Offspring spawning never force-loads chunks: if terrain is unloaded, births wait up to 10 extra seconds, then cancel. Pending 2-second births are cancelled on server restart rather than saved.

## Suggested testing sessions

1. **Baseline:** leave all new modes off. Confirm the existing 0.1.5 waves, split children, Creative damage, pounces, and dawn cleanup.
2. **Darkness:** enable `darkness`; then test `darknessonly` separately in Survival and Creative. Darkness-only should not remove health or apply poison. Turn both off to restore baseline.
3. **Baby hordes:** enable `babyhorde`. For a quick deterministic check, set `babyHordeChance` to `1.0` in the config and reload. Count six babies and verify none split. Restore the default `0.15` afterward.
4. **Multiplier:** start at `2`, then `4`; confirm wave size, one-wave-at-a-time behavior, and server performance. Reset to `1` between sessions.
5. **Creeper:** enable `creeper`, turn parent block damage off if you want the test terrain preserved, then use `testcreeper`. Let it explode and time the babies. Test killing the parent before explosion, killing both babies, and using clear/disable during the 2-second delay. Finally test rare intermissions naturally.

Do not overwrite the public 0.1.5 JAR or upload this alpha to Modrinth until the gradual playtesting is complete.

## Automated checks

GitHub Actions uses Java 25 and a real Fabric dedicated server. The suite retains the Creative-damage and actual pounce-physics checks, and adds command/config validation, Darkness-only damage/poison suppression, delayed real creeper explosions and births, one-generation-only splitting, terrain protection, and cancellation by disable. The test mod is excluded from the distributed JAR.
