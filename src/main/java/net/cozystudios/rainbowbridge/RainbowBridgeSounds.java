package net.cozystudios.rainbowbridge;

import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;

public class RainbowBridgeSounds {
    public static SoundEvent OCARINA = registerSound("ocarina");
    public static SoundEvent OCARINA_TWO = registerSound("ocarinatwo");

	private static SoundEvent registerSound(String id) {
		Identifier identifier = Identifier.of(TheRainbowBridge.MOD_ID, id);
		return Registry.register(Registries.SOUND_EVENT, identifier, SoundEvent.of(identifier));
	}

	public static void register() {
		TheRainbowBridge.LOGGER.info("Registering " + TheRainbowBridge.MOD_ID + " Sounds");
		// Technically this method can stay empty, but some developers like to notify
		// the console, that certain parts of the mod have been successfully initialized
	}
}
