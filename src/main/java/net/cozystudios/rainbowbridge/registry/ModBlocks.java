package net.cozystudios.rainbowbridge.registry;

import net.cozystudios.rainbowbridge.block.HomeMarkerBlock;
import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
import net.minecraft.block.Block;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public class ModBlocks {
    public static final Block HOME_MARKER_BLOCK = new HomeMarkerBlock(
            FabricBlockSettings.create()
                    .strength(-1.0F, -1.0F)
                    .nonOpaque()
                    .noCollision()
                    .dropsNothing());

    public static void register() {
        Registry.register(Registries.BLOCK,
                new Identifier("rainbowbridge", "home_marker"),
                HOME_MARKER_BLOCK);
    }
}
