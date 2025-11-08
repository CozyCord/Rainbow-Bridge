package net.cozystudios.rainbowbridge;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.buffer.Unpooled;
import net.cozystudios.rainbowbridge.items.RainbowCollarItem;
import net.cozystudios.rainbowbridge.items.TheRainbowBridgeItems;
import net.cozystudios.rainbowbridge.petdatabase.PetData;
import net.cozystudios.rainbowbridge.petdatabase.PetTracker;
import net.cozystudios.rainbowbridge.petdatabase.PetWatcher;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;

public class TheRainbowBridge implements ModInitializer {
    public static final String MOD_ID = "rainbowbridge";
    public static final RainbowBridgeConfig CONFIG = RainbowBridgeConfig.createAndLoad();

    // This logger is used to write text to the console and the log file.
    // It is considered best practice to use your mod id as the logger's name.
    // That way, it's clear which mod wrote info, warnings, and errors.
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        // This code runs as soon as Minecraft is in a mod-load-ready state.
        // However, some things (like resources) may still be uninitialized.
        // Proceed with mild caution.

        TheRainbowBridgeItems.registerItems();
        PetWatcher.register();
        TheRainbowBridgeCommands.register();
        RainbowBridgeNet.register();

        // cause dogs are annoying and return from a interaction early, we have to
        // overwrite nbt methods
        UseEntityCallback.EVENT.register(((playerEntity, world, hand, entity, entityHitResult) -> {
            if (entityHitResult != null) {
                return ActionResult.PASS;
            }
            if (world.isClient)
                return ActionResult.PASS;

            if (entity instanceof TameableEntity tame) {
                if (!tame.isOwner(playerEntity))
                    return ActionResult.PASS;

                ItemStack stack = playerEntity.getStackInHand(hand);
                if (stack.getItem() instanceof RainbowCollarItem collarItem) {
                    return collarItem.applyCollar(stack, playerEntity, tame);
                } else if (stack.isEmpty() && playerEntity.isSneaking()) {
                    ItemStack collar = RainbowCollarItem.getCollar(playerEntity, tame);
                    if (collar == null)
                        return ActionResult.PASS;
                    RainbowCollarItem.removePet(tame);

                    playerEntity.giveItemStack(collar);
                    return ActionResult.CONSUME;
                }

            }
            return ActionResult.PASS;
        }));

        // Register a global receiver to handle pet tracker requests from clients
        ServerPlayNetworking.registerGlobalReceiver(RainbowBridgePackets.REQUEST_PET_TRACKER,
                (server, player, handler, buf, responseSender) -> {
                    server.execute(() -> {
                        var petList = PetTracker.serializePetList(player);

                        // send back to the client
                        ServerPlayNetworking.send(player, RainbowBridgePackets.RESPONSE_PET_TRACKER, petList);
                    });
                });

        // Register a global receiver to handle pet summon requests from clients
        ServerPlayNetworking.registerGlobalReceiver(RainbowBridgePackets.REQUEST_PET_TELEPORT,
                (server, player, handler, buf, responseSender) -> {
                    try {
                        UUID petUuid = buf.readUuid();
                        double x = buf.readDouble();
                        double y = buf.readDouble();
                        double z = buf.readDouble();
                        Identifier dim = buf.readIdentifier();
                        Boolean shouldWander = buf.readBoolean();

                        server.execute(() -> {
                            PetTracker tracker = PetTracker.get(server);
                            PetData petData = tracker.getTrackedMap().get(petUuid);
                            TameableEntity entity = null;
                            RegistryKey<World> targetWorldKey = RegistryKey.of(RegistryKeys.WORLD, dim);

                            if (petData != null) {
                                // Either get the existing entity or recreate it if necessary
                                var pdh = petData.getEntity(server).join();
                                if (pdh != null) {
                                    pdh.entity().discard();
                                }
                                    entity = petData.recreateEntity(server, targetWorldKey, x, y, z);

                                if (entity == null) {
                                    System.err.println("Failed to recreate entity for pet UUID: " + petUuid);
                                    return;
                                }

                                if (shouldWander) {
                                    TameableWanderHelper.makeTameableWander(entity);
                                } else {
                                    entity.setSitting(false);
                                }

                            } else {
                                System.err.println("No pet data found for UUID: " + petUuid);
                            }
                        });
                    } catch (Exception e) {
                        System.err.println("Bad summon packet from " + player.getName().getString() + ": " + e);
                    } finally {
                        ServerPlayNetworking.send(player, RainbowBridgePackets.RESPONSE_PET_TELEPORT,
                                new PacketByteBuf(Unpooled.buffer()));
                    }
                });

        ServerEntityEvents.ENTITY_LOAD.register((entity, world) -> {
            if (entity instanceof TameableEntity) {
                PetTracker tracker = PetTracker.get(world.getServer());
                if (tracker.getRecreatedMap().removeIf((uuid) -> uuid.equals(entity.getUuid()))) {
                    tracker.markDirty();
                    entity.discard();
                }
            }
        });
    }
}