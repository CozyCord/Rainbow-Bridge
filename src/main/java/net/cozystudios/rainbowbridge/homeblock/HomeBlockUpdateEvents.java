package net.cozystudios.rainbowbridge.homeblock;

import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BiConsumer;

import net.minecraft.util.math.BlockPos;

public class HomeBlockUpdateEvents {

    // Thread-safe list for concurrent modifications
    private static final CopyOnWriteArrayList<BiConsumer<UUID, BlockPos>> listeners = new CopyOnWriteArrayList<>();

    /** Register a listener to be called whenever a home is updated */
    public static void subscribe(BiConsumer<UUID, BlockPos> listener) {
        listeners.add(listener);
    }

    /** Remove a listener if it’s no longer needed */
    public static void unsubscribe(BiConsumer<UUID, BlockPos> listener) {
        listeners.remove(listener);
    }

    /** Notify all listeners that a home position changed */
    public static void fire(UUID playerUuid, BlockPos pos) {
        for (var listener : listeners) {
            listener.accept(playerUuid, pos);
        }
    }
}
