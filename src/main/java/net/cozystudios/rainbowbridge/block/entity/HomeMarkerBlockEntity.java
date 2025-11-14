package net.cozystudios.rainbowbridge.block.entity;

import net.cozystudios.rainbowbridge.registry.ModBlockEntities;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.util.math.BlockPos;

public class HomeMarkerBlockEntity extends BlockEntity {

    public HomeMarkerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.HOME_MARKER, pos, state);
    }
}
