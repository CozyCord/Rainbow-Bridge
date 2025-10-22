package net.cozystudios.rainbowbridge.petdatabase;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.Entity;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.World;

import java.util.Map;
import java.util.UUID;

public class petWatcher {
    private static int tickCounter = 0;

    public static void register(){
        ServerTickEvents.END_SERVER_TICK.register(petWatcher::onServerTick);
    }

    private static void onServerTick(MinecraftServer minecraftServer) {
        //this should prolly be a config
        if (++tickCounter % 100 != 0) return;

        petTracker tracker = petTracker.get(minecraftServer);

        for (Map.Entry<UUID, petData> pet : tracker.getTrackedMap().entrySet()) {
            petData petdata = pet.getValue();

            RegistryKey<World> worldKey = RegistryKey.of(RegistryKeys.WORLD, petdata.dim);
            ServerWorld world = minecraftServer.getWorld(worldKey);
            if (world == null) continue;


            Entity scannedEntity = world.getEntity(petdata.uuid);
            if (scannedEntity == null) continue;

            petdata.position = scannedEntity.getBlockPos();
        }
        tracker.markDirty();
    }
}
