package net.cozystudios.rainbowbridge.items;

import java.util.List;
import java.util.UUID;

import org.jetbrains.annotations.Nullable;

import net.cozystudios.rainbowbridge.RainbowBridgeSounds;
import net.cozystudios.rainbowbridge.RaycastHelper;
import net.cozystudios.rainbowbridge.TameableWanderHelper;
import net.cozystudios.rainbowbridge.TaskScheduler;
import net.cozystudios.rainbowbridge.client.ClientOcarinaRegistry;
import net.cozystudios.rainbowbridge.client.ClientPetData;
import net.cozystudios.rainbowbridge.client.ClientPetList;
import net.cozystudios.rainbowbridge.homeblock.HomeBlock;
import net.cozystudios.rainbowbridge.homeblock.HomeBlock.HomeBlockHandle;
import net.cozystudios.rainbowbridge.petdatabase.OcarinaRegistry;
import net.cozystudios.rainbowbridge.petdatabase.PetData;
import net.cozystudios.rainbowbridge.petdatabase.PetTracker;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.sound.SoundCategory;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class RainbowOcarinaItem extends Item {
    public RainbowOcarinaItem(Settings settings) {
        super(settings);
    }

    @Override
    @Environment(EnvType.CLIENT)
    public boolean hasGlint(ItemStack stack) {
        if (stack.getNbt() == null || !stack.getNbt().contains("ocarinaUuid"))
            return false;

        UUID ocarinaUuid = UUID.fromString(stack.getNbt().getString("ocarinaUuid"));

        return ClientOcarinaRegistry.hasPet(ocarinaUuid);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity player, Hand hand) {
        ItemStack stack = player.getStackInHand(hand);
        // --- Check cooldown ---
        if (!player.getItemCooldownManager().isCoolingDown(this)) {

            if (!world.isClient) {
                NbtCompound nbt = stack.getOrCreateNbt();
                UUID ocarinaUuid = nbt.contains("ocarinaUuid")
                        ? UUID.fromString(nbt.getString("ocarinaUuid"))
                        : null;
                OcarinaRegistry registry = OcarinaRegistry.get(world.getServer());
                UUID petUuid = registry.getPetForOcarina(ocarinaUuid);

                // Summon bound pet
                if (!player.isSneaking() && petUuid != null) {
                    PetTracker tracker = PetTracker.get(world.getServer());
                    PetData pd = tracker.get(petUuid);
                    if (pd != null) {
                        BlockPos pos = RaycastHelper.getSafeBlock(player);
                        TaskScheduler.schedule(() -> {
                            pd.recreateEntity(
                                    world.getServer(),
                                    world.getRegistryKey(),
                                    pos.getX(),
                                    pos.getY(),
                                    pos.getZ());
                            var pdh = pd.getEntity(world.getServer()).join();
                            if (pdh != null && pdh.entity() != null) {
                                TameableWanderHelper.stopWandering(pdh.entity());
                            }
                        }, 30);
                    }

                    player.getItemCooldownManager().set(this, 60);
                }
            }

            if (player.isSneaking()) {
                player.playSound(
                        RainbowBridgeSounds.OCARINA_TWO,
                        SoundCategory.PLAYERS,
                        .7f,
                        1.0f);

            } else {
                player.playSound(
                        RainbowBridgeSounds.OCARINA,
                        SoundCategory.PLAYERS,
                        .7f,
                        1.0f);
            }
        }
        return TypedActionResult.success(player.getStackInHand(hand));

    }

    /** Handles interactions from Rainbow Pet entity callback */
    public ActionResult useOcarinaFromPet(World world, PlayerEntity player, ItemStack stack, PetData petData,
            TameableEntity tame) {
        NbtCompound nbt = stack.getOrCreateNbt();
        UUID ocarinaUuid = nbt.contains("ocarinaUuid")
                ? UUID.fromString(nbt.getString("ocarinaUuid"))
                : null;
        OcarinaRegistry registry = OcarinaRegistry.get(world.getServer());
        UUID petUuid = registry.getPetForOcarina(ocarinaUuid);

        // Bind the ocarina to pet if not already assigned to it
        if (player.isSneaking() && (petUuid == null || !petUuid.equals(petData.uuid))) {

            UUID ocarinaId = nbt.getString("ocarinaUuid") != ""
                    ? UUID.fromString(nbt.getString("ocarinaUuid"))
                    : null;
            // Assign uuid to ocarina if it doesn't have one
            if (!nbt.contains("ocarinaUuid")) {
                ocarinaId = UUID.randomUUID();
                nbt.putString("ocarinaUuid", ocarinaId.toString());
            }

            registry.register(ocarinaId, petData.uuid, world.getServer());

            player.sendMessage(Text.translatable("message.rainbowbridge.ocarina_bound"), true);
            return ActionResult.SUCCESS;
        }

        if (!player.getItemCooldownManager().isCoolingDown(this)) {
            // Send pet home if using using ocarina on assigned pet
            if (player.isSneaking() && registry.getPetForOcarina(ocarinaUuid) != null &&
                    registry.getPetForOcarina(ocarinaUuid).equals(petData.uuid)) {

                // Get homeblock from pet data, or fall back to default homeblock
                HomeBlockHandle hbh = petData.getHomeBlock();
                if (hbh == null) {
                    hbh = HomeBlock.get(world.getServer()).getHome(world.getServer(), player.getUuid());
                }
                if (hbh != null) {
                    TameableWanderHelper.makeTameableWander(tame);
                    petData.recreateEntity(
                            world.getServer(),
                            world.getRegistryKey(),
                            hbh.pos().getX(),
                            hbh.pos().getY(),
                            hbh.pos().getZ());
                    player.playSound(
                            RainbowBridgeSounds.OCARINA_TWO,
                            SoundCategory.PLAYERS,
                            .7f,
                            1.0f);

                    player.getItemCooldownManager().set(this, 60);
                    return ActionResult.PASS;
                }
            }
        }

        return ActionResult.PASS;
    }

    @Override
    @Environment(EnvType.CLIENT)
    public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
        NbtCompound nbt = stack.getNbt();
        if (nbt != null && nbt.contains("ocarinaUuid")) {
            UUID ocarinaUuid = UUID.fromString(nbt.getString("ocarinaUuid"));
            UUID petUuid = ClientOcarinaRegistry.getPet(ocarinaUuid);
            if (petUuid != null) {
                ClientPetData pd = ClientPetList.getPet(petUuid);
                if (pd != null) {
                    tooltip.add(Text.literal(pd.name).formatted(Formatting.AQUA));
                }
            }
        }

        if (Screen.hasShiftDown()) {
            List<OrderedText> lines = Tooltip.wrapLines(
                    MinecraftClient.getInstance(),
                    Text.translatable("tooltip.rainbowbridge.ocarina.info"));
            for (OrderedText line : lines) {
                StringBuilder sb = new StringBuilder();
                line.accept((index, style, codePoint) -> {
                    sb.appendCodePoint(codePoint);
                    return true;
                });
                tooltip.add(Text.literal(sb.toString()).formatted(Formatting.GRAY));
            }
        } else {
            tooltip.add(Text.translatable("tooltip.rainbowbridge.more_info").formatted(Formatting.GRAY));
        }
    }
}
