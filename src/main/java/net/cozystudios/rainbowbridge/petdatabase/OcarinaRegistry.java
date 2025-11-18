package net.cozystudios.rainbowbridge.petdatabase;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import io.netty.buffer.Unpooled;
import net.cozystudios.rainbowbridge.RainbowBridgeNet;
import net.cozystudios.rainbowbridge.packets.OcarinaUpdatePacket;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.PersistentState;
import net.minecraft.world.PersistentStateManager;
import net.minecraft.world.World;

public class OcarinaRegistry extends PersistentState {

    public static final UUID EMPTY_UUID = new UUID(0L, 0L);

    // ocarinaUuid -> petUuid
    private final Map<UUID, UUID> ocarinaToPet = new HashMap<>();

    public static OcarinaRegistry get(MinecraftServer server) {
        PersistentStateManager mgr = server.getWorld(World.OVERWORLD).getPersistentStateManager();

        return mgr.getOrCreate(
                OcarinaRegistry::fromNbt,
                OcarinaRegistry::new,
                "rainbowbridge_ocarina_registry");
    }

    public OcarinaRegistry() {
    }

    // ----------- LOADING ----------
    public static OcarinaRegistry fromNbt(NbtCompound tag) {
        OcarinaRegistry reg = new OcarinaRegistry();

        NbtList list = tag.getList("entries", NbtElement.COMPOUND_TYPE);
        for (NbtElement e : list) {
            NbtCompound c = (NbtCompound) e;
            UUID ocarina = c.getUuid("ocarina");
            UUID pet = c.getUuid("pet");
            reg.ocarinaToPet.put(ocarina, pet);
        }

        return reg;
    }

    // ----------- SAVING ----------
    @Override
    public NbtCompound writeNbt(NbtCompound tag) {
        NbtList list = new NbtList();

        for (var entry : ocarinaToPet.entrySet()) {
            NbtCompound c = new NbtCompound();
            c.putUuid("ocarina", entry.getKey());
            c.putUuid("pet", entry.getValue());
            list.add(c);
        }

        tag.put("entries", list);
        return tag;
    }

    /** Writes the full registry into a PacketByteBuf */
    public PacketByteBuf serializeList() {
        PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
        buf.writeInt(ocarinaToPet.size());
        for (Map.Entry<UUID, UUID> entry : ocarinaToPet.entrySet()) {
            buf.writeUuid(entry.getKey());
            buf.writeUuid(entry.getValue());
        }
        return buf;
    }

    // ----------- OPERATIONS ----------

    public void register(UUID ocarinaUuid, UUID petUuid, MinecraftServer server) {
        ocarinaToPet.put(ocarinaUuid, petUuid);
        markDirty();

        // Send update to all connected clients
        PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
        buf.writeUuid(ocarinaUuid);
        buf.writeUuid(petUuid);

        RainbowBridgeNet.CHANNEL.serverHandle(server).send(new OcarinaUpdatePacket(ocarinaUuid, petUuid));
    }

    public void unregisterOcarina(UUID ocarinaUuid, MinecraftServer server) {
        ocarinaToPet.remove(ocarinaUuid);
        RainbowBridgeNet.CHANNEL.serverHandle(server).send(new OcarinaUpdatePacket(ocarinaUuid, null));
        markDirty();
    }

    public List<UUID> getOcarinasForPet(UUID petUuid) {
        List<UUID> result = new ArrayList<>();

        for (var entry : ocarinaToPet.entrySet()) {
            if (entry.getValue().equals(petUuid)) {
                result.add(entry.getKey());
            }
        }
        return result;
    }

    public UUID getPetForOcarina(UUID ocarinaUuid) {
        return ocarinaToPet.get(ocarinaUuid);
    }

    public void unregisterAllForPet(UUID petUuid, MinecraftServer server) {
        List<UUID> ocarinasToClear = new ArrayList<>();

        for (var entry : ocarinaToPet.entrySet()) {
            if (entry.getValue().equals(petUuid)) {
                ocarinasToClear.add(entry.getKey());
            }
        }

        // Now update map + send packets
        for (UUID ocarinaUuid : ocarinasToClear) {
            ocarinaToPet.remove(ocarinaUuid);
            RainbowBridgeNet.CHANNEL.serverHandle(server).send(
                    new OcarinaUpdatePacket(ocarinaUuid, EMPTY_UUID));
        }

        markDirty();
    }

}
