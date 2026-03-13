package com.example.examplemod.core;

import com.example.examplemod.capability.ClothingProvider;
import com.example.examplemod.capability.IClothingInventory;
import com.example.examplemod.network.ClothingNetworkHandler;
import com.example.examplemod.network.PacketSyncClothingInventory;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerLoggedInEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerRespawnEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerChangedDimensionEvent;

@Mod.EventBusSubscriber
public class CommonEventHandler {

    @SubscribeEvent
    public static void onPlayerJoin(PlayerLoggedInEvent event) {
        if (!event.player.world.isRemote && event.player instanceof EntityPlayerMP) {
            EntityPlayerMP player = (EntityPlayerMP) event.player;
            ClothingNetworkHandler.sendTo(new PacketSyncClothingInventory(player), player);
            ClothingNetworkHandler.sendToAllTracking(new PacketSyncClothingInventory(player), player);
        }
    }

    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        EntityPlayer original = event.getOriginal();
        EntityPlayer newPlayer = event.getEntityPlayer();

        // Copy capability data
        IClothingInventory oldInv = original.getCapability(ClothingProvider.CLOTHING_CAPABILITY, net.minecraft.util.EnumFacing.UP);
        IClothingInventory newInv = newPlayer.getCapability(ClothingProvider.CLOTHING_CAPABILITY, net.minecraft.util.EnumFacing.UP);

        if (oldInv != null && newInv != null) {
            newInv.copyFrom(oldInv);
        }
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerRespawnEvent event) {
        if (!event.player.world.isRemote && event.player instanceof EntityPlayerMP) {
            EntityPlayerMP player = (EntityPlayerMP) event.player;
            ClothingNetworkHandler.sendTo(new PacketSyncClothingInventory(player), player);
            ClothingNetworkHandler.sendToAllTracking(new PacketSyncClothingInventory(player), player);
        }
    }

    @SubscribeEvent
    public static void onDimensionChange(PlayerChangedDimensionEvent event) {
         if (!event.player.world.isRemote && event.player instanceof EntityPlayerMP) {
            EntityPlayerMP player = (EntityPlayerMP) event.player;
            ClothingNetworkHandler.sendTo(new PacketSyncClothingInventory(player), player);
            ClothingNetworkHandler.sendToAllTracking(new PacketSyncClothingInventory(player), player);
        }
    }

    @SubscribeEvent
    public static void onStartTracking(PlayerEvent.StartTracking event) {
        if (event.getTarget() instanceof EntityPlayer && !event.getEntityPlayer().world.isRemote) {
            EntityPlayer target = (EntityPlayer) event.getTarget();
            EntityPlayerMP tracker = (EntityPlayerMP) event.getEntityPlayer();

            // Send the target's clothing data to the tracker
            ClothingNetworkHandler.sendTo(new PacketSyncClothingInventory(target), tracker);
        }
    }
}

