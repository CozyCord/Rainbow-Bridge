package net.cozystudios.rainbowbridge;

import java.util.List;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.cozystudios.rainbowbridge.petdatabase.PetData;
import net.cozystudios.rainbowbridge.petdatabase.PetTracker;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.registry.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class TheRainbowBridgeCommands {

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(CommandManager.literal("listpets")
                    // /listpets → lists your own pets
                    .executes(TheRainbowBridgeCommands::listPetsSelf)

                    // /listpets <player> → lists another player's pets
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
        if (server == null)
            return 0;

        StringBuilder pets = new StringBuilder();
        List<PetData> petsList = PetTracker.get(server).getTrackedMap().values().stream()
                .filter(p -> p.ownerUUID.equals(player.getUuid()))
                .toList();

        for (int i = 0; i < petsList.size(); i++) {
            PetData pet = petsList.get(i);
            var pd = pet.getEntity(server).join();
            if (pd != null) {
                var tame = pd.entity();
                var nbt = pd.shoulderNbt();
                if (tame != null) {
                    // get custom name or default name if there is none
                    String name = tame.hasCustomName() ? tame.getCustomName().getString()
                            : tame.getType().getName().getString();

                    pets.append(name)
                            .append(": ");
                } else if (nbt != null) {

                    // name
                    String name = nbt.contains("CustomName")
                            ? Text.Serializer.fromJson(nbt.getString("CustomName")).getString()
                            : Registries.ENTITY_TYPE.get(new Identifier(nbt.getString("id"))).getName().getString();

                    pets.append(name)
                            .append(": ");
                }

                pets.append(pet.position.toShortString());

                if (i != petsList.size() - 1) {
                    pets.append("\n");
                }
            }
        }

        source.sendFeedback(() -> Text.literal(pets.toString()), false);
        return 1;
    }
}
