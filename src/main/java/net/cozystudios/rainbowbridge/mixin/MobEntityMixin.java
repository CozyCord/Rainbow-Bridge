package net.cozystudios.rainbowbridge.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.cozystudios.rainbowbridge.TheRainbowBridge;
import net.cozystudios.rainbowbridge.accessors.MobEntityAccessor;
import net.cozystudios.rainbowbridge.petdatabase.PetData;
import net.cozystudios.rainbowbridge.petdatabase.PetTracker;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.goal.GoalSelector;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.server.MinecraftServer;

@Mixin(MobEntity.class)
public abstract class MobEntityMixin implements MobEntityAccessor {
    @Shadow
    public GoalSelector goalSelector;

    @Override
    public GoalSelector rainbowbridge_getGoalSelector() {
        return this.goalSelector;
    }

    // this does not work, gotta check to see if its overriden
    @Inject(method = "canTarget", at = @At("HEAD"), cancellable = true)
    private void stopTamedPetsAttacking(EntityType<?> type, CallbackInfoReturnable<Boolean> cir) {
        MinecraftServer server = ((MobEntity) (Object) this).getWorld().getServer();
        if (server == null)
            return;

        PetData trackedPetData = PetTracker.get(server).get(((MobEntity) (Object) this).getUuid());
        if (trackedPetData != null) {
            cir.setReturnValue(false);
        }
    }

}