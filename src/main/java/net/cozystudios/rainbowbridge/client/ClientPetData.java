package net.cozystudios.rainbowbridge.client;

import java.util.UUID;

import org.jetbrains.annotations.Nullable;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

public class ClientPetData {
    public final UUID uuid;
    public final String name;
    public final String position;
    public final Entity entity;
    @Nullable
    public BlockPos homePosition;
    @Nullable
    public Identifier homeDimension;

    public ClientPetData(UUID uuid, String entityType, NbtCompound entityData, String name, String position,
            BlockPos homePosition, Identifier homeDimension) {
        this.uuid = uuid;
        this.name = name;
        this.position = position;
        this.homePosition = homePosition;
        this.homeDimension = homeDimension;
        EntityType<?> type = Registries.ENTITY_TYPE.get(new Identifier(entityType));

        this.entity = type.create(MinecraftClient.getInstance().world);
        assert this.entity != null;
        this.entity.readNbt(entityData);
    }
}
