# Azure Dragon — alpha.59

Original Minecraft cuboid models and pixel textures guided by the approved concept, not imported Sketchfab assets.

C tap: Hybrid at every tier. Tier 1 emerging blue scales/small horns/short tail; tier 2 larger body, horns, purple cape; tier 3 full powerful hybrid, longer articulated tail. Hybrid limits 30/60/180 seconds. C hold one second: mastery Full Dragon, maximum 150 seconds. Direct switches share remaining form time; no timer reset exploit. Form switch cooldown 2 seconds, transition 1/1.5 seconds. G/R element selection ends transformations through normal selection rules.

Z tap: Thunder Bagua 8/12/16 damage and rush 5/8/12 blocks; 1.75-second cooldown. Z hold one second: charged Bagua 14/20/26, six-tick windup, 5-second cooldown. X tap: intertwining alternating red/black lightning blades, damage 6/9/12, 28/28/40 range, one human or three hybrid blades; 3-second cooldown. X hold: twister radius 4/6/9, 2 seconds; 9-second cooldown. Hybrid scales reduce damage 35%, mastery 45%.

Dragon: WASD camera-relative flight, Jump ascend, Shift descend; no input hovers. Server steers and enforces walls/world border/loaded chunks/Birdcage. Body is 24 connected segments, four separately articulated legs at front and rear, flame clouds and gripping animation while moving. Invisible temporary body hitboxes route incoming attacks to the real owner with native damage cooldowns. Third-person camera automatically restores previous setting on exit. No creative flight permissions or permanent gravity changes.

Dragon Z tap Bolo Breath: 18 damage, 70 range, burning, 3.5-second cooldown. Z hold: 4-second stream, 6 damage per half second, 10-second cooldown; release stops the stream. X tap: five 12-damage lightning blades, 55 range, each enemy once per blade; native hurt cooldown prevents simultaneous triple damage. X hold: four-second twister, radius 9. Basic attack: bite (8) or hybrid club (6/8/10), 0.8-second cooldown. Native held item is never replaced or consumed.

Dragon C hold TWO seconds: Flame Dragon Torch 10 seconds, four food, 120-second persisted cooldown. Z consumes fiery state into Rising Dragon: Flame Bagua, a limited-steering charge, 36 damage once per enemy; transformation ends after the finisher. Short C hold exits dragon; C tap switches to Hybrid.

Dragon scales 50% damage reduction. Actual mastered elemental attacks bypass half the scale protection; Harmony elemental attacks bypass all. Ordinary attacks remain effective. No immunity to drowning/void. All four-element Harmony cancels normal fruit water curse as elsewhere. Transforms cost one food; active forms drain one food per ten seconds in Survival and end at low food. Maximum durations assume enough food. Form, projectile, hitbox and camera state clean up on selection change, death, disconnect, dimension change, suppression and server stop.

Commands: /creeperborn testfruit azure, eat fruit; /creeperborn azuremastery 1|2|3. Existing fruit must be released before eating another (Dark exception unchanged). New world singleton ownership, capture transfers, persistent cooldowns and rare Azure Fruitborn creeper/drop integrated. No actual block, liquid, fire or explosion changes.

Validation: server tests cover ownership/reload/capture/death, all evolving tiers, real damage/counters, rush/cooldown, blades/breath, 150-second timer, hover/movement, body hit redirection, ultimate and cleanup. Client smoke bakes icon and native meshes, submits all three hybrid tiers and effect variants, and checks isolated pose stacks. Human testing still needed for model scale, camera comfort, animation appearance and multiplayer feel.
