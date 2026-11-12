# Gas Gas — alpha.100

25th Creeper Fruit; discover a Gas-Gas Fruitborn Creeper or use `/creeperborn testfruit gas` for testing. Eat it, then `/creeperborn gasmastery 3` or `/creeperborn fruitmastery 3` to test mastery. G selects the fruit and displays its moves.

| Input | Move | Tier |
| --- | --- | --- |
| Z tap | Gas Shot: projectile, poison, small gas pocket | 1 |
| Z hold for 1 second | Choking Cloud: aimed gas pockets, damage, poison, slow, nausea pulse | 1 |
| X tap | Gas Dash: collision-checked dash with a gas trail | 2 |
| X hold for 1 second | Poison Veil: following cloud and 20% physical resistance | 2 |
| C tap | Combustion: nearby owned clouds turn orange, then detonate after 1 second | 3 |
| C hold for 1 second | Gas Body: native flight, 75% physical resistance, visible upper body and gas lower body | 3 |

Gas Body lasts up to two minutes, limited by food. Hold C again to end it. Fire, environmental damage and Harmony attackers bypass its physical resistance. Gas does not change the player's collision box or permit flight through walls. Water suppresses the fruit without Harmony.

Active Gas grants poison immunity. Nausea pulses last five seconds with ten seconds between applications per victim. Existing effects are not indefinitely refreshed by overlapping clouds. Gas fields cannot damage allies or the caster, expire on their own and are capped at 12 per owner and 96 globally. Combustion damages each victim once per simultaneous burst, consumes its clouds, and never edits terrain.

G can preview another fruit while carrying the form. Using the other fruit clears the Gas form, veil, clouds and shots. Death, disconnect, suppression, reset and restart recover granted flight permissions.

Validation: Gas server regression covers claims, tier gating, projectile hits, effects, wall collision, combustion, defenses, Holy Bag transfer, save reload and reset. The native two-client scenario covers poison/nausea against a connected player, form synchronization, combustion warning and cleanup, sustained flight/chunk streaming and transfer cleanup. Client startup verifies all Gas textures and model rendering paths.
