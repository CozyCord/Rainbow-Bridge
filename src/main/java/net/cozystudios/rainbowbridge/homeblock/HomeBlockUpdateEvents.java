package net.cozystudios.rainbowbridge.homeblock;

import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BiConsumer;

import net.minecraft.util.math.BlockPos;

public class HomeBlockUpdateEvents {
    public interface IListener extends BiConsumer<UUID, BlockPos> {}
    // Thread-safe list for concurrent modifications
    private static final CopyOnWriteArrayList<IListener> listeners = new CopyOnWriteArrayList<>();

    /** Register a listener to be called whenever a home is updated */
    public static void subscribe(IListener listener) {
        listeners.add(listener);
    }

    /** Remove a listener if it’s no longer needed */
    public static void unsubscribe(IListener listener) {
        listeners.remove(listener);
    }

    /** Notify all listeners that a home position changed */
    public static void fire(UUID playerUuid, BlockPos pos) {
        for (var listener : listeners) {
            listener.accept(playerUuid, pos);
        }
    }
}
