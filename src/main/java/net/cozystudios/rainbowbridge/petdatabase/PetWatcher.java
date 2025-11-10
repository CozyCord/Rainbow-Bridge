package net.cozystudios.rainbowbridge.petdatabase;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.Entity;
import net.minecraft.server.MinecraftServer;
import java.util.Map;
import java.util.UUID;

public class PetWatcher {
    private static int tickCounter = 0;

    public static void register(){
        ServerTickEvents.END_SERVER_TICK.register(PetWatcher::onServerTick);
    }

    private static void onServerTick(MinecraftServer minecraftServer) {
        //region Update position
        if (++tickCounter % 100 != 0) return;

        PetTracker tracker = PetTracker.get(minecraftServer);

        for (Map.Entry<UUID, PetData> pet : tracker.getTrackedMap().entrySet()) {
            PetData petdata = pet.getValue();

            Entity scannedEntity = petdata.getEntity(minecraftServer).join().entity();
            if (scannedEntity == null) continue;

            petdata.position = scannedEntity.getBlockPos();
        }
        //endregion

        tracker.markDirty();
    }
}
