package com.example.examplemod.network;

import com.example.examplemod.capability.ClothingCapabilityProvider;
import com.example.examplemod.capability.IClothingInventory;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.network.NetworkRegistry;

/**
 * Helper class for syncing clothing inventory to clients.
 */
public class SyncHelper {

    /** Tracking range for player entities in vanilla Minecraft. */
    private static final double TRACKING_RANGE = 512.0;

    /**
     * Sync the clothing inventory to the player themselves and all nearby tracking players.
     */
    public static void syncToTrackingPlayers(EntityPlayerMP player) {
        IClothingInventory inv = player.getCapability(ClothingCapabilityProvider.CLOTHING_CAP, null);
        if (inv == null) return;

        PacketSyncClothingInventory packet = new PacketSyncClothingInventory(player, inv);

        // Send to the player themselves
        ModNetworkHandler.INSTANCE.sendTo(packet, player);

        // Send to all nearby players who can see this player using TargetPoint
        NetworkRegistry.TargetPoint point = new NetworkRegistry.TargetPoint(
                player.dimension, player.posX, player.posY, player.posZ, TRACKING_RANGE);
        ModNetworkHandler.INSTANCE.sendToAllAround(packet, point);
    }

    /**
     * Sync clothing inventory to a specific player (used on login/dimension change).
     */
    public static void syncToPlayer(EntityPlayerMP target, EntityPlayer source) {
        IClothingInventory inv = source.getCapability(ClothingCapabilityProvider.CLOTHING_CAP, null);
        if (inv == null) return;

        PacketSyncClothingInventory packet = new PacketSyncClothingInventory(source, inv);
        ModNetworkHandler.INSTANCE.sendTo(packet, target);
    }
}

