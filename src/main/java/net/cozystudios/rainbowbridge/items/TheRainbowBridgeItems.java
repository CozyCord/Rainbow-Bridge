package net.cozystudios.rainbowbridge.items;

import net.cozystudios.rainbowbridge.TheRainbowBridge;
import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroupEntries;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemGroups;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class TheRainbowBridgeItems {

    public static final Item COLLAR = registerItem("collar",
            new RainbowCollarItem(new FabricItemSettings().maxCount(1)));
    public static final Item ROSTER = registerItem("pet_roster",
            new RainbowRosterItem(new FabricItemSettings().maxCount(1)));
    public static final Item OCARINA = registerItem("ocarina",
            new RainbowOcarinaItem(new FabricItemSettings().maxCount(1)));

    public static final ItemGroup RAINBOW_ITEMS = Registry.register(Registries.ITEM_GROUP,
            new Identifier(TheRainbowBridge.MOD_ID, "rainbowbridge"), FabricItemGroup.builder()
                    .displayName(Text.literal("The Rainbow Bridge"))
                    .icon(() -> new ItemStack(COLLAR)).entries(((displayContext, entries) -> {
                        entries.add(COLLAR);
                        entries.add(ROSTER);
                        entries.add(OCARINA);
                    })).build());

    private static void addItemsToTab(FabricItemGroupEntries entries) {
        entries.add(COLLAR);
        entries.add(ROSTER);
        entries.add(OCARINA);
    }

    private static Item registerItem(String name, Item item) {
        return Registry.register(Registries.ITEM, new Identifier(TheRainbowBridge.MOD_ID, name), item);
    }

    public static void register() {
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.FUNCTIONAL).register(TheRainbowBridgeItems::addItemsToTab);
    }
}
