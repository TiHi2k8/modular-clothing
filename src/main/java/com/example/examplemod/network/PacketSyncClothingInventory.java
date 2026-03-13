package com.example.examplemod.network;

import com.example.examplemod.capability.ClothingProvider;
import com.example.examplemod.capability.IClothingInventory;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class PacketSyncClothingInventory implements IMessage {
    private NBTTagCompound data;
    private int entityId;

    public PacketSyncClothingInventory() {}

    public PacketSyncClothingInventory(EntityPlayer player) {
        this.entityId = player.getEntityId();
        IClothingInventory inventory = player.getCapability(ClothingProvider.CLOTHING_CAPABILITY, null);
        this.data = inventory.serializeNBT();
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        this.entityId = buf.readInt();
        this.data = ByteBufUtils.readTag(buf);
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(this.entityId);
        ByteBufUtils.writeTag(buf, this.data);
    }

    public static class Handler implements IMessageHandler<PacketSyncClothingInventory, IMessage> {
        @Override
        public IMessage onMessage(PacketSyncClothingInventory message, MessageContext ctx) {
            Minecraft.getMinecraft().addScheduledTask(() -> {
                if (Minecraft.getMinecraft().world == null) return;
                EntityPlayer player = (EntityPlayer) Minecraft.getMinecraft().world.getEntityByID(message.entityId);
                if (player != null) {
                    IClothingInventory inventory = player.getCapability(ClothingProvider.CLOTHING_CAPABILITY, null);
                    if (inventory != null) {
                        inventory.deserializeNBT(message.data);
                    }
                }
            });
            return null;
        }
    }
}

