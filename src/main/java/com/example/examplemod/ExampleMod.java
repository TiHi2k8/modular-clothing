package com.example.examplemod;

import com.example.examplemod.capability.ClothingInventory;
import com.example.examplemod.capability.ClothingStorage;
import com.example.examplemod.capability.IClothingInventory;
import com.example.examplemod.client.ClientEventHandler;
import com.example.examplemod.client.KeybindHandler;
import com.example.examplemod.gui.ClothingGuiHandler;
import com.example.examplemod.network.ClothingNetworkHandler;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.network.NetworkRegistry;

@Mod(modid = ExampleMod.MODID, name = ExampleMod.NAME, version = ExampleMod.VERSION)
public class ExampleMod {
    public static final String MODID = "examplemod";
    public static final String NAME = "Modular Clothing";
    public static final String VERSION = "1.0";

    @SidedProxy(clientSide = "com.example.examplemod.ClientProxy", serverSide = "com.example.examplemod.CommonProxy")
    public static CommonProxy proxy;

    @Mod.Instance
    public static ExampleMod instance;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        CapabilityManager.INSTANCE.register(IClothingInventory.class, new ClothingStorage(), ClothingInventory::new);
        ClothingNetworkHandler.init();
        proxy.preInit(event);
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        NetworkRegistry.INSTANCE.registerGuiHandler(this, new ClothingGuiHandler());
        proxy.init(event);
    }
}
