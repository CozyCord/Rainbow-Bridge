package net.cozystudios.rainbowbridge.client;

import net.cozystudios.rainbowbridge.items.RainbowCollarItem;
import net.cozystudios.rainbowbridge.items.RainbowRosterItem;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.RegistryKey;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public class HomeBlockRenderer {

    public static void register() {
        WorldRenderEvents.AFTER_ENTITIES.register((context) -> {
            MinecraftClient client = MinecraftClient.getInstance();
            RegistryKey<World> dimKey = ClientHomeBlock.getDimKey();
            if (client.player != null) {
                ItemStack item = client.player.getMainHandStack();
                BlockPos homeBlock = null;
                // If holding roster, show default home block
                if (item.getItem() instanceof RainbowRosterItem && client.world.getRegistryKey().equals(dimKey)) {
                    homeBlock = ClientHomeBlock.get();

                    // If holding collar, show that pet's home block
                } else if (item.getItem() instanceof RainbowCollarItem) {
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
                VertexConsumerProvider.Immediate provider = client.getBufferBuilders()
                        .getEntityVertexConsumers();
                renderBlockOutline(homeBlock, context.matrixStack(), provider, 1f, 1f, 0f, 1f);
                provider.draw(); // flush the vertices so it actually appears
            }
        });

    }

    public static void renderBlockOutline(BlockPos pos, MatrixStack matrices, VertexConsumerProvider vertexConsumers,
            float r, float g, float b, float a) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null)
            return;

        Vec3d camPos = client.gameRenderer.getCamera().getPos();
        matrices.push();
        matrices.translate(-camPos.x, -camPos.y, -camPos.z);

        Box box = new Box(pos.getX(), pos.getY(), pos.getZ(), pos.getX() + 1, pos.getY() + 1, pos.getZ() + 1);

        // Get the vertex consumer for lines
        VertexConsumer consumer = vertexConsumers.getBuffer(RenderLayer.getLines());

        MatrixStack.Entry entry = matrices.peek();

        // 12 edges of the cube
        Vec3d[] corners = new Vec3d[] {
                new Vec3d(box.minX, box.minY, box.minZ),
                new Vec3d(box.maxX, box.minY, box.minZ),
                new Vec3d(box.maxX, box.minY, box.maxZ),
                new Vec3d(box.minX, box.minY, box.maxZ),
                new Vec3d(box.minX, box.maxY, box.minZ),
                new Vec3d(box.maxX, box.maxY, box.minZ),
                new Vec3d(box.maxX, box.maxY, box.maxZ),
                new Vec3d(box.minX, box.maxY, box.maxZ)
        };

        int[][] edges = new int[][] {
                { 0, 1 }, { 1, 2 }, { 2, 3 }, { 3, 0 }, // bottom
                { 4, 5 }, { 5, 6 }, { 6, 7 }, { 7, 4 }, // top
                { 0, 4 }, { 1, 5 }, { 2, 6 }, { 3, 7 } // verticals
        };

        for (int[] edge : edges) {
            Vec3d start = corners[edge[0]];
            Vec3d end = corners[edge[1]];

            consumer.vertex(entry.getPositionMatrix(), (float) start.x, (float) start.y, (float) start.z)
                    .color(r, g, b, a)
                    .light(0xF000F0)
                    .normal(0f, 1f, 0f)
                    .next();

            consumer.vertex(entry.getPositionMatrix(), (float) end.x, (float) end.y, (float) end.z)
                    .color(r, g, b, a)
                    .light(0xF000F0)
                    .normal(0f, 1f, 0f)
                    .next();
        }

        matrices.pop();
    }
}