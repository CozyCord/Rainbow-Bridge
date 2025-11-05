package net.cozystudios.rainbowbridge.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.cozystudios.rainbowbridge.accessors.TameableEntityDecorator;
import net.minecraft.entity.ai.goal.FollowOwnerGoal;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.passive.TameableEntity;

@Mixin(FollowOwnerGoal.class)
public abstract class FollowOwnerGoalMixin extends Goal {

    @Shadow
    protected TameableEntity tameable;

    @Inject(method = "canStart", at = @At("HEAD"), cancellable = true)
    private void onCanStart(CallbackInfoReturnable<Boolean> cir) {
        if (tameable instanceof TameableEntityDecorator td && td.rainbowbridge_isForceWander()) {
            cir.setReturnValue(false); // Skip following while wandering
        }
    }
}