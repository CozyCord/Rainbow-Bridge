package net.cozystudios.rainbowbridge.mixin;

import net.cozystudios.rainbowbridge.petdatabase.PetData;
import net.cozystudios.rainbowbridge.petdatabase.PetTracker;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.server.MinecraftServer;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.UUID;

@Mixin(TameableEntity.class)
public abstract class TameableEntityMixin {
    @Shadow
    @Nullable
    public abstract UUID getOwnerUuid();

    //disable other mobs being able to target pets
    @Inject(method = "canTarget", at = @At("HEAD"), cancellable = true)
    private void rainbowbridge$disableCanBeAttackedIfPet(LivingEntity target, CallbackInfoReturnable<Boolean> cir){
        MinecraftServer server = target.getWorld().getServer();
        if (server == null) return;
        PetData trackedPetData = PetTracker.get(server).get(((TameableEntity)(Object)this).getUuid());
        if (trackedPetData != null) {
            cir.setReturnValue(true);
        }
    }
}
