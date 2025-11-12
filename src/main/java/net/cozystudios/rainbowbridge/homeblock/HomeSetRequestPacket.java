package net.cozystudios.rainbowbridge.homeblock;

import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

public record HomeSetRequestPacket(BlockPos pos, Identifier dimId) {

}
