# Night Splitters

Fabric mod for Minecraft 26.3. Current build: **0.1.5-test**.

## Install the test build

Requires Java 25, Fabric Loader 0.19.5 or newer, and Fabric API 0.161.0+26.3.

1. Close Minecraft.
2. Download the **Night-Splitters-Test-Build** artifact from the latest successful [GitHub Actions run](https://github.com/zcaton99/Night-splitter/actions).
3. Extract the ZIP. Replace the previous Night Splitters JAR in the instance's `mods` folder with `night-splitters-0.1.5-test.jar`; keep only one Night Splitters version.
4. Start Minecraft with the matching Fabric profile.

This is a test build, pending another in-game round before public release.

## Airborne pounce in 0.1.5-test

Tagged swarm parents and offspring can launch at airborne Creative targets 2–25 blocks above them and within 24 horizontal blocks. Pounces need ground contact, clear line of sight, and takeoff headroom. Each launch has a random 5–8 second cooldown. The trajectory aims at the target's launch-time position, with no midair homing. Ground pursuit and melee range stay unchanged.

Vanilla impulse protection discounts the height gained by the pounce when landing. It does not grant general damage immunity: falling below the launch height can still deal damage from the extra drop. The protection ends with the impulse.

For this test round, try hovering at 5, 15, and 25 blocks above open ground. Move sideways after launch to dodge. Confirm cooldowns, safe landings, small-offspring pounces, and that ordinary spiders never gain this ability. Targets higher than 25 blocks cannot trigger a pounce.

## Damage behavior retained from 0.1.4-test

- Tagged swarm spiders pursue Creative players using the pursuit behavior confirmed in 0.1.3-test.
- Their direct melee hits now bypass the separate Creative ability immunity check and apply half incoming damage.
- The player's actual abilities and game mode are never changed.
- Ordinary spider attacks and environmental damage retain Creative immunity. Spectators and explicitly invulnerable entities remain protected.
- Survival and Adventure damage, wave spawning, splitting, budgets, cleanup, and cooldown logic are unchanged.

The damage change keeps vanilla armor, absorption, damage cooldowns, and difficulty processing.

## Existing wave mechanics

At night in the Overworld, eligible outdoor players can trigger one global wave of 2–3 parent cave spiders. Parents split once into two smaller offspring on death. A new wave waits until all tracked spiders and offspring are gone, then uses the configured 5–30 second cooldown. Existing slot caps, lifetimes, spawn checks, and dawn cleanup still apply. Peaceful difficulty and disabled mob-spawning game rules prevent new waves.

Configuration: `config/night-splitters.json`.

Operator commands: `/nightsplitters status`, `enable`, `disable`, `reload`, and `clear` (each subcommand follows `/nightsplitters`).

## Next in-game test round

Use a test world with cheats, Normal difficulty, nighttime, and open ground:

- **Creative:** swarm spiders chase and land hits. Creative normally hides the heart HUD; use `/data get entity @s Health` immediately before and after a hit to check actual health. Normal unarmored 4-point melee damage becomes 2 health points (one heart).
- **Creative immunity:** an untagged ordinary cave spider and fall damage still cannot hurt you.
- **Survival:** normal melee damage and pursuit still work.
- **Spectator:** swarms ignore you and do no damage.
- **Splitting:** each parent produces exactly two smaller children, which also pursue Creative players.
- **Wave completion:** no new wave starts while a parent or child is alive; after all are gone, wait 5–30 seconds.
- **Cleanup:** confirm dawn cleanup and the `clear` and `disable` commands.

Before sharing publicly, complete this checklist in game, including a dedicated-server or multiplayer test if that is how the mod will be used.

## Build and regression checks

GitHub Actions builds with Temurin Java 25, then boots a Fabric dedicated server with a separate test mod. It verifies tagged Creative melee damage at half strength, unchanged Creative abilities, ordinary-spider and fall immunity, full Survival damage, Spectator immunity, and explicit entity invulnerability. It also checks pounce eligibility, launch direction, cooldown bounds, no midair retargeting, safe landings, unchanged ordinary fall damage, child pounces, and trajectory heights from 2 through 25 blocks. The test mod is not included in the distributed JAR.

Run locally with Java 25:

```sh
bash gradlew build
mkdir -p build/regression-run
echo 'eula=true' > build/regression-run/eula.txt
bash gradlew runRegression
```

The regression run must print `NIGHT_SPLITTERS_DAMAGE_REGRESSION_PASSED`. The EULA line records acceptance of Minecraft's EULA for the local test server.
