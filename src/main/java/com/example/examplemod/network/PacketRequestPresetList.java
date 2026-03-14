package com.example.examplemod.network;

import com.example.examplemod.preset.TransformPresetManager;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

import java.util.List;

/**
 * Client → Server. Asks the server to send back the full preset list.
 */
public class PacketRequestPresetList implements IMessage {

    public PacketRequestPresetList() {}

    @Override public void fromBytes(ByteBuf buf) {}
    @Override public void toBytes(ByteBuf buf) {}

    public static class Handler implements IMessageHandler<PacketRequestPresetList, IMessage> {
        @Override
        public IMessage onMessage(PacketRequestPresetList msg, MessageContext ctx) {
            EntityPlayerMP player = ctx.getServerHandler().player;
            FMLCommonHandler.instance().getMinecraftServerInstance().addScheduledTask(() -> {
                List<TransformPresetManager.Preset> presets =
                        TransformPresetManager.load(player.getServer());
                ClothingNetworkHandler.INSTANCE.sendTo(new PacketPresetList(presets), player);
            });
            return null;
        }
    }
}
