# Alpha.97 testing notes

- Rubber's right-click now yields to boats and other friendly entity interactions. Mining, chests and furnaces still use ordinary Minecraft controls.
- The charge wind-up remains behind the owner in first person to keep the view clear. It is visible in third person and to other players; released punches retain their attack animation.
- Fruit weakness starts when the body is sufficiently immersed, rather than waiting for a tall Fox head to go underwater. Fox transformations end and the player sinks. All four mastered elements still protect the player; Axolotl retains its aquatic exception. Very shallow ankle water remains safe.
- The Holy Bag has a bright white pouch model, a gold seal and an enchanted glow. Existing bags receive the model during the normal inventory refresh. Collection and mastery still survive death.
- G switches the selected attack kit while retaining one existing transformation. Its timer continues. Starting another transformation replaces it; `/creeperborn formoff` explicitly ends it. Stored fruit mastery is retained. Forms end on death or disconnect, while the fruit collection remains permanent.
- The Three-Headed Dragon moves and turns continuously, with synchronized model and hitbox positions. Invisible hitboxes cannot inflict vanilla slime contact damage. Its damage source is named Three-Headed Dragon.
- The first five seconds are an awakening period without attacks. Breath locks its aim during the visible wind-up; the initial breath and sweep no longer combine with hidden contact damage into an instant death.
- Dragon visuals are temporary and are rebuilt after an unload. `/creeperborn canceldragon` clears loaded orphaned poses, hitboxes, fireballs and owned clouds even if the encounter is already inactive. Older saved dragon poses are discarded when their chunks load.

Stop the server and close Minecraft, then replace the previous Creeperborn JAR in both server and client `mods` folders. Use the matching version on every client. Back up the world first.

Quick test: `/creeperborn canceldragon`, then `/creeperborn testdragon` in a test world. Use Survival to check combat. Test G switching while in a form, a normal Rubber right-click on a boat, and an unmastered Fox entering deep water from the surface.
