package net.cozystudios.rainbowbridge.packets;

import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

/** Packet for updating the server whenever the player sets a spawn point */
public record RespawnUpdatePacket(BlockPos pos, Identifier dimId) {

}