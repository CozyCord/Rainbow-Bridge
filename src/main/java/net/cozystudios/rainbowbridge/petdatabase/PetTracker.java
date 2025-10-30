package net.cozystudios.rainbowbridge.petdatabase;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

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

public class PetTracker extends PersistentState {
    private final Map<UUID, PetData> tracked = new HashMap<>();

    public void addPet(TameableEntity tame, PlayerEntity user, ItemStack item) {
        TheRainbowBridge.LOGGER.info("adding new entity to be tracked: " + tame.getName());
        NbtCompound collar = new NbtCompound();
        item.writeNbt(collar);
        tracked.put(tame.getUuid(), new PetData(tame, user, collar));
    }

    public void removePet(UUID uuid){
        tracked.remove(uuid);
    }

    public Map<UUID, PetData> getTrackedMap(){
        return tracked;
    }

    public static PetTracker get(MinecraftServer server) {
        PersistentStateManager manager = server.getOverworld().getPersistentStateManager();
        return manager.getOrCreate(PetTracker::fromNBT, PetTracker::new, "rainbow_bridge_tracked_pets");
    }

    public PetData get(UUID uuid){
        return tracked.get(uuid);
    }

    public static List<PetData> getAllForUser(MinecraftServer server, PlayerEntity user) {
        return PetTracker.get(server).tracked.values().stream()
        .filter(pet -> pet.ownerUUID.equals(user.getUuid()))
        .toList();
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt) {
        NbtList list = new NbtList();
        for (PetData data : tracked.values()){
            list.add(data.toNbt());
        }

        nbt.put("pets", list);
        return nbt;
    }


    public static PetTracker fromNBT(NbtCompound nbt){
        PetTracker tracker = new PetTracker();
        NbtList list = nbt.getList("pets", NbtElement.COMPOUND_TYPE);

        list.forEach(nbtElement -> {
            PetData data = PetData.fromNbt((NbtCompound) nbtElement);
            tracker.tracked.put(data.uuid, data);
        });

        return tracker;
    }

}
