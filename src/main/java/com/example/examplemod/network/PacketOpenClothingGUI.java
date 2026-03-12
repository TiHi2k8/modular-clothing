package com.example.examplemod.network;

import com.example.examplemod.ExampleMod;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/**
 * Packet sent from client to server when the player presses the clothing keybind.
 * Server responds by opening the clothing GUI.
 */
public class PacketOpenClothingGUI implements IMessage {

    public PacketOpenClothingGUI() {
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        // No data needed
    }

    @Override
    public void toBytes(ByteBuf buf) {
        // No data needed
    }

    public static class Handler implements IMessageHandler<PacketOpenClothingGUI, IMessage> {
        @Override
        public IMessage onMessage(PacketOpenClothingGUI message, MessageContext ctx) {
            EntityPlayerMP player = ctx.getServerHandler().player;
            // Schedule on main thread to avoid concurrency issues
            player.getServerWorld().addScheduledTask(() -> {
                player.openGui(ExampleMod.instance, ExampleMod.GUI_CLOTHING_ID, player.world,
                        (int) player.posX, (int) player.posY, (int) player.posZ);
            });
            return null;
        }
    }
}

