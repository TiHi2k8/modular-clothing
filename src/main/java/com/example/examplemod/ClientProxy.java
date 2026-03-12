package com.example.examplemod;

import com.example.examplemod.render.LayerClothing;
import net.minecraft.client.renderer.entity.RenderPlayer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

import java.util.Map;

public class ClientProxy extends CommonProxy {
    @Override
    public void preInit(FMLPreInitializationEvent event) {
        super.preInit(event);
        KeyInputHandler.registerKeyBindings();
    }

    @Override
    public void init(FMLInitializationEvent event) {
        super.init(event);
        MinecraftForge.EVENT_BUS.register(new KeyInputHandler());

        // Register the clothing render layer on all player renderers (default + slim skin models)
        Map<String, RenderPlayer> skinMap = net.minecraft.client.Minecraft.getMinecraft().getRenderManager().getSkinMap();
        for (Map.Entry<String, RenderPlayer> entry : skinMap.entrySet()) {
            RenderPlayer renderPlayer = entry.getValue();
            renderPlayer.addLayer(new LayerClothing(renderPlayer));
        }

        ExampleMod.logger.info("Modular Clothing render layers registered for {} player skin types", skinMap.size());
    }
}

