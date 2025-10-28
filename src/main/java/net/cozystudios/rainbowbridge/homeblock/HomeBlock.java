package net.cozystudios.rainbowbridge.homeblock;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.jetbrains.annotations.Nullable;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.PersistentState;
import net.minecraft.world.PersistentStateManager;

public class HomeBlock extends PersistentState {
    private final Map<UUID, BlockPos> playerHomes = new HashMap<>();
    public static final String KEY = "rainbowbridge_home_blocks";

    // --- Access ---
    public static HomeBlock get(ServerWorld world) {
        PersistentStateManager manager = world.getPersistentStateManager();
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
                state.playerHomes.put(uuid, pos);
            } catch (Exception e) {
                System.err.println("[RainbowBridge] Skipped invalid home entry: " + key);
            }
        }
        return state;
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt) {
        NbtCompound homes = new NbtCompound();
        for (Map.Entry<UUID, BlockPos> e : playerHomes.entrySet()) {
            BlockPos pos = e.getValue();
            NbtCompound posTag = new NbtCompound();
            posTag.putInt("x", pos.getX());
            posTag.putInt("y", pos.getY());
            posTag.putInt("z", pos.getZ());
            homes.put(e.getKey().toString(), posTag);
        }
        nbt.put("Homes", homes);
        return nbt;
    }

    // --- API ---
    public void setHome(ServerPlayerEntity player, BlockPos pos) {
        playerHomes.put(player.getUuid(), pos);

        HomeBlockUpdateEvents.fire(player.getUuid(), pos);

        markDirty(); // tells Minecraft to save this state
    }

    @Nullable
    public BlockPos getHome(UUID playerUuid) {
        return playerHomes.get(playerUuid);
    }

    public boolean hasHome(UUID playerUuid) {
        return playerHomes.containsKey(playerUuid);
    }

    public void removeHome(UUID playerUuid) {
        if (playerHomes.remove(playerUuid) != null) {
            HomeBlockUpdateEvents.fire(playerUuid, null);
            markDirty();
        }
    }
}
