package net.cozystudios.rainbowbridge.items;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import net.cozystudios.rainbowbridge.RainbowBridgeNet;
import net.cozystudios.rainbowbridge.RaycastHelper;
import net.cozystudios.rainbowbridge.client.RosterScreen;
import net.cozystudios.rainbowbridge.homeblock.DefaultSetHomeRequestPacket;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.RegistryKey;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
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
            if (!player.isSneaking()) {
                openRosterScreen();
                player.playSound(SoundEvents.ITEM_BOOK_PAGE_TURN, 1.0F, 1.0F);
            }
        }

        if (!world.isClient) {
            // If sneak is held down set home block position
            if (player.isSneaking()) {
                RegistryKey<World> dim = player.getWorld().getRegistryKey();
                BlockPos pos = RaycastHelper.getSafeBlock(player);

                RainbowBridgeNet.CHANNEL.clientHandle().send(new DefaultSetHomeRequestPacket(pos, dim.getValue()));

                player.sendMessage(Text.translatable("message.rainbowbridge.default_home_set"), true);
            }
        }
        return TypedActionResult.success(player.getStackInHand(hand));
    }

    @Environment(EnvType.CLIENT)
    private void openRosterScreen() {
        MinecraftClient.getInstance().setScreen(new RosterScreen());
    }

    @Override
    public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
        if (Screen.hasShiftDown()) {
            tooltip.add(
                    Text.translatable("tooltip.rainbowbridge.roster.info")
                            .formatted(Formatting.GRAY));
        } else {
            tooltip.add(Text.translatable("tooltip.rainbowbridge.more_info").formatted(Formatting.GRAY));
        }
    }
}
