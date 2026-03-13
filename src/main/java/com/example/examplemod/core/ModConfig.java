package com.example.examplemod.core;

import com.example.examplemod.ExampleMod;
import net.minecraftforge.common.config.Config;

@Config(modid = ExampleMod.MODID)
public class ModConfig {
    @Config.Comment("General configuration")
    public static General general = new General();

    public static class General {
        @Config.Comment("Enable debug mode for rendering")
        public boolean debugRendering = false;
    }
}

