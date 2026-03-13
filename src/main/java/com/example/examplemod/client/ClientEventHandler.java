package com.example.examplemod.client;

import com.example.examplemod.capability.ClothingProvider;
import com.example.examplemod.render.ClothingRenderLayer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.RenderPlayer;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.Map;

public class ClientEventHandler {
    private boolean layersInitialized = false;

    @SubscribeEvent
    public void onRenderPlayer(RenderPlayerEvent.Post event) {
        if (!layersInitialized) {
            Map<String, RenderPlayer> skinMap = Minecraft.getMinecraft().getRenderManager().getSkinMap();
            for (RenderPlayer render : skinMap.values()) {
                render.addLayer(new ClothingRenderLayer(render));
            }
            layersInitialized = true;
        }
    }
}

