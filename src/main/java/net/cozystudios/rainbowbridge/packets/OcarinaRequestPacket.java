package net.cozystudios.rainbowbridge.packets;

import java.util.Map;
import java.util.UUID;

public record OcarinaRequestPacket (Map<UUID, UUID> bindings) {
    
}
