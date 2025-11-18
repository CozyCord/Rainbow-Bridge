package net.cozystudios.rainbowbridge.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.cozystudios.rainbowbridge.RainbowBridgeNet;
import net.cozystudios.rainbowbridge.packets.RespawnUpdatePacket;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

@Mixin(ServerPlayerEntity.class)
public abstract class ServerPlayerEntityMixin {
    @Inject(method = "setSpawnPoint", at = @At("RETURN"))
    private void onSetSpawnPoint(RegistryKey<World> dimension, BlockPos pos, float angle, boolean forced,
            boolean update, CallbackInfo ci) {
        RainbowBridgeNet.CHANNEL.clientHandle().send(
                new RespawnUpdatePacket(pos, dimension.getValue()));
    }
}