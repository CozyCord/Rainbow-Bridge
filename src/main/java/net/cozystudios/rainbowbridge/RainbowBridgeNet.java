package net.cozystudios.rainbowbridge;

import io.wispforest.owo.network.OwoNetChannel;
import net.minecraft.util.Identifier;

public class RainbowBridgeNet {
    public static final OwoNetChannel CHANNEL = OwoNetChannel.create(
        new Identifier("rainbowbridge", "main")
    );

    public static void register() {
        // Called in your ClientModInitializer / ModInitializer
    }
}