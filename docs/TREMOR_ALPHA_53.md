# Tremor alpha.53

Island Shaking charges a horizontal quake circle centered on the player, expanding to its tier radius over two seconds. Release triggers four outward circular rings of terrain-matched block fragments, spaced six ticks apart. The fragments pop up to 1.8 blocks, tilt and settle out of view, then expire. Real terrain remains unchanged.

Gekishin and Seaquake retain their existing dual-hand charge and damage behavior. Tests cover the centered charge, scheduled radial sequence, terrain-state copying, full cleanup, and existing abilities. Client smoke includes the new charge-circle model and pose isolation.
