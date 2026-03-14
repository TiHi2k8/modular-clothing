package com.example.examplemod.network;

import com.example.examplemod.capability.ClothingProvider;
import com.example.examplemod.capability.IClothingInventory;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/**
 * Client -> Server packet sent when the player confirms new transform values
 * (scale / XYZ offsets) for a specific clothing slot via GuiClothingTransform.
 */
public class PacketUpdateClothingTransform implements IMessage {
    private int layer;
    private int slot;
    private float scale;
    private float offsetX;
    private float offsetY;
    private float offsetZ;

    public PacketUpdateClothingTransform() {}

    public PacketUpdateClothingTransform(int layer, int slot, float scale, float offsetX, float offsetY, float offsetZ) {
        this.layer   = layer;
        this.slot    = slot;
        this.scale   = scale;
        this.offsetX = offsetX;
        this.offsetY = offsetY;
        this.offsetZ = offsetZ;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        layer   = buf.readInt();
        slot    = buf.readInt();
        scale   = buf.readFloat();
        offsetX = buf.readFloat();
        offsetY = buf.readFloat();
        offsetZ = buf.readFloat();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(layer);
        buf.writeInt(slot);
        buf.writeFloat(scale);
        buf.writeFloat(offsetX);
        buf.writeFloat(offsetY);
        buf.writeFloat(offsetZ);
    }

    public static class Handler implements IMessageHandler<PacketUpdateClothingTransform, IMessage> {
        @Override
        public IMessage onMessage(PacketUpdateClothingTransform message, MessageContext ctx) {
            EntityPlayerMP player = ctx.getServerHandler().player;
            player.getServerWorld().addScheduledTask(() -> {
                IClothingInventory inventory = player.getCapability(ClothingProvider.CLOTHING_CAPABILITY, null);
                if (inventory == null) return;
                if (message.layer < 0 || message.layer >= inventory.getLayerCount()) return;
                if (message.slot  < 0 || message.slot  >= 8)                         return;

                inventory.setSlotTransform(message.layer, message.slot,
                        message.scale, message.offsetX, message.offsetY, message.offsetZ);

                // Sync full inventory so the client's capability reflects the change
                ClothingNetworkHandler.sendToAllTracking(new PacketSyncClothingInventory(player), player);
                ClothingNetworkHandler.sendTo(new PacketSyncClothingInventory(player), player);
            });
            return null;
        }
    }
}
