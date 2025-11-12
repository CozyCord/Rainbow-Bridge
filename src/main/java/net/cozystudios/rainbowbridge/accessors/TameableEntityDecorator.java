package net.cozystudios.rainbowbridge.accessors;

import java.util.UUID;

public interface TameableEntityDecorator {
    boolean rainbowbridge_isForceWander();
    UUID rainbowbridge_uuid();

    void rainbowbridge_setForceWander(boolean value);
    void rainbowbridge_setUuid(UUID uuid);
    UUID rainbowbridge_getUuid();
}