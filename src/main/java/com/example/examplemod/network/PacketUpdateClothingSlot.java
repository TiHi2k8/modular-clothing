package com.example.examplemod.network;

import com.example.examplemod.capability.ClothingCapabilityProvider;
import com.example.examplemod.capability.IClothingInventory;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/**
 * Client -> Server packet to update a single clothing slot.
 * The server validates the change before applying it.
 */
public class PacketUpdateClothingSlot implements IMessage {

    private int slotIndex;
    private NBTTagCompound stackTag;

    public PacketUpdateClothingSlot() {
    }

    public PacketUpdateClothingSlot(int slotIndex, ItemStack stack) {
        this.slotIndex = slotIndex;
        this.stackTag = stack.writeToNBT(new NBTTagCompound());
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        slotIndex = buf.readInt();
        stackTag = ByteBufUtils.readTag(buf);
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(slotIndex);
        ByteBufUtils.writeTag(buf, stackTag);
    }

    public static class Handler implements IMessageHandler<PacketUpdateClothingSlot, IMessage> {
        @Override
        public IMessage onMessage(PacketUpdateClothingSlot message, MessageContext ctx) {
            EntityPlayerMP player = ctx.getServerHandler().player;
            player.getServerWorld().addScheduledTask(() -> {
                IClothingInventory inv = player.getCapability(ClothingCapabilityProvider.CLOTHING_CAP, null);
                if (inv != null) {
                    ItemStack stack = new ItemStack(message.stackTag);
                    if (inv.isItemValidForSlot(message.slotIndex, stack)) {
                        inv.setStackInSlot(message.slotIndex, stack);
                        // Sync to all tracking players
                        SyncHelper.syncToTrackingPlayers(player);
                    }
                }
            });
            return null;
        }
    }
}

