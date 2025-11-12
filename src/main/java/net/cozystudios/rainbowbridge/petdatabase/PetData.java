package net.cozystudios.rainbowbridge.petdatabase;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.jetbrains.annotations.Nullable;

import net.cozystudios.rainbowbridge.accessors.ShoulderAccessor;
import net.cozystudios.rainbowbridge.accessors.TameableEntityDecorator;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class PetData {
    /**
     * Represents the UUID of the PetData
     * 
     * @see #entityUuid
     */
    public final UUID uuid;
    public UUID entityUuid;
    public BlockPos position;
    public NbtCompound collar;
    public final UUID ownerUUID;
    public final String ownerName;
    /** The time, in milliseconds, when the pet was tamed */
    public final long tameTimestamp;
    private NbtCompound entityData;

    public NbtCompound getEntityData() {
        return entityData;
    }

    public record PetEntityHandle(@Nullable TameableEntity entity, @Nullable NbtCompound shoulderNbt) {
    }

    public PetData(TameableEntity tame, Entity player, NbtCompound collarItem, long tameDate) {
        this.uuid = ((TameableEntityDecorator) tame).rainbowbridge_getUuid(); // Separate UUID from entity for tracking purposes
        this.entityUuid = tame.getUuid();
        this.position = tame.getBlockPos();
        this.ownerName = player.getEntityName();
        this.ownerUUID = player.getUuid();
        this.collar = collarItem;
        this.entityData = new NbtCompound();
        tame.saveSelfNbt(this.entityData);
        this.entityData.putString("EntityType", tame.getType().toString());
        this.tameTimestamp = tameDate;
    }

    public PetData(UUID rainbowbridgeUuid, BlockPos pos, UUID ownerUUID, String ownerName, NbtCompound collarItem,
            String name, long tameDate, NbtCompound entityData) {
        this.uuid = rainbowbridgeUuid;
        this.entityUuid = uuid;
        this.position = pos;
        this.ownerName = ownerName;
        this.ownerUUID = ownerUUID;
        this.collar = collarItem;
        this.tameTimestamp = tameDate;
        this.entityData = entityData;
    }

    public NbtCompound toNbt() {
        NbtCompound tag = new NbtCompound();
        tag.putUuid("RainbowBridgeEntityUUID", uuid);

        tag.putUuid("ownerUUID", ownerUUID);
        tag.putString("ownerName", ownerName);
        tag.put("collarItem", collar);

        tag.putInt("x", position.getX());
        tag.putInt("y", position.getY());
        tag.putInt("z", position.getZ());

        tag.putLong("tameDate", tameTimestamp);

        tag.put("EntityData", entityData);
        return tag;
    }

    public static PetData fromNbt(NbtCompound tag) {
        UUID rainbowbridge_uuid = tag.getUuid("RainbowBridgeEntityUUID");
        BlockPos pos = new BlockPos(tag.getInt("x"), tag.getInt("y"), tag.getInt("z"));
        UUID ownerUUID = tag.getUuid("ownerUUID");
        String ownerName = tag.getString("ownerName");
        NbtCompound collarItem = tag.getCompound("collarItem");
        String name = tag.getString("name");
        long tameDate = tag.getLong("tameDate");
        NbtCompound entityData = tag.contains("EntityData") ? tag.getCompound("EntityData") : null;

        return new PetData(rainbowbridge_uuid, pos, ownerUUID, ownerName, collarItem, name, tameDate, entityData);
    }

    public void updateEntityData(java.util.function.Consumer<NbtCompound> editor) {
        if (entityData == null)
            return;
        editor.accept(entityData);
    }

    /**
     * 
     * @param server
     * @Nullable
     * @return entity or nbt if entity is on player's shoulder
     */
    public CompletableFuture<PetEntityHandle> getEntity(MinecraftServer server) {
        if (server == null)
            return CompletableFuture.completedFuture(null);

        for (ServerWorld world : server.getWorlds()) {
            var entity = world.getEntity(this.entityUuid);
            if (entity != null) {
                return CompletableFuture.completedFuture(new PetEntityHandle((TameableEntity) entity, null));
            }
        }

        for (ServerWorld world : server.getWorlds()) {
            for (Entity entity : world.iterateEntities()) {
                if (entity instanceof TameableEntity tame && tame instanceof TameableEntityDecorator data) {
                    if (data.rainbowbridge_getUuid().equals(uuid)) {
                        return CompletableFuture.completedFuture(new PetEntityHandle((TameableEntity) tame, null));
                    }
                }
            }
        }

        var player = server.getPlayerManager().getPlayer(ownerUUID);
        if (player != null) {

            NbtCompound left = player.getShoulderEntityLeft();
            NbtCompound right = player.getShoulderEntityRight();
            // Check if the entity is on a player's shoulder
            for (NbtCompound nbt : List.of(left, right)) {
                if (nbt != null && !nbt.isEmpty()) {
                    if (entityDataMatchesShoulder(nbt, entityData)) {
                        return CompletableFuture.completedFuture(new PetEntityHandle(null, nbt));
                    }
                }
            }

        }

        return CompletableFuture.completedFuture(new PetEntityHandle(null, null));
    }

    /**
     * Recreate Entity
     * 
     * @param server
     * @param worldKey - Dimension to spawn entity
     * @param x        - Position to spawn entity
     * @param y
     * @param z
     * @return
     */
    @Nullable
    public TameableEntity recreateEntity(MinecraftServer server, RegistryKey<World> worldKey, double x, double y,
            double z) {
        if (server == null || entityData == null)
            return null;

        String typeId = entityData.getString("id");
        EntityType<?> type = Registries.ENTITY_TYPE.get(new Identifier(typeId));

        Entity entity = type.create(server.getWorld(worldKey));
        if (!(entity instanceof TameableEntity tame))
            return null;

        // Remove from shoulder
        ServerPlayerEntity player = server.getPlayerManager().getPlayer(ownerUUID);
        if (player != null) {
            NbtCompound left = player.getShoulderEntityLeft();
            NbtCompound right = player.getShoulderEntityRight();

            if (!left.isEmpty() && left.containsUuid("UUID") && left.getUuid("UUID").equals(entityUuid)) {
                ((ShoulderAccessor) player).rainbowbridge_clearShoulder(entityUuid);
            }

            if (!right.isEmpty() && right.containsUuid("UUID") && right.getUuid("UUID").equals(entityUuid)) {
                ((ShoulderAccessor) player).rainbowbridge_clearShoulder(entityUuid);
            }
        }

        NbtCompound nbtCopy = entityData.copy();
        UUID oldUuid = entityUuid;
        entityUuid = UUID.randomUUID();
        nbtCopy.putUuid("UUID", entityUuid);
        nbtCopy.remove("Pos");
        nbtCopy.remove("Motion");
        nbtCopy.remove("Rotation");
        // Remove sitting because we can't rely on entity.setSitting to work on new
        // entities
        nbtCopy.remove("Sitting");
        nbtCopy.remove("DeathTime");

        tame.readNbt(nbtCopy);
        entityData = nbtCopy;
        server.getWorld(worldKey).spawnEntity(tame);

        tame.refreshPositionAndAngles(x, y, z, entity.getYaw(), entity.getPitch());
        PetTracker pt = PetTracker.get(server);
        pt.getRecreatedMap().add(oldUuid); // Mark for deletion
        return tame;
    }

    private boolean entityDataMatchesShoulder(NbtCompound shoulderNbt, NbtCompound savedEntityData) {
        // Compare type
        String shoulderId = shoulderNbt.getString("id");
        String savedId = savedEntityData.getString("id");
        if (!shoulderId.equals(savedId))
            return false;

        // Compare variant (for parrots)
        if (shoulderId.equals("minecraft:parrot")) {
            int shoulderVariant = shoulderNbt.getInt("Variant");
            int savedVariant = savedEntityData.getInt("Variant");
            if (shoulderVariant != savedVariant)
                return false;
        }

        // Compare custom name if present
        boolean shoulderHasName = shoulderNbt.contains("CustomName");
        boolean savedHasName = savedEntityData.contains("CustomName");

        if (shoulderHasName && savedHasName) {
            String shoulderName = Text.Serializer.fromJson(shoulderNbt.getString("CustomName")).getString();
            String savedName = Text.Serializer.fromJson(savedEntityData.getString("CustomName")).getString();
            return shoulderName.equals(savedName);
        }

        return !shoulderHasName && !savedHasName;
    }

}
