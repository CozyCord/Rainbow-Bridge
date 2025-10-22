package net.cozystudios.rainbowbridge.mixin;

import net.cozystudios.rainbowbridge.TheRainbowBridge;
import net.cozystudios.rainbowbridge.petdatabase.petData;
import net.cozystudios.rainbowbridge.petdatabase.petTracker;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MobEntity.class)
public abstract class MobEntityMixin {

    //this does not work, gotta check to see if its overriden
    @Inject(method = "canTarget", at = @At("HEAD"), cancellable = true)
    private void stopTamedPetsAttacking(EntityType<?> type, CallbackInfoReturnable<Boolean> cir) {
        TheRainbowBridge.LOGGER.info("balls");
        MinecraftServer server = ((MobEntity)(Object)this).getWorld().getServer();
        if (server == null) return;

        petData trackedPetData = petTracker.get(server).get(((MobEntity)(Object)this).getUuid());
        if (trackedPetData != null) {
            cir.setReturnValue(false);
        }
    }

}