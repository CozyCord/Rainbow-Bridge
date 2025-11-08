package net.cozystudios.rainbowbridge.homeblock;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.BiConsumer;

import org.jetbrains.annotations.Nullable;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.PersistentState;
import net.minecraft.world.PersistentStateManager;
import net.minecraft.world.World;

public class HomeBlock extends PersistentState {
    public record HomeBlockHandle(BlockPos pos, RegistryKey<World> dim) {
    };

    private final Map<UUID, HomeBlockHandle> playerHomes = new HashMap<>();
    public static final String KEY = "rainbowbridge_home_blocks";

    // --- Access ---
    public static HomeBlock get(MinecraftServer server) {
        PersistentStateManager manager = server.getOverworld().getPersistentStateManager();
        return manager.getOrCreate(HomeBlock::fromNbt, HomeBlock::new, KEY);
    }

    public static HomeBlock fromNbt(NbtCompound nbt) {
        HomeBlock state = new HomeBlock();
        NbtCompound homes = nbt.getCompound("Homes");
        for (String key : homes.getKeys()) {
            try {
                UUID uuid = UUID.fromString(key);
                NbtCompound posTag = homes.getCompound(key);
                BlockPos pos = new BlockPos(
                        posTag.getInt("x"),
                        posTag.getInt("y"),
                        posTag.getInt("z"));
                String dimStr = posTag.getString("dimension");
                RegistryKey<World> dim = RegistryKey.of(RegistryKeys.WORLD, new Identifier(dimStr));
                state.playerHomes.put(uuid, new HomeBlockHandle(pos, dim));
            } catch (Exception e) {
                System.err.println("[RainbowBridge] Skipped invalid home entry: " + key);
            }
        }
        return state;
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt) {
        NbtCompound homes = new NbtCompound();
        for (Map.Entry<UUID, HomeBlockHandle> e : playerHomes.entrySet()) {
            BlockPos pos = e.getValue().pos();
            RegistryKey<World> dim = e.getValue().dim();
            NbtCompound tag = new NbtCompound();
            tag.putInt("x", pos.getX());
            tag.putInt("y", pos.getY());
            tag.putInt("z", pos.getZ());
            tag.putString("dimension", dim.getValue().toString());
            homes.put(e.getKey().toString(), tag);
        }
        nbt.put("Homes", homes);
        return nbt;
    }

    // --- API ---
    public void setHome(ServerPlayerEntity player, BlockPos pos, RegistryKey<World> dim) {
        playerHomes.put(player.getUuid(), new HomeBlockHandle(pos, dim));

        HomeBlockUpdateEvents.fire(player.getUuid(), pos, dim);

        markDirty(); // tells Minecraft to save this state
    }

    @Nullable
    public HomeBlockHandle getHome(UUID playerUuid) {
        return playerHomes.get(playerUuid);
    }

    public boolean hasHome(UUID playerUuid) {
        return playerHomes.containsKey(playerUuid);
    }

    public void removeHome(UUID playerUuid) {
        if (playerHomes.remove(playerUuid) != null) {
            HomeBlockUpdateEvents.fire(playerUuid, null, null);
            markDirty();
        }
    }
}
