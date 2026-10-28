# Paw-Paw alpha.48

Paw Step: tier 1/2/3 travels 8/14/50 blocks, with 3/4/5 stored charges. Tap X again to redirect and start another step midair. One charge returns every three seconds; spent charges persist through reconnecting. Hold-X enemy launches keep their existing ranges.

Repel Guard lasts 1/2/5 seconds by tier. Paw facing uses explicit yaw/pitch without roll; the rotating air ring has an isolated renderer transform. Active visuals refresh a bounded lifetime; abandoned visuals expire automatically.

Pain bubbles launch from their guided position toward the crosshair target, detect contact from inside hitboxes, and retry temporarily rejected damage for a bounded lifetime. Status reports stored damage or a harmless full-health extraction.

Regression checks cover fifty-block movement, midair chaining, charge exhaustion/refill/logout persistence, upright orientation, visual expiry, off-axis pain targeting and retry after vanilla damage immunity. Client smoke checks transform isolation after Shock followed by Cannon. Two-player network feel and appearance still need in-game testing.
