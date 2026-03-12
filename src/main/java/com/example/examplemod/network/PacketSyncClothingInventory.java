package com.example.examplemod.network;

import com.example.examplemod.capability.ClothingCapabilityProvider;
import com.example.examplemod.capability.IClothingInventory;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * Server -> Client packet that syncs the entire clothing inventory.
 * Contains the player entity ID and NBT data for all 6 slots.
 * Supports syncing other players' clothing for rendering.
 */
public class PacketSyncClothingInventory implements IMessage {

    private int entityId;
    private NBTTagCompound data;

    public PacketSyncClothingInventory() {
    }

    public PacketSyncClothingInventory(EntityPlayer player, IClothingInventory inventory) {
        this.entityId = player.getEntityId();
        this.data = inventory.serializeNBT();
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        entityId = buf.readInt();
        data = ByteBufUtils.readTag(buf);
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(entityId);
        ByteBufUtils.writeTag(buf, data);
    }

    public static class Handler implements IMessageHandler<PacketSyncClothingInventory, IMessage> {
        @Override
        @SideOnly(Side.CLIENT)
        public IMessage onMessage(PacketSyncClothingInventory message, MessageContext ctx) {
            Minecraft.getMinecraft().addScheduledTask(() -> {
                Entity entity = Minecraft.getMinecraft().world.getEntityByID(message.entityId);
                if (entity instanceof EntityPlayer) {
                    EntityPlayer player = (EntityPlayer) entity;
                    IClothingInventory inv = player.getCapability(ClothingCapabilityProvider.CLOTHING_CAP, null);
                    if (inv != null && message.data != null) {
                        inv.deserializeNBT(message.data);
                    }
                }
            });
            return null;
        }
    }
}

