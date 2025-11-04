package net.cozystudios.rainbowbridge.petdatabase;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.jetbrains.annotations.Nullable;

import net.cozystudios.rainbowbridge.ShoulderAccessor;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ChunkTicketType;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.Unit;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
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

    public record PetEntityHandle(@Nullable TameableEntity entity, @Nullable NbtCompound shoulderNbt) {
    }

    /**
     * 
     * @param server
     * @return entity or nbt if entity is on player's shoulder
     */
    public CompletableFuture<PetEntityHandle> getEntity(MinecraftServer server) {
        if (server == null)
            return CompletableFuture.completedFuture(null);

        // Convert stored Identifier to a RegistryKey<World>
        RegistryKey<World> worldKey = RegistryKey.of(RegistryKeys.WORLD, this.dim);

        // Get the ServerWorld
        ServerWorld world = server.getWorld(worldKey);
        if (world == null)
            return CompletableFuture.completedFuture(null);

        var player = server.getPlayerManager().getPlayer(ownerUUID);
        if (player != null) {

            NbtCompound left = player.getShoulderEntityLeft();
            NbtCompound right = player.getShoulderEntityRight();
            // First, check if the entity is on a player's shoulder
            for (NbtCompound nbt : List.of(left, right)) {
                if (nbt != null && !nbt.isEmpty()) {
                    if (entityDataMatchesShoulder(nbt, entityData)) {
                        return CompletableFuture.completedFuture(new PetEntityHandle(null, nbt));
                    }
                }
            }

        }

        // Look up the entity by UUID, load chunks if needed
        Entity entity = world.getEntity(this.uuid);
        if (entity instanceof TameableEntity tame) {
            return CompletableFuture.completedFuture(new PetEntityHandle(tame, null));
        } else {
            loadChunks(server).join();
            entity = world.getEntity(this.uuid);
            if (entity instanceof TameableEntity tame) {
                return CompletableFuture.completedFuture(new PetEntityHandle(tame, null));
            }
        }

        return CompletableFuture.completedFuture(null);
    }

    @Nullable
    public TameableEntity recreateEntity(MinecraftServer server) {
        if (server == null || entityData == null)
            return null;

        String typeId = entityData.getString("id");
        EntityType<?> type = Registries.ENTITY_TYPE.get(new Identifier(typeId));

        RegistryKey<World> worldKey = RegistryKey.of(RegistryKeys.WORLD, this.dim);
        Entity entity = type.create(server.getWorld(worldKey));
        if (!(entity instanceof TameableEntity tame))
            return null;

        // Remove from shoulder
        ServerPlayerEntity player = server.getPlayerManager().getPlayer(ownerUUID);
        if (player != null) {
            NbtCompound left = player.getShoulderEntityLeft();
            NbtCompound right = player.getShoulderEntityRight();

            if (!left.isEmpty() && left.containsUuid("UUID") && left.getUuid("UUID").equals(uuid)) {
                ((ShoulderAccessor) player).rainbowbridge_clearShoulder(uuid);
            }

            if (!right.isEmpty() && right.containsUuid("UUID") && right.getUuid("UUID").equals(uuid)) {
                ((ShoulderAccessor) player).rainbowbridge_clearShoulder(uuid);
            }
        }

        tame.readNbt(entityData); // load all saved NBT
        server.getWorld(worldKey).spawnEntity(tame);
        return tame;
    }

    /** Load chunk(s) pet entity is in */
    protected CompletableFuture<Boolean> loadChunks(MinecraftServer server) {
        RegistryKey<World> worldKey = RegistryKey.of(RegistryKeys.WORLD, this.dim);
        ServerWorld world = server.getWorld(worldKey);
        if (world != null) {
            // Ensure chunk is loaded (force load if needed)
            world.getChunkManager().addTicket(
                    ChunkTicketType.START,
                    new ChunkPos(position),
                    2,
                    Unit.INSTANCE);
            world.getChunk(position);
            return CompletableFuture
                    .completedFuture(true);
        } else {
            return CompletableFuture.completedFuture(null);
        }
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
