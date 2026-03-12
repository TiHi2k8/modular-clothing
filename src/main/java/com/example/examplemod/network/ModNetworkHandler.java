package com.example.examplemod.network;

import com.example.examplemod.ExampleMod;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.relauncher.Side;

/**
 * Central network handler for the modular clothing mod.
 * Registers all packets and provides the network channel instance.
 */
public class ModNetworkHandler {

    public static final SimpleNetworkWrapper INSTANCE = NetworkRegistry.INSTANCE.newSimpleChannel(ExampleMod.MODID);

    private static int packetId = 0;

    public static void registerPackets() {
        // Client -> Server: request to open the clothing GUI
        INSTANCE.registerMessage(PacketOpenClothingGUI.Handler.class, PacketOpenClothingGUI.class, packetId++, Side.SERVER);

        // Server -> Client: full sync of all clothing slots
        INSTANCE.registerMessage(PacketSyncClothingInventory.Handler.class, PacketSyncClothingInventory.class, packetId++, Side.CLIENT);

        // Client -> Server: update a single clothing slot
        INSTANCE.registerMessage(PacketUpdateClothingSlot.Handler.class, PacketUpdateClothingSlot.class, packetId++, Side.SERVER);
    }
}

