# Door-Door alpha.21

Door is a separate rare fruit alongside Rubber. One fruit per player and one living owner per fruit per world. Existing Rubber ownership saves migrate without losing ownership. Death releases the owned fruit and its progression; elemental progress remains.

## Moves

| Key | Move | Time | Radius from entrance | Cooldown after session closes |
| --- | --- | --- | --- | --- |
| Z | Door Passage | 20s | 64 blocks | 10s |
| X | Rescue Door | 6s | 20 blocks | 30s |
| C | Shared Passage | 30s | 96 blocks | 45s |

Z/C create a visible warped door ahead on clear ground. Walk into it. X immediately enters. While traveling, Z requests an exit at your feet. The exit needs clear body space, solid ground, and no water/lava/fire. Unsupported or blocked exits are rejected without ending travel.

All-four elemental mastery adds 50% to travel time/range and increases the shared admission window from five to eight seconds. Shared guests enter voluntarily after the owner enters, fly independently inside the same radius, and share the owner's deadline. After the owner creates an exit, guests have five seconds to walk into it or press Z; remaining guests arrive automatically. Before an exit is created, guests can press Z to return to their own entrance.

Timeout/cancellation/disconnect returns each traveler to their saved safe entrance, never the nearest arbitrary wall. If the saved location becomes obstructed, a nearby safe location is searched; vanilla respawn placement is the last fallback. Range, world border, height and loaded-chunk bounds restore the last in-bounds position. No cross-dimension travel. A saved player tag recovers the original mode/position/flight after a restart.

This prototype uses temporary vanilla Spectator mode internally for noclip, flight and interaction prevention, with a custom bounded travel session. Exiting restores the previous game mode and flight settings. Cyan HUD tint, translucent cyan-outlined living figures, and cyan dust/cloud puffs provide the first visual pass. Client appearance and actual multiplayer synchronization need playtesting; server fixtures do not establish visual quality.

## Progression and tests

Tier 1 unlocks Z; tier 2 requires 10 meaningful uses + 300 active seconds; tier 3 requires 40 uses + 1200 active seconds. Moving with Door selected advances time. Entering a passage, meaningful relocation, and transporting a passenger grant rate-limited use credit. A fruit's water weakness and all-four mastery protection follow the existing rules.

- `/creeperborn testfruit door` gives an operator a Door fruit. Players who own Rubber must actually die before eating Door, or use a fresh test world/player.
- `/creeperborn testcreeper door` spawns a test Fruitborn five blocks east.
- `/creeperborn doormastery 1|2|3` sets Door test progression after eating.
- G selects the owned fruit; B enables Z/X/C controls.
- `/creeperborn door exit` requests exit/return without client keybindings.
- `/creeperborn status` reports Door progress and ownership.

Test each mode with and without elemental mastery; blocked exits, tight rooms, ceilings, unsupported air, range limits, timeouts, water weakness, shared entry/exit, disconnect/rejoin, and graceful server restart.

## Later design: Holy Bag (not implemented)

A proposed permanent multi-fruit unlock earned through four distinct elemental guardian villagers and a cleric trade requiring all four elemental hearts/orbs. Each newly eaten fruit still needs separate mastery and must be available under the world's unique ownership rule. Exact trade sequence, guardian proximity/verification and whether the bag unlock survives death remain to be decided before implementation.
