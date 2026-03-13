package com.example.examplemod;

import com.example.examplemod.client.ClientEventHandler;
import com.example.examplemod.client.KeybindHandler;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

public class ClientProxy extends CommonProxy {
    @Override
    public void preInit(FMLPreInitializationEvent event) {
        super.preInit(event);
        MinecraftForge.EVENT_BUS.register(new ClientEventHandler());
    }

    @Override
    public void init(FMLInitializationEvent event) {
        super.init(event);
        KeybindHandler.init();
        MinecraftForge.EVENT_BUS.register(new KeybindHandler());
    }
}
