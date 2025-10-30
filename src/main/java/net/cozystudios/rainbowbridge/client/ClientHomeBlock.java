package net.cozystudios.rainbowbridge.client;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import net.cozystudios.rainbowbridge.RainbowBridgeNet;
import net.cozystudios.rainbowbridge.homeblock.HomeRequestPacket;
import net.cozystudios.rainbowbridge.homeblock.HomeBlockUpdateEvents;
import net.minecraft.util.math.BlockPos;

public class ClientHomeBlock {

    private static final List<Runnable> listeners = new ArrayList<>();
    private static @Nullable BlockPos blockPos;

    static {
        // Subscribe to server updates once when class is loaded
        HomeBlockUpdateEvents.subscribe((playerUuid, blockPos) -> {
            set(blockPos);
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

    public static synchronized void set(BlockPos blockPos) {
        ClientHomeBlock.blockPos = blockPos;
        notifyListeners();
    }

    @Nullable
    public static synchronized BlockPos get() {
        return blockPos;
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
