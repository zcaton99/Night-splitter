# Alpha.102 — village-to-nation progress

Successful Elemental Trident conversion now founds that element's nation automatically when no such nation exists, using the player's location as its center. Existing adult guardians of the matching element are enrolled as well. Existing Overworld, four-element unlock, ownership and 192-block nation spacing rules remain in force.

Each successful conversion shows a brief lower-left progress update, for example “Air Nation: 6/9 adult guardians — 3 more needed within 64 blocks.” The count uses distinct living adult villagers inside the nation's radius, so repeatedly clicking one villager does not increase progress. `/creeperborn nations` also reports current progress.

At nine guardians, a ten-second warning starts automatically. The raid then activates and runs its existing three waves. Falling below nine during the countdown cancels it until the population recovers. Countdowns pause while nobody is nearby, survive reload, and do not run in Peaceful. Completed trials cannot start again. Failed raids require a deliberate retry via the existing start command or another trident conversion; they do not loop automatically.

Existing converted villages count when a guardian is touched with the trident again. Existing founded nations retain their original centers. Conversions outside the established nation's radius explain where guardians need to be gathered. Manual founding, starting and offerings remain supported.

Validation extends the nation regression with per-conversion counts, remaining requirements, automatic founding, duplicate prevention, countdown cancellation/recovery, reload, and automatic activation. The existing complete four-nation, raid-wave, offering, and boss-unlock checks remain in the suite.
