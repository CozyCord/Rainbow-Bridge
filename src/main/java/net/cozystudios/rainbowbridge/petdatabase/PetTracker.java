package net.cozystudios.rainbowbridge.petdatabase;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import io.netty.buffer.Unpooled;
import net.cozystudios.rainbowbridge.RainbowBridgePackets;
import net.cozystudios.rainbowbridge.TheRainbowBridge;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.world.PersistentState;
import net.minecraft.world.PersistentStateManager;
import net.minecraft.world.World;

public class PetTracker extends PersistentState {
    private final Map<UUID, PetData> tracked = new HashMap<>();
    private final Set<UUID> recreated = new HashSet<>(); // Entities that have been recreated and must be removed

    public void addPet(TameableEntity tame, PlayerEntity player, ItemStack item, long tameTimestamp) {
        TheRainbowBridge.LOGGER.info("adding new entity to be tracked: " + tame.getName());
        NbtCompound collar = new NbtCompound();
        item.writeNbt(collar);
        tracked.put(tame.getUuid(), new PetData(tame, player, collar, tameTimestamp));
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

    /** Map of entities marked for deletion */
    public Set<UUID> getRecreatedMap() {
        return recreated;
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
        List<PetData> pets = new ArrayList<>(PetTracker.getAllForPlayer(player));
        pets.sort((a, b) -> Long.compare(a.tameTimestamp, b.tameTimestamp));

        PacketByteBuf out = new PacketByteBuf(Unpooled.buffer());
        out.writeInt(pets.size());

        for (PetData pet : pets) {
            RegistryKey<World> worldKey = RegistryKey.of(RegistryKeys.WORLD, pet.dim);
            ServerWorld world = player.getServer().getWorld(worldKey);

            // Check if world is loaded
            if (world != null) {
                var data = pet.getEntity(player.getServer()).join();

                if (data != null) {
                    if (data.entity() != null) {
                        var entity = data.entity();
                        out.writeString(Registries.ENTITY_TYPE.getId(entity.getType()).toString());
                        NbtCompound entityNbt = new NbtCompound();
                        entity.saveNbt(entityNbt);
                        out.writeNbt(entityNbt);

                        String name = entity.hasCustomName()
                                ? entity.getCustomName().getString()
                                : entity.getType().getName().getString();
                        out.writeString(name);
                    } else if (data.shoulderNbt() != null) {
                        NbtCompound nbt = data.shoulderNbt();
                        out.writeString(nbt.getString("id"));
                        out.writeNbt(nbt);

                        String name = nbt.contains("CustomName")
                                ? Text.Serializer.fromJson(nbt.getString("CustomName")).getString()
                                : Registries.ENTITY_TYPE.get(new Identifier(nbt.getString("id"))).getName().getString();
                        out.writeString(name);
                    }
                } else {
                    // Entity not loaded, fallback to saved NBT
                    NbtCompound nbt = pet.getEntityData(); // use the saved entityData
                    out.writeString(nbt.getString("id"));
                    out.writeNbt(nbt);
                    String name = nbt.contains("CustomName")
                            ? Text.Serializer.fromJson(nbt.getString("CustomName")).getString()
                            : Registries.ENTITY_TYPE.get(new Identifier(nbt.getString("id"))).getName().getString();
                    out.writeString(name);
                }
            } else {
                // World is unloaded, just send saved NBT
                NbtCompound nbt = pet.getEntityData();
                out.writeString(nbt.getString("id"));
                out.writeNbt(nbt);
                String name = nbt.contains("CustomName")
                        ? Text.Serializer.fromJson(nbt.getString("CustomName")).getString()
                        : Registries.ENTITY_TYPE.get(new Identifier(nbt.getString("id"))).getName().getString();
                out.writeString(name);
            }

            // Always send the stored position
            out.writeString(pet.position.toShortString());
        }

        return out;
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt) {
        NbtList petList = new NbtList();
        for (PetData data : tracked.values()) {
            petList.add(data.toNbt());
        }
        nbt.put("pets", petList);

        NbtList recreatedList = new NbtList();
        for (UUID uuid : recreated) {
            NbtCompound tag = new NbtCompound();
            tag.putUuid("uuid", uuid);
            recreatedList.add(tag);
        }
        nbt.put("recreatedPets", recreatedList);

        return nbt;
    }

    public static PetTracker fromNBT(NbtCompound nbt) {
        PetTracker tracker = new PetTracker();

        // load pets
        NbtList petList = nbt.getList("pets", NbtElement.COMPOUND_TYPE);
        for (NbtElement e : petList) {
            PetData data = PetData.fromNbt((NbtCompound) e);
            tracker.tracked.put(data.uuid, data);
        }

        // load recreated UUIDs
        NbtList recreatedList = nbt.getList("recreatedPets", NbtElement.COMPOUND_TYPE);
        for (NbtElement e : recreatedList) {
            NbtCompound tag = (NbtCompound) e;
            if (tag.containsUuid("uuid")) {
                tracker.recreated.add(tag.getUuid("uuid"));
            }
        }

        return tracker;
    }

}
