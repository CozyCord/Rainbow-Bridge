package net.cozystudios.rainbowbridge.mixin;

import java.util.UUID;

import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.cozystudios.rainbowbridge.petdatabase.PetData;
import net.cozystudios.rainbowbridge.petdatabase.PetTracker;
import net.minecraft.entity.Entity;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.text.Text;

@Mixin(Entity.class)
public class EntityMixin {
    @Shadow
    protected UUID uuid;

    // Update PetData NBT whenever entity is renamed
    @Inject(method = "setCustomName", at = @At("TAIL"))
    public void rainbowbridge$onSetCustomName(@Nullable Text name, CallbackInfo ci) {
        Entity entity = (Entity) (Object) this;
        if (entity instanceof TameableEntity) {
            PetTracker tracker = PetTracker.get(entity.getServer());
            PetData pd = tracker.get(uuid);
            if (pd == null) {
                System.err.println("[RainbowBridge] Could not find PetData for entity " + uuid);
            } else {
                pd.updateEntityData(nbt -> nbt.putString("CustomName", Text.Serializer.toJson(name)));
            }
        }
    }

}
