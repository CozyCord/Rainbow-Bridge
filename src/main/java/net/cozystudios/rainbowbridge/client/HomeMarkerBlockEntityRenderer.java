package net.cozystudios.rainbowbridge.client;

import org.joml.Matrix4f;

import net.cozystudios.rainbowbridge.block.entity.HomeMarkerBlockEntity;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Box;

@Environment(EnvType.CLIENT)
public class HomeMarkerBlockEntityRenderer implements BlockEntityRenderer<HomeMarkerBlockEntity> {

    public HomeMarkerBlockEntityRenderer(BlockEntityRendererFactory.Context ctx) {
    }

    @Override
    public void render(HomeMarkerBlockEntity entity, float tickDelta,
            MatrixStack matrices, VertexConsumerProvider vertexConsumers,
            int light, int overlay) {

        BlockState state = entity.getCachedState(); // get the block state
        MinecraftClient client = MinecraftClient.getInstance();
        client.getBlockRenderManager().renderBlockAsEntity(state, matrices, vertexConsumers, light, overlay);

        // Slightly expand to avoid z-fighting
        float o = 0.002f;

        Box box = new Box(
                0 - o, 0 - o, 0 - o,
                1 + o, 1 + o, 1 + o);

        // RenderLayer for line outlines
        VertexConsumer vc = vertexConsumers.getBuffer(RenderLayer.getLines());

        // Rainbow pulse
        float time = (System.currentTimeMillis() % 5000) / 5000f;
        float r = (float) Math.sin(time * Math.PI * 2) * 0.5f + 0.5f;
        float g = (float) Math.sin(time * Math.PI * 2 + 2) * 0.5f + 0.5f;
        float b = (float) Math.sin(time * Math.PI * 2 + 4) * 0.5f + 0.5f;
        float a = 0.4f; // semi-transparent

        // Get the block sprite
        Identifier textureId = new Identifier("rainbowbridge", "block/home_marker");
        Sprite sprite = MinecraftClient.getInstance()
                .getSpriteAtlas(SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE)
                .apply(textureId);

        // // Draw faces (you can reuse your old drawFaces method)
        // drawFaces(matrices.peek(), consumer, sprite, r, g, b, a, 0xF000F0, overlay);

        // VertexConsumer for translucent faces
        VertexConsumer faceVc = vertexConsumers.getBuffer(RenderLayer.getTranslucent());

        drawBoxFaces(matrices, faceVc, box, r, g, b, a);

        // First pass
        drawBoxOutline(matrices, vc, box, 1f, 1f, 1f, 1f);

        // Second pass offsets — make lines thicker
        double offset = 0.002; // adjust this for wider lines
        matrices.push();
        matrices.translate(offset, offset, 0);
        drawBoxOutline(matrices, vc, box, 1f, 1f, 1f, 1f);
        matrices.pop();

        matrices.push();
        matrices.translate(-offset, offset, 0);
        drawBoxOutline(matrices, vc, box, 1f, 1f, 1f, 1f);
        matrices.pop();

        matrices.push();
        matrices.translate(offset, -offset, 0);
        drawBoxOutline(matrices, vc, box, 1f, 1f, 1f, 1f);
        matrices.pop();

        matrices.push();
        matrices.translate(-offset, -offset, 0);
        drawBoxOutline(matrices, vc, box, 1f, 1f, 1f, 1f);
        matrices.pop();
    }

