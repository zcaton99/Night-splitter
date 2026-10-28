# Paw alpha.49

Ursus Shock grows and compresses in front of the player while charging. Release emits expanding air rings centered on the caster, damaging enemies in all directions within tier radii 4/7/10 blocks. It no longer selects a distant target.

A native entity-tick watchdog removes untracked legacy paw displays, even those lacking alpha.48 tags. Active visuals retain bounded expiry; unrelated displays are preserved. All alpha.48 step and pain fixes remain.

Regression covers front/back/side damage, radius exclusion, native orphan cleanup, unrelated display preservation, and existing Paw tests. Client smoke includes shockwave rendering and transform isolation.
