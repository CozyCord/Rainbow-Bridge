package net.cozystudios.rainbowbridge;

import io.wispforest.owo.network.OwoNetChannel;
import net.cozystudios.rainbowbridge.homeblock.HomeBlock;
import net.cozystudios.rainbowbridge.homeblock.HomeRequestPacket;
import net.cozystudios.rainbowbridge.homeblock.HomeUpdatePacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

public class RainbowBridgeNet {
    public static final OwoNetChannel CHANNEL = OwoNetChannel.create(
            new Identifier("rainbowbridge", "main"));

    public static void register() {
        RainbowBridgeNet.CHANNEL.registerServerbound(HomeRequestPacket.class, (packet, handler) -> {
            ServerPlayerEntity player = handler.player();
            ServerWorld world = player.getServerWorld();

            // Get the persistent HomeBlock instance for this world
            HomeBlock homes = HomeBlock.get(world);
            BlockPos homePos = homes.getHome(player.getUuid());

            // Send it back to the client
            RainbowBridgeNet.CHANNEL.serverHandle(player).send(
                    new HomeUpdatePacket(homePos));
        });

        RainbowBridgeNet.CHANNEL.registerServerbound(HomeUpdatePacket.class, (packet, handler) -> {
            BlockPos pos = packet.pos();
            HomeBlock.get(handler.player().getServerWorld()).setHome(handler.player(), pos);
        });

        RainbowBridgeNet.CHANNEL.registerClientboundDeferred(HomeUpdatePacket.class);
    }
}