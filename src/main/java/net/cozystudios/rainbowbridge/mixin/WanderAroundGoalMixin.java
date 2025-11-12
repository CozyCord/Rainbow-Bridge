package net.cozystudios.rainbowbridge.mixin;

import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.cozystudios.rainbowbridge.accessors.TameableEntityDecorator;
import net.cozystudios.rainbowbridge.homeblock.HomeBlock;
import net.cozystudios.rainbowbridge.homeblock.HomeBlock.HomeBlockHandle;
import net.minecraft.entity.ai.goal.WanderAroundGoal;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.Vec3d;

@Mixin(WanderAroundGoal.class)
public abstract class WanderAroundGoalMixin {
    @Shadow
    @Final
    private PathAwareEntity mob; // shadow the private field

    @Shadow
    @Nullable
    protected abstract Vec3d getWanderTarget();

    @Inject(method = "getWanderTarget", at = @At("RETURN"), cancellable = true)
    private void clampWanderRadius(CallbackInfoReturnable<Vec3d> cir) {
        Vec3d target = cir.getReturnValue();
        if (target == null)
            return;

        // Clamp to radius of homeblock if forceWander is true
        if (this.mob instanceof TameableEntity pet) {
            if (pet instanceof TameableEntityDecorator
                    && ((TameableEntityDecorator) pet).rainbowbridge_isForceWander()) {

                HomeBlock homes = HomeBlock.get((MinecraftServer) pet.getServer());
                HomeBlockHandle home = homes.getHome(pet.getServer(), (pet.getOwnerUuid()));
                if (home != null) {
                    Vec3d homePos = Vec3d.ofCenter(home.pos());
                    double wanderRadius = 10; // Distance from home block pet can wander

                    if (target.distanceTo(homePos) > wanderRadius) {
                        Vec3d direction = target.subtract(homePos).normalize();
                        target = homePos.add(direction.multiply(wanderRadius));
                        cir.setReturnValue(target);
                    }
                }
            }
        }
    }
}