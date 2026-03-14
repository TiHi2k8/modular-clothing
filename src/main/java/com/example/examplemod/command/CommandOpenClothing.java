package com.example.examplemod.command;

import com.example.examplemod.ExampleMod;
import com.example.examplemod.gui.ClothingGuiHandler;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;

public class CommandOpenClothing extends CommandBase {
    @Override
    public String getName() {
        return "clothing";
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "/clothing";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 0;
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        if (!(sender instanceof EntityPlayerMP)) {
            throw new CommandException("commands.generic.playerEntityOnly");
        }

        EntityPlayerMP player = (EntityPlayerMP) sender;
        player.openGui(ExampleMod.instance, ClothingGuiHandler.GUI_ID, player.world, (int) player.posX, (int) player.posY, (int) player.posZ);
    }
}
