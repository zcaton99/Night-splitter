# RDT: Connected Freedom — Chapter 1 alpha

A separate Fabric mod by Zach Caton, built around the RDT progression discussed for a hesitant, curious first pup. This first prototype ends with curiosity about a toy. Full tug, fetch, obedience, other pup profiles, and a polished custom dog model are future chapters.

## Install

Minecraft **26.3**, Java **25**, Fabric Loader **0.19.5+**, Fabric API **0.161.0+26.3**. Install the same RDT JAR on the dedicated server and each player's client. This mod uses its own item IDs, progress file and controls and can be installed alongside Creeperborn. Back up a test world before trying an alpha.

## Start playing

Press **J** to open your field guide, or craft a book and bone together to make one. `/rdt` also opens the same menu without requiring operator permission. Meet your pup on clear, dry ground in the Overworld. Choose their name. Each player has one individual pup, represented by a protected Minecraft wolf for this alpha.

1. Choose **Yes** to release into exploration. Walk with the pup on clear ground.
2. At the line's limit, **pause the walk**. Say the pup's **name** once. Allow a short opportunity to connect; use **Help** if needed. Early familiar help also follows automatically after a brief opportunity.
3. Once the pup reconnects and loosens the line, continue. Build three successful check-ins.
4. After a success, choose **Celebrate**, then walk or jog in a small clear loop with your pup for a few seconds. Both partners must actually move together. Practice twice.
5. Explore around a tree or solid post. When the line crosses a block, stop and retrace your route. **Help** encourages the pup along a clear path. Resolving one actual crossed line records the lesson.
6. Once these experiences and at least 20 blocks of shared walking are recorded, hold your RDT Play Toy and choose **Ready**, then **Yes**. The pup needs to actually approach before curiosity is recorded.
7. Choose **Done**, then share a quiet pause to complete the prototype chapter.

The current chapter uses visible learning milestones to help test the sequence. Those counts and comfort/engagement values are game estimates, not measurements of a real dog's feelings. The pup's actual movement and response determine progress; button presses alone do not complete the lessons.

## Default keys

| Key | Action |
|---|---|
| J | Field guide, name entry and coaching menu |
| N | Say the pup's name for connection |
| Y | Yes — release or engage after Ready |
| H | Familiar help |
| K | Start a moving celebration after a success |
| U | Ready — invite the first toy interaction |
| O | Done — end the activity and settle |

All controls can be rebound under Controls → RDT Training. The field guide also provides all actions as buttons. Right-clicking your pup opens the menu. Change the line among **3, 5 and 7 blocks** in the menu. Seven blocks is approximately 23 feet at Minecraft's usual block scale; it is a simplified game line, not a simulation of a commercial leash.

## Teaching decisions

- Name recognition asks for connection and is distinct from the future Let's go and Here recalls.
- The cycle of help follows the cue. Familiar help should support understanding rather than repeated commands.
- Yes releases into the selected context. Ready is not permission to grab; Done starts a settling period.
- The four windows apply throughout daily life and walks. The guide explains Work, Enough, Play and Free; full work-cue mechanics are outside this first alpha.
- Original coaching text summarizes the supplied RDT direction. Uploaded course notes and their personal information are not distributed with this mod.
- Practice around real dogs belongs with an appropriate adult and suitable supervision. The game teaches observation and does not declare unfamiliar dogs safe to approach.

## Development and tests

This project lives in `rdt-dog-training/` on the `rdt/prototype` branch of the existing repository. Build from this folder with `bash ../gradlew -p . build`. The resulting JAR includes only the RDT project, not Creeperborn or the test sources.

The dedicated workflow builds the JAR, checks learning/persistence and the native crafting recipe, renders journal screens, and runs two real clients against a dedicated server with network delay. Native multiplayer checks cover individual pup ownership, actual pathfinding and line connections, a real post obstruction, moving celebrations, the toy approach, persistence reload and both journals.

Progress is saved in each world's `rdt-progress.json`. The first alpha supports one Overworld walking pup per player. If you leave the area or dimension, the pup waits; the journal reports the last recorded location. This is not yet a cross-dimension companion system.
