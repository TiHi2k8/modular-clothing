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
 * Client → Server. Saves (or overwrites) a named transform preset on the server.
 * Any player can save presets; only OPs can delete them.
 */
public class PacketSavePreset implements IMessage {

    private String  name;
    private boolean perAxisMode;
    private float   sx, sy, sz, ox, oy, oz;

    public PacketSavePreset() {}

    public PacketSavePreset(String name, boolean perAxisMode,
                             float sx, float sy, float sz,
                             float ox, float oy, float oz) {
        this.name        = name;
        this.perAxisMode = perAxisMode;
        this.sx = sx; this.sy = sy; this.sz = sz;
        this.ox = ox; this.oy = oy; this.oz = oz;
    }

    @Override
    public void toBytes(ByteBuf buf) {
        ByteBufUtils.writeUTF8String(buf, name);
        buf.writeBoolean(perAxisMode);
        buf.writeFloat(sx); buf.writeFloat(sy); buf.writeFloat(sz);
        buf.writeFloat(ox); buf.writeFloat(oy); buf.writeFloat(oz);
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        name        = ByteBufUtils.readUTF8String(buf);
        perAxisMode = buf.readBoolean();
        sx = buf.readFloat(); sy = buf.readFloat(); sz = buf.readFloat();
        ox = buf.readFloat(); oy = buf.readFloat(); oz = buf.readFloat();
    }

    public static class Handler implements IMessageHandler<PacketSavePreset, IMessage> {
        @Override
        public IMessage onMessage(PacketSavePreset msg, MessageContext ctx) {
            EntityPlayerMP player = ctx.getServerHandler().player;
            FMLCommonHandler.instance().getMinecraftServerInstance().addScheduledTask(() -> {
                // Sanitize name length
                String name = msg.name.length() > 40 ? msg.name.substring(0, 40) : msg.name;
                TransformPresetManager.addOrUpdate(player.getServer(), name, msg.perAxisMode,
                        msg.sx, msg.sy, msg.sz, msg.ox, msg.oy, msg.oz);
                // Send updated list back to the requesting player
                List<TransformPresetManager.Preset> presets =
                        TransformPresetManager.load(player.getServer());
                ClothingNetworkHandler.INSTANCE.sendTo(new PacketPresetList(presets), player);
            });
            return null;
        }
    }
}
