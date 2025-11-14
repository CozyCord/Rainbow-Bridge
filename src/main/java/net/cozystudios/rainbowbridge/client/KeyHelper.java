package net.cozystudios.rainbowbridge.client;

import org.lwjgl.glfw.GLFW;

import net.minecraft.client.MinecraftClient;

public class KeyHelper {

    public static boolean isAltDown() {
        MinecraftClient client = MinecraftClient.getInstance();
        long window = client.getWindow().getHandle();
        return GLFW.glfwGetKey(window, GLFW.GLFW_KEY_LEFT_ALT) == GLFW.GLFW_PRESS
                || GLFW.glfwGetKey(window, GLFW.GLFW_KEY_RIGHT_ALT) == GLFW.GLFW_PRESS;
    }
}
