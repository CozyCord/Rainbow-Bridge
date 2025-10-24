package net.cozystudios.rainbowbridge.petdatabase;

import java.util.UUID;

import net.minecraft.entity.EntityType;
import net.minecraft.registry.Registries;
import org.jetbrains.annotations.Nullable;

import net.minecraft.entity.Entity;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class PetData {
    public final UUID uuid;
    public final Identifier dim;
    public BlockPos position;
    public NbtCompound collar;
    public final UUID ownerUUID;
    public final String ownerName;
    private final NbtCompound entityData;

    public PetData(TameableEntity tame, Entity player, NbtCompound collarItem) {
        this.uuid = tame.getUuid();
        this.dim = tame.getWorld().getRegistryKey().getValue();
        this.position = tame.getBlockPos();
        this.ownerName = player.getEntityName();
        this.ownerUUID = player.getUuid();
        this.collar = collarItem;
        this.entityData = new NbtCompound();
        tame.saveSelfNbt(this.entityData);
        this.entityData.putString("EntityType", tame.getType().toString());
    }

    public PetData(UUID uuid, Identifier dim, BlockPos pos, UUID ownerUUID, String ownerName, NbtCompound collarItem,
                   String name, NbtCompound entityData) {
        this.uuid = uuid;
        this.dim = dim;
        this.position = pos;
        this.ownerName = ownerName;
        this.ownerUUID = ownerUUID;
        this.collar = collarItem;
        this.entityData = entityData;
    }

    public NbtCompound toNbt() {
        NbtCompound tag = new NbtCompound();
        tag.putUuid("UUID", uuid);
        tag.putString("Dimension", dim.toString());

        tag.putUuid("ownerUUID", ownerUUID);
        tag.putString("ownerName", ownerName);
        tag.put("collarItem", collar);

        tag.putInt("x", position.getX());
        tag.putInt("y", position.getY());
        tag.putInt("z", position.getZ());

        tag.put("EntityData", entityData);
        return tag;
    }

    public static PetData fromNbt(NbtCompound tag) {
        UUID uuid = tag.getUuid("UUID");
        Identifier dim = new Identifier(tag.getString("Dimension"));
        BlockPos pos = new BlockPos(tag.getInt("x"), tag.getInt("y"), tag.getInt("z"));
        UUID ownerUUID = tag.getUuid("ownerUUID");
        String ownerName = tag.getString("ownerName");
        NbtCompound collarItem = tag.getCompound("collarItem");
        String name = tag.getString("name");
        NbtCompound entityData = tag.contains("EntityData") ? tag.getCompound("EntityData") : null;
        return new PetData(uuid, dim, pos, ownerUUID, ownerName, collarItem, name, entityData);
    }

    public TameableEntity getEntity(MinecraftServer server) {
        if (server == null)
            return null;

        // Convert stored Identifier to a RegistryKey<World>
        RegistryKey<World> worldKey = RegistryKey.of(RegistryKeys.WORLD, this.dim);

        // Get the ServerWorld
        ServerWorld world = server.getWorld(worldKey);
        if (world == null)
            return null;

        // Look up the entity by UUID
        Entity entity = world.getEntity(this.uuid);
        if (entity instanceof TameableEntity tame) {
            return tame;
        }

        return null;
    }

    @Nullable
    public TameableEntity recreateEntity(MinecraftServer server) {
        if (server == null || entityData == null) return null;


        String typeId = entityData.getString("EntityType");
        EntityType<?> type = Registries.ENTITY_TYPE.get(new Identifier(typeId));

        RegistryKey<World> worldKey = RegistryKey.of(RegistryKeys.WORLD, this.dim);
        Entity entity = type.create(server.getWorld(worldKey));
        if (!(entity instanceof TameableEntity tame)) return null;

        tame.readNbt(entityData); // load all saved NBT
        return tame;
    }

}
