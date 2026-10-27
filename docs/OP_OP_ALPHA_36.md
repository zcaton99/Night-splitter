# Op-Op alpha.36

Minecraft 26.3, Fabric, Java 25. Install the same JAR on client and server.

## Controls and progression

Select with G; toggle combat with B. All attacks need the caster and target inside the same active ROOM.

| Tier | Tap | Hold |
| --- | --- | --- |
| 1, Z | Toggle ROOM | Gamma Knife |
| 2, X | Counter Shock | Heart Extraction (aim steadily for 3 seconds) |
| 3, C | Shambles | Takt (lift, release to throw) |

ROOM is a noncolliding, light-blue transparent sphere fixed at its creation point. Diameter is 25 / 32 / 40 blocks; duration 30 / 60 / 90 seconds. Opening and abilities cost hunger, with one food point per five active seconds. Ends at low food, death, logout, dimension change, fruit suppression or switching to another fruit. Leaving ROOM prevents casting but does not move its center. Closing has a five-second recovery.

Tier 2 requires 10 uses and five active minutes; tier 3 requires 40 uses and one active hour. Mastered Shambles has five charges (three otherwise), refilling five seconds after the last successful swap. Swaps check both bodies, loaded destinations, world border and riding status.

## Extracted hearts

Unlimited distinct live targets at once, one cube per victim. Cube names identify both victim and extractor. Right-click for one heart of damage, once per second; repeat to kill. Sneak and hold use for three seconds to deliberately crush. Sneak-use directly on the linked living target returns its heart.

Fire, lava, explosions and other destructive item removals kill the linked victim. A destroyed heart for an offline player queues death until join. Chunk unloading is not destruction. Dropped cubes do not naturally despawn; after five minutes on the ground they enter durable return storage and deliver to the extractor. Offline owners and full inventories defer delivery. Inventory/container storage pauses the ground timer.

Target death invalidates its cube; extractor death releases all their hearts. Copies of an invalidated token are inert. Hearts and fruit ownership save separately as world JSON files. No terrain edits.

## Test commands

- /creeperborn testfruit op
- /creeperborn testcreeper op
- /creeperborn opmastery 1|2|3
- /creeperborn fruitmastery 1|2|3 (currently selected fruit for Dark)
- /creeperborn status

Test ROOM boundary entry/exit, all six controls, hunger and mastery, swap geometry, unlimited heart collection and labels, repeat squeezes, safe return, physical destruction, logout/restart and a full inventory. Use a disposable world for lethal-heart tests. Networked two-player interaction still needs real player testing even after native server regression and client startup checks.
