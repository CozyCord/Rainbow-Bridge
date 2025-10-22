package net.cozystudios.rainbowbridge;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.cozystudios.rainbowbridge.petdatabase.petData;
import net.cozystudios.rainbowbridge.petdatabase.petTracker;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

public class TheRainbowBridgeCommands {

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(CommandManager.literal("listpets")
                // /listpets  → lists your own pets
                .executes(TheRainbowBridgeCommands::listPetsSelf)

                // /listpets <player>  → lists another player's pets
                .then(CommandManager.argument("player", EntityArgumentType.player())
                    .executes(TheRainbowBridgeCommands::listPetsOther)));
        });
    }

    // /listpets (no argument)
    private static int listPetsSelf(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity player = context.getSource().getPlayer();
        return listPetsInternal(context.getSource(), player);
    }

    // /listpets <player>
    private static int listPetsOther(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity player = EntityArgumentType.getPlayer(context, "player");
        return listPetsInternal(context.getSource(), player);
    }

    // Shared logic
    private static int listPetsInternal(ServerCommandSource source, ServerPlayerEntity player) {
        MinecraftServer server = source.getServer();
        if (server == null) return 0;

        StringBuilder pets = new StringBuilder();
        for (petData pet : petTracker.get(server).getTrackedMap().values()) {
            if (pet.ownerUUID.equals(player.getUuid())) {
                pets.append(pet.uuid).append(":").append(pet.position).append("\n");
            }
        }

        source.sendFeedback(() -> Text.literal(pets.toString()), false);
        return 1;
    }
}

