package net.cozystudios.rainbowbridge.client;

import com.mojang.blaze3d.systems.RenderSystem;

import net.cozystudios.rainbowbridge.items.RainbowCollarItem;
import net.cozystudios.rainbowbridge.items.RainbowRosterItem;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.RegistryKey;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.World;

public class HomeBlockRenderer {

        public static void register() {
                WorldRenderEvents.AFTER_ENTITIES.register((context) -> {
                        MinecraftClient client = MinecraftClient.getInstance();
                        RegistryKey<World> dimKey = ClientHomeBlock.getDimKey();
                        if (client.player != null) {
                                BlockPos homeBlock = null;
                                ItemStack item = client.player.getMainHandStack();

                                // If holding roster, show default home block
                                if (item.getItem() instanceof RainbowRosterItem
                                                && client.world.getRegistryKey().equals(dimKey)
                                                && KeyHelper.isAltDown()) {
                                        homeBlock = ClientHomeBlock.get();
                                }

                                // If holding collar, show that pet's home block
                                else if (item.getItem() instanceof RainbowCollarItem && KeyHelper.isAltDown()) {
                                        var dim = item.hasNbt() && item.getNbt().contains("HomeDim")
                                                        ? new Identifier(item.getNbt().getString("HomeDim"))
                                                        : null;
                                        if (dim == null || !client.world.getRegistryKey().getValue().equals(dim))
                                                return;
                                        homeBlock = item.hasNbt() && item.getNbt().contains("HomePos")
                                                        ? BlockPos.fromLong(item.getNbt().getLong("HomePos"))
                                                        : null;
                                }

                                if (homeBlock == null)
                                        return;

                                var matrices = context.matrixStack();
                                var camera = context.camera();
                                matrices.push();

                                RenderSystem.enablePolygonOffset();
                                RenderSystem.polygonOffset(1f, -10f);

                                matrices.translate(
                                                homeBlock.getX() - camera.getPos().x,
                                                homeBlock.getY() - camera.getPos().y,
                                                homeBlock.getZ() - camera.getPos().z);

                                VertexConsumer buffer = client.getBufferBuilders()
                                                .getEntityVertexConsumers()
                                                .getBuffer(RenderLayer.getLines());

                                // Rainbow color pulse
                                float time = (System.currentTimeMillis() % 5000) / 5000f;
                                float r = (float) Math.sin(time * Math.PI * 2) * 0.5f + 0.5f;
                                float g = (float) Math.sin(time * Math.PI * 2 + 2) * 0.5f + 0.5f;
                                float b = (float) Math.sin(time * Math.PI * 2 + 4) * 0.5f + 0.5f;

                                VoxelShape shape = VoxelShapes.fullCube();

                                WorldRenderer.drawShapeOutline(
                                                matrices,
                                                buffer,
                                                shape,
                                                0, 0, 0, // x, y, z offset inside matrix
                                                r, g, b, // color
                                                1.0f,
                                                false
                                );
                                RenderSystem.polygonOffset(0f, 0f);
                                RenderSystem.disablePolygonOffset();

                                matrices.pop();
                                client.getBufferBuilders().getEntityVertexConsumers().draw();

                        }
                });
        }

}