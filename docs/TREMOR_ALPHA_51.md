# Tremor-Tremor — alpha.51

Minecraft 26.3 / Fabric / Java 25. Tenth fruit; preserves all alpha.50 Paw fixes.

| Input | Move | Tiers 1 / 2 / 3 |
|---|---|---|
| Tap Z | Quake Punch / weapon-coated Kabutowari | Reach 5/7/9; damage 7/10/14; 1s cooldown |
| Hold Z 1s, release | Gekishin | Close blast radius 4/7/10; damage 12/18/26; 8s cooldown |
| Tap X | Shock Ball | Traveling sphere range 24/36/48; damage 7/10/14; 2.5s cooldown |
| Hold X 1s, release | Kaishin / Seaquake | Early tiers forward wall travels 20/30, width 10/16, height 5/8; damage 10/14; 25s cooldown |
| Mastered hold X | Twin tsunamis | Two opposing walls, 24 wide and 14 tall, start 20 blocks to each side of an area 12 blocks ahead, each crosses 40 blocks over 2s; damage 18 per wave |
| Tap C | Gurash | Controlled hop and downward impact; radius 5/8/12, damage 8/12/18; 6s cooldown |
| Hold C 2s, release | Shima Yurashi / Island Shaking | Radial launch radius 8/14/22; damage 10/16/24; 30s cooldown |

Tap moves cost one hunger point, held moves three; powers require food above six. Harmony adds 15% damage and shortens cooldowns. Cooldowns persist through logout. Mastery uses existing 10 uses/300s and 40 uses/3600s progression.

Island Shaking briefly lifts and tilts visual ground plates above the existing surface. Air fractures branch outward at impact; vibration spheres and expanding rings are original model geometry. Tsunamis are curled blue display meshes with white crests. Every effect has a hard lifetime and a native entity-tick orphan check. No attacks edit blocks, place water, create explosions, or damage passive mobs, armor stands or item frames. Solid walls block damage through line-of-sight checks. The waves are supernatural visual effects and also work on dry land.

Gurash respects collision, build height and Birdcage, prevents competing movement during its short flight, and gives temporary landing protection. Suppression, water, fruit switching, logout and death clear active effects. No permanent gravity or camera changes.

Includes unique persistent ownership, natural Fruitborn Creeper/drop, Dark capture/second power support, water curse, original inventory icon and commands. The icon source is `docs/fruit-tremor.svg`.

## Try it

Replace your previous JAR. Run `/creeperborn testfruit tremor`, eat it, select fruit with G and enable combat with B as needed. Use `/creeperborn tremormastery 1`, `2`, or `3`. `/creeperborn test tremor` spawns its Fruitborn Creeper. Test holding X at mastery in an open area: the waves approach from opposite sides and pass through each other.

Server regression exercises all six inputs, weapon strike, cooldown/charge gates, real wave/projectile damage, controlled slam, preserved building/fluid blocks, expiry, ownership, death/drop and suppression. Client smoke bakes all ten fruit icons and submits every Tremor effect while checking transform isolation. Multiplayer timing, visual scale and overall feel need human play testing.
