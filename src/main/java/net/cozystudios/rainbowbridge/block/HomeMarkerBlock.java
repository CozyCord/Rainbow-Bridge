package net.cozystudios.rainbowbridge.block;

import net.cozystudios.rainbowbridge.block.entity.HomeMarkerBlockEntity;
import net.minecraft.block.Block;
import net.minecraft.block.BlockEntityProvider;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldView;

public class HomeMarkerBlock extends Block implements BlockEntityProvider {

    public HomeMarkerBlock(Settings settings) {
        super(settings.nonOpaque().noCollision().dropsNothing());
    }

    @Override
    public BlockRenderType getRenderType(BlockState state) {
        return BlockRenderType.MODEL; // important so it uses the model/texture
    }

    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new HomeMarkerBlockEntity(pos, state);
    }

    @Override
    public boolean canPlaceAt(BlockState state, WorldView world, BlockPos pos) {
        return false; // cannot place normally (prevents accidental placement)
    }

    @Override
    public void onBreak(World world, BlockPos pos, BlockState state, PlayerEntity player) {
        // do nothing
    }

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand,
            BlockHitResult hit) {
        return ActionResult.PASS; // ignore right clicks
    }

    @Override
    public boolean emitsRedstonePower(BlockState state) {
        return false; // prevent redstone interaction
    }
}