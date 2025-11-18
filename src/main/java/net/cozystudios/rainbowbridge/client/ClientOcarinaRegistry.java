package net.cozystudios.rainbowbridge.client;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.jetbrains.annotations.Nullable;

import net.cozystudios.rainbowbridge.RainbowBridgePackets;
import net.cozystudios.rainbowbridge.petdatabase.OcarinaRegistry;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

public class ClientOcarinaRegistry {

    // Maps each ocarina to the pet it's bound to (or null if unbound)
    private static Map<UUID, UUID> MAP = new HashMap<>();

    public static void initialize() {
        ClientPlayNetworking.registerGlobalReceiver(
                RainbowBridgePackets.REQUEST_OCARINA_REGISTRY,
                (client, handler, buf, responseSender) -> {
                    int size = buf.readInt();
                    Map<UUID, UUID> newRegistry = new HashMap<>();
                    for (int i = 0; i < size; i++) {
                        UUID ocarina = buf.readUuid();
                        UUID pet = buf.readUuid();
                        newRegistry.put(ocarina, pet);
                    }

                    // Update client-side copy on the main thread
                    client.execute(() -> {
                        ClientOcarinaRegistry.MAP = newRegistry;
                    });
                });
    }

    public static void set(UUID ocarinaId, @Nullable UUID petId) {
        if (ocarinaId != null) {
            MAP.put(ocarinaId, petId);
        }
    }

    @Nullable
    public static UUID getPet(UUID ocarinaId) {
        if (ocarinaId == null)
            return null;

        UUID petId = MAP.get(ocarinaId);

        if (petId == null || petId.equals(OcarinaRegistry.EMPTY_UUID)) {
            return null;
        }

        return petId;
    }

    public static boolean hasPet(UUID ocarinaId) {
        return getPet(ocarinaId) != null;
    }

    public static void clear() {
        MAP.clear();
    }
}
