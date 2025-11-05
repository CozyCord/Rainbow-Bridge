package net.cozystudios.rainbowbridge;

import net.cozystudios.rainbowbridge.accessors.TameableEntityDecorator;
import net.cozystudios.rainbowbridge.accessors.MobEntityAccessor;
import net.cozystudios.rainbowbridge.homeblock.HomeBlock;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.ai.goal.WanderAroundGoal;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class TameableWanderHelper {

    /**
     * Makes a tameable entity wander around temporarily as if untamed.
     * Does not permanently change tamed status.
     */
    public static void makeTameableWander(TameableEntity entity) {
        if (entity == null || entity.getWorld().isClient)
            return;

        entity.setSitting(false);
        entity.getNavigation().stop();

        try {
            MobEntityAccessor accessor = (MobEntityAccessor) entity;
            // Enable wandering
            if (accessor instanceof TameableEntityDecorator wanderable) {
                wanderable.rainbowbridge_setForceWander(true);
            }
            accessor.rainbowbridge_getGoalSelector().add(5, new WanderAroundGoal(entity, 1.0, 20));

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void stopWandering(TameableEntity entity) {
        if (entity instanceof TameableEntityDecorator wanderable) {
            wanderable.rainbowbridge_setForceWander(false);

            // Remove any existing WanderAroundGoal
            try {
                MobEntityAccessor accessor = (MobEntityAccessor) entity;

                accessor.rainbowbridge_getGoalSelector().getGoals().forEach(prioritizedGoal -> {
                    Goal goal = prioritizedGoal.getGoal();
                    if (goal instanceof WanderAroundGoal) {
                        if (prioritizedGoal.isRunning()) {
                            prioritizedGoal.stop();
                        }
                        accessor.rainbowbridge_getGoalSelector().remove(goal);
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

}