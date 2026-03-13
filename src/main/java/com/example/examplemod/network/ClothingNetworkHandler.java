package com.example.examplemod.network;

import com.example.examplemod.ExampleMod;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.relauncher.Side;

public class ClothingNetworkHandler {
    public static final SimpleNetworkWrapper INSTANCE = NetworkRegistry.INSTANCE.newSimpleChannel(ExampleMod.MODID);
    private static int packetId = 0;

    public static void init() {
        INSTANCE.registerMessage(PacketUpdateClothingSlot.Handler.class, PacketUpdateClothingSlot.class, packetId++, Side.SERVER);
        INSTANCE.registerMessage(PacketSyncClothingInventory.Handler.class, PacketSyncClothingInventory.class, packetId++, Side.CLIENT);
        INSTANCE.registerMessage(PacketOpenClothingGUI.Handler.class, PacketOpenClothingGUI.class, packetId++, Side.SERVER);
        INSTANCE.registerMessage(PacketChangeClothingLayer.Handler.class, PacketChangeClothingLayer.class, packetId++, Side.SERVER);
    }

    // Helper method to sync inventory to all tracking players
    public static void sendToAllTracking(PacketSyncClothingInventory packet, net.minecraft.entity.Entity entity) {
        INSTANCE.sendToAllTracking(packet, entity);
    }

    // Helper method to sync inventory to self
    public static void sendTo(PacketSyncClothingInventory packet, net.minecraft.entity.player.EntityPlayerMP player) {
        INSTANCE.sendTo(packet, player);
    }
}
