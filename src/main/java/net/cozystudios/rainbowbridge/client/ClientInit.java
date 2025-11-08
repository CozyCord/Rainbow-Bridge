package net.cozystudios.rainbowbridge.client;

import java.util.ArrayList;
import java.util.List;

import org.lwjgl.glfw.GLFW;

import io.netty.buffer.Unpooled;
import net.cozystudios.rainbowbridge.RainbowBridgeNet;
import net.cozystudios.rainbowbridge.RainbowBridgePackets;
import net.cozystudios.rainbowbridge.homeblock.HomeBlockUpdateEvents;
import net.cozystudios.rainbowbridge.homeblock.HomeUpdatePacket;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.world.World;

public class ClientInit implements ClientModInitializer {
    private static KeyBinding openBookKey;

    @Override
    public void onInitializeClient() {
        openBookKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.rainbowbridge.open_book",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_P,
                "category.rainbowbridge.keys"));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (openBookKey.wasPressed()) {
                MinecraftClient mc = MinecraftClient.getInstance();
                if (mc != null) {
                    mc.setScreen(new RosterScreen());
                }
            }
        });

        ClientPlayNetworking.registerGlobalReceiver(RainbowBridgePackets.RESPONSE_PET_TRACKER,
                (client, handler, buf, responseSender) -> {
                    int size = buf.readInt();
                    List<ClientPetData> pets = new ArrayList<>();
                    for (int i = 0; i < size; i++) {
                        String entityTypeId = buf.readString(32767);
                        NbtCompound nbt = buf.readNbt();
                        String name = buf.readString(32767);
                        String position = buf.readString(32767);

                        pets.add(new ClientPetData(entityTypeId, nbt, name, position));
                    }

                    // update GUI on the main thread
                    client.execute(() -> {
                        ClientPetList.setPets(pets);
                    });
                });

        // Listen to home block update packet
        RainbowBridgeNet.CHANNEL.registerClientbound(HomeUpdatePacket.class, ((packet, content) -> {
            MinecraftClient.getInstance().execute(() -> {
                RegistryKey<World> dim = RegistryKey.of(RegistryKeys.WORLD, packet.dimId());
                HomeBlockUpdateEvents.fire(MinecraftClient.getInstance().player.getUuid(), packet.pos(), dim);
            });
        }));

        // Register a listener for when the player joins a world/server
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            // Send a packet to request pet data from the server
            PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
            ClientPlayNetworking.send(RainbowBridgePackets.REQUEST_PET_TRACKER, buf);

            ClientHomeBlock.initialize();
        });

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            ClientHomeBlock.reset();
            ClientPetList.reset();
        });
    }

}
