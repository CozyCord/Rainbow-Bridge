package net.cozystudios.rainbowbridge.homeblock;

import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

/** Packet for handling the default home block */
public record DefaultSetHomeRequestPacket(BlockPos pos, Identifier dimId) {

}
