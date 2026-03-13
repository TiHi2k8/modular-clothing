package com.example.examplemod.network;

import com.example.examplemod.capability.ClothingProvider;
import com.example.examplemod.capability.IClothingInventory;
import com.example.examplemod.gui.ClothingContainer;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class PacketChangeClothingLayer implements IMessage {
    private int layer;

    public PacketChangeClothingLayer() { }

    public PacketChangeClothingLayer(int layer) {
        this.layer = layer;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        this.layer = buf.readInt();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(this.layer);
    }

    public static class Handler implements IMessageHandler<PacketChangeClothingLayer, IMessage> {
        @Override
        public IMessage onMessage(PacketChangeClothingLayer message, MessageContext ctx) {
            EntityPlayerMP player = ctx.getServerHandler().player;
            player.getServerWorld().addScheduledTask(() -> {
                // Validate if player has clothing container open
                // Also update the player capability if logic requires persistence of "selected layer" (optional)

                IClothingInventory inventory = player.getCapability(ClothingProvider.CLOTHING_CAPABILITY, net.minecraft.util.EnumFacing.UP);
                if (inventory != null) {
                    // If requested layer is beyond count, add layers until it exists (simple logic)
                    while (inventory.getLayerCount() <= message.layer) {
                        inventory.addLayer();
                        // Important: Need to save/sync this change!
                    }

                    // If the container is open, update it
                    if (player.openContainer instanceof ClothingContainer) {
                         ClothingContainer container = (ClothingContainer) player.openContainer;
                         container.setCurrentLayer(message.layer);
                         container.detectAndSendChanges(); // Force update of slots
                    }

                    // Sync because we might have added a layer
                    ClothingNetworkHandler.sendToAllTracking(new PacketSyncClothingInventory(player), player);
                    ClothingNetworkHandler.sendTo(new PacketSyncClothingInventory(player), player);
                }
            });
            return null;
        }
    }
}
