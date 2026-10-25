package com.rdt.nightsplitters;

/** Parents cost two slots, reserving both children before a parent is spawned. */
public final class SwarmBudget {
    private SwarmBudget() {}
    public static int cost(boolean parent) { return parent ? 2 : 1; }
    public static boolean canAddParent(int occupied, int cap) { return occupied <= cap - 2; }
}
