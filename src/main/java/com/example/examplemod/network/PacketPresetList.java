package com.example.examplemod.network;

import com.example.examplemod.preset.TransformPresetManager;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.ArrayList;
import java.util.List;

/**
 * Server → Client. Carries the full list of server-stored transform presets.
 * When received, if GuiClothingPresets is open it refreshes its list.
 */
public class PacketPresetList implements IMessage {

    public List<TransformPresetManager.Preset> presets = new ArrayList<>();

    public PacketPresetList() {}

    public PacketPresetList(List<TransformPresetManager.Preset> presets) {
        this.presets = presets;
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(presets.size());
        for (TransformPresetManager.Preset p : presets) {
            ByteBufUtils.writeUTF8String(buf, p.name);
            buf.writeFloat(p.scaleX);  buf.writeFloat(p.scaleY);  buf.writeFloat(p.scaleZ);
            buf.writeFloat(p.offsetX); buf.writeFloat(p.offsetY); buf.writeFloat(p.offsetZ);
        }
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        int size = buf.readInt();
        for (int i = 0; i < size; i++) {
            String name = ByteBufUtils.readUTF8String(buf);
            float sx = buf.readFloat(), sy = buf.readFloat(), sz = buf.readFloat();
            float ox = buf.readFloat(), oy = buf.readFloat(), oz = buf.readFloat();
            presets.add(new TransformPresetManager.Preset(name, sx, sy, sz, ox, oy, oz));
        }
    }

    @SideOnly(Side.CLIENT)
    public static class Handler implements IMessageHandler<PacketPresetList, IMessage> {
        @Override
        public IMessage onMessage(PacketPresetList msg, MessageContext ctx) {
            Minecraft mc = Minecraft.getMinecraft();
            mc.addScheduledTask(() -> {
                // Deliver to GuiClothingPresets via its static callback
                com.example.examplemod.gui.GuiClothingPresets.deliverPresets(msg.presets);
            });
            return null;
        }
    }
}
