package net.cozystudios.rainbowbridge.client;

import org.jetbrains.annotations.Nullable;

import io.wispforest.owo.ui.component.EntityComponent;
import io.wispforest.owo.ui.core.Sizing;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.nbt.NbtCompound;

public class DynamicEntityComponent extends EntityComponent<Entity> {

    @SuppressWarnings("unchecked")
    public DynamicEntityComponent(Sizing sizing, EntityType<?> type, @Nullable NbtCompound nbt) {
        super(sizing, (EntityType<Entity>) type, nbt);
        this.scaleToFit(true); // optional, ensures entity fits the box
    }

    /**
     * Factory method to create a component from a ClientPetData instance
     */
    public static DynamicEntityComponent fromPet(ClientPetData pet, float size) {
        return new DynamicEntityComponent(
            Sizing.fixed((int) size),
            pet.entity.getType(),
            pet.entity.writeNbt(new NbtCompound())
        );
    }
}