package com.example.examplemod.network;

import com.example.examplemod.ExampleMod;
import com.example.examplemod.gui.ClothingGuiHandler;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class PacketOpenClothingGUI implements IMessage {
    public PacketOpenClothingGUI() {}

    @Override
    public void fromBytes(ByteBuf buf) {}

    @Override
    public void toBytes(ByteBuf buf) {}

    public static class Handler implements IMessageHandler<PacketOpenClothingGUI, IMessage> {
        @Override
        public IMessage onMessage(PacketOpenClothingGUI message, MessageContext ctx) {
            EntityPlayerMP player = ctx.getServerHandler().player;
            player.getServerWorld().addScheduledTask(() -> {
                player.openGui(ExampleMod.instance, ClothingGuiHandler.GUI_ID, player.world, (int) player.posX, (int) player.posY, (int) player.posZ);
            });
            return null;
        }
    }
}
