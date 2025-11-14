package net.cozystudios.rainbowbridge.registry;

import net.cozystudios.rainbowbridge.block.entity.HomeMarkerBlockEntity;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public class ModBlockEntities {

    public static BlockEntityType<HomeMarkerBlockEntity> HOME_MARKER;

    public static void register() {
        HOME_MARKER = Registry.register(
            Registries.BLOCK_ENTITY_TYPE,
            new Identifier("rainbowbridge", "home_marker"),
            FabricBlockEntityTypeBuilder.create(HomeMarkerBlockEntity::new,
                    ModBlocks.HOME_MARKER_BLOCK)
                    .build()
        );
    }
}