package dev.znxki.zNxPiglinRush.api.storage;

/**
 * Persistence backend for plugin statistics.
 *
 * <p>Implementations must be thread-safe: {@link #flush()} may be called
 * from a background thread, and {@link #loadTotalSpawns()} is called once
 * on startup from the main thread before any async tasks are scheduled.
 */
public interface StorageBackend {
    void open();

    void close();

    void flush();

    long loadTotalSpawns();
}
