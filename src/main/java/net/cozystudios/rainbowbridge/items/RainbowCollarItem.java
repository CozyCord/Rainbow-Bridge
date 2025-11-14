package net.cozystudios.rainbowbridge.items;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.jetbrains.annotations.Nullable;

import net.cozystudios.rainbowbridge.RainbowBridgeNet;
import net.cozystudios.rainbowbridge.RaycastHelper;
import net.cozystudios.rainbowbridge.homeblock.HomeSetRequestPacket;
import net.cozystudios.rainbowbridge.petdatabase.PetData;
import net.cozystudios.rainbowbridge.petdatabase.PetTracker;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class RainbowCollarItem extends Item {
    private static final String UUIDtag = "animalUUID";

    public RainbowCollarItem(Settings settings) {
        super(settings);
    }

    @Override
    public boolean hasGlint(ItemStack stack) {
        NbtCompound nbt = stack.getNbt();
        return nbt != null && nbt.contains("HomePos");
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity player, Hand hand) {
        ItemStack stack = player.getStackInHand(hand);

        if (!world.isClient) {
            if (player.isSneaking()) {
                RegistryKey<World> dim = player.getWorld().getRegistryKey();
                NbtCompound nbt = stack.getOrCreateNbt();

                BlockPos pos = RaycastHelper.getSafeBlock(player);

                nbt.putLong("HomePos", pos.asLong());
                nbt.putString("HomeDim", dim.getValue().toString());
                stack.setNbt(nbt);

                player.sendMessage(Text.translatable("message.rainbowbridge.home_set"), true);
            }
        }

        return TypedActionResult.success(stack);
    }

    public ActionResult applyCollar(ItemStack item, PlayerEntity user, TameableEntity tame) {
        // maybe a config setting here or something
        tame.setInvulnerable(true);

        var tracker = PetTracker.get(user.getServer());
        // tell server to track the pet
        tracker.addPet(tame, user, item, Instant.now().toEpochMilli());
        if (!user.isCreative())
            item.decrement(1);

        PetData pd = tracker.getByEntityId(tame.getUuid());
        var pos = item.hasNbt() && item.getNbt().contains("HomePos")
                ? BlockPos.fromLong(item.getNbt().getLong("HomePos"))
                : null;
        var dim = item.hasNbt() && item.getNbt().contains("HomeDim")
                ? new Identifier(item.getNbt().getString("HomeDim"))
                : null;

        RainbowBridgeNet.CHANNEL.clientHandle().send(new HomeSetRequestPacket(pd.uuid, pos, dim));
        user.getWorld().playSound(
                null, // null = broadcast to nearby players
                tame.getBlockPos(), // sound position
                SoundEvents.ITEM_ARMOR_EQUIP_LEATHER, // pick any sound here
                SoundCategory.PLAYERS,
                1.0F, 1.0F);

        return ActionResult.SUCCESS;
    }

    public static ItemStack getCollar(PlayerEntity user, TameableEntity tame, PetData petData) {
        MinecraftServer server = tame.getWorld().getServer();
        if (server == null)
            return null;
        if (petData == null)
            return null;

        user.getWorld().playSound(
                null, // null = broadcast to nearby players
                tame.getBlockPos(), // sound position
                SoundEvents.ITEM_ARMOR_EQUIP_LEATHER, // pick any sound here
                SoundCategory.PLAYERS,
                1.0F, 1.0F);
        return ItemStack.fromNbt(petData.collar);
    }

    public static void removePet(TameableEntity tame, PetData petData) {
        MinecraftServer server = tame.getWorld().getServer();
        if (server == null)
            return;
        PetTracker.get(server).removePet(server, petData.uuid);
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
        NbtCompound nbt = stack.getNbt();
        if (nbt != null && nbt.contains("HomePos")) {
            BlockPos pos = BlockPos.fromLong(nbt.getLong("HomePos"));

            tooltip.add(Text.literal("Home Position: ")
                    .append(Text.literal(pos.getX() + ", " + pos.getY() + ", " + pos.getZ())
                            .formatted(Formatting.AQUA)));
        }
        if (!Screen.hasShiftDown()) {
            tooltip.add(
                    Text.translatable("tooltip.rainbowbridge.more_info")
                            .formatted(Formatting.GRAY));
        } else {
            List<OrderedText> lines = Tooltip.wrapLines(
                    MinecraftClient.getInstance(),
                    Text.translatable("tooltip.rainbowbridge.collar.info"));
            for (OrderedText line : lines) {
                StringBuilder sb = new StringBuilder();
                line.accept((index, style, codePoint) -> {
                    sb.appendCodePoint(codePoint);
                    return true;
                });
                tooltip.add(Text.literal(sb.toString()).formatted(Formatting.GRAY));
            }
        }
    }

}
