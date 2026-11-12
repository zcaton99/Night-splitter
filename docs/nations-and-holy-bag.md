# Four Nations and the Holy Bag — alpha.97

Progress is shared by everyone in the world. A Holy Bag is an individual, permanent unlock.

## Establish each nation

1. Collect all four elemental hearts to earn the Elemental Trident.
2. Choose a village in the Overworld and stand within 16 blocks of an adult villager.
3. Run `/creeperborn nation found earth` (or `water`, `fire`, `air`). The current position becomes that nation's center. Centers must be at least 192 blocks apart.
4. Select the matching element and use the trident on nine adult villagers within 64 blocks of the center. Their jobs, trades and homes remain intact. Each villager can belong to only one nation.
5. Run `/creeperborn nation start earth` when ready. Founding or reaching nine does not start a raid until you activate it.

Each trial has three waves of 6, 8 and 10 enhanced attackers. Earth faces ravagers and vindicators; Water faces witches and pillagers; Fire faces evokers and vindicators; Air faces evokers and pillagers. Guardians fight alongside players. Kill every wave's attackers to advance; unloading them does not count as a kill.

A trial fails if all of its guardians die or its 20-minute active timer expires. Rebuild to nine living guardians and activate a retry. Trial progression pauses while no living player is within the nation. Successful trials remain complete even if villagers later die.

`/creeperborn nations` lists the four centers and their progress. Any sibling can contribute to the same progression. Trident ownership checks for assigning individual guardians still apply.

## Four-element offerings

After all four trials pass, bring one of each elemental heart to each nation. Stand inside its radius and run `/creeperborn nation offer earth` (substitute that nation's element).

Each offering consumes one Earth, Water, Fire and Air heart, once. Its guardians can then cycle through all four elemental attacks. Completing all four offerings unlocks the summoning recipe for the whole server. These offerings require 16 hearts in total.

## Summon the Three-Headed Dragon

Use `/creeperborn ritual` while carrying **4 End Crystals and 1 Nether Star** to craft one Dragon Covenant. Each repeat summon costs a new covenant.

Defeat the normal Ender Dragon first. With no normal dragon alive, stand within 16 blocks of the central End exit portal and use the covenant. Only one Three-Headed Dragon encounter can run at a time. The normal dragon's respawn ritual remains separate.

The boss has a shared body-health pool and three separate head-health pools. Its starting health scales for 1–6 nearby survival players: 2,700 body HP plus 900 per extra player; each head has 180 HP plus 60 per extra player.

- Center: telegraphed breath lanes and close bites.
- Left: dragon fireballs against distant or flying players.
- Right: sweeping attacks and knockback against nearby groups.

Head damage also chips away at body health. Severing a head removes its model and attacks and exposes the body to double damage for ten seconds. All heads severed leaves the body vulnerable. Below 25% body health, surviving heads attack more frequently. The boss makes gradual low passes so melee attacks can reach it.

Killing the boss drops **exactly one physical Holy Bag**, irrespective of player count. In the End, the reward appears above the central exit portal so it cannot be lost over the void. The bag is protected from ordinary item damage and has an extended pickup lifetime. Decide which sibling gets it; repeat the ritual for another bag.

## Permanent fruit collection

Use the bag to consume it and permanently unlock fruit storage. It does not grant fruits automatically: discover and eat them as usual. Existing fruit-ownership rules still apply; fruits reserved by another owner cannot be eaten.

Press **G** to cycle collected fruits, or use `/creeperborn bag` for clickable choices. `/creeperborn bag fox` equips a specific collected fruit. Each fruit retains its own mastery. G changes the selected attack kit without canceling one already-active transformation. Its timer continues, and starting a different transformation replaces it. Other stored fruits remain inactive. Use `/creeperborn formoff` to end a form explicitly.

Death retains the bag unlock, equipped fruit, stored fruits and their mastery. Dark's capture cannot steal a Holy Bag owner's collection. Players without the bag keep the original death and fruit-slot rules. Elemental progression is retained as before.

The world stores nation, encounter and collection ledgers alongside the existing fruit ownership ledger. Back up the whole world folder together.

## Operator testing

Use these only in a test world:

- `/creeperborn testdragon`: summon an encounter near you without nation requirements, including outside the End.
- `/creeperborn canceldragon`: dismiss the encounter without a reward. Normal resummoning still requires a new covenant.
- `/creeperborn testholybag`: receive one test bag.
- Existing `/creeperborn testfruit …` and mastery commands still work.

Close Minecraft and stop the dedicated server before replacing the Creeperborn JAR in both `mods` folders. All players need the matching version.
