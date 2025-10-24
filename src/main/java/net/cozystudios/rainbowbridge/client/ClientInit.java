package net.cozystudios.rainbowbridge.client;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.nbt.NbtCompound;
import org.lwjgl.glfw.GLFW;

import net.cozystudios.rainbowbridge.RainbowBridgePackets;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;

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
                        if (client.currentScreen instanceof RosterScreen rosterScreen) {
                            rosterScreen.setPets(pets);
                        }
                    });
                });
    }

}
