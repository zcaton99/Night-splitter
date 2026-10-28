# Love-Love and Sand-Sand — combined alpha.54

Minecraft 26.3 / Fabric / Java 25. Fruits 11 and 12; retains alpha.53 Tremor/Paw behavior. All geometry is temporary: no block placement, excavation, real sand, water placement or explosions.

## Love-Love

| Input | Move | Tiers 1 / 2 / 3 |
|---|---|---|
| Tap Z | Pistol Kiss | Heart projectile; 4/6/8 damage and 20/25/30 petrification; range 20/28/36; 0.8s recovery |
| Hold Z 1s, release | Love-Love Beam | Broad aimed heart beam; 8/10/14 damage and 45/55/65 buildup; range 16/24/32; 6s recovery |
| Tap X | Perfume Femur | Forward kick; range 3/4/5, damage 6/8/10 and 25/30/35 buildup; 1.75s recovery |
| Hold X 1s, release | Slave Arrow | Seven heart arrows in a fan; each 3/4/5 damage and 12/15/18 buildup; 8s recovery |
| Tap C | Heart Step | Collision-respecting directional dodge, 4/6/8 blocks; 2s recovery |
| Hold C 1s, release | Stone Breaker | Shatters frozen enemies within 6/9/12 blocks, 10/15/22 damage and releases the statue; 9s recovery |

Buildup slows targets and adds visible stone patches. At 100 they become a stone statue: players freeze at most two seconds; mobs 1.5/2.25/3 seconds. After release they resist new buildup for five seconds. Avoiding hits starts buildup decay after 1.5s. Frozen mob AI is restored to its original state. Stone Breaker releases its target only when the damage is accepted. Solid walls block attacks.

## Sand-Sand

| Input | Move | Tiers 1 / 2 / 3 |
|---|---|---|
| Tap Z | Desert Spada | Traveling crescent; 4/5/7 damage, range 18/26/34; 1s recovery |
| Hold Z 1s, release | Desert Girasole | Aimed quicksand radius 3/5/7; pulls and slows above unchanged terrain for 5s; 9s recovery |
| Tap X | Sables | Traveling sand tornado, short carry and 2/3/4 chip damage; 5s recovery |
| Hold X 2s, release | Desert Storm | Follows caster; radius 4/6/8, 1/2/3 chip damage and brief enemy blindness; lasts 3/4/5s; 20s recovery |
| Tap C | Sand Drift | Dash 8/12/16 blocks; 3/4/5 stored charges; one charge returns every 3s |
| Hold C 1s, release | Desert Drain / Sand Coffin | Nearby aimed enemy wrapped in an assembling sand coffin; periodic drain, compression and final crumble; 12s recovery |

Coffin reach is 4/5/6 blocks. Players are trapped at most two seconds, mobs 2/3/4 seconds. Periodic damage is 2/3/4; final pulse 4/6/8. Only actual health damage heals the caster, at half its value with a six-health total cap. Failed damage, absorption-only hits and invulnerable targets give no free healing. Native mob AI is restored on release/interruption. Coffins cannot replace an existing restraint. The burial never moves victims beneath real blocks.

Tap moves cost one food point; held moves two. Powers require food above six. Harmony boosts damage and shortens normal cooldowns by 15%; drift charge recovery remains three seconds. Cooldowns and spent drift charges survive reconnecting. Standard fruit mastery thresholds apply.

Both include unique persistent ownership, natural Fruitborn Creepers/drops, capture tokens and Dark second-power support, water curse, custom icons, charge/effect models, root/input enforcement and bounded cleanup. Death, logout, water, selection changes and Dark suppression clear active effects. Effect displays have hard lifetimes and native orphan checks.

## Test in game

Replace the previous JAR. Use `/creeperborn testfruit love` or `/creeperborn testfruit sand`, then eat. Existing normal fruit ownership must be released before eating a different normal fruit. Select with G; B toggles combat if needed. `/creeperborn lovemastery 1|2|3` and `/creeperborn sandmastery 1|2|3` select tiers. `/creeperborn test love` or `test sand` spawns the matching Fruitborn.

Server regression exercises all twelve inputs, real damage/buildup/freeze/shatter, player time limits and recovery, original AI restoration, walls, pulls/carry/blindness, healing limits and failed-damage healing, dash distance/collision/charge persistence, world/claim persistence, capture transfer, natural drops, suppression and cleanup. Client smoke loads all twelve fruit icons and submits every effect with pose-isolation checks. Human testing is still needed for appearance, multiplayer feel and balance.
