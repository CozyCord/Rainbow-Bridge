package net.cozystudios.rainbowbridge.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

public class ClientPetData {
    public final String name;
    public final String position;
    public final Entity entity;

    public ClientPetData(String entityType, NbtCompound entityData, String name, String position) {
        this.name = name;
        this.position = position;
        EntityType<?> type = Registries.ENTITY_TYPE.get(new Identifier(entityType));

        this.entity = type.create(MinecraftClient.getInstance().world);
        assert this.entity != null;
        this.entity.readNbt(entityData);
    }
}
