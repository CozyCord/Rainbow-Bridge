package net.cozystudios.rainbowbridge.items;

import net.cozystudios.rainbowbridge.RainbowBridgeNet;
import net.cozystudios.rainbowbridge.client.RosterScreen;
import net.cozystudios.rainbowbridge.homeblock.HomeUpdatePacket;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.RegistryKey;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class RainbowRosterItem extends Item {
    public RainbowRosterItem(Settings settings) {
        super(settings);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity player, Hand hand) {
        // Only open screen on the client side
        if (world.isClient) {
            // If sneak is held down set home block position
            if (player.isSneaking()) {
                BlockPos pos = player.getBlockPos();
                RegistryKey<World> dim = player.getWorld().getRegistryKey();
                RainbowBridgeNet.CHANNEL.clientHandle().send(new HomeUpdatePacket(pos, dim.getValue()));

                player.sendMessage(Text.translatable("message.rainbowbridge.home_set"));
            } else {
                openRosterScreen();
                player.playSound(SoundEvents.ITEM_BOOK_PAGE_TURN, 1.0F, 1.0F);
            }
        }
        return TypedActionResult.success(player.getStackInHand(hand));
    }

    @Environment(EnvType.CLIENT)
    private void openRosterScreen() {
        MinecraftClient.getInstance().setScreen(new RosterScreen());
    }
}
