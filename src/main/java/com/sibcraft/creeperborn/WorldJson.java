package com.sibcraft.creeperborn;

import com.google.gson.Gson;
import java.nio.file.*;
import java.io.IOException;

/** Small world-owned ledgers. Invalid data fails visibly instead of resetting earned progress. */
final class WorldJson {
    static final Gson GSON = new Gson();
    static <T> T read(Path path, Class<T> type, T fallback) {
        try {
            if (!Files.exists(path)) return fallback;
            T value = GSON.fromJson(Files.readString(path), type);
            if (value == null) throw new IOException("Empty ledger");
            return value;
        } catch (Exception e) { throw new IllegalStateException("Cannot read " + path.getFileName(), e); }
    }
    static void write(Path path, Object value) {
        if (path == null) throw new IllegalStateException("World ledger is not loaded");
        try {
            Files.createDirectories(path.getParent());
            Path temp = path.resolveSibling(path.getFileName() + ".tmp");
            Files.writeString(temp, GSON.toJson(value) + "\n");
            try { Files.move(temp,path,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING); }
            catch (AtomicMoveNotSupportedException e) { Files.move(temp,path,StandardCopyOption.REPLACE_EXISTING); }
        } catch (IOException e) { throw new IllegalStateException("Cannot save " + path.getFileName(), e); }
    }
}
