package net.cozystudios.rainbowbridge;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

public class RaycastHelper {
    /**
     * Finds a safe BlockPos for an entity.
     * Tries the hit block, then adjacent blocks toward the player, then falls back
     * to player position.
     *
     * @return A safe BlockPos; defaults to player's feet if no safe spot found.
     */
    public static BlockPos getSafeBlock(PlayerEntity player) {
        double reach = player.isCreative() ? 5.0D : 4.5D;
        World world = player.getWorld();
        HitResult hit = player.raycast(reach, 0.0F, false);

        if (hit.getType() == HitResult.Type.BLOCK) {
            BlockHitResult blockHit = (BlockHitResult) hit;
            BlockPos hitPos = blockHit.getBlockPos();
            Direction face = blockHit.getSide();

            // Start with the block in front of the hit face
            BlockPos start = hitPos.offset(face);

            // List of candidate positions: the block in front, then the cardinal neighbors
            List<BlockPos> candidates = new ArrayList<>();
            candidates.add(start);

            // Add the other horizontal neighbors around the hit block
            for (Direction dir : Direction.Type.HORIZONTAL) {
                BlockPos candidate = hitPos.offset(dir);
                if (!candidate.equals(start)) {
                    candidates.add(candidate);
                }
            }

            // Check each candidate upwards for a safe block (air with solid block
            // underneath)
            int maxUpwards = 1;
            for (BlockPos candidate : candidates) {
                for (int i = 0; i <= maxUpwards; i++) {
                    BlockPos pos = candidate.up(i);
                    BlockPos below = pos.down();
                    if (world.isAir(pos) && !world.isAir(below)) {
                        return pos;
                    }
                }
            }
        }

        // Fallback to player's current block
        return player.getBlockPos();
    }

}