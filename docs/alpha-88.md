# Creeper Born alpha.88

- Rubber: normal punches reach 20 blocks (charged 28; Gear 5 30), 0.2-second normal recovery, 1.2-second Gatling rest. Curled white shoulder/back smoke; Balloon uses the player's skin.
- Time: consecutive eligible dodges without a forced vulnerability gap. Creeper warning and explosion hits use long safe-ground escapes, at least 15 blocks from the explosion source. Mastered elements and tier-three fruit attacks still bypass Time. Mirage rendering avoids the first-person view; persistent clock/trails follow behind. C tap: Chrono Break damages, slows, then echoes.
- Fox: cloak Z tap summons 2/4/6 attacking doubles, subsequent taps swap positions; Z hold Rasengan. Full fox keeps Z claw / hold fireballs. Cloak overlays attach before vanilla rendering transforms; giant camera frames the head for aiming.
- Axolotl: fin follows the tail, giant head aiming camera, brief status messages instead of constant instructions. Tap/hold cooldowns are independent, so Regrowth does not lock Gill Jet.
- Inputs: larger bounded packet budget for simultaneous movement, heartbeat and release events; selecting an active power synchronizes combat input availability.
- Mastery: `/creeperborn fruitmastery 1|2|3` handles the owned fruit. Added `/creeperborn timemastery 1|2|3` and `/creeperborn rubbermastery 1|2|3`; existing foxmastery and axolotlmastery remain available. Requires cheats/operator permission.

## Crash report

The supplied Windows report records an `EXCEPTION_ACCESS_VIOLATION` in Java's render thread at `DynamicGpuDataStorageMapped.writeDataBatchedMultiple`, running Microsoft Java 25.0.1 with ZGC and compact object headers. It is not an ordinary Java exception identifying a mod source line. The club was the activity reported by the tester; its causal role is unconfirmed. Native two-client coverage now includes repeated Azure club swings and lightning slashes with both first- and third-person cameras. A successful Linux CI run cannot prove the Windows runtime issue is resolved.
