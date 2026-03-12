package com.example.examplemod;

import com.example.examplemod.capability.ClothingCapabilityStorage;
import com.example.examplemod.capability.ClothingInventory;
import com.example.examplemod.capability.IClothingInventory;
import com.example.examplemod.gui.ModGuiHandler;
import com.example.examplemod.network.ModNetworkHandler;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.Mod.Instance;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import org.apache.logging.log4j.Logger;

@Mod(modid = ExampleMod.MODID, name = ExampleMod.NAME, version = ExampleMod.VERSION)
public class ExampleMod
{
    public static final String MODID = "examplemod";
    public static final String NAME = "Modular Clothing";
    public static final String VERSION = "1.0";

    /** GUI ID for the clothing inventory screen */
    public static final int GUI_CLOTHING_ID = 0;

    public static Logger logger;

    @Instance(MODID)
    public static ExampleMod instance;

    @SidedProxy(clientSide = "com.example.examplemod.ClientProxy", serverSide = "com.example.examplemod.CommonProxy")
    public static CommonProxy proxy;

    @EventHandler
    public void preInit(FMLPreInitializationEvent event)
    {
        logger = event.getModLog();

        // Register the clothing inventory capability
        CapabilityManager.INSTANCE.register(IClothingInventory.class, new ClothingCapabilityStorage(), ClothingInventory::new);

        // Register network packets
        ModNetworkHandler.registerPackets();

        proxy.preInit(event);
    }

    @EventHandler
    public void init(FMLInitializationEvent event)
    {
        // Register the GUI handler for the clothing screen
        NetworkRegistry.INSTANCE.registerGuiHandler(instance, new ModGuiHandler());

        // Register player event handler (capabilities, lifecycle, sync)
        MinecraftForge.EVENT_BUS.register(new PlayerEventHandler());

        proxy.init(event);
    }
}
