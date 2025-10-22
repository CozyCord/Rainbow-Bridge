package net.cozystudios.rainbowbridge;

import net.cozystudios.rainbowbridge.items.RainbowCollarItem;
import net.cozystudios.rainbowbridge.items.TheRainbowBridgeItems;
import net.cozystudios.rainbowbridge.petdatabase.petWatcher;
import net.fabricmc.api.ModInitializer;

import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TheRainbowBridge implements ModInitializer {
	public static final String MOD_ID = "rainbowbridge";

	// This logger is used to write text to the console and the log file.
	// It is considered best practice to use your mod id as the logger's name.
	// That way, it's clear which mod wrote info, warnings, and errors.
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		// This code runs as soon as Minecraft is in a mod-load-ready state.
		// However, some things (like resources) may still be uninitialized.
		// Proceed with mild caution.

        TheRainbowBridgeItems.registerItems();
        petWatcher.register();
        TheRainbowBridgeCommands.register();

        //cause dogs are annoying and return from a interaction early, we have to overwrite nbt methods
        UseEntityCallback.EVENT.register(((playerEntity, world, hand, entity, entityHitResult) -> {
            if (entityHitResult !=  null){
                return ActionResult.PASS;
            }
            if (world.isClient) return ActionResult.PASS;

            if (entity instanceof TameableEntity tame){
                if (!tame.isOwner(playerEntity)) return ActionResult.PASS;

                ItemStack stack = playerEntity.getStackInHand(hand);
                if (stack.getItem() instanceof RainbowCollarItem collarItem) {
                    return collarItem.applyCollar(stack, playerEntity, tame);
                } else if (stack.isEmpty() && playerEntity.isSneaking()) {
                    ItemStack collar = RainbowCollarItem.getCollar(playerEntity, tame);
                    if (collar == null) return ActionResult.PASS;
                    RainbowCollarItem.removePet(tame);

                    playerEntity.giveItemStack(collar);
                    return ActionResult.CONSUME;
                }

            }
            return ActionResult.PASS;
        }));
	}
}