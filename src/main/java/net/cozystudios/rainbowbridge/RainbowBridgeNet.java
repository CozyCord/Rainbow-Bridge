package net.cozystudios.rainbowbridge;

import io.wispforest.owo.network.OwoNetChannel;
import net.cozystudios.rainbowbridge.homeblock.HomeBlock;
import net.cozystudios.rainbowbridge.homeblock.HomeBlock.HomeBlockHandle;
import net.cozystudios.rainbowbridge.homeblock.HomeRequestPacket;
import net.cozystudios.rainbowbridge.homeblock.HomeSetRequestPacket;
import net.cozystudios.rainbowbridge.homeblock.HomeUpdatePacket;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

public class RainbowBridgeNet {
    public static final OwoNetChannel CHANNEL = OwoNetChannel.create(
            new Identifier("rainbowbridge", "main"));

    public static void register() {
        RainbowBridgeNet.CHANNEL.registerServerbound(HomeRequestPacket.class, (packet, handler) -> {
            ServerPlayerEntity player = handler.player();

            // Get the persistent HomeBlock instance for this world
            HomeBlock homes = HomeBlock.get(player.getServer());
            HomeBlockHandle hbh = homes.getHome(player.getUuid());

            if (hbh != null) {
                // Send it back to the client
                RainbowBridgeNet.CHANNEL.serverHandle(player).send(
                        new HomeUpdatePacket(hbh.pos(), hbh.dim().getValue()));
            }
        });

        RainbowBridgeNet.CHANNEL.registerServerbound(HomeSetRequestPacket.class, (packet, handler) -> {
            HomeBlock.get(handler.player().getServer()).setHome(handler.player(), packet.pos(),
                    RegistryKey.of(RegistryKeys.WORLD, packet.dimId()));
        });

        RainbowBridgeNet.CHANNEL.registerClientboundDeferred(HomeUpdatePacket.class);
    }
}