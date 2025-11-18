package net.cozystudios.rainbowbridge.packets;

import java.util.UUID;

// Contains the UUIDs of the ocarina and the pet to bind it to
public record OcarinaUpdatePacket(UUID ocarinaUuid, UUID petUuid) {

}
