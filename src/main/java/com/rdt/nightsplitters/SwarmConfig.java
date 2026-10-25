package com.rdt.nightsplitters;

public final class SwarmConfig {
    public boolean enabled = true;
    public int waveIntervalSeconds = 25; // Legacy setting; wave cooldown now uses min/max.
    public int parentsPerWave = 3; // Upper bound of random 2-3 spider waves.
    public int minWaveSize = 2;
    public int minWaveDelaySeconds = 5;
    public int maxWaveDelaySeconds = 30;
    public int globalSlotCap = 96;
    public int localSlotCap = 32;
    public double parentSpeedMultiplier = 1.125;
    public double childSpeedMultiplier = 1.2375;
    public double childScale = 0.5;
    public double parentHealth = 12;
    public double childHealth = 6;
    public boolean cleanupAtDawn = true;
    public int lifetimeSeconds = 600;
    public void validate() {
        if (waveIntervalSeconds < 5 || waveIntervalSeconds > 3600
            || parentsPerWave < 2 || parentsPerWave > 3
            || minWaveSize < 2 || minWaveSize > parentsPerWave
            || minWaveDelaySeconds < 5 || maxWaveDelaySeconds > 30
            || minWaveDelaySeconds > maxWaveDelaySeconds
            || globalSlotCap < 2 || globalSlotCap > 256
            || localSlotCap < 2 || localSlotCap > globalSlotCap
            || !range(parentSpeedMultiplier, 1, 4) || !range(childSpeedMultiplier, 1, 4)
            || !range(childScale, .25, .9) || !range(parentHealth, 1, 100)
            || !range(childHealth, 1, 100) || lifetimeSeconds < 30 || lifetimeSeconds > 3600)
            throw new IllegalArgumentException("Night Splitters config is outside documented bounds");
    }
    private static boolean range(double value, double low, double high) {
        return Double.isFinite(value) && value >= low && value <= high;
    }
}
