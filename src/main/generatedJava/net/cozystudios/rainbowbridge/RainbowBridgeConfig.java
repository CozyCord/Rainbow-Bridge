package net.cozystudios.rainbowbridge;

import blue.endless.jankson.Jankson;
import io.wispforest.owo.config.ConfigWrapper;
import io.wispforest.owo.config.Option;
import io.wispforest.owo.util.Observable;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public class RainbowBridgeConfig extends ConfigWrapper<net.cozystudios.rainbowbridge.RainbowBridgeConfigModel> {

    public final Keys keys = new Keys();



    private RainbowBridgeConfig() {
        super(net.cozystudios.rainbowbridge.RainbowBridgeConfigModel.class);
    }

    private RainbowBridgeConfig(Consumer<Jankson.Builder> janksonBuilder) {
        super(net.cozystudios.rainbowbridge.RainbowBridgeConfigModel.class, janksonBuilder);
    }

    public static RainbowBridgeConfig createAndLoad() {
        var wrapper = new RainbowBridgeConfig();
        wrapper.load();
        return wrapper;
    }

    public static RainbowBridgeConfig createAndLoad(Consumer<Jankson.Builder> janksonBuilder) {
        var wrapper = new RainbowBridgeConfig(janksonBuilder);
        wrapper.load();
        return wrapper;
    }



    public static class Keys {

    }
}

