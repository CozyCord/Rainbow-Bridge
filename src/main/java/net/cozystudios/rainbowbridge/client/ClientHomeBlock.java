package net.cozystudios.rainbowbridge.client;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import net.cozystudios.rainbowbridge.RainbowBridgeNet;
import net.cozystudios.rainbowbridge.homeblock.HomeBlockUpdateEvents;
import net.cozystudios.rainbowbridge.packets.HomeRequestPacket;
import net.minecraft.registry.RegistryKey;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class ClientHomeBlock {

    private static final List<Runnable> listeners = new ArrayList<>();
    private static @Nullable BlockPos blockPos;
    private static @Nullable RegistryKey<World> dimension;

    static {
        // Subscribe to server updates once when class is loaded
        HomeBlockUpdateEvents.subscribe((playerUuid, blockPos, dim) -> {
            set(blockPos, dim);
        });
    }

    /**
     * Sends a request to get the home block position
     */
    public static void initialize() {
        RainbowBridgeNet.CHANNEL.clientHandle().send(new HomeRequestPacket()); // Request home position from server
    }

    public static void reset() {
        blockPos = null;
        listeners.clear();
    }

    public static synchronized void set(BlockPos blockPos, RegistryKey<World> dim) {
        ClientHomeBlock.blockPos = blockPos;
        ClientHomeBlock.dimension = dim;
        notifyListeners();
    }

    @Nullable
    public static synchronized BlockPos get() {
        return blockPos;
    }

    public static synchronized RegistryKey<World> getDimKey() {
        return dimension;
    }

    // --- subscription methods ---
    public static void addListener(Runnable listener) {
        listeners.add(listener);
    }

    public static void removeListener(Runnable listener) {
        listeners.remove(listener);
    }

    private static void notifyListeners() {
        for (Runnable listener : List.copyOf(listeners)) {
            listener.run();
        }
    }
}
