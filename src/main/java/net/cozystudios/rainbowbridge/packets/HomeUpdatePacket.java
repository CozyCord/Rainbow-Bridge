package net.cozystudios.rainbowbridge.packets;

import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

public record HomeUpdatePacket(BlockPos pos, Identifier dimId) {

}
