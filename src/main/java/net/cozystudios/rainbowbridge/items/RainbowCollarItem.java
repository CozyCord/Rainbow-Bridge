package net.cozystudios.rainbowbridge.items;

import net.cozystudios.rainbowbridge.petdatabase.petData;
import net.cozystudios.rainbowbridge.petdatabase.petTracker;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.world.World;

import java.util.UUID;

public class RainbowCollarItem extends Item {
    private static final String UUIDtag = "animalUUID";

    public RainbowCollarItem(Settings settings) {
        super(settings);
    }

    public ActionResult applyCollar(ItemStack item, PlayerEntity user, TameableEntity tame) {
            //maybe a config setting here or something
            tame.setInvulnerable(true);

            //tell server to track the pet
            petTracker.get(user.getServer()).addPet(tame, user, item);

            item.decrement(1);

            //for debugging, maybe a
            user.sendMessage(Text.literal(tame.getOwnerUuid().toString()));

            return ActionResult.SUCCESS;
    }

    public static ItemStack getCollar(TameableEntity tame){
        MinecraftServer server = tame.getWorld().getServer();
        if (server == null) return null;
        petData data = petTracker.get(server).get(tame.getUuid());
        if (data == null) return null;
        return ItemStack.fromNbt(data.collar);
    }

    public static void removePet(TameableEntity tame){
        MinecraftServer server = tame.getWorld().getServer();
        if (server == null) return;
        petTracker.get(server).removePet(tame.getUuid());
        tame.setInvulnerable(false);
    }

    public petData getBoundPetData(World world, ItemStack item){
        if (!item.hasNbt() || !item.getNbt().containsUuid(UUIDtag)) return null;
        UUID uuid = item.getNbt().getUuid(UUIDtag);

        MinecraftServer server = world.getServer();
        if (server == null) return null;
        return petTracker.get(server).get(uuid);
    }

}
