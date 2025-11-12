package net.cozystudios.rainbowbridge.mixin;

import java.util.UUID;

import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.cozystudios.rainbowbridge.accessors.TameableEntityDecorator;
import net.cozystudios.rainbowbridge.petdatabase.PetData;
import net.cozystudios.rainbowbridge.petdatabase.PetTracker;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.MinecraftServer;

@Mixin(TameableEntity.class)
public abstract class TameableEntityMixin implements TameableEntityDecorator {
    private UUID rainbowbridge_uuid = UUID.randomUUID();

    @Override
    public void rainbowbridge_setUuid(UUID uuid) {
        this.rainbowbridge_uuid = uuid;
    }

    @Override
    public UUID rainbowbridge_getUuid() {
        return this.rainbowbridge_uuid;
    }

    @Shadow
    @Nullable
    public abstract UUID getOwnerUuid();

    @Unique
    private boolean rainbowbridge_forceWander = false;

    public boolean rainbowbridge_isForceWander() {
        return rainbowbridge_forceWander;
    }

    public void rainbowbridge_setForceWander(boolean value) {
        this.rainbowbridge_forceWander = value;
    }

    // disable other mobs being able to target pets
    @Inject(method = "canTarget", at = @At("HEAD"), cancellable = true)
    private void rainbowbridge$disableCanBeAttackedIfPet(LivingEntity target, CallbackInfoReturnable<Boolean> cir) {
        MinecraftServer server = target.getWorld().getServer();
        if (server == null)
            return;
        PetData trackedPetData = PetTracker.get(server).get(((TameableEntity) (Object) this).getUuid());
        if (trackedPetData != null) {
            cir.setReturnValue(true);
        }
    }

    // Remove wandering behavior when setSetting is called
    @Inject(method = "setSitting", at = @At("HEAD"))
    private void onSetSitting(boolean sitting, CallbackInfo ci) {
        this.rainbowbridge_forceWander = false;
    }

    @Inject(method = "writeCustomDataToNbt", at = @At("TAIL"))
    private void writeWanderStateToNbt(NbtCompound nbt, CallbackInfo ci) {
        nbt.putBoolean("RainbowBridgeForceWander", this.rainbowbridge_isForceWander());
        nbt.putUuid("RainbowBridgeEntityUUID", this.rainbowbridge_getUuid());
    }

    @Inject(method = "readCustomDataFromNbt", at = @At("TAIL"))
    private void readWanderStateFromNbt(NbtCompound nbt, CallbackInfo ci) {
        if (nbt.contains("RainbowBridgeForceWander")) {
            this.rainbowbridge_setForceWander(nbt.getBoolean("RainbowBridgeForceWander"));
        }
        if (nbt.contains("RainbowBridgeEntityUUID")) {
            UUID entityUuid = nbt.getUuid("RainbowBridgeEntityUUID");
            if (entityUuid != null) {
                this.rainbowbridge_setUuid(entityUuid);
            }
        }
    }

}
