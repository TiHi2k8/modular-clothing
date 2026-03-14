package com.example.examplemod.network;

import com.example.examplemod.capability.ClothingProvider;
import com.example.examplemod.capability.IClothingInventory;
import com.example.examplemod.gui.ClothingContainer;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/**
 * Client -> Server: toggle rendering mode for specific parts.
 * partType: 0=Chest/Arms, 1=Pants/Legs, 2=Shoes/Feet
 */
public class PacketSetPartMode implements IMessage {
    private int layer;
    private int partType;
    private boolean enabled;

    public PacketSetPartMode() {}

    public PacketSetPartMode(int layer, int partType, boolean enabled) {
        this.layer = layer;
        this.partType = partType;
        this.enabled = enabled;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        layer = buf.readInt();
        partType = buf.readInt();
        enabled = buf.readBoolean();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(layer);
        buf.writeInt(partType);
        buf.writeBoolean(enabled);
    }

    public static class Handler implements IMessageHandler<PacketSetPartMode, IMessage> {
        @Override
        public IMessage onMessage(PacketSetPartMode message, MessageContext ctx) {
            EntityPlayerMP player = ctx.getServerHandler().player;
            player.getServerWorld().addScheduledTask(() -> {
                IClothingInventory inventory = player.getCapability(ClothingProvider.CLOTHING_CAPABILITY, null);
                if (inventory == null) return;
                if (message.layer < 0 || message.layer >= inventory.getLayerCount()) return;

                switch (message.partType) {
                    case 0:
                        inventory.setChestArmsMode(message.layer, message.enabled);
                        break;
                    case 1:
                        inventory.setPantsLegsMode(message.layer, message.enabled);
                        break;
                    case 2:
                        inventory.setShoesFeetMode(message.layer, message.enabled);
                        break;
                }

                ClothingNetworkHandler.sendToAllTracking(new PacketSyncClothingInventory(player), player);
                ClothingNetworkHandler.sendTo(new PacketSyncClothingInventory(player), player);

                // Update container if open
                if (player.openContainer instanceof ClothingContainer) {
                    ((ClothingContainer) player.openContainer).updateSlotPositions();
                }
            });
            return null;
        }
    }
}
