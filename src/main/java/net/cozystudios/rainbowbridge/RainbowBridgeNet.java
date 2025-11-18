package net.cozystudios.rainbowbridge;

import io.wispforest.owo.network.OwoNetChannel;
import net.cozystudios.rainbowbridge.homeblock.HomeBlock;
import net.cozystudios.rainbowbridge.homeblock.HomeBlock.HomeBlockHandle;
import net.cozystudios.rainbowbridge.packets.DefaultSetHomeRequestPacket;
import net.cozystudios.rainbowbridge.packets.HomeRequestPacket;
import net.cozystudios.rainbowbridge.packets.HomeSetRequestPacket;
import net.cozystudios.rainbowbridge.packets.HomeUpdatePacket;
import net.cozystudios.rainbowbridge.packets.OcarinaUpdatePacket;
import net.cozystudios.rainbowbridge.packets.RespawnUpdatePacket;
import net.cozystudios.rainbowbridge.petdatabase.PetData;
import net.cozystudios.rainbowbridge.petdatabase.PetTracker;
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
            HomeBlockHandle hbh = homes.getHome(player.getServer(), player.getUuid());

            if (hbh != null) {
                // Send it back to the client
                RainbowBridgeNet.CHANNEL.serverHandle(player).send(
                        new HomeUpdatePacket(hbh.pos(), hbh.dim().getValue()));
            }
        });

        // Set individual pet home block
        RainbowBridgeNet.CHANNEL.registerServerbound(HomeSetRequestPacket.class, (packet, handler) -> {
            PetData pd = PetTracker.get(handler.player().getServer()).get(packet.uuid());
            if (pd != null) {
                pd.homePosition = packet.pos();
                pd.homeDimension = RegistryKey.of(RegistryKeys.WORLD, packet.dimId());
            }
        });

        // Set default home block
        RainbowBridgeNet.CHANNEL.registerServerbound(DefaultSetHomeRequestPacket.class, (packet, handler) -> {
            HomeBlock.get(handler.player().getServer()).setHome(handler.player(), packet.pos(),
                    RegistryKey.of(RegistryKeys.WORLD, packet.dimId()));
        });

        /** Update client with new home position if respawn point changes and home is not set */
        RainbowBridgeNet.CHANNEL.registerServerbound(RespawnUpdatePacket.class, (packet, handler) -> {
            if (!HomeBlock.get(handler.player().getServer()).hasHome(handler.player().getUuid())) {

                RainbowBridgeNet.CHANNEL.serverHandle(handler.player()).send(new HomeUpdatePacket(packet.pos(), packet.dimId()));
            }
        });

        RainbowBridgeNet.CHANNEL.registerClientboundDeferred(HomeUpdatePacket.class);
        RainbowBridgeNet.CHANNEL.registerClientboundDeferred(OcarinaUpdatePacket.class);
    }
}