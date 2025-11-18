package net.cozystudios.rainbowbridge.packets;

import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

/** Packet for handling the default home block */
public record DefaultSetHomeRequestPacket(BlockPos pos, Identifier dimId) {

}
