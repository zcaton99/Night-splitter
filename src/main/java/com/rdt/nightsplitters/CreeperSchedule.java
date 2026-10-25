package com.rdt.nightsplitters;

import java.util.function.IntSupplier;

/** Counts complete spider waves; a failed spawn stays due until a later wave. */
public final class CreeperSchedule {
    private int remaining;
    public boolean completeSpiderWave(IntSupplier randomThree) {
        if (remaining == 0) remaining = 3 + randomThree.getAsInt();
        if (remaining > 1) { remaining--; return false; }
        return true;
    }
    public int remaining() { return remaining; }
    public void reset() { remaining = 0; }
}
