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

    public static void register(){
        CommandRegistrationCallback.EVENT.register(((commandDispatcher, commandRegistryAccess, registrationEnvironment) -> {
            commandDispatcher.register(CommandManager.literal("listpets")
                    .then(CommandManager.argument("player", EntityArgumentType.player())
                            .executes(TheRainbowBridgeCommands::listPets)
                    ));
        }));
    }

    private static int listPets(CommandContext<ServerCommandSource> commandContext) throws CommandSyntaxException {
        MinecraftServer server = commandContext.getSource().getServer();
        if (server == null) return 0;

        StringBuilder pets = new StringBuilder();
        ServerPlayerEntity player = EntityArgumentType.getPlayer(commandContext, "player");

        for (petData pet : petTracker.get(server).getTrackedMap().values()) {
            if (pet.ownerUUID.equals(player.getUuid())) {
                pets.append(pet.uuid);
                pets.append(":");

                pets.append(pet.position);
            }
        }

        commandContext.getSource().sendFeedback(()->Text.literal(pets.toString()),false);
        return 1;
    }
}
