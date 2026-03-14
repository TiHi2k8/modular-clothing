package com.example.examplemod.network;

import com.example.examplemod.capability.ClothingProvider;
import com.example.examplemod.capability.IClothingInventory;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/**
 * Client -> Server: toggle whether the CHEST slot also renders arms.
 * Sent when the player shift+left-clicks the CHEST slot in ClothingGui.
 */
public class PacketSetChestArmsMode implements IMessage {
    private int layer;
    private boolean showArms;

    public PacketSetChestArmsMode() {}

    public PacketSetChestArmsMode(int layer, boolean showArms) {
        this.layer    = layer;
        this.showArms = showArms;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        layer    = buf.readInt();
        showArms = buf.readBoolean();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(layer);
        buf.writeBoolean(showArms);
    }

    public static class Handler implements IMessageHandler<PacketSetChestArmsMode, IMessage> {
        @Override
        public IMessage onMessage(PacketSetChestArmsMode message, MessageContext ctx) {
            EntityPlayerMP player = ctx.getServerHandler().player;
            player.getServerWorld().addScheduledTask(() -> {
                IClothingInventory inventory = player.getCapability(ClothingProvider.CLOTHING_CAPABILITY, null);
                if (inventory == null) return;
                if (message.layer < 0 || message.layer >= inventory.getLayerCount()) return;

                inventory.setChestArmsMode(message.layer, message.showArms);

                ClothingNetworkHandler.sendToAllTracking(new PacketSyncClothingInventory(player), player);
                ClothingNetworkHandler.sendTo(new PacketSyncClothingInventory(player), player);
            });
            return null;
        }
    }
}
