package net.cozystudios.rainbowbridge.homeblock;

import java.util.UUID;

import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

/** Packet for handling requests to set individual pets' home blocks
 * @uuid The pet's UUID
 */
public record HomeSetRequestPacket(UUID uuid, BlockPos pos, Identifier dimId) {

}
