package com.example.examplemod.network;

import com.example.examplemod.capability.ClothingProvider;
import com.example.examplemod.capability.IClothingInventory;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class PacketUpdateClothingSlot implements IMessage {
    private int slotId;
    private ItemStack stack;

    public PacketUpdateClothingSlot() {}

    public PacketUpdateClothingSlot(int slotId, ItemStack stack) {
        this.slotId = slotId;
        this.stack = stack;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        this.slotId = buf.readInt();
        this.stack = ByteBufUtils.readItemStack(buf);
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(this.slotId);
        ByteBufUtils.writeItemStack(buf, this.stack);
    }

    public static class Handler implements IMessageHandler<PacketUpdateClothingSlot, IMessage> {
        @Override
        public IMessage onMessage(PacketUpdateClothingSlot message, MessageContext ctx) {
            EntityPlayerMP player = ctx.getServerHandler().player;
            player.getServerWorld().addScheduledTask(() -> {
                IClothingInventory inventory = player.getCapability(ClothingProvider.CLOTHING_CAPABILITY, null);
                if (inventory != null) {
                    inventory.setStackInSlot(message.slotId, message.stack);
                    // Sync to all tracking players
                    ClothingNetworkHandler.INSTANCE.sendToAllTracking(new PacketSyncClothingInventory(player), player);
                    // Sync to self
                    ClothingNetworkHandler.INSTANCE.sendTo(new PacketSyncClothingInventory(player), player);
                }
            });
            return null;
        }
    }
}

