package net.cozystudios.rainbowbridge.petdatabase;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import io.netty.buffer.Unpooled;
import net.cozystudios.rainbowbridge.RainbowBridgePackets;
import net.cozystudios.rainbowbridge.TheRainbowBridge;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.registry.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.world.PersistentState;
import net.minecraft.world.PersistentStateManager;

public class PetTracker extends PersistentState {
    private final Map<UUID, PetData> tracked = new HashMap<>();

    public void addPet(TameableEntity tame, PlayerEntity player, ItemStack item) {
        TheRainbowBridge.LOGGER.info("adding new entity to be tracked: " + tame.getName());
        NbtCompound collar = new NbtCompound();
        item.writeNbt(collar);
        tracked.put(tame.getUuid(), new PetData(tame, player, collar));
        var petList = PetTracker.serializePetList(player);
        ServerPlayNetworking.send((ServerPlayerEntity) player, RainbowBridgePackets.RESPONSE_PET_TRACKER, petList);
    }

    public void removePet(MinecraftServer server, UUID uuid) {
        var player = server.getPlayerManager().getPlayer(tracked.get(uuid).ownerUUID);
        tracked.remove(uuid);
        var petList = PetTracker.serializePetList(player);
        ServerPlayNetworking.send((ServerPlayerEntity) player, RainbowBridgePackets.RESPONSE_PET_TRACKER, petList);
    }

    public Map<UUID, PetData> getTrackedMap() {
        return tracked;
    }

    public static PetTracker get(MinecraftServer server) {
        PersistentStateManager manager = server.getOverworld().getPersistentStateManager();
        return manager.getOrCreate(PetTracker::fromNBT, PetTracker::new, "rainbow_bridge_tracked_pets");
    }

    public PetData get(UUID uuid) {
        return tracked.get(uuid);
    }

    public static List<PetData> getAllForPlayer(PlayerEntity player) {
        return PetTracker.get(player.getServer()).tracked.values().stream()
                .filter(pet -> pet.ownerUUID.equals(player.getUuid()))
                .toList();
    }

    public static PacketByteBuf serializePetList(PlayerEntity player) {
        // get your PersistentState
        List<PetData> pets = PetTracker.getAllForPlayer(player);

        // serialize the data into a PacketByteBuf
        PacketByteBuf out = new PacketByteBuf(Unpooled.buffer());
        out.writeInt(pets.size());

        for (PetData pet : pets) {
            var data = pet.getEntity(player.getServer()).join();

            if (data != null) {
                if (data.entity() != null) {
                    var entity = data.entity();
                    // entity type
                    out.writeString(Registries.ENTITY_TYPE.getId(entity.getType()).toString());
                    // entity data
                    NbtCompound entityNbt = new NbtCompound();
                    entity.saveNbt(entityNbt);
                    out.writeNbt(entityNbt);
                    // get custom name or default name if there is none
                    String name = entity.hasCustomName() ? entity.getCustomName().getString()
                            : entity.getType().getName().getString();
                    out.writeString(name);

                }
                // Entity is on player's shoulder -- use saved NBT
                else if (data.shoulderNbt() != null) {
                    NbtCompound nbt = data.shoulderNbt();

                    // entity type
                    out.writeString(nbt.getString("id"));
                    // entity NBT
                    out.writeNbt(nbt);

                    String name;

                    // name
                    if (nbt.contains("CustomName")) {
                        name = Text.Serializer.fromJson(nbt.getString("CustomName")).getString();
                    } else {
                        name = Registries.ENTITY_TYPE.get(new Identifier(nbt.getString("id"))).getName().getString();
                    }

                    out.writeString(name);
                }

            } else {
                out.writeString("minecraft:bat");
                out.writeNbt(new NbtCompound());
                out.writeString("Could not locate pet!");
            }
            out.writeString(pet.position.toShortString());
        }

        return out;
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt) {
        NbtList list = new NbtList();
        for (PetData data : tracked.values()) {
            list.add(data.toNbt());
        }

        nbt.put("pets", list);
        return nbt;
    }

    public static PetTracker fromNBT(NbtCompound nbt) {
        PetTracker tracker = new PetTracker();
        NbtList list = nbt.getList("pets", NbtElement.COMPOUND_TYPE);

        list.forEach(nbtElement -> {
            PetData data = PetData.fromNbt((NbtCompound) nbtElement);
            tracker.tracked.put(data.uuid, data);
        });

        return tracker;
    }

}
