# Tremor alpha.52

Seaquake now sends two low opposing ground fronts inward at every tier. They stop at their shared center instead of crossing. Enemies contacted by a front are pushed toward the center with collision-respecting server movement, then take a small squeeze hit. Capture lasts at most 70 ticks and clears on interruption, logout and death.

Island Shaking and moving ground fronts copy the actual surface BlockState into native temporary BlockDisplay fragments, preserving grass tops/sides, sand, stone and other native block models. The original terrain stays in place. Native rendering preserves model textures and tint handling. Fragments lift/tilt and expire; no water, block writes or explosions are used.

Tests cover inward enemy movement, matching grass/sand display states, untouched build/fluid blocks, cleanup and previous Tremor/Paw behavior. Human play testing remains useful for visual scale and crowd-control feel.

Gekishin and Seaquake charge two separate vibration spheres at hand height, following player aim. Release or cancel discards both. Incoming ground fronts use jittering fracture/ripple geometry rather than water walls.
