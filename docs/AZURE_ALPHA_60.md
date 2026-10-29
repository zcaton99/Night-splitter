# Azure alpha.60 — default Hybrid, elements and smoother flight

Hybrid is the default Azure appearance from eating the fruit, with emerging/developed/mastered looks at tiers 1/2/3. It has no timer and no idle hunger drain. Attack food costs remain. R selects elements without removing Hybrid or Dragon. G restores Azure attacks without resetting the form, Dragon's remaining 150-second timer, or attack/ultimate cooldowns.

| Input | Hybrid | Full Dragon |
| --- | --- | --- |
| Z tap | Thunder Bagua rush | Bolo Breath |
| Z hold 1 second | Destroyer Bagua heavy strike | Sustained Bolo Breath, max 4 seconds |
| X tap | Three intertwined red/black lightning blades | Five-blade lightning fan |
| X hold 1 second | Twister Smash | Dragon Twister |
| C tap | Thunderclap: radial club/lightning impact | Flame Dragon Torch; C tap again launches Rising Dragon: Flame Bagua |
| C hold 1 second | Enter Full Dragon at mastery | Return to Hybrid |

Thunderclap: radius 4/6/9, damage 6/9/12, eight-second cooldown. Torch: up to ten seconds, persisted two-minute cooldown, four food cost. Finisher returns to Hybrid. Z remains breath throughout Torch. Dragon remains limited to 150 seconds, drains one food per ten seconds in survival, and returns to Hybrid early at low food. Hybrid never drops to a separate human moveset. Water weakness and Dark suppression still disable Azure; Harmony retains the existing water exception. Death releases fruit ownership normally.

Dragon movement uses the same collision-aware flight motion for local prediction and server movement. Continuous movement no longer teleports the player every tick. A dedicated once-per-tick input stream preserves the combat rate budget; finite aim and controls are validated. Large positional drift still receives bounded server corrections. WASD moves, Jump rises, Shift descends; no keys or opposite keys hover.

Camera: roughly 30-block reveal, smooth two-second move to 6.5 blocks behind/above the head. Distance is interpolated at render time, with native camera collision and an additional upward ray. Previous camera preference restores when Dragon ends.

Dragon helpers now have persistent native invisibility plus a client render safeguard keyed by a synchronized hidden name. The body begins in an S curve, follows bounded section spacing, and uses longer overlapping beveled sections, pale belly plates and a larger head. All four flame-cloud feet remain, including the rear pair. Original models and textures; no external model files imported.

Validation includes real native helper ticks (invisibility persistence), element-switch appearance/timer/flight checks, default Hybrid/Thunderclap, revised ultimate controls, damage/counters, movement/hover, ownership/capture/death, body spacing and cleanup. Client smoke checks camera interpolation, hidden-helper render-state mixing, all forms and visual meshes, icon baking, and pose isolation. These checks do not establish final in-game visual quality or real-network smoothness; the user's playtest remains necessary.

Testing: install this JAR on client and server. Existing Azure owner can use G; new owner uses /creeperborn testfruit azure and eats it. /creeperborn azuremastery 1|2|3 changes tier. Earlier alpha.59 client/server must both be replaced because alpha.60 adds a flight payload. All prior fruits retained. No destructive terrain, fire, liquid placement or explosions.
