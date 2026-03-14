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
                if (inventory == null) return;

                // Hard cap: never exceed 10 layers (indices 0-9)
                if (message.layer < 0 || message.layer > 9) return;

                // Add layers if the target does not yet exist (addLayer enforces the cap internally)
                while (inventory.getLayerCount() <= message.layer) {
                    inventory.addLayer();
                }

                // Clamp to what actually exists (in case addLayer was blocked by the cap)
                int targetLayer = Math.min(message.layer, inventory.getLayerCount() - 1);

                if (player.openContainer instanceof ClothingContainer) {
                    ClothingContainer container = (ClothingContainer) player.openContainer;
                    container.setCurrentLayer(targetLayer);
                    container.detectAndSendChanges();
                }

                ClothingNetworkHandler.sendToAllTracking(new PacketSyncClothingInventory(player), player);
                ClothingNetworkHandler.sendTo(new PacketSyncClothingInventory(player), player);
            });
            return null;
        }
    }
}
