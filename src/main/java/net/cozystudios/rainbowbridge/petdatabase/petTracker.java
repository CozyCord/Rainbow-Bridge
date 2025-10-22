package net.cozystudios.rainbowbridge.petdatabase;

import net.cozystudios.rainbowbridge.TheRainbowBridge;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.PersistentState;
import net.minecraft.world.PersistentStateManager;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class petTracker extends PersistentState {
    private final Map<UUID, petData> tracked = new HashMap<>();

    public void addPet(TameableEntity tame, PlayerEntity user, ItemStack item) {
        TheRainbowBridge.LOGGER.info("adding new entity to be tracked: " + tame.getName());
        NbtCompound collar = new NbtCompound();
        item.writeNbt(collar);
        tracked.put(tame.getUuid(), new petData(tame, user, collar));
    }

    public void removePet(UUID uuid){
        tracked.remove(uuid);
    }

    public Map<UUID, petData> getTrackedMap(){
        return tracked;
    }

    public static petTracker get(MinecraftServer server) {
        PersistentStateManager manager = server.getOverworld().getPersistentStateManager();
        return manager.getOrCreate(petTracker::fromNBT, petTracker::new, "rainbow_bridge_tracked_pets");
    }

    public petData get(UUID uuid){
        return tracked.get(uuid);
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt) {
        NbtList list = new NbtList();
        for (petData data : tracked.values()){
            list.add(data.toNbt());
        }

        nbt.put("pets", list);
        return nbt;
    }


    public static petTracker fromNBT(NbtCompound nbt){
        petTracker tracker = new petTracker();
        NbtList list = nbt.getList("pets", NbtElement.COMPOUND_TYPE);

        list.forEach(nbtElement -> {
            petData data = petData.fromNbt((NbtCompound) nbtElement);
            tracker.tracked.put(data.uuid, data);
        });

        return tracker;
    }

}
