package net.cozystudios.rainbowbridge.petdatabase;

import java.util.UUID;

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

public class petData {
    public final UUID uuid;
    public final Identifier dim;
    public BlockPos position;
    public NbtCompound collar;
    public final UUID ownerUUID;
    public final String ownerName;

    public petData(TameableEntity tame, Entity player, NbtCompound collarItem) {
        this.uuid = tame.getUuid();
        this.dim = tame.getWorld().getRegistryKey().getValue();
        this.position = tame.getBlockPos();
        this.ownerName = player.getEntityName();
        this.ownerUUID = player.getUuid();
        this.collar = collarItem;
    }

    public petData(UUID uuid, Identifier dim, BlockPos pos, UUID ownerUUID, String ownerName, NbtCompound collarItem) {
        this.uuid = uuid;
        this.dim = dim;
        this.position = pos;
        this.ownerName = ownerName;
        this.ownerUUID = ownerUUID;
        this.collar = collarItem;
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
        return tag;
    }

    public static petData fromNbt(NbtCompound tag) {
        UUID uuid = tag.getUuid("UUID");
        Identifier dim = new Identifier(tag.getString("Dimension"));
        BlockPos pos = new BlockPos(tag.getInt("x"), tag.getInt("y"), tag.getInt("z"));
        UUID ownerUUID = tag.getUuid("ownerUUID");
        String ownerName = tag.getString("ownerName");
        NbtCompound collarItem = tag.getCompound("collarItem");
        return new petData(uuid, dim, pos, ownerUUID, ownerName, collarItem);
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

}
