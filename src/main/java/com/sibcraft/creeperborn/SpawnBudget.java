package com.sibcraft.creeperborn;

import com.google.gson.Gson;
import java.nio.file.*;
import java.io.IOException;
import java.util.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.level.storage.LevelResource;

/** Limits successful special conversions, never ordinary spawning or an existing creeper's loot. */
public final class SpawnBudget {
    public record Entry(String dimension, double x, double z, String player, boolean fruit, long tick) {}
    public static final class Ledger {
        private final List<Entry> entries = new ArrayList<>();
        public Ledger() {}
        public Ledger(Collection<Entry> saved) { entries.addAll(saved); }
        public List<Entry> snapshot() { return List.copyOf(entries); }
        public boolean take(String dimension, double x, double z, String player, boolean fruit, long tick,
                            long window, double radius, int areaLimit, int playerLimit) {
            entries.removeIf(e -> e.tick() > tick || tick - e.tick() >= window);
            long area = entries.stream().filter(e -> e.fruit() == fruit && e.dimension().equals(dimension)
                && Math.hypot(e.x() - x, e.z() - z) <= radius).count();
            long personal = player == null ? 0 : entries.stream().filter(e -> e.fruit() == fruit && player.equals(e.player())).count();
            if (area >= areaLimit || player != null && personal >= playerLimit) return false;
            entries.add(new Entry(dimension, x, z, player, fruit, tick));
            return true;
        }
    }
    private static Ledger ledger = new Ledger();
    private static Path path;
    private SpawnBudget() {}

    public static void load(MinecraftServer server) {
        path = server.getWorldPath(LevelResource.ROOT).resolve("creeperborn-spawn-budget.json");
        ledger = new Ledger();
        try {
            if (Files.exists(path)) {
                Entry[] saved = new Gson().fromJson(Files.readString(path), Entry[].class);
                if (saved == null || saved.length > 10000) throw new IOException("Invalid spawn budget");
                for (Entry e : saved) if (e == null || e.dimension() == null || !Double.isFinite(e.x())
                    || !Double.isFinite(e.z()) || e.tick() < 0) throw new IOException("Invalid spawn entry");
                ledger = new Ledger(Arrays.asList(saved));
            }
        } catch (Exception e) { throw new IllegalStateException("Cannot read Creeperborn spawn budget", e); }
    }

    public static boolean allow(Creeper creeper, boolean fruit) {
        var config = Creeperborn.config();
        if (!config.limitSpecialSpawns) return true;
        ServerLevel level = (ServerLevel)creeper.level();
        var nearest = level.players().stream().filter(p -> !p.isSpectator() && p.isAlive()
            && p.distanceToSqr(creeper) <= 128 * 128).min(Comparator.comparingDouble(p -> p.distanceToSqr(creeper)));
        String player = nearest.map(p -> p.getUUID().toString()).orElse(null);
        long tick = level.getServer().overworld().getGameTime();
        boolean accepted = ledger.take(level.dimension().identifier().toString(), creeper.getX(), creeper.getZ(), player,
            fruit, tick, config.specialSpawnWindowSeconds * 20L, config.specialSpawnRadius,
            fruit ? config.fruitSpawnsPerArea : config.elementSpawnsPerArea,
            fruit ? config.fruitSpawnsPerPlayer : config.elementSpawnsPerPlayer);
        if (accepted && path != null) save();
        return accepted;
    }

    private static void save() {
        try {
            Path temp = path.resolveSibling(path.getFileName() + ".tmp");
            Files.writeString(temp, new Gson().toJson(ledger.snapshot()) + "\n");
            try { Files.move(temp, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE); }
            catch (AtomicMoveNotSupportedException e) { Files.move(temp, path, StandardCopyOption.REPLACE_EXISTING); }
        } catch (IOException e) { throw new IllegalStateException("Cannot save Creeperborn spawn budget", e); }
    }
    public static void clear() { ledger = new Ledger(); path = null; }
}