    private void drawBoxOutline(MatrixStack matrices, VertexConsumer vc, Box b,
            float r, float g, float bC, float a) {

        MatrixStack.Entry entry = matrices.peek();
        Matrix4f pos = entry.getPositionMatrix();

        // 12 edges for a cube outline
        drawLine(vc, pos, b.minX, b.minY, b.minZ, b.maxX, b.minY, b.minZ, r, g, bC, a);
        drawLine(vc, pos, b.maxX, b.minY, b.minZ, b.maxX, b.maxY, b.minZ, r, g, bC, a);
        drawLine(vc, pos, b.maxX, b.maxY, b.minZ, b.minX, b.maxY, b.minZ, r, g, bC, a);
        drawLine(vc, pos, b.minX, b.maxY, b.minZ, b.minX, b.minY, b.minZ, r, g, bC, a);

        drawLine(vc, pos, b.minX, b.minY, b.maxZ, b.maxX, b.minY, b.maxZ, r, g, bC, a);
        drawLine(vc, pos, b.maxX, b.minY, b.maxZ, b.maxX, b.maxY, b.maxZ, r, g, bC, a);
        drawLine(vc, pos, b.maxX, b.maxY, b.maxZ, b.minX, b.maxY, b.maxZ, r, g, bC, a);
        drawLine(vc, pos, b.minX, b.maxY, b.maxZ, b.minX, b.minY, b.maxZ, r, g, bC, a);

        drawLine(vc, pos, b.minX, b.minY, b.minZ, b.minX, b.minY, b.maxZ, r, g, bC, a);
        drawLine(vc, pos, b.maxX, b.minY, b.minZ, b.maxX, b.minY, b.maxZ, r, g, bC, a);
        drawLine(vc, pos, b.maxX, b.maxY, b.minZ, b.maxX, b.maxY, b.maxZ, r, g, bC, a);
        drawLine(vc, pos, b.minX, b.maxY, b.minZ, b.minX, b.maxY, b.maxZ, r, g, bC, a);
    }

    private void drawLine(VertexConsumer vc, Matrix4f pos,
            double x1, double y1, double z1,
            double x2, double y2, double z2,
            float r, float g, float b, float a) {

        vc.vertex(pos, (float) x1, (float) y1, (float) z1).color(r, g, b, a).texture(0f, 0f).overlay(0).light(0xF000F0)
                .normal(0f, 1f, 0f).next();
        vc.vertex(pos, (float) x2, (float) y2, (float) z2).color(r, g, b, a).texture(0f, 0f).overlay(0).light(0xF000F0)
                .normal(0f, 1f, 0f).next();
    }

    private void drawBoxFaces(MatrixStack matrices, VertexConsumer vc, Box b,
            float r, float g, float bC, float a) {
        Matrix4f pos = matrices.peek().getPositionMatrix();

        // Bottom
        drawQuad(vc, pos, b.minX, b.minY, b.minZ, b.maxX, b.minY, b.maxZ, r, g, bC, a);
        // Top
        drawQuad(vc, pos, b.minX, b.maxY, b.minZ, b.maxX, b.maxY, b.maxZ, r, g, bC, a);
        // Sides
        drawQuad(vc, pos, b.minX, b.minY, b.minZ, b.minX, b.maxY, b.maxZ, r, g, bC, a);
        drawQuad(vc, pos, b.maxX, b.minY, b.minZ, b.maxX, b.maxY, b.maxZ, r, g, bC, a);
        drawQuad(vc, pos, b.minX, b.minY, b.minZ, b.maxX, b.maxY, b.minZ, r, g, bC, a);
        drawQuad(vc, pos, b.minX, b.minY, b.maxZ, b.maxX, b.maxY, b.maxZ, r, g, bC, a);
    }

    private void drawQuad(VertexConsumer vc, Matrix4f mat,
            double x1, double y1, double z1,
            double x2, double y2, double z2,
            float r, float g, float b, float a) {
        // Render as two triangles
        vc.vertex(mat, (float) x1, (float) y1, (float) z1).color(r, g, b, a).texture(0f, 0f).overlay(0).light(0xF000F0)
                .normal(0f, 1f, 0f).next();
        vc.vertex(mat, (float) x1, (float) y2, (float) z2).color(r, g, b, a).texture(0f, 0f).overlay(0).light(0xF000F0)
                .normal(0f, 1f, 0f).next();
        vc.vertex(mat, (float) x2, (float) y2, (float) z2).color(r, g, b, a).texture(0f, 0f).overlay(0).light(0xF000F0)
                .normal(0f, 1f, 0f).next();

        vc.vertex(mat, (float) x2, (float) y2, (float) z2).color(r, g, b, a).texture(0f, 0f).overlay(0).light(0xF000F0)
                .normal(0f, 1f, 0f).next();
        vc.vertex(mat, (float) x2, (float) y1, (float) z2).color(r, g, b, a).texture(0f, 0f).overlay(0).light(0xF000F0)
                .normal(0f, 1f, 0f).next();
        vc.vertex(mat, (float) x1, (float) y1, (float) z1).color(r, g, b, a).texture(0f, 0f).overlay(0).light(0xF000F0)
                .normal(0f, 1f, 0f).next();
    }

}
