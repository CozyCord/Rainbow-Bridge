package net.cozystudios.rainbowbridge.items;

import java.util.List;
import java.util.UUID;

import org.jetbrains.annotations.Nullable;

import net.cozystudios.rainbowbridge.petdatabase.PetData;
import net.cozystudios.rainbowbridge.petdatabase.PetTracker;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.world.World;

public class RainbowCollarItem extends Item {
    private static final String UUIDtag = "animalUUID";

    public RainbowCollarItem(Settings settings) {
        super(settings);
    }

    public ActionResult applyCollar(ItemStack item, PlayerEntity user, TameableEntity tame) {
        // maybe a config setting here or something
        tame.setInvulnerable(true);

        // tell server to track the pet
        PetTracker.get(user.getServer()).addPet(tame, user, item);
        if (!user.isCreative())
            item.decrement(1);

        user.getWorld().playSound(
                null, // null = broadcast to nearby players
                tame.getBlockPos(), // sound position
                SoundEvents.ITEM_ARMOR_EQUIP_LEATHER, // pick any sound here
                SoundCategory.PLAYERS,
                1.0F, 1.0F);

        return ActionResult.SUCCESS;
    }

    public static ItemStack getCollar(PlayerEntity user, TameableEntity tame) {
        MinecraftServer server = tame.getWorld().getServer();
        if (server == null)
            return null;
        PetData data = PetTracker.get(server).get(tame.getUuid());
        if (data == null)
            return null;

        user.getWorld().playSound(
                null, // null = broadcast to nearby players
                tame.getBlockPos(), // sound position
                SoundEvents.ITEM_ARMOR_EQUIP_LEATHER, // pick any sound here
                SoundCategory.PLAYERS,
                1.0F, 1.0F);
        return ItemStack.fromNbt(data.collar);
    }

    public static void removePet(TameableEntity tame) {
        MinecraftServer server = tame.getWorld().getServer();
        if (server == null)
            return;
        PetTracker.get(server).removePet(server, tame.getUuid());
        tame.setInvulnerable(false);
    }

    public PetData getBoundPetData(World world, ItemStack item) {
        if (!item.hasNbt() || !item.getNbt().containsUuid(UUIDtag))
            return null;
        UUID uuid = item.getNbt().getUuid(UUIDtag);

        MinecraftServer server = world.getServer();
        if (server == null)
            return null;
        return PetTracker.get(server).get(uuid);
    }

    @Override
    public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
        if (Screen.hasShiftDown()) {
            tooltip.add(
                    Text.translatable("tooltip.rainbowbridge.collar.info")
                            .formatted(Formatting.GRAY));
        } else {
            tooltip.add(Text.translatable("tooltip.rainbowbridge.more_info").formatted(Formatting.GRAY));
        }
    }

}
