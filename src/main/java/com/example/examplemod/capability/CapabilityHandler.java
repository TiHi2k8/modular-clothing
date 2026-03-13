package com.example.examplemod.capability;

import com.example.examplemod.ExampleMod;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

@Mod.EventBusSubscriber(modid = ExampleMod.MODID)
public class CapabilityHandler {
    public static final ResourceLocation CLOTHING_CAP = new ResourceLocation(ExampleMod.MODID, "clothing_inventory");

    @SubscribeEvent
    public static void attachCapabilities(AttachCapabilitiesEvent<Entity> event) {
        if (event.getObject() instanceof EntityPlayer) {
            event.addCapability(CLOTHING_CAP, new ClothingProvider());
        }
    }
}

