package net.cozystudios.rainbowbridge.homeblock;

import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.logging.log4j.util.TriConsumer;

import net.minecraft.registry.RegistryKey;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class HomeBlockUpdateEvents {
    public interface IListener extends TriConsumer<UUID, BlockPos, RegistryKey<World>> {}
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
    public static void fire(UUID playerUuid, BlockPos pos, RegistryKey<World> dim) {
        for (var listener : listeners) {
            listener.accept(playerUuid, pos, dim);
        }
    }
}
