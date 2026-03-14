package com.example.examplemod.network;

import com.example.examplemod.preset.TransformPresetManager;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

import java.util.List;

/**
 * Client → Server. Deletes a named preset.
 * Server-side OP check: silently ignored if the player is not OP (permission level ≥ 2).
 */
public class PacketDeletePreset implements IMessage {

    private String name;

    public PacketDeletePreset() {}

    public PacketDeletePreset(String name) {
        this.name = name;
    }

    @Override
    public void toBytes(ByteBuf buf) {
        ByteBufUtils.writeUTF8String(buf, name);
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        name = ByteBufUtils.readUTF8String(buf);
    }

    public static class Handler implements IMessageHandler<PacketDeletePreset, IMessage> {
        @Override
        public IMessage onMessage(PacketDeletePreset msg, MessageContext ctx) {
            EntityPlayerMP player = ctx.getServerHandler().player;
            FMLCommonHandler.instance().getMinecraftServerInstance().addScheduledTask(() -> {
                // OP check: canSendCommands checks permission level >= 2 (standard OP)
                if (!player.getServer().getPlayerList().canSendCommands(player.getGameProfile())) {
                    return; // not OP — silently reject
                }
                TransformPresetManager.delete(player.getServer(), msg.name);
                // Send updated list back
                List<TransformPresetManager.Preset> presets =
                        TransformPresetManager.load(player.getServer());
                ClothingNetworkHandler.INSTANCE.sendTo(new PacketPresetList(presets), player);
            });
            return null;
        }
    }
}
