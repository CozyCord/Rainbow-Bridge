package net.cozystudios.rainbowbridge.mixin;

import java.util.UUID;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import net.cozystudios.rainbowbridge.accessors.ShoulderAccessor;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;

@Mixin(PlayerEntity.class)
public abstract class PlayerEntityMixin implements ShoulderAccessor {

    @Shadow public abstract NbtCompound getShoulderEntityLeft();
    @Shadow public abstract NbtCompound getShoulderEntityRight();
    @Shadow protected abstract void setShoulderEntityLeft(NbtCompound nbt);
    @Shadow protected abstract void setShoulderEntityRight(NbtCompound nbt);

    @Override
    public void rainbowbridge_clearShoulder(UUID petUUID) {
        if (!getShoulderEntityLeft().isEmpty() && getShoulderEntityLeft().containsUuid("UUID") &&
            getShoulderEntityLeft().getUuid("UUID").equals(petUUID)) {
            setShoulderEntityLeft(new NbtCompound());
        }
        if (!getShoulderEntityRight().isEmpty() && getShoulderEntityRight().containsUuid("UUID") &&
            getShoulderEntityRight().getUuid("UUID").equals(petUUID)) {
            setShoulderEntityRight(new NbtCompound());
        }
    }
}