package net.cozystudios.rainbowbridge.client;

import net.cozystudios.rainbowbridge.items.RainbowCollarItem;
import net.cozystudios.rainbowbridge.items.RainbowRosterItem;
import net.cozystudios.rainbowbridge.registry.ModBlocks;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.block.Block;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class HomeMarkerManager {
    private static BlockPos activePos = null;
    private static RegistryKey<World> activeDim = null;

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null || client.world == null)
                return;

            BlockPos target = null;
            RegistryKey<World> targetDim = null;

            ItemStack item = client.player.getMainHandStack();

            // ALT must be held
            if (!KeyHelper.isAltDown()) {
                clearMarker(client);
                return;
            }

            // Holding roster → show default home block
            if (item.getItem() instanceof RainbowRosterItem) {
                RegistryKey<World> dimKey = ClientHomeBlock.getDimKey();
                if (client.world.getRegistryKey().equals(dimKey)) {
                    target = ClientHomeBlock.get();
                    targetDim = dimKey;
                }
            }

            // Holding collar → show its home block
            else if (item.getItem() instanceof RainbowCollarItem) {
                NbtCompound tag = item.getNbt();
                if (tag != null) {
                    String dim = tag.getString("HomeDim");
                    BlockPos hp = tag.contains("HomePos") ? BlockPos.fromLong(tag.getLong("HomePos")) : null;

                    if (hp != null) {
                        target = hp;
                        targetDim = RegistryKey.of(RegistryKeys.WORLD, new Identifier(dim));
                    }
                }

                if (target != null && !client.world.getRegistryKey().equals(targetDim)) {
                    // Wrong dimension → do nothing
                    clearMarker(client);
                    return;
                }
            }

            // No target → remove any existing marker
            if (target == null) {
                clearMarker(client);
                return;
            }

            // Place or move marker if changed
            updateMarker(client, target, targetDim);
        });
    }

    private static void updateMarker(MinecraftClient client, BlockPos pos, RegistryKey<World> dim) {
        if (activePos != null && activePos.equals(pos) && activeDim == dim)
            return; // No change

        clearMarker(client);

        // Set new marker
        client.world.setBlockState(pos,
                ModBlocks.HOME_MARKER_BLOCK.getDefaultState(),
                Block.NOTIFY_LISTENERS);

        activePos = pos;
        activeDim = dim;
    }

    private static void clearMarker(MinecraftClient client) {
        if (activePos != null && client.world.getRegistryKey().equals(activeDim)) {
            client.world.removeBlock(activePos, false);
        }
        activePos = null;
        activeDim = null;
    }
}
